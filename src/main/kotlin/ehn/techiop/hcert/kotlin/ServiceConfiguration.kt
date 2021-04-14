package ehn.techiop.hcert.kotlin

import com.google.zxing.BarcodeFormat
import ehn.techiop.hcert.kotlin.chain.Base45Service
import ehn.techiop.hcert.kotlin.chain.CborProcessingChain
import ehn.techiop.hcert.kotlin.chain.CborService
import ehn.techiop.hcert.kotlin.chain.CompressorService
import ehn.techiop.hcert.kotlin.chain.CoseService
import ehn.techiop.hcert.kotlin.chain.CryptoService
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
import ehn.techiop.hcert.kotlin.chain.RandomRsaKeyCryptoService
import ehn.techiop.hcert.kotlin.chain.TwoDimCodeService
import ehn.techiop.hcert.kotlin.chain.ValSuiteService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ServiceConfiguration {

    @Bean
    fun qrCodeService(): TwoDimCodeService {
        return TwoDimCodeService(350, BarcodeFormat.QR_CODE)
    }

    @Bean
    fun cryptoServiceEc(): CryptoService {
        return RandomEcKeyCryptoService()
    }

    @Bean
    fun cryptoServiceRsa(): CryptoService {
        return RandomRsaKeyCryptoService()
    }

    @Bean
    fun cborService(): CborService {
        return DefaultCborService()
    }

    @Bean
    fun faultyCborService(): CborService {
        return FaultyCborService()
    }

    @Bean
    fun coseEcService(cryptoServiceEc: CryptoService): CoseService {
        return DefaultCoseService(cryptoServiceEc)
    }

    @Bean
    fun coseRsaService(cryptoServiceRsa: CryptoService): CoseService {
        return DefaultCoseService(cryptoServiceRsa)
    }

    @Bean
    fun faultyCoseService(cryptoServiceEc: CryptoService): CoseService {
        return FaultyCoseService(cryptoServiceEc)
    }

    @Bean
    fun valSuiteService(): ValSuiteService {
        return DefaultValSuiteService()
    }

    @Bean
    fun faultyValSuiteService(): ValSuiteService {
        return FaultyValSuiteService()
    }

    @Bean
    fun compressorService(): CompressorService {
        return DefaultCompressorService()
    }

    @Bean
    fun faultyCompressorService(): CompressorService {
        return FaultyCompressorService()
    }

    @Bean
    fun base45Service(): Base45Service {
        return DefaultBase45Service()
    }

    @Bean
    fun faultyBase45Service(): Base45Service {
        return FaultyBase45Service()
    }

    @Bean
    fun cborProcessingChainEc(
        cborService: CborService,
        coseEcService: CoseService,
        valSuiteService: ValSuiteService,
        compressorService: CompressorService,
        base45Service: Base45Service
    ): CborProcessingChainAdapter {
        return CborProcessingChainAdapter(
            "EC Key",
            CborProcessingChain(cborService, coseEcService, valSuiteService, compressorService, base45Service),
            qrCodeService()
        )
    }

    @Bean
    fun cborProcessingChainRsa(
        cborService: CborService,
        coseRsaService: CoseService,
        valSuiteService: ValSuiteService,
        compressorService: CompressorService,
        base45Service: Base45Service
    ): CborProcessingChainAdapter {
        return CborProcessingChainAdapter(
            "RSA Key",
            CborProcessingChain(cborService, coseRsaService, valSuiteService, compressorService, base45Service),
            qrCodeService()
        )
    }

    @Bean
    fun cborProcessingChainFaultyCbor(
        faultyCborService: CborService,
        coseEcService: CoseService,
        valSuiteService: ValSuiteService,
        compressorService: CompressorService,
        base45Service: Base45Service
    ): CborProcessingChainAdapter {
        return CborProcessingChainAdapter(
            "Faulty CBOR",
            CborProcessingChain(faultyCborService, coseEcService, valSuiteService, compressorService, base45Service),
            qrCodeService()
        )
    }

    @Bean
    fun cborProcessingChainFaultyCose(
        cborService: CborService,
        faultyCoseService: CoseService,
        valSuiteService: ValSuiteService,
        compressorService: CompressorService,
        base45Service: Base45Service
    ): CborProcessingChainAdapter {
        return CborProcessingChainAdapter(
            "Faulty COSE",
            CborProcessingChain(cborService, faultyCoseService, valSuiteService, compressorService, base45Service),
            qrCodeService()
        )
    }

    @Bean
    fun cborProcessingChainFaultyValSuite(
        cborService: CborService,
        coseEcService: CoseService,
        faultyValSuiteService: ValSuiteService,
        compressorService: CompressorService,
        base45Service: Base45Service
    ): CborProcessingChainAdapter {
        return CborProcessingChainAdapter(
            "Faulty ValSuite",
            CborProcessingChain(cborService, coseEcService, faultyValSuiteService, compressorService, base45Service),
            qrCodeService()
        )
    }

    @Bean
    fun cborProcessingChainFaultyBase45(
        cborService: CborService,
        coseEcService: CoseService,
        valSuiteService: ValSuiteService,
        compressorService: CompressorService,
        faultyBase45Service: Base45Service
    ): CborProcessingChainAdapter {
        return CborProcessingChainAdapter(
            "Faulty Base45",
            CborProcessingChain(cborService, coseEcService, valSuiteService, compressorService, faultyBase45Service),
            qrCodeService()
        )
    }

    @Bean
    fun cborProcessingChainFaultyCompressor(
        cborService: CborService,
        coseEcService: CoseService,
        valSuiteService: ValSuiteService,
        faultyCompressorService: CompressorService,
        base45Service: Base45Service
    ): CborProcessingChainAdapter {
        return CborProcessingChainAdapter(
            "Faulty Compressor",
            CborProcessingChain(cborService, coseEcService, valSuiteService, faultyCompressorService, base45Service),
            qrCodeService()
        )
    }


}
