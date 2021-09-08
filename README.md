# Programmatic Guaranteed Delivery Stats

The Programmatic Guaranteed (PG) Delivery Stats (Del Stats) server provides endpoints for 4 services in a PG cluster:
1. A central repository for PBS instances to store (POST) delivery progress reports;
2. A central repository for Planning Adapters to query (GET) delivery progress reports;
3. A central repository for General Planners to query (GET) token spend reports;
4. A central repository for reporting systems to query (GET) line item summaries.

Details about endpoints for this service can be found [here](docs/delivery_stats_endpoints.md)

# Getting Started
## _Technical Stack_

- Build and Deploy
  - Maven
  - Java 8
  - CentOS 7.3
  - Docker
- Service
  - Spring Boot
  - Lombok
  - Graphite
- Database
  - MySQL 5.7
- Tests
  - Junit5
  - Mockito
  - H2

## _Building_

To build the project, you will need 
[Java 8 JDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
and [Maven](https://maven.apache.org/) installed.

To verify the installed Java run in console:
```bash
java -version
```
which should show something like (yours may be different but must show 1.8):
```
java version "1.8.0_241"
Java(TM) SE Runtime Environment (build 1.8.0_241-b07)
Java HotSpot(TM) 64-Bit Server VM (build 25.241-b07, mixed mode)
```

Follow next steps to create JAR which can be deployed locally. 
- Download or clone a project and checkout master or master-dev (once open sourced this information will change)
```bash
git clone https://github.rp-core.com/ContainerTag/pg-del-stats-svc.git
```

- Move to project directory:
```bash
cd pg-del-stats-svc
```

- Run below command to build project:
```bash
mvn clean verify package
```

## _Configuration_

Configuration is handled by [Spring Boot](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html), 
which supports properties files, YAML files, environment variables and command-line arguments for setting config values.

The source code includes minimal required configuration file `src/main/resources/application.yaml`.
These properties can be extended or modified with an external configuration file.

For example, `metrics-config.yaml`:
```yaml
metrics:
  graphite:
    enabled: true
    prefix: custom.pg-central.delstats
    host: localhost
    port: 3003
    interval: 60
```
For properties not specified or overriden in `metrics-config.yaml`, application will look for default settings  in `src/main/resources/application.yaml` file.

To use external application configuration just add the following as start up arguments:
```bash
--spring.config.additional-location=/path/to/metrics-config.yaml
```
Details on all available configuration tags can be found in [configuration document](docs/config-app.md)

## _Data Source_

The server requires a MySQL database version 5.7.x (x being 23 and up) to be available with the
[Del Stats schema](sql/schema.sql) created. If not running locally or using a non-standard port,
make sure to update the JDBC URL configuration in the application.yaml.

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/stats?useSSL=false&useLegacyDatetimeCode=false
```

## _Running_

The project build has been tested at runtime with a Java 8 runtime. 
The default configuration is expecting a MySQL instance at the standard port (3306) running on localhost,
so You need to point to configure a database instance with the [Del Stats schema](sql/schema.sql) created.
Then run your server with the following command:
```bash
java -jar target/pg-del-stats-svc.jar
```

## _Basic check_

Go to [http://localhost:8080/del-stats/api/v1/health](http://localhost:8080/del-stats/api/v1/health) 
and verify that status in the response body is `UP`.


## _Code Style_

The [pom.xml](pom.xml) is configured to enforce a coding style defined in [checkstyle.xml](checkstyle.xml).

The intent here is to maintain a common style across the project and rely on the process to enforce it instead of individuals.
