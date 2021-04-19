package ehn.techiop.hcert.kotlin

import com.google.zxing.BarcodeFormat
import ehn.techiop.hcert.kotlin.chain.*
import ehn.techiop.hcert.kotlin.chain.impl.*
import ehn.techiop.hcert.kotlin.chain.faults.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ServiceConfiguration {

    @Bean
    fun qrCodeService(): TwoDimCodeService {
        return DefaultTwoDimCodeService(350, BarcodeFormat.QR_CODE)
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
    fun cryptoServiceRsa3072(): CryptoService {
        return RandomRsa3072KeyCryptoService()
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
    fun coseRsa3072Service(cryptoServiceRsa3072: CryptoService): CoseService {
        return DefaultCoseService(cryptoServiceRsa3072)
    }

    @Bean
    fun faultyCoseService(cryptoServiceEc: CryptoService): CoseService {
        return FaultyCoseService(cryptoServiceEc)
    }

    @Bean
    fun contextIdentifierService(): ContextIdentifierService {
        return DefaultContextIdentifierService()
    }

    @Bean
    fun faultyContextIdentifierService(): ContextIdentifierService {
        return FaultyContextIdentifierService()
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
        contextIdentifierService: ContextIdentifierService,
        compressorService: CompressorService,
        base45Service: Base45Service
    ): CborProcessingChainAdapter {
        return CborProcessingChainAdapter(
            "EC Key",
            CborProcessingChain(cborService, coseEcService, contextIdentifierService, compressorService, base45Service),
            qrCodeService()
        )
    }

    @Bean
    fun cborProcessingChainRsa(
        cborService: CborService,
        coseRsaService: CoseService,
        contextIdentifierService: ContextIdentifierService,
        compressorService: CompressorService,
        base45Service: Base45Service
    ): CborProcessingChainAdapter {
        return CborProcessingChainAdapter(
            "RSA 2048 Key",
            CborProcessingChain(cborService, coseRsaService, contextIdentifierService, compressorService, base45Service),
            qrCodeService()
        )
    }

    @Bean
    fun cborProcessingChainRsa3072(
        cborService: CborService,
        coseRsa3072Service: CoseService,
        contextIdentifierService: ContextIdentifierService,
        compressorService: CompressorService,
        base45Service: Base45Service
    ): CborProcessingChainAdapter {
        return CborProcessingChainAdapter(
            "RSA 3072 Key",
            CborProcessingChain(cborService, coseRsa3072Service, contextIdentifierService, compressorService, base45Service),
            qrCodeService()
        )
    }

    @Bean
    fun cborProcessingChainFaultyCbor(
        faultyCborService: CborService,
        coseEcService: CoseService,
        contextIdentifierService: ContextIdentifierService,
        compressorService: CompressorService,
        base45Service: Base45Service
    ): CborProcessingChainAdapter {
        return CborProcessingChainAdapter(
            "Faulty CBOR",
            CborProcessingChain(faultyCborService, coseEcService, contextIdentifierService, compressorService, base45Service),
            qrCodeService()
        )
    }

    @Bean
    fun cborProcessingChainFaultyCose(
        cborService: CborService,
        faultyCoseService: CoseService,
        contextIdentifierService: ContextIdentifierService,
        compressorService: CompressorService,
        base45Service: Base45Service
    ): CborProcessingChainAdapter {
        return CborProcessingChainAdapter(
            "Faulty COSE",
            CborProcessingChain(cborService, faultyCoseService, contextIdentifierService, compressorService, base45Service),
            qrCodeService()
        )
    }

    @Bean
    fun cborProcessingChainFaultyValSuite(
        cborService: CborService,
        coseEcService: CoseService,
        faultyContextIdentifierService: ContextIdentifierService,
        compressorService: CompressorService,
        base45Service: Base45Service
    ): CborProcessingChainAdapter {
        return CborProcessingChainAdapter(
            "Faulty ValSuite",
            CborProcessingChain(cborService, coseEcService, faultyContextIdentifierService, compressorService, base45Service),
            qrCodeService()
        )
    }

    @Bean
    fun cborProcessingChainFaultyBase45(
        cborService: CborService,
        coseEcService: CoseService,
        contextIdentifierService: ContextIdentifierService,
        compressorService: CompressorService,
        faultyBase45Service: Base45Service
    ): CborProcessingChainAdapter {
        return CborProcessingChainAdapter(
            "Faulty Base45",
            CborProcessingChain(cborService, coseEcService, contextIdentifierService, compressorService, faultyBase45Service),
            qrCodeService()
        )
    }

    @Bean
    fun cborProcessingChainFaultyCompressor(
        cborService: CborService,
        coseEcService: CoseService,
        contextIdentifierService: ContextIdentifierService,
        faultyCompressorService: CompressorService,
        base45Service: Base45Service
    ): CborProcessingChainAdapter {
        return CborProcessingChainAdapter(
            "Faulty Compressor",
            CborProcessingChain(cborService, coseEcService, contextIdentifierService, faultyCompressorService, base45Service),
            qrCodeService()
        )
    }


}
