package ehn.techiop.hcert.kotlin

import com.google.zxing.BarcodeFormat
import ehn.techiop.hcert.kotlin.chain.CborProcessingChain
import ehn.techiop.hcert.kotlin.chain.SampleData
import ehn.techiop.hcert.kotlin.chain.VaccinationData
import ehn.techiop.hcert.kotlin.chain.VerificationResult
import ehn.techiop.hcert.kotlin.chain.faults.FaultyBase45Service
import ehn.techiop.hcert.kotlin.chain.faults.FaultyCborService
import ehn.techiop.hcert.kotlin.chain.faults.FaultyCompressorService
import ehn.techiop.hcert.kotlin.chain.faults.FaultyCoseService
import ehn.techiop.hcert.kotlin.chain.faults.FaultyContextIdentifierService
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
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

class TestSuiteTests {

    private val title = "Test"
    private val titleCorrect = "Correct"
    private val titleFaultyBase45 = "FaultyBase45"
    private val titleFaultyCompressor = "FaultyCompressor"
    private val titleFaultyContextIdentifier = "FaultyContextIdentifier"
    private val titleFaultyCose = "FaultyCose"
    private val titleFaultyCbor = "FaultyCbor"
    private val qrCodeService = DefaultTwoDimCodeService(350, BarcodeFormat.QR_CODE)
    private val cryptoService = RandomEcKeyCryptoService()
    private val cborService = DefaultCborService()
    private val coseService = DefaultCoseService(cryptoService)
    private val valSuiteService = DefaultContextIdentifierService()
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
    private val adapterFaultyContextIdentifier =
        CborProcessingChainAdapter(
            titleFaultyContextIdentifier,
            CborProcessingChain(cborService, coseService, FaultyContextIdentifierService(), compressorService, base45Service),
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
            adapterFaultyContextIdentifier,
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
                contextIdentifier = "HC1:"; base45Decoded = true; zlibDecoded = true; cborDecoded = true; coseVerified =
                true
            })
        assertVerification(
            loadQrCodeContent(cardViewModels, titleFaultyBase45),
            input,
            false,
            VerificationResult().apply { contextIdentifier = "HC1:" })
        assertVerification(
            loadQrCodeContent(cardViewModels, titleFaultyContextIdentifier),
            input,
            true,
            VerificationResult().apply {
                contextIdentifier = null; base45Decoded = true; zlibDecoded = true; cborDecoded = true; coseVerified = true
            })
        assertVerification(
            loadQrCodeContent(cardViewModels, titleFaultyCompressor),
            input,
            true,
            VerificationResult().apply {
                contextIdentifier = "HC1:"; base45Decoded = true; zlibDecoded = true; cborDecoded = true; coseVerified =
                true
            })
        assertVerification(
            loadQrCodeContent(cardViewModels, titleFaultyCose),
            input,
            true,
            VerificationResult().apply {
                contextIdentifier = "HC1:"; base45Decoded = true; zlibDecoded = true; cborDecoded = true
            })
        assertVerification(
            loadQrCodeContent(cardViewModels, titleFaultyCbor),
            input,
            false,
            VerificationResult().apply {
                contextIdentifier = "HC1:"; base45Decoded = true; zlibDecoded = true; coseVerified = true
            })
    }

    private fun loadQrCodeContent(cardViewModels: List<CardViewModel>, title: String): String {
        val cvm = cardViewModels.first { it.title.endsWith(title) }
        return cvm.base64Items.find { it.title.startsWith("Prefixed Compressed COSE") }?.value
            ?: throw AssertionError()
    }

    private fun assertVerification(
        chainOutput: String,
        jsonInput: String,
        expectDataToMatch: Boolean,
        expectedResult: VerificationResult
    ) {
        val verificationResult = VerificationResult()
        val vaccinationData = chainCorrect.verify(chainOutput, verificationResult)
        val decodedFromInput =
            Json { isLenient = true; ignoreUnknownKeys = true }.decodeFromString<VaccinationData>(jsonInput)
        assertThat(verificationResult.base45Decoded, equalTo(expectedResult.base45Decoded))
        assertThat(verificationResult.cborDecoded, equalTo(expectedResult.cborDecoded))
        assertThat(verificationResult.coseVerified, equalTo(expectedResult.coseVerified))
        assertThat(verificationResult.zlibDecoded, equalTo(expectedResult.zlibDecoded))
        assertThat(verificationResult.contextIdentifier, equalTo(expectedResult.contextIdentifier))
        if (expectDataToMatch) {
            assertThat(vaccinationData, equalTo(decodedFromInput))
        } else {
            assertThat(vaccinationData, not(equalTo(decodedFromInput)))
        }
    }

}
