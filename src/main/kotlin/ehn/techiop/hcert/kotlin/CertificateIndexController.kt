package ehn.techiop.hcert.kotlin

import ehn.techiop.hcert.kotlin.chain.CryptoService
import ehn.techiop.hcert.kotlin.chain.asBase64
import ehn.techiop.hcert.kotlin.chain.common.PkiUtils
import ehn.techiop.hcert.kotlin.trust.TrustListEncodeService
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Base64


@RestController
class CertificateIndexController(
    private val trustListServiceAdapter: TrustListServiceAdapter
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    @GetMapping(value = ["/cert/{kid}"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getCertByKidText(@PathVariable kid: String): ResponseEntity<String> {
        log.info("/cert/$kid called (text)")
        return ResponseEntity.ok(loadCertificate(kid).asBase64())
    }

    @GetMapping(value = ["/cert/{kid}"], produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun getCertByKidBinary(@PathVariable kid: String): ResponseEntity<ByteArray> {
        log.info("/cert/$kid called (binary)")
        return ResponseEntity.ok(loadCertificate(kid))
    }

    @GetMapping(value = ["/cert/list"], produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun getTrustedCertFile(): ResponseEntity<ByteArray> {
        log.info("/cert/list called")
        return ResponseEntity.ok(trustListServiceAdapter.getTrustList())
    }

    private fun loadCertificate(requestKid: String): ByteArray {
        val kid = Base64.getUrlDecoder().decode(requestKid)
        return trustListServiceAdapter.cryptoServices.map { it.getCertificate() }
            .firstOrNull { PkiUtils.calcKid(it) contentEquals kid }?.encoded
            ?: throw IllegalArgumentException("kid not known: $requestKid")
    }

}

class TrustListServiceAdapter(signingService: CryptoService, internal val cryptoServices: Set<CryptoService>) {

    private val trustListService = TrustListEncodeService(signingService)
    private val internalCertificates = cryptoServices.map { it.getCertificate() }.toSet()
    private val certificateFromSk =
        CertificateFactory.getInstance("X.509").generateCertificate(
            """
            -----BEGIN CERTIFICATE-----
            MIIBXTCCAQMCEAmvCwfrzosTNEsW+nJSjDMwCgYIKoZIzj0EAwIwMTEiMCAGA1UE
            AwwZTmF0aW9uYWwgQ1NDQSBvZiBTbG92YWtpYTELMAkGA1UEBhMCU0swHhcNMjEw
            NDE2MTEyNzQxWhcNMjYwMzAxMTEyNzQxWjA1MSYwJAYDVQQDDB1EU0MgbnVtYmVy
            IHdvcmtlciBvZiBTbG92YWtpYTELMAkGA1UEBhMCU0swWTATBgcqhkjOPQIBBggq
            hkjOPQMBBwNCAAQxjqpOs+6L3VCjYzqeaaT41JhdrhZhPuqGxu3gArWsn652jJQM
            PpNbmuJrsC5/zUADeqwWrnXf+sRkfy/cmeSxMAoGCCqGSM49BAMCA0gAMEUCIQD8
            sn5/VJtpKzSIIEknta6IaguYcjCNLDjwXKSKjpsZIQIgNgDQtZ7s0abo8auVg6DT
            6jgK9xJtUWydnQcRTTgIGWI=
            -----END CERTIFICATE-----
        """.trimIndent().byteInputStream()
        ) as X509Certificate

    internal fun getTrustList() = trustListService.encode(internalCertificates + certificateFromSk)

}
