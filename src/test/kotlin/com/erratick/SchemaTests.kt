package com.erratick

import com.google.gson.JsonParser
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Basic Reading of Measurements Schema Data
 */
class SchemaTests {

    @Test
    fun `list measurements`() {
        withTestApplication(Application::module) {
            handleRequest(HttpMethod.Get, "/measurements").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                var json = JsonParser.parseString(response.content.toString()).asJsonArray
                assertTrue("Expected at least one measurement in the list") {
                    json.size() > 0
                }
            }
        }
    }

    @Test
    fun `get measurement`() {
        withTestApplication(Application::module) {
            handleRequest(HttpMethod.Get, "/measurements/${MeasurementId.clicks_impressions}").apply {

                assertEquals(HttpStatusCode.OK, response.status())

                var json = JsonParser.parseString(response.content.toString()).asJsonObject
                assertEquals(
                    MeasurementId.clicks_impressions.toString(),
                    json.get("id").asString,
                    "Response content returned not as expected"
                )

            }
        }
    }

}