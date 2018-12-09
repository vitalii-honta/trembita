<img src="https://github.com/vitalii-honta/trembita/blob/master/media/trembita-p.png" alt="trembita"/>
 
## Description 
Project Trembita - Functional Data Pipelining library. 
Lets you query and transform your `not enough big` data in a pure functional, typesafe & declarative way.


## Processing modules
- [kernel](./kernel) - lazy (parallel) data pipelines, QL for grouping/aggregations and stateful computations using [Cats](https://github.com/typelevel/cats) and [Shapeless](https://github.com/milessabin/shapeless) 

## Data sources 
 - Any `Iterable` - just wrap your collection into `DataPipeline`
 - [cassandra connector](./cassandra_connector) - fetch rows from your `Cassandra` database with `CassandraSource`
 - [cassandra phantom](./cassandra_connector_phantom) - provides [Phantom](https://github.com/outworkers/phantom) library support
 
## Miscelone
 - [trembita slf4j](./trembita-slf4j) - provides [slf4j](https://www.slf4j.org/) logging support. Use it with any compatible logging backend ([logback](https://logback.qos.ch/), [log4j](https://logging.apache.org/log4j/2.x/))
 - [trembita circe](./serialization/circe) - allows to convert aggregation results directly into JSON using [Circe](https://github.com/circe/circe)
