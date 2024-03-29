# helmify

Generate a Helm Chart for an existing application. 

## Currently supporting

- [x] Spring Boot
- [x] Quarkus
- [x] Maven
- [x] Gradle

## What does it do

First step to using helmify is to provide a buildfile. For supported frameworks see above.
This can be a Maven pom.xml or a Gradle build.gradle / build.gradle.kts file. You can either upload the file
or have helmify pull it from a Spring Initializr URL (of Initializr's "Share" feature).

Next, helmify will query you for additional information:

- The URL of your docker image repository
- The tag to use for your docker image
- optionally a Docker Image Pull Secret can be specified

helmify will pre-populate the generated helm chart with this information.

Finally, helmify will set up a helm chart tailored to your application's needs. It will:

- create a configmap.yaml and write a spring application.properties file which contains coordinates to external
  resources:
  - spring.datasource.url
  - spring.rabbitmq.host/port
- if spring actuator is found, helmify will configure it to run on a separate port which will be used for readiness
  and liveness probes
- create / update secrets.yaml to store credentials for external resources
- update deployment.yaml to mount resource credential secretKeyRefs as environment variables into your pods
  - spring datasource username / password
  - spring rabbitmq username / password
- update deployment.yaml to run an initContainer for each external resource which blocks until the resource is ready
- update deployment.yaml to mount a runtime configuration file (spring application.properties)
- update deployment.yaml to configure readiness/liveness probes
- update values.yaml with a config block for each external resource
- update values.yaml with global values like hostnames
- update values.yaml to align naming to artifactId
- update values.yaml to use user-supplied docker image repository url

... and offer the generated helm chart as a zip file for download.

## Stack

- Java 21
- [Spring Boot 3.x](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Thymeleaf](https://www.thymeleaf.org/doc/tutorials/3.1/thymeleafspring.html)
- [htmx](https://htmx.org/docs/)
- [Chota](https://jenil.github.io/chota/#docs)

## Build

```shell
mvn spring-boot:build-image -Dspring-boot.build-image.imageName=helmify/helmify
```
