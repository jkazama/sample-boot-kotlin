sample-boot-kotlin
---

### Introduction

This is a DDD sample implementation based on [Spring Boot](http://projects.spring.io/spring-boot/) / [Spring Security](http://projects.spring.io/spring-security/) / [Kotlin](https://kotlinlang.org/).
The basic foundation is borrowed from the Java version [sample-boot-hibernate](https://github.com/jkazama/sample-boot-hibernate).
This is not a framework, so please use it as a base template when starting up a project that uses Spring Boot.

For the core concepts, please refer to the materials presented previously ([Domain-Driven Design using Spring Boot](http://www.slideshare.net/jkazama/jsug-20141127)).

For UI implementation samples, please refer to [sample-ui-vue](https://github.com/jkazama/sample-ui-vue) / [sample-ui-react](https://github.com/jkazama/sample-ui-react).

---

This sample can also be used as an implementation example of domain modeling using Spring Boot. Since this is an implementation approach that has actually been used in projects of considerable scale, I hope you can use it as a reference case when modeling.

*※ As noted in the JavaDoc, this is a reference implementation level. It does not contain production-level code.*

#### Layering Concept

This is an orthodox three-layer model, but the infrastructure layer is considered as a cross-cutting interpretation.

| Layer          | Characteristics                                                        |
| -------------- | ---------------------------------------------------------------------- |
| UI             | Exposes use case processing (integrates remoting and external sites as needed) |
| Application    | Aggregates use case processing (including external resource access)    |
| Domain         | Pure domain processing (does not depend on external resources)         |
| Infrastructure | Provides DI containers, ORM, various libraries, and message resources  |

The UI layer's exposed processing is usually done using JSP or Thymeleaf, but in this sample, we assume the use of different types of clients and only provide API through RESTful API. (Client applications need to be prepared separately)

#### Spring Boot Usage Policy

Spring Boot can be used in various ways, but in this sample we use it with the following policies:

- Use yml for configuration files. Do not use extended files like xml for Bean definitions.
- For registered Beans, limit component scanning for both startup speed and maintainability, and supplement with definition classes
    - `ApplicationConfig` / `ApplicationDbConfig` / `ApplicationSecurityConfig`
    - Enable automatic scanning only under `controller` / `usecase`
- Exception handling is defined at the endpoint (RestErrorAdvice / RestErrorController). Disable whitelabel functionality.
- Specialize in Hibernate as the JPA implementation.
- Spring Security authentication method uses HttpSession, which has been common for a long time, instead of basic authentication.
- Provide simple implementations for basic utilities that Spring does not support.

#### Kotlin Coding Policy

- Actively use Gradle's Kotlin plugins
    - `kotlin-spring` / `kotlin-jpa`
- Singleton Beans placed in the DI container are usually defined as normal classes with @Component annotation
    - The `kotlin-spring` benefit automatically applies `open`
- Data transfer objects (DTOs) such as request parameters, return values, and entities are defined as data classes
- DI is mainly performed through constructor injection
- Bean Validation annotations need @field annotation to be applied properly
    - Extended Bean Validation annotations were created in Java since the creation method in Kotlin was unclear
- All fields in DTO classes using Bean Validation allow null
    - It is necessary to temporarily accept null state when evaluating
- Entities allow side-effect processing, while business requirement DTOs do not
    - All requests that cause side effects to Entities are aggregated through business methods

#### Package Structure

Please refer to the following for package/resource structure:

```
main
  kotlin
    sample
      context                         … Infrastructure layer
      controller                      … UI layer
      model                           … Domain layer
      usecase                         … Application layer
      util                            … General utilities
      - Application.kt                … Executable startup class
  resources
    - application.yml                 … Configuration file
    - ehcache.xml                     … Spring Cache configuration file
    - logback-spring.xml              … Logging configuration file
    - messages-validation.properties  … Exception message resources
    - messages.properties             … Message resources
```

### Sample Use Case

The sample use case assumes a simple flow as follows:

- **A customer with an account balance of 1 million yen** makes a withdrawal request (occurrence T, settlement T + 3).
- **The system** advances the business day.
- **The system** confirms the withdrawal request. (Allows withdrawal cancellation until confirmation)
- **The system** reflects the deposit/withdrawal cash flow that reached the settlement date to the account balance.

### Operation Verification

This sample uses [Gradle](https://gradle.org/), so you can easily verify operation in an IDE or console.

*※Please run on a terminal with internet connection as library downloads are performed automatically.*

#### Server Startup (IntelliJ IDEA)

To start using [IntelliJ IDEA](https://www.jetbrains.com/idea/), please make this sample into a project with the following steps:

1. Select "File -> New -> Project From Existing Sources..." and select build.gradle of this sample, then press *OK*

Execute this sample with the following steps:

1. Right-click on *Application.kt* and select "Run `sample.Application.kt...`"
1. When "Started Application" is output to the *Console* tab, startup is complete on port 8080
1. Launch a browser and check the status at "http://localhost:8080/management/health"

#### Server Startup (Console)

To run from Windows/Mac console, use Gradle console commands.

*※JDK8 or higher installation is required in advance.*

1. Move to the console directly under the downloaded *sample-boot-kotlin* directory
1. Execute "gradlew bootRun"
1. When "Started Application" is output to the console, startup is complete on port 8080
1. Launch a browser and check the status at "http://localhost:8080/management/health"

#### Client Verification

After starting the server in Eclipse or console, please execute each verification method of `SampleClient` under the test package as a unit test.

##### Customer Use Cases

| URL                              | Processing           | Execution Arguments |
| -------------------------------- | ------------------- | ------------------- |
| `/api/asset/cio/withdraw`        | Wire transfer withdrawal request | [`accountId`: sample, `currency`: JPY, `absAmount`: request amount] |
| `/api/asset/cio/unprocessedOut/` | Wire transfer withdrawal unprocessed search | - |

*※Wire transfer withdrawal request is POST, others are GET*

##### Internal Use Cases

| URL                     | Processing                    | Execution Arguments                               |
| ----------------------- | ----------------------------- | ------------------------------------------------- |
| `/api/admin/asset/cio/` | Wire transfer deposit/withdrawal request search | [`updFromDay`: yyyy-MM-dd, `updToDay`: yyyy-MM-dd] |

*※GET*

##### Batch Use Cases

| URL                                     | Processing                                          | Execution Arguments |
| --------------------------------------- | --------------------------------------------------- | ------------------- |
| `/api/system/job/daily/processDay`      | Advance business day (simple day progression only) | -                   |
| `/api/system/job/daily/closingCashOut`  | Close withdrawal requests for the current business day | -               |
| `/api/system/job/daily/realizeCashflow` | Realize deposit/withdrawal cash flow (reflect to balance on settlement date) | - |

*※POST*

### Creating Distribution Jar

With Spring Boot, you can run an application with a single distribution file by creating an Executable Jar (jar that includes libraries and static resources).

1. Execute "gradlew build" from the console
1. A jar is output directly under `build/libs`, so distribute it to a Java8 or later execution environment
1. Execute "java -jar xxx.jar" from the console in the execution environment to start

> By adding "--spring.profiles.active=[profile name]" to the execution arguments, you can change the configuration values in application.yml by profile unit.

### Dependent Libraries

> Please refer to `build.gradle` for actual detailed definitions

### Supplementary Explanation (Infrastructure Layer)

This is a brief explanation of the infrastructure layer.

※For more detailed overview, please refer to the comments written in the code

#### DB / Transaction

Under `sample.context.orm`. This is ORM support implementation to bring domain implementation closer to Entity. Since we take an approach that does not write domain processing in Repository (defined per schema unit), we do not use JpaRepository provided by Spring Boot.
Transaction definitions can be a source of trouble, so they are only allowed in the application layer and are applied in a narrowly limited form as much as possible. For simple readOnly processing only, we take method-level response using `@Transactional([Bean name])`.

We assume two types of schemas: standard business logic use (`DefaultRepository`) and system use (`SystemRepository`). In Entity implementation, we do not depend on the schema, but let the passing side (mainly the application layer) make the decision.

> It is also possible to use JpaRepository provided by Spring Data JPA. (It connects to the schema managed by DefaultRepository by default)

#### Authentication/Authorization

Under `sample.context.security`. We assume two patterns: customer (ROLE_USER) / employee (ROLE_ADMIN). The means of providing user information (UserDetails) for each is defined in `sample.usecase.SecurityService`.

To enable authentication/authorization functionality, set `true` to `extension.security.auth.enabled` in `application.yml` (false by default for testing purposes). We do not consider coexistence of customers/employees in the same VM. When starting in employee-only mode, set `extension.security.auth.admin` to `true` by switching profiles at startup, etc.

#### User Audit

Under `sample.context.audit`. Obtains information about "when", "who", and "what was done" from both customer and system perspectives. Intended for use in the application layer. It switches the output destination and information according to the type of logged-in `Actor` (User/System). It can be used to obtain behavioral evidence during operation.

#### Exceptions

Provides `ValidationException` which has exceptions stacked per field as a general concept.
Exceptions are handled collectively at the terminal UI layer. Specifically, the application layer and domain layer simply throw runtime exceptions for their respective purposes to the upper layer, and exception catching is performed through implicit insertion using AOP in `sample.controller.RestErrorAdvise`.

#### Date/Time

Uses the `time` library added in Java8 via `sample.context.Timestamper`. Business day calculation considering holidays, etc. includes domain concepts, so it is separately defined in `sample.model.BusinessDayHandler`.

#### Caching

Uses @Cacheable provided by Spring in `AccountService`, etc. It would be better to unify either the UI layer or application layer, but in this sample, it is only applied to the application layer. It is recommended not to use JPA's cache mechanism except when needed within Entity.

#### Testing

As patterns, we consider a total of 2 patterns: regular JUnit tests and Entity tests limited to Hibernate only (base class is `EntityTestSupport`).

> We do not perform tests using Spring containers here as they tend to be costly.

### License

The license for this sample, including all code, is *MIT License*.
Please feel free to use it as a base implementation sample when starting up projects using Spring Boot.
