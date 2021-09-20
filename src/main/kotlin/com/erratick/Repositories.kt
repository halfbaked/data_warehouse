package com.erratick

import com.influxdb.client.kotlin.InfluxDBClientKotlin
import com.influxdb.client.kotlin.InfluxDBClientKotlinFactory
import com.influxdb.client.write.Point
import com.typesafe.config.ConfigFactory
import io.ktor.config.*
import kotlinx.coroutines.channels.consume
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.toList
import kotlinx.coroutines.runBlocking


interface MeasurementRepository {
    fun list(): List<Measurement>
    fun get(id: MeasurementId): Measurement?
}

class MeasurementRepositoryImpl: MeasurementRepository {
    /**
     * There is no need to store the measurement objects in a database.
     * Defining them in the code is sufficient.
     */
    override fun list(): List<Measurement> {
        return listOf(
            Measurement(
                id = MeasurementId.clicks_impressions,
                metrics = listOf(MetricId.clicks, MetricId.impressions, MetricId.ctr),
                dimensions = listOf("datasource", "campaign")
            ),
        )
    }

    override fun get(id: MeasurementId): Measurement? {
        return list().find { it.id == id }
    }
}

/**
 * The DataPoint Repository is the point that communicates with the database
 */
interface DataPointRepository {
    /**
     * Adds one datapoint
     */
    fun add(datapoint: Point)
    /**
     * Adds many datapoints at once
     */
    fun addAll(datapoints: List<Point>)
    /**
     * Query the data points
     */
    fun query(measurement: Measurement, query: Query): List<QueryResultList>
}

class DataPointRepositoryImpl: DataPointRepository {
    /**
     * The attribute is of type Point which is a class provided by the influxdb client.
     * Ideally would be better to use our own DataPoint class and then translate to the influxdb class within the method.
     * This would reduce the coupling to influxdb.
     */
    override fun add(datapoint: Point) {
        val influx = clientInstance()
        val writeApi = influx.getWriteKotlinApi()
        runBlocking {  writeApi.writePoint(datapoint) }
        influx.close()
    }

    override fun addAll(datapoints: List<Point>) {
        val influx = clientInstance()
        val writeApi = influx.getWriteKotlinApi()
        runBlocking {  writeApi.writePoints(datapoints) }
        influx.close()
    }

    override fun query(measurement: Measurement, query: Query): List<QueryResultList> {
        val config = HoconApplicationConfig(ConfigFactory.load()).config("influxdb")
        val bucket = config.property("bucket").getString()
        val fluxQuery = FluxQueryBuilderFactoryImpl().builder(bucket, query).build()
        println("fluxQuery: $fluxQuery")
        val influx = clientInstance()

        // The results from influxdb are a flat list. We use a map here to index the results by table id
        // before presenting as a list of lists of results. Transforming from the flat list to the heirarchical
        // structure makes it easier to present a series of line graphs from one request.
        val tables = mutableMapOf<Int, QueryResultList>()
        runBlocking {
            influx
                .getQueryKotlinApi()
                .query(fluxQuery)
                .consumeEach{
                    if(tables[it.table] == null) tables[it.table] = QueryResultList()
                    tables[it.table]!!.add(it.toQueryResult(measurement))
                }
        }
        return tables.values.toList()
    }

    fun clientInstance(): InfluxDBClientKotlin {
        val config = HoconApplicationConfig(ConfigFactory.load()).config("influxdb")
        val bucket = config.property("bucket").getString()
        return InfluxDBClientKotlinFactory
            .create(
                config.property("url").getString(),
                config.property("token").getString().toCharArray(),
                config.property("org").getString(),
                bucket)
    }

}