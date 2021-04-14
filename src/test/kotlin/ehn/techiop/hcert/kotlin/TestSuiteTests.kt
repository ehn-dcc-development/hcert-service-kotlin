package ehn.techiop.hcert.kotlin

import com.google.zxing.BarcodeFormat
import ehn.techiop.hcert.kotlin.chain.CborProcessingChain
import ehn.techiop.hcert.kotlin.chain.DefaultBase45Service
import ehn.techiop.hcert.kotlin.chain.DefaultCborService
import ehn.techiop.hcert.kotlin.chain.DefaultCompressorService
import ehn.techiop.hcert.kotlin.chain.DefaultCoseService
import ehn.techiop.hcert.kotlin.chain.DefaultValSuiteService
import ehn.techiop.hcert.kotlin.chain.FaultyBase45Service
import ehn.techiop.hcert.kotlin.chain.FaultyCborService
import ehn.techiop.hcert.kotlin.chain.FaultyCompressorService
import ehn.techiop.hcert.kotlin.chain.FaultyCoseService
import ehn.techiop.hcert.kotlin.chain.FaultyValSuiteService
import ehn.techiop.hcert.kotlin.chain.RandomEcKeyCryptoService
import ehn.techiop.hcert.kotlin.chain.SampleData
import ehn.techiop.hcert.kotlin.chain.TwoDimCodeService
import ehn.techiop.hcert.kotlin.chain.VaccinationData
import ehn.techiop.hcert.kotlin.chain.VerificationResult
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.not
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.Matchers.lessThan
import org.junit.jupiter.api.Test

class TestSuiteTests {

    private val title = "Test"
    private val titleCorrect = "Correct"
    private val titleFaultyBase45 = "FaultyBase45"
    private val titleFaultyCompressor = "FaultyCompressor"
    private val titleFaultyValSuite = "FaultyValSuite"
    private val titleFaultyCose = "FaultyCose"
    private val titleFaultyCbor = "FaultyCbor"
    private val qrCodeService = TwoDimCodeService(350, BarcodeFormat.QR_CODE)
    private val cryptoService = RandomEcKeyCryptoService()
    private val cborService = DefaultCborService()
    private val coseService = DefaultCoseService(cryptoService)
    private val valSuiteService = DefaultValSuiteService()
    private val compressorService = DefaultCompressorService()
    private val base45Service = DefaultBase45Service()
    private val chainCorrect =
        CborProcessingChain(cborService, coseService, valSuiteService, compressorService, base45Service)
    private val adapterCorrect = CborProcessingChainAdapter(titleCorrect, chainCorrect, qrCodeService)
    private val adapterFaultyBase45 =
        CborProcessingChainAdapter(
            titleFaultyBase45,
            CborProcessingChain(cborService, coseService, valSuiteService, compressorService, FaultyBase45Service()),
            qrCodeService
        )
    private val adapterFaultyCompressor =
        CborProcessingChainAdapter(
            titleFaultyCompressor,
            CborProcessingChain(cborService, coseService, valSuiteService, FaultyCompressorService(), base45Service),
            qrCodeService
        )
    private val adapterFaultyValSuite =
        CborProcessingChainAdapter(
            titleFaultyValSuite,
            CborProcessingChain(cborService, coseService, FaultyValSuiteService(), compressorService, base45Service),
            qrCodeService
        )
    private val adapterFaultyCose =
        CborProcessingChainAdapter(
            titleFaultyCose,
            CborProcessingChain(
                cborService,
                FaultyCoseService(cryptoService),
                valSuiteService,
                compressorService,
                base45Service
            ),
            qrCodeService
        )
    private val adapterFaultyCbor =
        CborProcessingChainAdapter(
            titleFaultyCbor,
            CborProcessingChain(FaultyCborService(), coseService, valSuiteService, compressorService, base45Service),
            qrCodeService
        )
    private val cborViewAdapter = CborViewAdapter(
        setOf(
            adapterCorrect,
            adapterFaultyBase45,
            adapterFaultyCompressor,
            adapterFaultyValSuite,
            adapterFaultyCose,
            adapterFaultyCbor
        )
    )

