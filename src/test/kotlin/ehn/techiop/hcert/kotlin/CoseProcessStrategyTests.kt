package ehn.techiop.hcert.kotlin

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.zxing.BarcodeFormat
import ehn.techiop.hcert.data.Eudgc
import ehn.techiop.hcert.kotlin.chain.Chain
import ehn.techiop.hcert.kotlin.chain.SampleData
import ehn.techiop.hcert.kotlin.chain.VerificationResult
import ehn.techiop.hcert.kotlin.chain.impl.DefaultBase45Service
import ehn.techiop.hcert.kotlin.chain.impl.DefaultCborService
import ehn.techiop.hcert.kotlin.chain.impl.DefaultCompressorService
import ehn.techiop.hcert.kotlin.chain.impl.DefaultContextIdentifierService
import ehn.techiop.hcert.kotlin.chain.impl.DefaultCoseService
import ehn.techiop.hcert.kotlin.chain.impl.DefaultTwoDimCodeService
import ehn.techiop.hcert.kotlin.chain.impl.RandomEcKeyCryptoService
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
    private val coseService = DefaultCoseService(cryptoService)
    private val contextIdentifierService = DefaultContextIdentifierService()
    private val compressorService = DefaultCompressorService()
    private val base45Service = DefaultBase45Service()
    private val processingChain =
        Chain(cborService, coseService, contextIdentifierService, compressorService, base45Service)
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
        val vaccinationData = processingChain.decode(input, VerificationResult())
        val decodedFromInput = ObjectMapper().readValue(jsonInput, Eudgc::class.java)
        assertThat(vaccinationData, equalTo(decodedFromInput))
    }


    companion object {

        @JvmStatic
        @Suppress("unused")
        fun inputProvider() = listOf(
            TestInput(SampleData.testNaa, 664, 790, 606),
            TestInput(SampleData.testRat, 582, 754, 579),
            TestInput(SampleData.vaccination, 552, 724, 559),
            TestInput(SampleData.recovery, 498, 668, 501)
        )

    }

    data class TestInput(val input: String, val cborSize: Int, val coseSize: Int, val finalSize: Int)


}
