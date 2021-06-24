package ehn.techiop.hcert.kotlin

import com.google.zxing.BarcodeFormat
import ehn.techiop.hcert.kotlin.chain.Chain
import ehn.techiop.hcert.kotlin.chain.SampleData
import ehn.techiop.hcert.kotlin.chain.VerificationResult
import ehn.techiop.hcert.kotlin.chain.impl.DefaultBase45Service
import ehn.techiop.hcert.kotlin.chain.impl.DefaultCborService
import ehn.techiop.hcert.kotlin.chain.impl.DefaultCompressorService
import ehn.techiop.hcert.kotlin.chain.impl.DefaultContextIdentifierService
import ehn.techiop.hcert.kotlin.chain.impl.DefaultCoseService
import ehn.techiop.hcert.kotlin.chain.impl.DefaultCwtService
import ehn.techiop.hcert.kotlin.chain.impl.DefaultHigherOrderValidationService
import ehn.techiop.hcert.kotlin.chain.impl.DefaultSchemaValidationService
import ehn.techiop.hcert.kotlin.chain.impl.DefaultTwoDimCodeService
import ehn.techiop.hcert.kotlin.chain.impl.RandomEcKeyCryptoService
import ehn.techiop.hcert.kotlin.data.GreenCertificate
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.startsWith
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.Matchers.lessThan
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class CoseProcessStrategyTests {

    private val title = "CoseProcessStrategyTest"
    private val qrCodeService = DefaultTwoDimCodeService(350, BarcodeFormat.QR_CODE)
    private val cryptoService = RandomEcKeyCryptoService()
    private val cborService = DefaultCborService()
    private val cwtService = DefaultCwtService()
    private val coseService = DefaultCoseService(cryptoService)
    private val contextIdentifierService = DefaultContextIdentifierService()
    private val compressorService = DefaultCompressorService()
    private val base45Service = DefaultBase45Service()
    private val higherOrderValidationService = DefaultHigherOrderValidationService()
    private val schemaValidationService = DefaultSchemaValidationService()
    private val processingChain =
        Chain(higherOrderValidationService, schemaValidationService, cborService, cwtService, coseService, compressorService, base45Service, contextIdentifierService)
    private val processingChainAdapter = ChainAdapter(title, processingChain, qrCodeService)

    @ParameterizedTest
    @MethodSource("inputProvider")
    fun success(input: TestInput) {
        val cardViewModel = processingChainAdapter.process(title, input.input)

        assertThat(cardViewModel.title, startsWith(title))
        assertThat(cardViewModel.base64Items.find { it.title == "CBOR (Hex)" }?.value?.length, isAround(input.cborSize))
        assertThat(cardViewModel.base64Items.find { it.title == "COSE (Hex)" }?.value?.length, isAround(input.coseSize))

        val prefixedCompressedCose =
            cardViewModel.base64Items.find { it.title.startsWith("Prefixed Compressed COSE") }?.value
        assertThat(prefixedCompressedCose?.length, isAround(input.finalSize))
        if (prefixedCompressedCose == null) throw AssertionError()
        assertPlain(prefixedCompressedCose, input.input)
    }

    private fun isAround(input: Int) = allOf(greaterThan(input.div(10) * 9), lessThan(input.div(10) * 11))

    private fun assertPlain(input: String, jsonInput: String) {
        val decodeResult = processingChain.decode(input)
        val vaccinationData = decodeResult.chainDecodeResult.eudgc
        val decodedFromInput = Json.decodeFromString<GreenCertificate>(jsonInput)
        assertThat(vaccinationData, equalTo(decodedFromInput))
    }


    companion object {

        @JvmStatic
        @Suppress("unused")
        fun inputProvider() = listOf(
            TestInput(SampleData.testNaa, 642, 852, 648),
            TestInput(SampleData.testRat, 610, 820, 628),
            TestInput(SampleData.vaccination, 576, 786, 604),
            TestInput(SampleData.recovery, 522, 732, 549)
        )

    }

    data class TestInput(val input: String, val cborSize: Int, val coseSize: Int, val finalSize: Int)


}
