package com.erratick

import com.influxdb.query.FluxRecord

fun FluxRecord.toQueryResult(measurement: Measurement) = QueryResult(
    value = value,
    time = time,
    dimensions = values.filter { it.key in  measurement.dimensions }.map {
        it.key to it.value.toString()
    }.toMap()
)