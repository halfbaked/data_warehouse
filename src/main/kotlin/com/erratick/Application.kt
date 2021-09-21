package com.erratick

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.typesafe.config.ConfigFactory
import io.ktor.application.*
import io.ktor.config.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.locations.*
import io.ktor.routing.*
import io.ktor.util.*
import org.koin.dsl.module
import org.koin.ktor.ext.Koin
import java.text.SimpleDateFormat

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module(testing: Boolean = false) {


    install(DataConversion) {

        // Using this data conversion definition to handle when we require parameters to be key:value pairs
        // such as with filterBy
        // There should of course be property validation
        // Expects the query to be in the form key1:value1,key2:value2
        convert<QueryKeyValueList> {
            decode { values, _ ->
                QueryKeyValueList().apply {
                    values.forEach { value ->
                        value.split(",").map{ it.split(":") }.forEach { kv ->
                            put(kv[0], kv[1])
                        }
                    }
                }
            }

            encode { value ->
                when (value) {
                    null -> listOf()
                    is Map<*,*> -> listOf(value.map { "${it.key}:${it.value}" }.joinToString(separator = ","))
                    else -> throw DataConversionException("Cannot convert $value as Date")
                }
            }
        }
        convert<QueryStringList> {
            decode { values, _ ->
                QueryStringList().apply {
                    values.forEach { value -> addAll(value.split(","))}
                }
            }
        }
        convert<Aggregate> {
            decode { values, _ ->
                println("values ${values}")
                Aggregate.valueOf(values.first())
            }
        }
    }

    install(Koin) {
        modules(koinModule)
    }
    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
            register(ContentType.Application.Json, JacksonConverter(JsonMapper.defaultMapper))
        }
    }
    install(Locations)
    routing {
        setupRoutes()
        setupErrorHandling()
    }

}

val koinModule = module {

    val config = HoconApplicationConfig(ConfigFactory.load()).config("influxdb")
    val bucket = config.property("bucket").getString()

    single<FluxQueryBuilderFactory> { FluxQueryBuilderFactoryImpl() }
    single<LoaderFactory> { LoaderFactoryImpl() }
}

object JsonMapper {
    // automatically installs the Kotlin module
    val defaultMapper: ObjectMapper = jacksonObjectMapper()

    init {
        defaultMapper.configure(SerializationFeature.INDENT_OUTPUT, true)
        defaultMapper.registerModule(JavaTimeModule())
        defaultMapper.setDateFormat(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"))
//        defaultMapper.configure(SerializationFeature.WRAP_ROOT_VALUE, true)
    }
}


