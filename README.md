# Electronic Health Certificate Service Kotlin

Run the service with `./gradlew bootRun`, browse it at <http://localhost:8080/ehn>, fill in some data (use the provided data!) and click on "Generate COSE" to view the result. This service is also deployed at <https://dgc.a-sit.at/ehn>.

## Endpoints

Get the certificate for verification at `/cert/{kid}`: Set your `Accept` to `text/plain` to get the certificate in Base64, or set it to `application/octet-stream` to get binary data.
`{kid}` is expected to be the Base64-URL representation of the bytes from `KID` claim of the CWT.

Get the contents of encoded sample data directly at `/qrc/vaccination`, `/qrc/test`, `/qrc/recovery`.

Get a trust list (V1 format) at `/cert/list`, or in V2 format at `/cert/listv2` and `/cert/sigv2`. See below for details.

That trust list contains the local certificates, as well as the certificates from the [DGC Acceptance Gatway](https://github.com/eu-digital-green-certificates/dgc-gateway) (see below for configuration).

Certificates used as the document signing certificate as well as the trust list certificate are also displayed in the index page, when running this service.

## TrustList V2

There is also an option to create (on the service) and read (in the app) a list of trusted certificates for verification of HCERTs. This is implemented as a content file and a separate signature file. We assume, that the certificates are relatively stable, therefore the content file will be updated infrequently. On the other hand, the signature file is valid for 48 hours only (that's configurable), and possibly downloaded from client apps frequently.

The server can create the content and signature files:
```Java
// Load the private key and certificate from somewhere ...
String privateKeyPem = "-----BEGIN PRIVATE KEY-----\nMIIEvQIBADAN...";
String certificatePem = "-----BEGIN CERTIFICATE-----\nMIICsjCCAZq...";
CryptoService cryptoService = new FileBasedCryptoService(privateKeyPem, certificatePem);
TrustListV2EncodeService trustListService = new TrustListV2EncodeService(cryptoService);
// alternative: new TrustListV2EncodeService(cryptoService, Duration.ofHours(24));

// Load the list of trusted certificates from somewhere ...
Set<X509Certificate> trustedCerts = new HashSet<>(cert1, cert2, ...);
byte[] encodedTrustList = trustListService.encodeContent(trustedCerts);
byte[] encodedSignature = trustListService.encodeSignature(encodedTrustList);
```

The client can use it for verification:
```Java
String trustListAnchor = "-----BEGIN CERTIFICATE-----\nMIICsjCCAZq...";
CertificateRepository trustAnchor = new PrefilledCertificateRepository(trustListAnchor);
byte[] encodedTrustList = // file download etc.
byte[] encodedSignature = // file download etc.
CertificateRepository repository = new TrustListCertificateRepository(encodedSignature, encodedTrustList, trustAnchor);
Chain chain = Chain.buildVerificationChain(repository);

// Continue as in the example above ...
VerificationResult verificationResult = new VerificationResult();
Eudgc dgc = chain.decode(input, verificationResult);
```

The content file of the TrustList is a CBOR structure. We can define the schema loosely in this way:
```
TRUSTLIST := {
  c := [{
    c := the bytes of the X.509 encoded certificate
    i := the bytes of the KID (first 8 bytes of SHA-256 digest of the certificate
  }]
}
```

The signature file contains a COSE Sign1Message with a CWT like this:
```
CWT := {
  NOT_BEFORE (5) := timestamp (seconds since UNIX epoch)
  EXPIRATION (4) := timestamp (seconds since UNIX epoch)
  SUBJECT (2)    := bytes of the SHA-256 digest of the content file
}
```

An (non-normative) example of the content file is:

```
bf61639fbf6169488eb407136a44c0b4616359015d3082015930820100a003020102020500df95c6
54300a06082a8648ce3d0403023010310e300c06035504030c0545432d4d65301e170d3730303130
313030303030305a170d3730303133313030303030305a3010310e300c06035504030c0545432d4d
653059301306072a8648ce3d020106082a8648ce3d0301070342000464c98565ad3125915b744d85
9739afc5d739499151c686f3be462a29010de3d3477a5b25899e4524f2da82bf52bb208be3a2af22
12496c005481f34172e70f23a3473045300e0603551d0f0101ff0404030205a030330603551d2504
2c302a060c2b06010401008e378f650101060c2b06010401008e378f650102060c2b06010401008e
378f650103300a06082a8648ce3d040302034700304402200c2fb9aeb6bc031e87cdfb9b0feec23e
3d5d6d721f028eb80f291f33466cbd4502205b7f114f88e05ba8de3596968075e1702c7f679aea90
069bceb0aba467feeda7ffbf61694872e870bd32eadad161635902eb308202e7308201cfa0030201
020204603a47dd300d06092a864886f70d01010b05003011310f300d06035504030c065253412d4d
65301e170d3730303130313030303030305a170d3730303133313030303030305a3011310f300d06
035504030c065253412d4d6530820122300d06092a864886f70d01010105000382010f003082010a
0282010100a7ae474c87f637d3321bf143dad2cedbb2d94a16eba7f1ee2d54d202a37a8e08da960e
1ac88e687426a6f276ee816e6edf4fce44cb0e9a98d635a271cb54f8e917b98ea4447549def2250d
2342a70667118e462bb29cfb815ed9d99164013e96b66f39bba123f4cea377b672c4fb9b84545be2
f5fc0da4be3fe0a55446b3478cbcda68fc8d199dd92a100339ff65ef7e117e38937b5950139112e0
10d5a8ccc0c88158bcb2adb5a538f12443b54a0797ac413f579bdce79d151b8651108d09484a5543
d0651fb5f4683edfdabbb0f078b15acf7721c97e7a7be9256a98fba15b94c85bde37f7874d73eaec
5bf12ba627170a9e087f26c2ce9caeb3c15845ac570203010001a3473045300e0603551d0f0101ff
0404030205a030330603551d25042c302a060c2b06010401008e378f650101060c2b06010401008e
378f650102060c2b06010401008e378f650103300d06092a864886f70d01010b050003820101009e
99394f2bb579f362dc0f035319786f0485b778c3b9d04789e7c27694ab25089d66b3b7f9babacd9e
58d1ad318a7102013ee2678e46c6fe80b8273cff845a25cfffdabd56fcbc9aad6c154005ae0c73e0
80b4d86210da45f4c7e4afdb6bb86963f576c485770e61b76443d9637e6cb32f885d55a135bced94
82c9d74a1ab09be5a255da3b5517f08db26092c0ba007b835d78edc5f21cfc9608879ea21f23f62b
a5c2e2bd4850af403fa47745a568f2b82cfd94730a1368a5874e3a3f710f99f4abac44d8d584db98
50b8bc006eb94ead45f591b6dd9992f7810a5fb28f340fb66284947f3ba07fe17d7491b1541c06d6
1b02ca3687f8bb505ab24c3d6f55b9ffffff
```

... decoded to the following CBOR structure:

```
{
  "c": [
    {
      "i": h'8EB407136A44C0B4',           // the KID of the certificate
      "c": h'308201593082010<TRUNCATED>'  // the X.509 encoded certificate
    },
    {
      "i": h'72E870BD32EADAD1',           // the KID of the certificate
      "c": h'308202E7308201C<TRUNCATED>'  // the X.509 encoded certificate
    }
  ]
}
```

An (non-normative) example of the signature file is:

```
d28450a3182a020448f7b1e7dd8e75cc120126a0582ca30258201a10e4cab0f604840cfd52cb6c0f
423cf19de9d6c2cb0359583d97fe200f7f24041a0002a300050058400204f76cd98082adaaebe623
e7932d203586b18b38d13ea81cf8811e787d8812d753d51d465edc8a3044b8ba82e24083b7535fd0
ce3306f52665871b69923cd1
```

... decoded to the following COSE structure:

```
18([
  h'A3182A020448F7B1E7DD8E75CC120126',
  {},
  h'A30258201A10E4CAB0F604840CFD52CB6C0F423CF19DE9D6C2CB0359583D97FE200F7F24041A
    0002A3000500',
  h'0204F76CD98082ADAAEBE623E7932D203586B18B38D13EA81CF8811E787D8812D753D51D465E
    DC8A3044B8BA82E24083B7535FD0CE3306F52665871B69923CD1'
])
```

... with this protected header:

```
{
  42: 2,                  // the version number of the format
  4: h'F7B1E7DD8E75CC12', // the KID of the signing certificate
  1: -7,                  // the key type of the signing certificate, i.e. EC
}
```

... and this CBOR content:

```
{
  2: h'1A10E4CAB0F604840CFD52CB6C0F423C
       F19DE9D6C2CB0359583D97FE200F7F24', // bytes of the SHA-256 digest of the content file
  5: 1619788643,                          // timestamp, after which the signature is valid
  4: 1619961443                           // timestamp, before which the signature is valid
}
```

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
