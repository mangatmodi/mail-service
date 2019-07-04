# Mail Service

The service enables users to send mail asynchronously by exposing an HTTP API 
## Design
<img src="https://raw.githubusercontent.com/mangatmodi/mail-service/master/Mail-Service-Design.png"/>

1. User sends PUT request send mail.
2. **Mail API** returns 202 Accepted and asynchronously saves the record in `mail` Kafka topic. Check [open api spec](https://raw.githubusercontent.com/mangatmodi/mail-service/master/src/main/resources/swagger.yml).
3. **Send Mail Service** reads from Kafka, download the attachments and sends the request to send mail to SMTP server
4. If downloading the attachment fails, or the request to SMTP fails, the kafka record is added to `rejected-mail` Kafka topic with `retry=0`.
5. Same Mail Service reads from `rejected-mail` record and increment retry every time it fails
6. The Service does not write back to kafka if number of retries are equal to the configured settings 

### Requirements to run/build
1. Gradle 5.4.1, Gradle wrapper is shipped together to download teh gradle version
2. Kotlin 1.3.40+ (Only if running locally)
3. JDK 8+
4. Docker

### Instructions to use
The project is build in two configuration `dev/non-dev`. To run/develop locally first we need to start kafka and fakesmtp services. We use docker-compose here.
```bash
#From project root
CONFIG_PROFILE=dev ./gradlew composeUp
```
The task above also builds the _fat-jar_ of the project. There are two entry points, one for each service. Either use IDE or commandLine to start the services by giving entry point in the argument.
```bash
#API Service
HOST_ADDRESS=localhost PORT=8080 java -jar build/lib/mail-service-0.1.jar -s API

#Send Mail Service
HOST_ADDRESS=localhost PORT=8081 java -jar build/lib/mail-service-0.1.jar -s SEND_MAIL
```   
`HOST_ADDRESS` and `PORT` are required by both services to listen and to connect to kafka brokers.

At this point you can send mail request to the API. Some urls to help during development.
1. Kafka: http://localhost:3030
2. fakesmtp: http://localhost:1080
3. Open api sec: http://localhost:8080/doc

While the services are running tests can be run for verification. Functional tests are provided which cover end to end cases for each service taking them as black-box. We also have test suite for configuration.

After development is done, complete end to end tests can be done as
```bash
#stop containers
./gradlew composeDown
./gradlew test
``` 

### Key Design/Architecture Decisions

#### Build System
Gradle provides enough power required for building the package and is among most popular build system in Java world.

#### Packaging System
The project is packed into a fat-jar which includes all the non jdk dependencies. This jar is further packed into `OpenJDK:11-slim` based docker image. 

#### Programming Language/Platform
JVM platform and Kotlin was selected simply for my familiarity and preference. The code is compiled to `JDK8` bytecode to have wider reach as higher versions are still not widely adopted. However it is being run at `JRE-11` due to major improvements in JVM and GC.

#### Config System
HOCON config is selected due to it being *typesafe* and more *powerful* than plain yaml and json based configs. HOCON also uses environment variables. Thus many of the properties can be configured at the site by just changing the environment variables platform.  

#### Web Server Framework
KTor is an extremely lightweight framework for writing micro services. Supports *Netty* for NIO and is very easy to configure. It is based on Kotlin coroutines.

#### RxJava/2
RxJava/2 implements observer pattern and provides a monadic-like api to easily compose functions(chaining!) and do error handling. It is un-opinionated on multithreading and provides utilities to the developers to configure concurrency.

#### Vert.x 
Vert.x is another very popular microservice framework based on the event-loop model. Where it contains `2 x #cpu-cores` event loops. However we here use only Kafka and SMTP clients from vert.x, because Ktor though lagging in features, provides simpler API.

#### Dependency Injection
Guice is a feature packed DI framework, making it easy to provide dependencies.   

### Salient Features

- Coding Style: I tried to keep coding type safe, where many issues could be found at the compile time, and used sealed classes(Algebraic Data Type:Sum) and other properties in Kotlin to ensure exhaustiveness. Exhaustiveness makes it easier to reason about code and force the developer to handle all the branches.
- Distributed Log Tracing: Added a rudimentary example on how to trace logs across services. Send an API request with `X-Request-ID` set in the header, you could actually see logs logging in both the services with the same ID.
- Send Mail Service uses RxChain for executing the logic, but uses a custom IO thread pool, with back pressure. This prevents blocking the event-loop (reading from kafka), but also adds back-pressure to kafka consumer to avoid OOM or creating too many threads/tasks which is the case with the default schedulers in Rx library.
- `/health` endpoint is exposed on each service.
- Docker images are tagged with the commit id.             

### Required Improvements  
The project was highly limited as a POC and several things can be improved. Following things come to the mind before similar could be attempted for production.
1. MDC is by no means a good solution for request tracing. It is thread-local and we could have thread switches while serving a request.  
2. The Mail API is not REST compliant as we are not creating any resource. We shall actually add a persistence at the backend and the job status shall be exposed and updated via an API. 
3. The use of known exceptions should be avoided. `Either` pattern could be used to capture errors and provide exhaustiveness
4. HOCON is hard to configure when every property needs to managed at the site. For that Consul or ConfigMap might be a better choice.
5. Metrics should be collected, may be with Micrometer. They should be exported, either by exposing an endpoint or writing them into another system like statsd, from where they could be exported into prometheus or influxdb 
6. A UI for swagger could be exported.
7. Overall code quality could still be improved. Due to time constraints I have cut some corners like forced change from nullable to non-nullable types etc.  
    
