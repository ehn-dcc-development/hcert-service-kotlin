# Electronic Health Certificate Service Kotlin

Run the service with `./gradlew bootRun`, browse it at <http://localhost:8080/certservice>, fill in some data (use the provided data!) and click on "Generate COSE" to view the result.

## Endpoints

Get the certificate for verification at `/cert/{kid}`: Set your `Accept` to `text/plain` to get the certificate in Base64, or set it to `application/octet-stream` to get binary data.
`{kid}` is expected to be the Base64-URL representation of the bytes from `KID` claim of the CWT.

## Dependencies

To pull the dependency of `hcert-kotlin` (<https://github.com/ehn-digital-green-development/hcert-kotlin>), create a personal access token (read <https://docs.github.com/en/packages/guides/configuring-gradle-for-use-with-github-packages>), and add `gpr.user` and `gpr.key` in your `~/.gradle/gradle.properties`. Alternatively, install the dependency locally with `./gradlew publishToMavenLocal` in the directory `hcert-kotlin`.

## Libraries

This library uses the following dependencies:
 - [Spring Boot](https://github.com/spring-projects/spring-boot), under the Apache-2.0 License
 - [Kotlin](https://github.com/JetBrains/kotlin), under the Apache-2.0 License
 - [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization), under the Apache-2.0 License
 - [COSE-JAVA](https://github.com/cose-wg/cose-java), under the BSD-3-Clause License
 - [ZXing](https://github.com/zxing/zxing), under the Apache-2.0 License
 - [Jackson](https://github.com/FasterXML/jackson-module-kotlin), under the Apache-2.0 License
