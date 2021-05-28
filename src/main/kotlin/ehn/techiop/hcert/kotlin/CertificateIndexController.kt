package ehn.techiop.hcert.kotlin

import ehn.techiop.hcert.kotlin.chain.asBase64
import ehn.techiop.hcert.kotlin.chain.common.PkiUtils
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.security.MessageDigest
import java.util.Base64


@RestController
class CertificateIndexController(
    private val trustListServiceAdapter: TrustListServiceAdapter,
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

    @GetMapping(value = ["/cert/listv2"], produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun getTrustListV2(): ResponseEntity<ByteArray> {
        log.info("/cert/listv2 called")
        val body = trustListServiceAdapter.trustListV2Content
        return ResponseEntity.ok().eTag(sha256(body)).body(body)
    }

    @GetMapping(value = ["/cert/sigv2"], produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun getTrustListSigV2(): ResponseEntity<ByteArray> {
        log.info("/cert/sigv2 called")
        val body = trustListServiceAdapter.trustListV2Sig
        return ResponseEntity.ok().eTag(sha256(body)).body(body)
    }

    private fun loadCertificate(requestKid: String): ByteArray {
        val kid = Base64.getUrlDecoder().decode(requestKid)
        return trustListServiceAdapter.cryptoServices.map { it.getCertificate() }
            .firstOrNull { PkiUtils.calcKid(it) contentEquals kid }?.encoded
            ?: throw IllegalArgumentException("kid not known: $requestKid")
    }

    private fun sha256(input: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(input).toHexString()

    private fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

}

