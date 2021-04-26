package ehn.techiop.hcert.kotlin

import com.fasterxml.jackson.databind.ObjectMapper
import ehn.techiop.hcert.data.Eudgc
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
        val result = chain.encode(ObjectMapper().readValue(input, Eudgc::class.java))
        val qrCode = qrCodeService.encode(result.step5Prefixed)
        return CardViewModel(
            "$superTitle: $title",
            input = input,
            base64Items = listOf(
                Base64Item("CBOR (Hex)", result.step1Cbor.toHexString()),
                Base64Item("COSE (Hex)", result.step2Cose.toHexString()),
                Base64Item("Compressed COSE (Base45)", base45Service.encode(result.step3Compressed)),
                Base64Item("Prefixed Compressed COSE (Base45)", result.step5Prefixed)
            ),
            codeResources = listOf(
                CodeResource("QR Code", qrCode),
            )
        )
    }

    fun processSingle(input: String) =
        chain.encode(ObjectMapper().readValue(input, Eudgc::class.java)).step5Prefixed

}

private fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }