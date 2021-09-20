package com.erratick

import com.google.gson.JsonParser
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QueryTests {

    @Test
    fun `basic query ctr`() {

        withTestApplication(Application::module) {

            // Add sample data
            val dataPointRepository = DataPointRepositoryImpl()
            dataPointRepository.add(
                Point.measurement(MeasurementId.clicks_impressions.toString())
                    .addTag("campaign", "summer")
                    .addTag("datasource", "sales_report_1")
                    .addField("clicks", 55)
                    .addField("impressions", 1002)
                    .time(Instant.now().toEpochMilli(), WritePrecision.MS)
            )

            handleRequest(
                HttpMethod.Get,
                "/query/${MeasurementId.clicks_impressions}?metric=${MetricId.ctr}").apply {

                assertEquals(HttpStatusCode.OK, response.status())

                var json = JsonParser.parseString(response.content.toString()).asJsonArray
                assertTrue("Expected the query to have yielded results but the result was empty") {
                    json.size() > 0
                }
            }
        }
    }

    @Test
    fun `Total Clicks for a given Datasource for a given Date range`() {

        withTestApplication(Application::module) {

            // 2 datasources one of which we will be filtering by
            val datasource = getRandomString(50)
            val datasource2 = getRandomString(50)

            // Define the range
            val endDate = Instant.now()
            val startDate = endDate.minus(30, ChronoUnit.DAYS)

            // Add sample data for desired datasource in desired range
            val dataPointRepository = DataPointRepositoryImpl()
            dataPointRepository.addAll(
                listOf(
                    Point.measurement(MeasurementId.clicks_impressions.toString())
                        .addTag("campaign", "summer")
                        .addTag("datasource", datasource)
                        .addField("clicks", 10)
                        .addField("impressions", 1002)
                        .time(startDate.plus(2, ChronoUnit.DAYS), WritePrecision.MS),
                    Point.measurement(MeasurementId.clicks_impressions.toString())
                        .addTag("campaign", "summer")
                        .addTag("datasource", datasource)
                        .addField("clicks", 20)
                        .addField("impressions", 1002)
                        .time(startDate.plus(6, ChronoUnit.DAYS), WritePrecision.MS),
                    Point.measurement(MeasurementId.clicks_impressions.toString())
                        .addTag("campaign", "summer")
                        .addTag("datasource", datasource)
                        .addField("clicks", 35)
                        .addField("impressions", 1002)
                        .time(startDate.plus(12, ChronoUnit.DAYS), WritePrecision.MS),
                )
            )

            // Add sample data in the range but the other datasource
            dataPointRepository.addAll(
                listOf(
                    Point.measurement(MeasurementId.clicks_impressions.toString())
                        .addTag("campaign", "summer")
                        .addTag("datasource", datasource2)
                        .addField("clicks", 10)
                        .addField("impressions", 1002)
                        .time(startDate.plus(2, ChronoUnit.DAYS), WritePrecision.MS),
                    Point.measurement(MeasurementId.clicks_impressions.toString())
                        .addTag("campaign", "summer")
                        .addTag("datasource", datasource2)
                        .addField("clicks", 20)
                        .addField("impressions", 1002)
                        .time(startDate.plus(6, ChronoUnit.DAYS), WritePrecision.MS),
                    Point.measurement(MeasurementId.clicks_impressions.toString())
                        .addTag("campaign", "summer")
                        .addTag("datasource", datasource2)
                        .addField("clicks", 35)
                        .addField("impressions", 1002)
                        .time(startDate.plus(12, ChronoUnit.DAYS), WritePrecision.MS),
                )
            )

            // Add sample data with the desired datasource but outside the range
            dataPointRepository.addAll(
                listOf(
                    Point.measurement(MeasurementId.clicks_impressions.toString())
                        .addTag("campaign", "summer")
                        .addTag("datasource", datasource)
                        .addField("clicks", 10)
                        .addField("impressions", 1002)
                        .time(startDate.minus(2, ChronoUnit.DAYS), WritePrecision.MS),
                    Point.measurement(MeasurementId.clicks_impressions.toString())
                        .addTag("campaign", "summer")
                        .addTag("datasource", datasource)
                        .addField("clicks", 20)
                        .addField("impressions", 1002)
                        .time(startDate.minus(6, ChronoUnit.DAYS), WritePrecision.MS),
                    Point.measurement(MeasurementId.clicks_impressions.toString())
                        .addTag("campaign", "summer")
                        .addTag("datasource", datasource)
                        .addField("clicks", 35)
                        .addField("impressions", 1002)
                        .time(startDate.minus(12, ChronoUnit.DAYS), WritePrecision.MS),
                )
            )

            val queryString = "metric=${MetricId.clicks}" +
                "&start=${startDate.epochSecond}&end=${endDate.epochSecond}" +
                "&groupBy=datasource&filterBy=datasource:$datasource&aggregate=sum"

            handleRequest(
                HttpMethod.Get,
                "/query/${MeasurementId.clicks_impressions}?$queryString").apply {

                assertEquals(HttpStatusCode.OK, response.status())

                println(response.content.toString())

                var queryResultList = JsonParser.parseString(response.content.toString()).asJsonArray
                assertEquals(1, queryResultList.size(), "Expect only one list of results for this query")

                var queryResults = queryResultList.get(0).asJsonArray
                assertTrue("Expected the query to have yielded results but the result was empty") {
                    queryResults.size() > 0
                }

                var queryResult = queryResults.get(0).asJsonObject
                var totalClicks = queryResult.get("value").asInt
                assertEquals(65, totalClicks)
            }
        }
    }

    @Test
    fun `Click-Through Rate (CTR) per Datasource and Campaign`() {

        withTestApplication(Application::module) {

            val datasource1 = getRandomString(50)
            val campaign1 = getRandomString(50)
            val datasource2 = getRandomString(50)
            val campaign2 = getRandomString(50)
            val time = Instant.now()

            val dataPointRepository = DataPointRepositoryImpl()

            // Create points for the datasource1,campaign1 combination
            dataPointRepository.addAll(
                listOf(
                    Point.measurement(MeasurementId.clicks_impressions.toString())
                        .addTag("campaign", campaign1)
                        .addTag("datasource", datasource1)
                        .addField("clicks", 10)
                        .addField("impressions", 1002)
                        .time(time, WritePrecision.MS),
                    Point.measurement(MeasurementId.clicks_impressions.toString())
                        .addTag("campaign", campaign1)
                        .addTag("datasource", datasource1)
                        .addField("clicks", 20)
                        .addField("impressions", 1002)
                        .time(time, WritePrecision.MS),
                    Point.measurement(MeasurementId.clicks_impressions.toString())
                        .addTag("campaign", campaign1)
                        .addTag("datasource", datasource1)
                        .addField("clicks", 35)
                        .addField("impressions", 1002)
                        .time(time, WritePrecision.MS),
                )
            )

            // Create points for the datasource2,campaign2 combination
            dataPointRepository.addAll(
                listOf(
                    Point.measurement(MeasurementId.clicks_impressions.toString())
                        .addTag("campaign", campaign2)
                        .addTag("datasource", datasource2)
                        .addField("clicks", 10)
                        .addField("impressions", 1002)
                        .time(time, WritePrecision.MS),
                    Point.measurement(MeasurementId.clicks_impressions.toString())
                        .addTag("campaign", campaign2)
                        .addTag("datasource", datasource2)
                        .addField("clicks", 20)
                        .addField("impressions", 1002)
                        .time(time, WritePrecision.MS),
                    Point.measurement(MeasurementId.clicks_impressions.toString())
                        .addTag("campaign", campaign2)
                        .addTag("datasource", datasource2)
                        .addField("clicks", 35)
                        .addField("impressions", 1002)
                        .time(time, WritePrecision.MS),
                )
            )

            // Create points for the datasource1,campaign2 combination
            dataPointRepository.addAll(
                listOf(
                    Point.measurement(MeasurementId.clicks_impressions.toString())
                        .addTag("campaign", campaign2)
                        .addTag("datasource", datasource1)
                        .addField("clicks", 10)
                        .addField("impressions", 1002)
                        .time(time, WritePrecision.MS),
                    Point.measurement(MeasurementId.clicks_impressions.toString())
                        .addTag("campaign", campaign2)
                        .addTag("datasource", datasource1)
                        .addField("clicks", 20)
                        .addField("impressions", 1002)
                        .time(time, WritePrecision.MS),
                    Point.measurement(MeasurementId.clicks_impressions.toString())
                        .addTag("campaign", campaign2)
                        .addTag("datasource", datasource1)
                        .addField("clicks", 35)
                        .addField("impressions", 1002)
                        .time(time, WritePrecision.MS),
                )
            )

            val queryString = "metric=${MetricId.clicks}&groupBy=datasource,campaign"

            handleRequest(
                HttpMethod.Get,
                "/query/${MeasurementId.clicks_impressions}?$queryString").apply {

                assertEquals(HttpStatusCode.OK, response.status())

                println(response.content.toString())

                var queryResultList = JsonParser.parseString(response.content.toString()).asJsonArray
                assert(queryResultList.size() >= 3){
                    "Expect at least 3 lists of results, one for each combination of datasource and campaign," +
                    " but only found ${queryResultList.size()}"
                }

                // Examine each list of results, and confirm all results have the same campaign and datasource
                queryResultList.forEach {
                    val queryResults = it.asJsonArray
                    val firstResult = queryResults.first().asJsonObject
                    val expectedCampaign = firstResult.get("dimensions").asJsonObject.get("campaign").asString
                    val expectedDatasource = firstResult.get("dimensions").asJsonObject.get("datasource").asString
                    queryResults.forEach { r ->
                        r.asJsonObject.apply {
                            assertEquals(expectedCampaign, get("dimensions").asJsonObject.get("campaign").asString)
                            assertEquals(expectedDatasource, get("dimensions").asJsonObject.get("datasource").asString)
                        }
                    }
                }
            }
        }

    }

    @Test
    fun `Impressions over time (daily)`() {
        val campaign = getRandomString(50)
        val dataPointRepository = DataPointRepositoryImpl()
        dataPointRepository.addAll(
            listOf(
                Point.measurement(MeasurementId.clicks_impressions.toString())
                    .addTag("campaign", campaign)
                    .addTag("datasource", "datasource1")
                    .addField("clicks", 10)
                    .addField("impressions", 1002)
                    .time(Instant.now().minus(5, ChronoUnit.DAYS), WritePrecision.MS),
                Point.measurement(MeasurementId.clicks_impressions.toString())
                    .addTag("campaign", campaign)
                    .addTag("datasource", "datasource1")
                    .addField("clicks", 20)
                    .addField("impressions", 1002)
                    .time(Instant.now().minus(2, ChronoUnit.DAYS), WritePrecision.MS),
                Point.measurement(MeasurementId.clicks_impressions.toString())
                    .addTag("campaign", campaign)
                    .addTag("datasource", "datasource1")
                    .addField("clicks", 35)
                    .addField("impressions", 1002)
                    .time(Instant.now().minus(3, ChronoUnit.DAYS), WritePrecision.MS),
            )
        )

        val queryString = "metric=${MetricId.impressions}&filterBy=campaign:$campaign&window=1d&start=-6d"
        withTestApplication(Application::module) {
            handleRequest(
                HttpMethod.Get,
                "/query/${MeasurementId.clicks_impressions}?$queryString").apply {

                assertEquals(HttpStatusCode.OK, response.status())

                //println(response.content.toString())

                var queryResultListList = JsonParser.parseString(response.content.toString()).asJsonArray
                assert(queryResultListList.size() == 1){
                    "There should only be one result list as specifying a window will also aggregate, compressing to one result set"
                }

                var queryResultList = queryResultListList.get(0).asJsonArray
                assert(queryResultList.size() == 7) {
                    "As the range was -7, and we were applying a daily window, 7 results were expected but found ${queryResultList.size()}."
                }
            }
        }
    }

    private fun getRandomString(length: Int) : String {
        val charset = ('a'..'z')
        return (1..length)
            .map { charset.random() }
            .joinToString("")
    }

}