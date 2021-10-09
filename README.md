### Email based 2 Factor Authentication: implementation using Spring-boot, Spring-security and Java-Mail API

### Local Setup
* Install Java 17 (recommended to use [SdkMan](https://sdkman.io))

`sdk install java 17-open`
* Install Maven (recommended to use [SdkMan](https://sdkman.io))

`sdk install maven`

* Clone the repo and Go to application.properties under src/main/resources and replace the below mentioned values with correct ones

```
spring.mail.username = <your email-id goes here>
spring.mail.password = <your app-password/password goes here>
```

* Run the below command in core

`mvn clean install`

* To start the application, run any of the below 2 commands

`mvn spring-boot:run &`

`java -jar /target/email-based-two-factor-auth-spring-boot-0.0.1-SNAPSHOT.jar`

* Access the swagger-ui

`http://localhost:8080/swagger-ui.html`

