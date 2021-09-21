package com.erratick

interface FluxQueryBuilder {
    fun build(): String
}

/**
 * Builds a flux query. The logic is split into multiple sections to facilitate code reuse among query builders.
 *
 * Note: avoiding using the string template methods  trimMargin or trimIndent as for some reason they are removing the
 * pipe symbols needed for flux queries.
 */
open class BasicFluxQueryBuilder(val bucket: String, val query: Query): FluxQueryBuilder {

    override fun build(): String = """
        ${bucket()}
        ${range()}
        ${filter()}
        ${group()}
        ${groupByTime()}
        ${aggregate()}
    """

    fun bucket(): String = """from(bucket: "$bucket")"""

    fun range(): String = """|> range(start: ${query.start ?: "0"}, stop: ${query.end ?: "now()"})"""

    open fun filter(): String = """
        |> filter(fn: (r) =>
            r._measurement == "${query.measurement}" and r._field == "${query.metric}"
            ${filterBy()}
        )
    """

    fun filterBy(): String  =
        query.filterBy?.entries?.joinToString { """ and r["${it.key}"] == "${it.value}" """ } ?: ""

    fun group(): String =
        if(query.groupByDimensions != null && query.groupByDimensions.isNotEmpty())
            """|> group(columns: [ ${query.groupByDimensions.joinToString(",") { "\"$it\"" }} ])"""
        else
            "|> group()" // Otherwise data is grouped by tag combination by default

    fun groupByTime(): String =
        if(query.groupByTime != null)
            """|> aggregateWindow(every: ${query.groupByTime}, fn: ${query.aggregate ?: "sum"})"""
        else
            ""

    fun aggregate(): String =
        if(query.aggregate != null && query.groupByTime == null) " |> ${query.aggregate}()"
        else ""

}

class CtrFluxQueryBuilder(bucket: String, query: Query): BasicFluxQueryBuilder(bucket, query) {
//    override fun filter(): String = """
//        |> filter(fn: (r) =>
//            (r._field == "clicks" or r._field == "impressions")
//            ${filterBy()}
//        )
//        |> pivot(
//            rowKey:["_time"],
//            columnKey: ["_field"],
//            valueColumn: "_value"
//        )
//        |> map(fn: (r) =>
//            ({ r with _value: float(v:r.clicks) / float(v:r.impressions) })
//        )
//    """

    override fun build(): String {
        return if(query.aggregate != null && query.groupByTime == null)
            buildSummary()
        else
            buildTimeSeries()
    }

    /**
     * This query will only work for time series data (Non-aggregated)
     */
    private fun buildTimeSeries(): String = """
        ${bucket()}
        ${range()}
        |> filter(fn: (r) =>
            (r._field == "clicks" or r._field == "impressions")
            ${filterBy()}
        )
        |> pivot(
            rowKey:["_time"],
            columnKey: ["_field"],
            valueColumn: "_value"
        )
        |> map(fn: (r) =>
            ({ r with _value: float(v:r.clicks) / float(v:r.impressions) })
        )
        ${group()}
        ${groupByTime()}        
    """.trimIndent()

    /**
     * This query will only work for aggregated data (no time component)
     */
    private fun buildSummary(): String = """
    clicks = ${bucket()}
        ${range()}
        |> filter(fn: (r) =>
            r._field == "clicks"
            ${filterBy()}
        )
        ${group()}
        ${groupByTime()}
        ${aggregate()}
        
        impressions = ${bucket()}
        ${range()}
        |> filter(fn: (r) =>
            r._field == "impressions"
            ${filterBy()}
        )
        ${group()}
        ${groupByTime()}
        ${aggregate()}        
        join(
          tables: {impressions:impressions, clicks:clicks},
          on: [${joinTableOn().joinToString(separator = ",") { "\"$it\"" }}]
        )
        |> map(fn: (r) => ({
            r with _value: float(v:r._value_clicks) / float(v:r._value_impressions)                      
        }))        
        |> yield()
    """

    /**
     * Logic here will help us determine what fields we join on when joining the impressions and clicks
     * tables to make the ctr table
     */
    private fun joinTableOn(): List<String>{
        val columns = mutableListOf<String>("_stop", "_start")
        if(query.groupByDimensions != null && query.groupByDimensions.isNotEmpty()) columns.addAll(query.groupByDimensions)
        return columns
    }
}

interface FluxQueryBuilderFactory {
    fun builder(bucket: String, query: Query): FluxQueryBuilder
}

class FluxQueryBuilderFactoryImpl: FluxQueryBuilderFactory {
    override fun builder(bucket: String, query: Query): FluxQueryBuilder =
        when(query.metric) {
            MetricId.ctr -> CtrFluxQueryBuilder(bucket, query)
            else -> BasicFluxQueryBuilder(bucket, query)
        }
}