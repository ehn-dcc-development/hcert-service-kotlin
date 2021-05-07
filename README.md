# Electronic Health Certificate Service Kotlin

Run the service with `./gradlew bootRun`, browse it at <http://localhost:8080/ehn>, fill in some data (use the provided data!) and click on "Generate COSE" to view the result. This service is also deployed at <https://dgc.a-sit.at/ehn>.

## Endpoints

Get the certificate for verification at `/cert/{kid}`: Set your `Accept` to `text/plain` to get the certificate in Base64, or set it to `application/octet-stream` to get binary data.
`{kid}` is expected to be the Base64-URL representation of the bytes from `KID` claim of the CWT.

Get the contents of encoded sample data directly at `/qrc/vaccination`, `/qrc/test`, `/qrc/recovery`.

Get a trust list at `/cert/list`: CBOR encoded map signed with a fixed COSE key, with certificate:

```
-----BEGIN CERTIFICATE-----
MIIBJTCBy6ADAgECAgUAwvEVkzAKBggqhkjOPQQDAjAQMQ4wDAYDVQQDDAVFQy1N
ZTAeFw0yMTA0MjMxMTI3NDhaFw0yMTA1MjMxMTI3NDhaMBAxDjAMBgNVBAMMBUVD
LU1lMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE/OV5UfYrtE140ztF9jOgnux1
oyNO8Bss4377E/kDhp9EzFZdsgaztfT+wvA29b7rSb2EsHJrr8aQdn3/1ynte6MS
MBAwDgYDVR0PAQH/BAQDAgWgMAoGCCqGSM49BAMCA0kAMEYCIQC51XwstjIBH10S
N701EnxWGK3gIgPaUgBN+ljZAs76zQIhAODq4TJ2qAPpFc1FIUOvvlycGJ6QVxNX
EkhRcgdlVfUb
-----END CERTIFICATE-----
```

That trust list contains the local certificates, as well as the certificates from the [DGC Test Gatway](https://github.com/eu-digital-green-certificates/dgc-gateway) (see below for configuration).

Certificates used as the document signing certificate as well as the trust list certificate are also displayed in the index page, when running this service.

## Configuration

Sample configuration:

```yaml
server:
  port: 9000
  servlet:
    context-path: /ehn
logging:
  file:
    path: log
app:
  chain:
    ec-private: classpath:ec-chain-private.pem
    ec-cert: classpath:ec-chain-cert.pem
  trust-list:
    ec-private: classpath:ec-trust-list-private.pem
    ec-cert: classpath:ec-trust-list-cert.pem
dgc:
  gateway:
    connector:
      enabled: true
      endpoint: https://test-dgcg-ws.tech.ec.europa.eu
      proxy:
        enabled: false
      max-cache-age: 300
     tls-trust-store:
        password: dgcg-p4ssw0rd
        path: classpath:tls-truststore.jks
      tls-key-store:
        alias: mtls_private_cert
        password: dgcg-p4ssw0rd
        path: /var/lib/ssl/mtls.jks
      trust-anchor:
        alias: ta_tst
        password: dgcg-p4ssw0rd
        path: /var/lib/ssl/ta.jks
```

See also <https://github.com/eu-digital-green-certificates/dgc-lib> for configuration of the gateway connector

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
 - [dgc-lib](https://github.com/eu-digital-green-certificates/dgc-lib), under the Apache-2.0 License
