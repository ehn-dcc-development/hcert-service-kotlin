# Electronic Health Certificate Service Kotlin

Run the service with `./gradlew bootRun`, browse it at <http://localhost:8080/certservice>, fill in some data (use the provided data!) and click on "Generate COSE" to view the result.

## Endpoints

Get the certificate for verification at `/cert/{kid}`: Set your `Accept` to `text/plain` to get the certificate in Base64, or set it to `application/octet-stream` to get binary data.

## TODO

- Use the JSON schema for data classes

## Dependencies

To pull the dependency of `hcert-kotlin` (<https://github.com/ehn-digital-green-development/hcert-kotlin>), create a personal access token (read <https://docs.github.com/en/packages/guides/configuring-gradle-for-use-with-github-packages>), and add `gpr.user` and `gpr.key` in your `~/.gradle/gradle.properties`. Alternatively, install the dependency locally with `./gradlew publishToMavenLocal` in the directory `hcert-kotlin`.
