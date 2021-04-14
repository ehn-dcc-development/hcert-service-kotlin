package ehn.techiop.hcert.kotlin

import com.google.zxing.BarcodeFormat
import ehn.techiop.hcert.kotlin.chain.Base45Service
import ehn.techiop.hcert.kotlin.chain.CborProcessingChain
import ehn.techiop.hcert.kotlin.chain.CborService
import ehn.techiop.hcert.kotlin.chain.CompressorService
import ehn.techiop.hcert.kotlin.chain.DefaultCoseService
import ehn.techiop.hcert.kotlin.chain.DefaultValSuiteService
import ehn.techiop.hcert.kotlin.chain.RandomKeyCryptoService
import ehn.techiop.hcert.kotlin.chain.SampleData
import ehn.techiop.hcert.kotlin.chain.TwoDimCodeService
import ehn.techiop.hcert.kotlin.chain.VaccinationData
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.Matchers.lessThan
import org.junit.jupiter.api.Test

class CoseProcessStrategyTests {

    private val title = "CoseProcessStrategyTest"
    private val qrCodeService = TwoDimCodeService(350, BarcodeFormat.QR_CODE)
    private val cryptoService = RandomKeyCryptoService()
    private val cborService = CborService()
    private val coseService = DefaultCoseService(cryptoService)
    private val valSuiteService = DefaultValSuiteService()
    private val compressorService = CompressorService()
    private val base45Service = Base45Service()
    private val cborProcessingChain =
        CborProcessingChain(cborService, coseService, valSuiteService, compressorService, base45Service)
    private val cborViewAdapter = CborViewAdapter(cborProcessingChain, base45Service, qrCodeService)

    @Test
    fun recovery() {
        val cardViewModel = cborViewAdapter.process(title, SampleData.recovery)

        assertThat(cardViewModel.title, equalTo(title))
        assertThat(cardViewModel.base64Items.find { it.title == "CBOR (Hex)" }?.value?.length, isAround(562))
        assertThat(cardViewModel.base64Items.find { it.title == "COSE (Hex)" }?.value?.length, isAround(742))

        val prefixedCompressedCose = cardViewModel.base64Items.find { it.title.startsWith("Prefixed Compressed COSE") }?.value
        assertThat(prefixedCompressedCose?.length, isAround(549))
        if (prefixedCompressedCose == null) throw AssertionError()
        assertPlain(prefixedCompressedCose, SampleData.recovery)
    }

    @Test
    fun vaccination() {
        val cardViewModel = cborViewAdapter.process(title, SampleData.vaccination)

        assertThat(cardViewModel.title, equalTo(title))
        assertThat(cardViewModel.base64Items.find { it.title == "CBOR (Hex)" }?.value?.length, isAround(1086))
        assertThat(cardViewModel.base64Items.find { it.title == "COSE (Hex)" }?.value?.length, isAround(1266))

        val prefixedCompressedCose = cardViewModel.base64Items.find { it.title.startsWith("Prefixed Compressed COSE") }?.value
        assertThat(prefixedCompressedCose?.length, isAround(730))
        if (prefixedCompressedCose == null) throw AssertionError()
        assertPlain(prefixedCompressedCose, SampleData.vaccination)
    }

    @Test
    fun test() {
        val cardViewModel = cborViewAdapter.process(title, SampleData.test)

        assertThat(cardViewModel.title, equalTo(title))
        assertThat(cardViewModel.base64Items.find { it.title == "CBOR (Hex)" }?.value?.length, isAround(872))
        assertThat(cardViewModel.base64Items.find { it.title == "COSE (Hex)" }?.value?.length, isAround(1052))

        val prefixedCompressedCose = cardViewModel.base64Items.find { it.title.startsWith("Prefixed Compressed COSE") }?.value
        assertThat(prefixedCompressedCose?.length, isAround(691))
        if (prefixedCompressedCose == null) throw AssertionError()
        assertPlain(prefixedCompressedCose, SampleData.test)
    }

    private fun isAround(input: Int) = allOf(greaterThan(input.div(10) * 9), lessThan(input.div(10) * 11))

    private fun assertPlain(input: String, jsonInput: String) {
        val vaccinationData = cborProcessingChain.verify(input)
        val decodedFromInput =
            Json { isLenient = true; ignoreUnknownKeys = true }.decodeFromString<VaccinationData>(jsonInput)
        assertThat(vaccinationData, equalTo(decodedFromInput))
    }

}
