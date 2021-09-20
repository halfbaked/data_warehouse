## Data Warehouse
In understanding the design of the data warehouse it's worth breaking it into 3 areas:
- Loading - the loading of the data into the data warehouse
- Queries - interrogating the data to provide useful data sets 
- Schemas - describing what data can be queried (or indeed loaded) and how


### API Documentation
An api doc written in OpenApi format is available on Apiary and can be found [here](https://datawarehouse2.docs.apiary.io/#).

### Data Model / Nomenclature

The modeling comes from a mix of the specification document and the influxdb.  

#### Measurement
A group of data points with common dimensions and metrics. 
When we know data is of a certain measurement (type) we know how to load it and how it can be queried.
Calling it a measurement type or a measurement schema might be more accurate.

#### Project structure
In bigger projects classes would be separated into different folder/packages and individual files, but given the size of the
project, keeping the code in a few files was more elegant. 

- *Application* - the entry point of the application
- *Mappers* - logic to transform from one data model to another
- *Models* - the data classes aka domain objects
- *Repositories* - classes to access/load data from a data source
- *Routes* - request handling aka controllers

#### Validation
Data validation is currently limited to types including any enums defined. More should be invested in it. 
Currently it is possible to pass data directly to the database, which is fragile and insecure. 

#### CTR format
I opted for a CTR format akin to basis points. Similar to how money is often represented in data, it is a menas
of storing partial values without the use of sometimes messy floats. 