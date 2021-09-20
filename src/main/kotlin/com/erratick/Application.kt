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
import io.ktor.response.*
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
        get("/") {

//            val config = HoconApplicationConfig(ConfigFactory.load()).config("influxdb")
//            val bucket = config.property("bucket").getString()
//            val influx = InfluxDBClientKotlinFactory
//                .create(
//                    config.property("url").getString(),
//                    config.property("token").getString().toCharArray(),
//                    config.property("org").getString(),
//                    bucket)
//
//            val writeApi = influx.getWriteKotlinApi()
//            val measurement = MeasurementRepositoryImpl().get("clicks_impressions") ?: throw Exception("Could not find measurement")
//
//            val point = Point.measurement(measurement.id.toString())
//                .addTag("campaign", "summer")
//                .addTag("datasource", "google")
//                .addField("clicks", 55)
//                .addField("impressions", 88)
//                .time(Instant.now().toEpochMilli(), WritePrecision.MS)
//
//            writeApi.writePoint(point)
//
//
//            //writeApi.writeRecord("ctr_measurement,campaign=spring clicks2=2,impressions2=10", WritePrecision.NS)
//
//            val datasource = "google"
//            val totalClicksForAGivenDatasourceAndRange =
//                """
//                    from(bucket: "$bucket") |>
//                    range(start: 0) |>
//                    filter(fn: (r) =>
//                        r._measurement == "ctr_measurement" and
//                        r._field == "clicks" and
//                        r.datasource == "$datasource"
//                    ) |>
//                    sum()
//                """.trimIndent()
//
//            val impressionsOverTimeDaily = """
//                from(bucket: "$bucket") |>
//                range(start: 0) |>
//                filter(fn: (r) =>
//                    r._measurement == "ctr_measurement" and
//                    r._field == "impressions"
//                ) |>
//                aggregateWindow(every: 1d, fn: sum)
//            """.trimIndent()
//
//            val clickthroughRatePerDatasourceAndCampaign = """
//                from(bucket: "$bucket") |>
//                range(start: 0) |>
//                filter(fn: (r) =>
//                    r._measurement == "ctr_measurement" and
//                    ( r._field == "clicks" or r._field == "impressions" )
//                ) |>
//                pivot(
//                    rowKey:["_time"],
//                    columnKey: ["_field"],
//                    valueColumn: "_value"
//                ) |>
//                map(fn: (r) =>
//                    ({ r with _value: 10000 * r.clicks / r.impressions })
//                )
//                |> group(columns: ["datasource", "campaign"])
//                |> mean()
//            """.trimIndent()
//
//            /*|> group(columns:["datasource", "campaign"]) |>
//                mean()*/
//
//            runBlocking {
//                influx
//                    .getQueryKotlinApi()
//                    .query(totalClicksForAGivenDatasourceAndRange)
//                    .consumeEach { r ->
//                        val result = QueryResult(
//                            value = r.value,
//                            start = r.start,
//                            end = r.stop,
//                            dimensions = r.values.filter { it.key in  measurement.dimensions }.map {
//                                Dimension(name = it.key, value = it.value.toString())
//                            }
//                        )
//                        println("result: ${result}")
//                    }
//            }
//            println("")
//            runBlocking {
//                influx
//                    .getQueryKotlinApi()
//                    .query(impressionsOverTimeDaily)
//                    .consumeEach { r ->
//                        val result = QueryResult(
//                            value = r.value,
//                            start = r.start,
//                            end = r.stop,
//                            dimensions = r.values.filter { it.key in  measurement.dimensions }.map {
//                                Dimension(name = it.key, value = it.value.toString())
//                            }
//                        )
//                        println("result: ${result}")
//                    }
//            }
//            println("")
//            runBlocking {
//                influx
//                    .getQueryKotlinApi()
//                    .query(clickthroughRatePerDatasourceAndCampaign)
//                    .consumeEach { r ->
//                        val result = QueryResult(
//                            value = r.value,
//                            start = r.start,
//                            end = r.stop,
//                            dimensions = r.values.filter { it.key in  measurement.dimensions }.map {
//                                Dimension(name = it.key, value = it.value.toString())
//                            }
//                        )
//                        println("result: ${result}")
//                    }
//            }
//
//            influx.close()

            call.respondText("Hello, world!")
        }
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


