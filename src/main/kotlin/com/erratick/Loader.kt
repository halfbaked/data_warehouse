package com.erratick

import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.MappingIterator
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*


/**
 * Loads data of a particular measurement / format
 */
interface Loader {
    fun load(data: String)
}

/**
 * Extracts data from the click_impression measurement
 */
class ClickImpressionLoader: Loader {
    override fun load(data: String) {

        // Use Jackson to map each row of the csv to a dto
        val mapper = CsvMapper().apply {
            registerModule(KotlinModule())
            registerModule(JavaTimeModule())
            dateFormat = SimpleDateFormat("MM/dd/yy")

            // Did those clicks happen on my Tuesday or your Tuesday?
            // We are assuming the dates in the data are UTC and are hardcoding as such here, but ideally
            // this should be configurable. The ideal of course would be for the input data to have that detail
            // built into the date format.
            setTimeZone(TimeZone.getTimeZone("UTC"))
        }
        val schema = mapper.schemaFor(ClickImpressionMeasurement::class.java)
        val iterator: MappingIterator<ClickImpressionMeasurement> = mapper.readerFor(ClickImpressionMeasurement::class.java).with(schema)
            .readValues(data)

        DataPointRepositoryImpl().addAll(
            iterator.readAll().map{
                Point.measurement(MeasurementId.clicks_impressions.toString())
                    .addTag("campaign", it.campaign)
                    .addTag("datasource", it.datasource)
                    .addField("clicks", it.clicks)
                    .addField("impressions", it.impressions)
                    .time(it.time.toInstant(), WritePrecision.MS)
            }.toList()
        )
    }
}

@JsonPropertyOrder("datasource", "campaign", "time", "clicks", "impressions")
data class ClickImpressionMeasurement(
    val datasource: String,
    val campaign: String,
    val time: Date,
    val clicks: Int,
    val impressions: Int,
)

/**
 * Produces loaders
 */
interface LoaderFactory {
    /**
     * Returns the appropriate MeasurementLoader for the given measurementId
     */
    fun loader(measurementId: MeasurementId): Loader
}

class LoaderFactoryImpl: LoaderFactory {
    override fun loader(measurementId: MeasurementId): Loader {
        return when(measurementId){
            MeasurementId.clicks_impressions ->
                ClickImpressionLoader()
            else ->
                throw Exception("Factory unable to create loader. Unhandled measurementId $measurementId")
        }
    }
}

