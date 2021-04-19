package ehn.techiop.hcert.kotlin

import com.google.zxing.BarcodeFormat
import ehn.techiop.hcert.kotlin.chain.CborProcessingChain
import ehn.techiop.hcert.kotlin.chain.SampleData
import ehn.techiop.hcert.kotlin.chain.VaccinationData
import ehn.techiop.hcert.kotlin.chain.impl.DefaultBase45Service
import ehn.techiop.hcert.kotlin.chain.impl.DefaultCborService
import ehn.techiop.hcert.kotlin.chain.impl.DefaultCompressorService
import ehn.techiop.hcert.kotlin.chain.impl.DefaultCoseService
import ehn.techiop.hcert.kotlin.chain.impl.DefaultTwoDimCodeService
import ehn.techiop.hcert.kotlin.chain.impl.DefaultContextIdentifierService
import ehn.techiop.hcert.kotlin.chain.impl.RandomEcKeyCryptoService
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
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
    private val valSuiteService = DefaultContextIdentifierService()
    private val compressorService = DefaultCompressorService()
    private val base45Service = DefaultBase45Service()
    private val processingChain =
        CborProcessingChain(cborService, coseService, valSuiteService, compressorService, base45Service)
    private val processingChainAdapter = CborProcessingChainAdapter(title, processingChain, qrCodeService)

    @Test
    fun recovery() {
        val cardViewModel = processingChainAdapter.process(title, SampleData.recovery)

        assertThat(cardViewModel.title, startsWith(title))
        assertThat(cardViewModel.base64Items.find { it.title == "CBOR (Hex)" }?.value?.length, isAround(562))
        assertThat(cardViewModel.base64Items.find { it.title == "COSE (Hex)" }?.value?.length, isAround(742))

        val prefixedCompressedCose =
            cardViewModel.base64Items.find { it.title.startsWith("Prefixed Compressed COSE") }?.value
        assertThat(prefixedCompressedCose?.length, isAround(549))
        if (prefixedCompressedCose == null) throw AssertionError()
        assertPlain(prefixedCompressedCose, SampleData.recovery)
    }

    @Test
    fun vaccination() {
        val cardViewModel = processingChainAdapter.process(title, SampleData.vaccination)

        assertThat(cardViewModel.title, startsWith(title))
        assertThat(cardViewModel.base64Items.find { it.title == "CBOR (Hex)" }?.value?.length, isAround(1086))
        assertThat(cardViewModel.base64Items.find { it.title == "COSE (Hex)" }?.value?.length, isAround(1266))

        val prefixedCompressedCose =
            cardViewModel.base64Items.find { it.title.startsWith("Prefixed Compressed COSE") }?.value
        assertThat(prefixedCompressedCose?.length, isAround(730))
        if (prefixedCompressedCose == null) throw AssertionError()
        assertPlain(prefixedCompressedCose, SampleData.vaccination)
    }

    @Test
    fun test() {
        val cardViewModel = processingChainAdapter.process(title, SampleData.test)

        assertThat(cardViewModel.title, startsWith(title))
        assertThat(cardViewModel.base64Items.find { it.title == "CBOR (Hex)" }?.value?.length, isAround(872))
        assertThat(cardViewModel.base64Items.find { it.title == "COSE (Hex)" }?.value?.length, isAround(1052))

        val prefixedCompressedCose =
            cardViewModel.base64Items.find { it.title.startsWith("Prefixed Compressed COSE") }?.value
        assertThat(prefixedCompressedCose?.length, isAround(691))
        if (prefixedCompressedCose == null) throw AssertionError()
        assertPlain(prefixedCompressedCose, SampleData.test)
    }

    private fun isAround(input: Int) = allOf(greaterThan(input.div(10) * 9), lessThan(input.div(10) * 11))

    private fun assertPlain(input: String, jsonInput: String) {
        val vaccinationData = processingChain.verify(input)
        val decodedFromInput =
            Json { isLenient = true; ignoreUnknownKeys = true }.decodeFromString<VaccinationData>(jsonInput)
        assertThat(vaccinationData, equalTo(decodedFromInput))
    }

}
