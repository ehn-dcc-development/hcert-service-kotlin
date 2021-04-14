package ehn.techiop.hcert.kotlin

import ehn.techiop.hcert.kotlin.chain.Base45Service
import ehn.techiop.hcert.kotlin.chain.CborProcessingChain
import ehn.techiop.hcert.kotlin.chain.TwoDimCodeService
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class CborViewAdapter(
    private val cborProcessingChain: CborProcessingChain,
    private val base45Service: Base45Service,
    private val qrCodeService: TwoDimCodeService
) {

    fun process(title: String, input: String): CardViewModel {
        val result =
            cborProcessingChain.process(Json { isLenient = true; ignoreUnknownKeys = true }.decodeFromString(input))
        val qrCode = qrCodeService.encode(result.prefixedEncodedCompressedCose)
        return CardViewModel(
            title,
            input = input,
            base64Items = listOf(
                Base64Item("CBOR (Hex)", result.cbor.toHexString()),
                Base64Item("COSE (Hex)", result.cose.toHexString()),
                Base64Item("Compressed COSE (Base45)", base45Service.encode(result.compressedCose)),
                Base64Item("Prefixed Compressed COSE (Base45)", result.prefixedEncodedCompressedCose)
            ),
            codeResources = listOf(
                CodeResource("Valid QR Code", qrCode),
            )
        )
    }

}

private fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }