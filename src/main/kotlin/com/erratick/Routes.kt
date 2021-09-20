package com.erratick

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.locations.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

fun Routing.setupRoutes(){

    val loaderFactory by inject<LoaderFactory>()

    get<Query> { query ->
        val measurement = MeasurementRepositoryImpl().get(query.measurement!!) ?:
            throw Exception("Could not find measurement ${query.measurement}")

        val dataPointRepository = DataPointRepositoryImpl()
        call.respond(
            HttpStatusCode.OK,
            dataPointRepository.query(measurement, query)
        )
    }

    route ("/measurements"){
        get {
            val measurementRepository = MeasurementRepositoryImpl()
            call.respond(HttpStatusCode.OK, measurementRepository.list())
        }
    }

    route ("/measurements/{measurementId}"){
        get {
            val measurementId = MeasurementId.valueOf(
                call.parameters["measurementId"] ?: throw Exception("Measurement id expected")
            )
            val measurement = MeasurementRepositoryImpl().get(measurementId) ?:
                throw Exception("Measurement $measurementId not found")
            call.respond(HttpStatusCode.OK, measurement)
        }
    }

    /**
     * Handles requests to load measurements into the database.
     * Assumes measurements are in CSV format, but content negotiation would be a nice upgrade.
     */
    route("/load/{measurementId}"){
        post {
            val measurementId = MeasurementId.valueOf(
                call.parameters["measurementId"] ?: throw Exception("Measurement id expected")
            )
            loaderFactory.loader(measurementId).load(call.receive<String>())
            call.respond(HttpStatusCode.Created, "Measurements created")
        }
    }
}

fun Routing.setupErrorHandling(){
    install(StatusPages) {
        val logger = LoggerFactory.getLogger("StatusPages")

        exception<InvalidFormatException> { cause ->
            val fieldName = cause.path.first().fieldName
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf(
                    "code" to "fieldName.invalid",
                    "message" to "${cause.value} is not valid for ${fieldName}",
                    "field" to fieldName
                )
            )
        }
        exception<MissingKotlinParameterException> { cause ->
            val fieldName = cause.parameter.name
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf(
                    "code" to "fieldName.required",
                    "message" to "${fieldName} is required",
                    "field" to fieldName
                )
            )
        }
        exception<JsonParseException> {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf(
                    "code" to "format.invalid",
                    "message" to "Invalid content. Review your json structure to ensure you have no trailing commas, and that all quotes and brackets are terminated as expected.",
                )
            )
        }
        exception<Exception> { cause ->
            logger.error(cause.message, cause)
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
}