    @Test
    fun vaccination() {
        val input = SampleData.vaccination
        val cardViewModels = cborViewAdapter.process(title, input)

        assertVerification(
            loadQrCodeContent(cardViewModels, titleCorrect),
            input,
            true,
            VerificationResult().apply {
                valSuitePrefix = "HC1"; base45Decoded = true; zlibDecoded = true; cborDecoded = true; coseVerified =
                true
            })
        assertVerification(
            loadQrCodeContent(cardViewModels, titleFaultyBase45),
            input,
            false,
            VerificationResult().apply { valSuitePrefix = "HC1" })
        assertVerification(
            loadQrCodeContent(cardViewModels, titleFaultyValSuite),
            input,
            true,
            VerificationResult().apply {
                valSuitePrefix = null; base45Decoded = true; zlibDecoded = true; cborDecoded = true; coseVerified = true
            })
        assertVerification(
            loadQrCodeContent(cardViewModels, titleFaultyCompressor),
            input,
            true,
            VerificationResult().apply {
                valSuitePrefix = "HC1"; base45Decoded = true; zlibDecoded = true; cborDecoded = true; coseVerified =
                true
            })
        assertVerification(
            loadQrCodeContent(cardViewModels, titleFaultyCose),
            input,
            true,
            VerificationResult().apply {
                valSuitePrefix = "HC1"; base45Decoded = true; zlibDecoded = true; cborDecoded = true
            })
        assertVerification(
            loadQrCodeContent(cardViewModels, titleFaultyCbor),
            input,
            false,
            VerificationResult().apply {
                valSuitePrefix = "HC1"; base45Decoded = true; zlibDecoded = true; coseVerified = true
            })
    }

    private fun loadQrCodeContent(cardViewModels: List<CardViewModel>, title: String): String {
        val cvm = cardViewModels.first { it.title.endsWith(title) }
        return cvm.base64Items.find { it.title.startsWith("Prefixed Compressed COSE") }?.value
            ?: throw AssertionError()
    }

    private fun isAround(input: Int) = allOf(greaterThan(input.div(10) * 9), lessThan(input.div(10) * 11))

    private fun assertSuccess(input: String, jsonInput: String) {
        val verificationResult = VerificationResult()
        val vaccinationData = chainCorrect.verify(input, verificationResult)
        val decodedFromInput =
            Json { isLenient = true; ignoreUnknownKeys = true }.decodeFromString<VaccinationData>(jsonInput)
        assertThat(vaccinationData, equalTo(decodedFromInput))
        assertThat(verificationResult.base45Decoded, equalTo(true))
        assertThat(verificationResult.cborDecoded, equalTo(true))
        //assertThat(verificationResult.coseVerified, equalTo(true))
        assertThat(verificationResult.zlibDecoded, equalTo(true))
        assertThat(verificationResult.valSuitePrefix, notNullValue())
    }

    private fun assertVerification(
        input: String,
        jsonInput: String,
        expectData: Boolean,
        expectedResult: VerificationResult
    ) {
        val verificationResult = VerificationResult()
        val vaccinationData = chainCorrect.verify(input, verificationResult)
        val decodedFromInput =
            Json { isLenient = true; ignoreUnknownKeys = true }.decodeFromString<VaccinationData>(jsonInput)
        assertThat(verificationResult.base45Decoded, equalTo(expectedResult.base45Decoded))
        assertThat(verificationResult.cborDecoded, equalTo(expectedResult.cborDecoded))
        assertThat(verificationResult.coseVerified, equalTo(expectedResult.coseVerified))
        assertThat(verificationResult.zlibDecoded, equalTo(expectedResult.zlibDecoded))
        assertThat(verificationResult.valSuitePrefix, equalTo(expectedResult.valSuitePrefix))
        if (expectData) {
            assertThat(vaccinationData, equalTo(decodedFromInput))
        } else {
            assertThat(vaccinationData, not(equalTo(decodedFromInput)))
        }
    }

}
