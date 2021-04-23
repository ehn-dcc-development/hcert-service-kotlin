package ehn.techiop.hcert.kotlin

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.zxing.BarcodeFormat
import ehn.techiop.hcert.data.DigitalGreenCertificate
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
import org.junit.jupiter.api.Test

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

    @Test
    fun recovery() {
        val cardViewModel = processingChainAdapter.process(title, SampleData.recovery)

        assertThat(cardViewModel.title, startsWith(title))
        assertThat(cardViewModel.base64Items.find { it.title == "CBOR (Hex)" }?.value?.length, isAround(440))
        assertThat(cardViewModel.base64Items.find { it.title == "COSE (Hex)" }?.value?.length, isAround(610))

        val prefixedCompressedCose =
            cardViewModel.base64Items.find { it.title.startsWith("Prefixed Compressed COSE") }?.value
        assertThat(prefixedCompressedCose?.length, isAround(471))
        if (prefixedCompressedCose == null) throw AssertionError()
        assertPlain(prefixedCompressedCose, SampleData.recovery)
    }

    @Test
    fun vaccination() {
        val cardViewModel = processingChainAdapter.process(title, SampleData.vaccination)

        assertThat(cardViewModel.title, startsWith(title))
        assertThat(cardViewModel.base64Items.find { it.title == "CBOR (Hex)" }?.value?.length, isAround(966))
        assertThat(cardViewModel.base64Items.find { it.title == "COSE (Hex)" }?.value?.length, isAround(1138))

        val prefixedCompressedCose =
            cardViewModel.base64Items.find { it.title.startsWith("Prefixed Compressed COSE") }?.value
        assertThat(prefixedCompressedCose?.length, isAround(657))
        if (prefixedCompressedCose == null) throw AssertionError()
        assertPlain(prefixedCompressedCose, SampleData.vaccination)
    }

    @Test
    fun test() {
        val cardViewModel = processingChainAdapter.process(title, SampleData.test)

        assertThat(cardViewModel.title, startsWith(title))
        assertThat(cardViewModel.base64Items.find { it.title == "CBOR (Hex)" }?.value?.length, isAround(664))
        assertThat(cardViewModel.base64Items.find { it.title == "COSE (Hex)" }?.value?.length, isAround(836))

        val prefixedCompressedCose =
            cardViewModel.base64Items.find { it.title.startsWith("Prefixed Compressed COSE") }?.value
        assertThat(prefixedCompressedCose?.length, isAround(604))
        if (prefixedCompressedCose == null) throw AssertionError()
        assertPlain(prefixedCompressedCose, SampleData.test)
    }

    private fun isAround(input: Int) = allOf(greaterThan(input.div(10) * 9), lessThan(input.div(10) * 11))

    private fun assertPlain(input: String, jsonInput: String) {
        val vaccinationData = processingChain.verify(input, VerificationResult())
        val decodedFromInput = ObjectMapper().readValue(jsonInput, DigitalGreenCertificate::class.java)
        assertThat(vaccinationData, equalTo(decodedFromInput))
    }

}
