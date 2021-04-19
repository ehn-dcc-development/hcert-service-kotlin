package ehn.techiop.hcert.kotlin

import ehn.techiop.hcert.kotlin.chain.CryptoService
import ehn.techiop.hcert.kotlin.chain.asBase64
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.security.cert.Certificate
import java.util.Base64


@RestController
class CertificateIndexController(
    private val cryptoServices: Set<CryptoService>
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    @GetMapping(value = ["/cert/{kid}"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getCertByKidText(@PathVariable kid: String): ResponseEntity<String> {
        log.info("/cert/$kid called (text)")
        return ResponseEntity.ok(loadCertificate(kid).encoded.asBase64())
    }

    @GetMapping(value = ["/cert/{kid}"], produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun getCertByKidBinary(@PathVariable kid: String): ResponseEntity<ByteArray> {
        log.info("/cert/$kid called (binary)")
        return ResponseEntity.ok(loadCertificate(kid).encoded)
    }

    private fun loadCertificate(requestKid: String): Certificate {
        val kid = Base64.getUrlDecoder().decode(requestKid)
        for (cryptoService in cryptoServices) {
            try {
                return cryptoService.getCertificate(kid)
            } catch (e: Throwable) {
                continue
            }
        }
        throw IllegalArgumentException("kid not known: $requestKid")
    }

}

