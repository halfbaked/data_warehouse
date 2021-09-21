package com.erratick

import com.influxdb.query.FluxRecord
import java.math.BigDecimal
import java.math.RoundingMode

fun FluxRecord.toQueryResult(measurement: Measurement) = QueryResult(
    value = setScaleIfBigDecimal(value),
    time = time,
    dimensions = values.filter { it.key in  measurement.dimensions }.map {
        it.key to it.value.toString()
    }.toMap()
)

fun setScaleIfBigDecimal(value: Any?): Any? {
    return if(value != null && value is Double)
                BigDecimal(value).setScale(5, RoundingMode.DOWN)
            else value
}