package ehn.techiop.hcert.kotlin

import com.fasterxml.jackson.databind.ObjectMapper
import ehn.techiop.hcert.data.DigitalGreenCertificate
import ehn.techiop.hcert.kotlin.chain.Chain
import ehn.techiop.hcert.kotlin.chain.TwoDimCodeService
import ehn.techiop.hcert.kotlin.chain.impl.DefaultBase45Service

class ChainAdapter(
    private val title: String,
    private val chain: Chain,
    private val qrCodeService: TwoDimCodeService
) {

    private val base45Service = DefaultBase45Service()

    fun process(superTitle: String, input: String): CardViewModel {
        val result = chain.process(ObjectMapper().readValue(input, DigitalGreenCertificate::class.java))
        val qrCode = qrCodeService.encode(result.prefixedEncodedCompressedCose)
        return CardViewModel(
            "$superTitle: $title",
            input = input,
            base64Items = listOf(
                Base64Item("CBOR (Hex)", result.cbor.toHexString()),
                Base64Item("COSE (Hex)", result.cose.toHexString()),
                Base64Item("Compressed COSE (Base45)", base45Service.encode(result.compressedCose)),
                Base64Item("Prefixed Compressed COSE (Base45)", result.prefixedEncodedCompressedCose)
            ),
            codeResources = listOf(
                CodeResource("QR Code", qrCode),
            )
        )
    }

    fun processSingle(input: String) =
        chain.process(
            ObjectMapper().readValue(input, DigitalGreenCertificate::class.java)
        ).prefixedEncodedCompressedCose

}

private fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }