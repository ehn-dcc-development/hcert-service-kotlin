package ehn.techiop.hcert.kotlin

import com.google.zxing.BarcodeFormat
import ehn.techiop.hcert.kotlin.chain.Base45Service
import ehn.techiop.hcert.kotlin.chain.CborProcessingChain
import ehn.techiop.hcert.kotlin.chain.CompressorService
import ehn.techiop.hcert.kotlin.chain.DefaultCborService
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

    private val qrCodeService = TwoDimCodeService(350, BarcodeFormat.QR_CODE)
    private val aztecService = TwoDimCodeService(350, BarcodeFormat.AZTEC)
    private val cryptoService = RandomKeyCryptoService()
    private val cborService = DefaultCborService(cryptoService)
    private val valSuiteService = DefaultValSuiteService()
    private val compressorService = CompressorService()
    private val base45Service = Base45Service()
    private val cborProcessingChain =
        CborProcessingChain(cborService, valSuiteService, compressorService, base45Service)
    private val cborViewAdapter = CborViewAdapter(cborProcessingChain, base45Service, qrCodeService, aztecService)

    @Test
    fun recovery() {
        val cardViewModel = cborViewAdapter.process(SampleData.recovery)

        assertThat(cardViewModel.title, equalTo("COSE"))
        assertThat(cardViewModel.base64Items.find { it.title == "CBOR (Base45)" }?.value?.length, isAround(386))
        assertThat(cardViewModel.base64Items.find { it.title == "COSE (Base45)" }?.value?.length, isAround(557))

        val prefixedCompressedCose = cardViewModel.base64Items.find { it.title == "Prefixed Compressed COSE" }?.value
        assertThat(prefixedCompressedCose?.length, isAround(549))
        if (prefixedCompressedCose == null) throw AssertionError()
        assertPlain(prefixedCompressedCose, SampleData.recovery)
    }

    @Test
    fun vaccination() {
        val cardViewModel = cborViewAdapter.process(SampleData.vaccination)

        assertThat(cardViewModel.title, equalTo("COSE"))
        assertThat(cardViewModel.base64Items.find { it.title == "CBOR (Base45)" }?.value?.length, isAround(779))
        assertThat(cardViewModel.base64Items.find { it.title == "COSE (Base45)" }?.value?.length, isAround(950))

        val prefixedCompressedCose = cardViewModel.base64Items.find { it.title == "Prefixed Compressed COSE" }?.value
        assertThat(prefixedCompressedCose?.length, isAround(721))
        if (prefixedCompressedCose == null) throw AssertionError()
        assertPlain(prefixedCompressedCose, SampleData.vaccination)
    }

    @Test
    fun test() {
        val cardViewModel = cborViewAdapter.process(SampleData.test)

        assertThat(cardViewModel.title, equalTo("COSE"))
        assertThat(cardViewModel.base64Items.find { it.title == "CBOR (Base45)" }?.value?.length, isAround(618))
        assertThat(cardViewModel.base64Items.find { it.title == "COSE (Base45)" }?.value?.length, isAround(789))

        val prefixedCompressedCose = cardViewModel.base64Items.find { it.title == "Prefixed Compressed COSE" }?.value
        assertThat(prefixedCompressedCose?.length, isAround(700))
        if (prefixedCompressedCose == null) throw AssertionError()
        assertPlain(prefixedCompressedCose, SampleData.test)
    }

    private fun isAround(input: Int) = allOf(greaterThan(input.div(10) * 9), lessThan(input.div(10) * 11))

    private fun assertPlain(input: String, jsonInput: String) {
        val vaccinationData = cborProcessingChain.verify(input)
        val decodedFromInput = Json { isLenient = true; ignoreUnknownKeys = true }.decodeFromString<VaccinationData>(jsonInput)
        assertThat(vaccinationData, equalTo(decodedFromInput))
    }

}
