package ehn.techiop.hcert.kotlin

import com.google.zxing.BarcodeFormat
import ehn.techiop.hcert.kotlin.chain.*
import ehn.techiop.hcert.kotlin.chain.faults.*
import ehn.techiop.hcert.kotlin.chain.impl.*
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
        return RandomRsaKeyCryptoService(3072)
    }

    @Bean
    fun cborService(): CborService {
        return DefaultCborService()
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
    fun contextIdentifierService(): ContextIdentifierService {
        return DefaultContextIdentifierService()
    }

    @Bean
    fun compressorService(): CompressorService {
        return DefaultCompressorService()
    }

    @Bean
    fun base45Service(): Base45Service {
        return DefaultBase45Service()
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
            "EC 256 Key",
            CborProcessingChain(cborService, coseEcService, contextIdentifierService, compressorService, base45Service),
            qrCodeService()
        )
    }

    @Bean
    fun cborProcessingChainRsa2048(
        cborService: CborService,
        coseRsaService: CoseService,
        contextIdentifierService: ContextIdentifierService,
        compressorService: CompressorService,
        base45Service: Base45Service
    ): CborProcessingChainAdapter {
        return CborProcessingChainAdapter(
            "RSA 2048 Key",
            CborProcessingChain(
                cborService,
                coseRsaService,
                contextIdentifierService,
                compressorService,
                base45Service
            ),
            qrCodeService()
        )
    }

    @Bean
    fun cborProcessingChainRsa3072(
        cborService: CborService,
        contextIdentifierService: ContextIdentifierService,
        compressorService: CompressorService,
        base45Service: Base45Service
    ): CborProcessingChainAdapter {
        return CborProcessingChainAdapter(
            "RSA 3072 Key",
            CborProcessingChain(
                cborService,
                DefaultCoseService(cryptoServiceRsa3072()),
                contextIdentifierService,
                compressorService,
                base45Service
            ),
            qrCodeService()
        )
    }

    @Bean
    fun cborProcessingChainFaultyCbor(
        coseEcService: CoseService,
        contextIdentifierService: ContextIdentifierService,
        compressorService: CompressorService,
        base45Service: Base45Service
    ): CborProcessingChainAdapter {
        return CborProcessingChainAdapter(
            "Faulty CBOR (expect: FAIL)",
            CborProcessingChain(
                FaultyCborService(),
                coseEcService,
                contextIdentifierService,
                compressorService,
                base45Service
            ),
            qrCodeService()
        )
    }

    @Bean
    fun cborProcessingChainNonVerifiableCose(
        cborService: CborService,
        contextIdentifierService: ContextIdentifierService,
        compressorService: CompressorService,
        base45Service: Base45Service
    ): CborProcessingChainAdapter {
        return CborProcessingChainAdapter(
            "Faulty COSE (non-verifiable signature) (expect: FAIL)",
            CborProcessingChain(
                cborService,
                NonVerifiableCoseService(cryptoServiceEc()),
                contextIdentifierService,
                compressorService,
                base45Service
            ),
            qrCodeService()
        )
    }

    @Bean
    fun cborProcessingChainUnprotectedCose(
        cborService: CborService,
        contextIdentifierService: ContextIdentifierService,
        compressorService: CompressorService,
        base45Service: Base45Service
    ): CborProcessingChainAdapter {
        return CborProcessingChainAdapter(
            "Unprotected COSE (KID in unprotected header) (expect: GOOD)",
            CborProcessingChain(
                cborService,
                UnprotectedCoseService(cryptoServiceEc()),
                contextIdentifierService,
                compressorService,
                base45Service
            ),
            qrCodeService()
        )
    }

    @Bean
    fun cborProcessingChainFaultyCose(
        cborService: CborService,
        contextIdentifierService: ContextIdentifierService,
        compressorService: CompressorService,
        base45Service: Base45Service
    ): CborProcessingChainAdapter {
        return CborProcessingChainAdapter(
            "Faulty COSE (not a valid COSE) (expect: FAIL)",
            CborProcessingChain(
                cborService,
                FaultyCoseService(cryptoServiceEc()),
                contextIdentifierService,
                compressorService,
                base45Service
            ),
            qrCodeService()
        )
    }

    @Bean
    fun cborProcessingChainFaultyContextIdentifier(
        cborService: CborService,
        coseEcService: CoseService,
        compressorService: CompressorService,
        base45Service: Base45Service
    ): CborProcessingChainAdapter {
        return CborProcessingChainAdapter(
            "Faulty ContextIdentifier (HC2:) (expect: FAIL)",
            CborProcessingChain(
                cborService,
                coseEcService,
                DefaultContextIdentifierService("HC2:"),
                compressorService,
                base45Service
            ),
            qrCodeService()
        )
    }

    @Bean
    fun cborProcessingChainNoopContextIdentifier(
        cborService: CborService,
        coseEcService: CoseService,
        compressorService: CompressorService,
        base45Service: Base45Service
    ): CborProcessingChainAdapter {
        return CborProcessingChainAdapter(
            "No-op ContextIdentifier (expect: GOOD)",
            CborProcessingChain(
                cborService,
                coseEcService,
                NoopContextIdentifierService(),
                compressorService,
                base45Service
            ),
            qrCodeService()
        )
    }

    @Bean
    fun cborProcessingChainFaultyBase45(
        cborService: CborService,
        coseEcService: CoseService,
        contextIdentifierService: ContextIdentifierService,
        compressorService: CompressorService
    ): CborProcessingChainAdapter {
        return CborProcessingChainAdapter(
            "Faulty Base45 (expect: FAIL)",
            CborProcessingChain(
                cborService,
                coseEcService,
                contextIdentifierService,
                compressorService,
                FaultyBase45Service()
            ),
            qrCodeService()
        )
    }

    @Bean
    fun cborProcessingChainFaultyCompressor(
        cborService: CborService,
        coseEcService: CoseService,
        contextIdentifierService: ContextIdentifierService,
        base45Service: Base45Service
    ): CborProcessingChainAdapter {
        return CborProcessingChainAdapter(
            "Faulty Compressor (expect: FAIL)",
            CborProcessingChain(
                cborService,
                coseEcService,
                contextIdentifierService,
                FaultyCompressorService(),
                base45Service
            ),
            qrCodeService()
        )
    }

    @Bean
    fun cborProcessingChainNoopCompressor(
        cborService: CborService,
        coseEcService: CoseService,
        contextIdentifierService: ContextIdentifierService,
        base45Service: Base45Service
    ): CborProcessingChainAdapter {
        return CborProcessingChainAdapter(
            "No-op Compressor (expect: GOOD)",
            CborProcessingChain(
                cborService,
                coseEcService,
                contextIdentifierService,
                NoopCompressorService(),
                base45Service
            ),
            qrCodeService()
        )
    }


}
