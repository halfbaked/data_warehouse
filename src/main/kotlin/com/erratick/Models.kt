package com.erratick

import io.ktor.locations.*
import java.time.Instant

/**
 * The nomenclature here can be a little ambiguous, so it is worth clarifying that measurement in this case
 * does not refer to a single measurement made in time, but more a type of measurement.
 * This class allows us to describe our data in a generic way, such that clients will know what metrics, dimensions,
 * etc. are available to them.
 */
data class Measurement(
    /**
     * Each measurement has a unique id.
     */
    val id: MeasurementId,
    /**
     * The values that are measured
     */
    val metrics: List<MetricId>,
    /**
     * Dimensions are additional descriptive data concerning the measurements.
     * Results can be grouped or filtered by one or more dimensions.
     * Dimensions can be seen as a discrete set of key:value tags applied to each measure.
     */
    val dimensions: List<String>,
)

enum class MeasurementId {
    clicks_impressions
}

enum class MetricId {
    clicks, impressions, ctr
}

/**
 * Defines the possible values that can be used when querying the data.
 * Using the Locations allows us to define what can be passed in a more typed and structured way.
 * Using strings for most of the variables here sort of negates some of the benefit of the typing, in favor of a more
 * flexible and succinct api.
 *
 * Properties of the query class that are not used in the @Location annotation are assumed to be query parameters.
 * Optional parameters should be a nullable type assigned null by default.
 */
@Location("/query/{measurement}")
data class Query(
    /**
     * Id of the measurement
     */
    val measurement: MeasurementId,
    /**
     * The metric to analyse
     */
    val metric: MetricId,
    /**
     * Range start
     */
    val start: String? = null,
    /**
     * Range end
     */
    val end: String? = null,
    /**
     * Map of key value pairs results can be filtered by
     */
    val filterBy: QueryKeyValueList? = null,
    /**
     * Results can be grouped by a particular dimension or a window of time (e.g. 1d)
     */
    val groupBy: QueryStringList? = null,
    /**
     * Aggregate function to apply
     */
    val aggregate: Aggregate? = null,
    /**
     * Group data into windows of time
     */
    val groupByTime: String? = null,
)

/**
 * When mapping the incoming query parameters we cannot use a generic Map<String,String>.
 * Using generics for type matching is always tricky because generics are essentially stripped when the bytecode
 * is compiled. Hence we need to subclass with our own custom type.
 */
class QueryKeyValueList: HashMap<String, String>()

class QueryStringList: ArrayList<String>()

enum class Aggregate { sum, mean }

data class QueryResult(
    val value: Any?,
    /**
     * The time stamp associate with the result
     */
    val time: Instant?,
    /**
     * List of dimensions and the values of those dimensions that pertain to this result.
     */
    val dimensions: Map<String,String>?
)

/**
 * List of query results
 */
class QueryResultList: ArrayList<QueryResult>()

data class Dimension(val name: String, val value: String)