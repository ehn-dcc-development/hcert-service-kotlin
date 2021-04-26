package ehn.techiop.hcert.kotlin

import ehn.techiop.hcert.kotlin.chain.CryptoService
import ehn.techiop.hcert.kotlin.chain.TrustListEncodeService
import ehn.techiop.hcert.kotlin.chain.asBase64
import ehn.techiop.hcert.kotlin.chain.common.PkiUtils
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
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
        return ResponseEntity.ok(trustListServiceAdapter.trustList)
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
    internal val trustList = trustListService.encode(cryptoServices.map { it.getCertificate() }.toSet())

}
