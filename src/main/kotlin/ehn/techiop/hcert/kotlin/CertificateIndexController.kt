package ehn.techiop.hcert.kotlin

import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.security.MessageDigest


@RestController
class CertificateIndexController(
    private val trustListServiceAdapter: TrustListServiceAdapter,
) {

    private val log = LoggerFactory.getLogger(this::class.java)

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

    private fun sha256(input: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(input).toHexString()

    private fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

}

