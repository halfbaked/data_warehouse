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
        ${window()}
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
        if(query.groupBy != null && query.groupBy.isNotEmpty())
            """|> group(columns: [ ${query.groupBy.joinToString(",") { "\"$it\"" }} ])"""
        else
            "|> group()" // Otherwise data is grouped by tag combination by default

    fun window(): String =
        if(query.window != null)
            """|> aggregateWindow(every: ${query.window}, fn: ${query.aggregate ?: "sum"})"""
        else
            ""

    fun aggregate(): String =
        if(query.aggregate != null && query.window == null) " |> ${query.aggregate}()"
        else ""

}

class CtrFluxQueryBuilder(bucket: String, query: Query): BasicFluxQueryBuilder(bucket, query) {
    override fun filter(): String = """
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
    """
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