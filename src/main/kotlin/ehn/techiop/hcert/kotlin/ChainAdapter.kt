package ehn.techiop.hcert.kotlin

import ehn.techiop.hcert.kotlin.chain.Chain
import ehn.techiop.hcert.kotlin.chain.TwoDimCodeService
import ehn.techiop.hcert.kotlin.chain.asBase64
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class ChainAdapter(
    private val title: String,
    private val chain: Chain,
    private val qrCodeService: TwoDimCodeService
) {

    fun process(superTitle: String, input: String): CardViewModel {
        val result = chain.encode(Json.decodeFromString(input))
        val qrCode = qrCodeService.encode(result.step5Prefixed)
        return CardViewModel(
            "$superTitle: $title",
            input = input,
            base64Items = listOf(
                Base64Item("CBOR (Hex)", result.step0Cbor.toHexString()),
                Base64Item("CWT (Hex)", result.step1Cwt.toHexString()),
                Base64Item("COSE (Hex)", result.step2Cose.toHexString()),
                Base64Item("Compressed COSE (Base45)", result.step4Encoded),
                Base64Item("Prefixed Compressed COSE (Base45)", result.step5Prefixed)
            ),
            codeResources = listOf(
                CodeResource("QR Code", qrCode.asBase64()),
            )
        )
    }

    fun processSingle(input: String) =
        chain.encode(Json.decodeFromString(input)).step5Prefixed

}

private fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }