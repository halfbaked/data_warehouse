# Data Warehouse
In understanding the design of the data warehouse it's worth breaking it into 3 areas:
- Loading - the loading of the data into the data warehouse
- Queries - interrogating the data to provide useful data sets 
- Schemas - describing what data can be queried (or indeed loaded) and how

## Loading

It is possible to load data via the api. The data is expected to be in csv format without an initial header line.

## Queries

### Examples
- /query/clicks_impressions?metric=impressions&start=2019-01-01T00:00:00Z&end=2019-01-03T00:00:00Z&groupByTime=1d&groupByDimensions=campaign
- /query/clicks_impressions?metric=impressions&filterBy=campaign:Google&groupByTime=1d&start=-6d
- /query/clicks_impressions?metric=clicks&groupByDimensions=datasource,campaign

### Parameter Validation
Parameter validation is currently limited to types including any enums defined. More should be invested in it.
Currently it is possible to query values directly to the database, which is fragile and insecure.

### Parameters
- metric (required)
- start - start of date range
- end - end of date range
- groupByDimensions - one or more dimensions to group results by
- filterBy - one or more key:value pairs to filter results by
- groupByTime - specify a duration to group results by e.g. 1w (1 week) 
- aggregate - the aggregate function to apply

### Result List Lists
Instead of results being presented in a flat list, they are presented as a list of lists. This added complexity
provides the ability to present multiple groups of data with one request/response to easily compare multiple time series
on one graph. 

```
    [
        [ Result1, Result2 ]
        [ Result3, Result4 ] 
    ]

```

## Schemas

#### Purpose
Schemas help to communicate the data structures supported. They can also serve a role in validating input.
In this project a schema is known as a measurement (type).

Data points with a common measurement, have a common sets of dimensions and metrics.
Measurements supported can be found by sending a GET request to `/measurements`.
When querying or loading data, the relevant measurment id must be provided.

## Documentation
- [Api document on Apiary](https://datawarehouse2.docs.apiary.io/#).
- [Influxdb](https://docs.influxdata.com/influxdb/v2.0/)
- [Ktor](https://ktor.io/docs/welcome.html)

## Code 

### Code structure
In bigger projects classes would be separated into different folder/packages and individual files, but given the size of the
project, keeping the code in a few files seemed more elegant. 

- *Application* - the entry point of the application
- *Mappers* - logic to transform from one data model to another
- *Models* - the data classes aka domain objects
- *Repositories* - classes to access/load data from a data source
- *Routes* - request handling aka controllers
- *QueryBuilder* - all code related to building the queries sent to the database
- *Loader* - all code related to loading data into the database

### Factory Pattern
The factory pattern employed for both loading and querying makes it easy to support more schemas / measurements.

### Dependency Injection
Dependency injection is provided by Koin, but is rarely used in the current code for the sake of expediency.  
Making better use of DI would make it easier to test/mock. 

## Building and Deploying
The ktor server can be run as an executable jar. The gradle file includes the shadow plugin which assists in 
building the executable jar necessary, and exposes the shadowJar gradle task you can use to do just that. 
The resulting executable jar will be found in build/libs.

In the root directory, you will find `docker-compose.yml` file that can be used to initalize containers for both the
app and the influxdb database. The compose file expects certain environment variables to be available. These could be
provided using a .env file. See `docker-compose.env` for an example, but it's recommended you change at least the more 
sensitive values within.

Note that older versions of docker compose don't support the --env-file option.

