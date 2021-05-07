package ehn.techiop.hcert.kotlin

import com.google.zxing.BarcodeFormat
import ehn.techiop.hcert.kotlin.chain.Base45Service
import ehn.techiop.hcert.kotlin.chain.CborService
import ehn.techiop.hcert.kotlin.chain.Chain
import ehn.techiop.hcert.kotlin.chain.CompressorService
import ehn.techiop.hcert.kotlin.chain.ContextIdentifierService
import ehn.techiop.hcert.kotlin.chain.CoseService
import ehn.techiop.hcert.kotlin.chain.CryptoService
import ehn.techiop.hcert.kotlin.chain.TwoDimCodeService
import ehn.techiop.hcert.kotlin.chain.faults.FaultyBase45Service
import ehn.techiop.hcert.kotlin.chain.faults.FaultyCborService
import ehn.techiop.hcert.kotlin.chain.faults.FaultyCompressorService
import ehn.techiop.hcert.kotlin.chain.faults.FaultyCoseService
import ehn.techiop.hcert.kotlin.chain.faults.NonVerifiableCoseService
import ehn.techiop.hcert.kotlin.chain.faults.NoopCompressorService
import ehn.techiop.hcert.kotlin.chain.faults.NoopContextIdentifierService
import ehn.techiop.hcert.kotlin.chain.faults.UnprotectedCoseService
import ehn.techiop.hcert.kotlin.chain.impl.DefaultBase45Service
import ehn.techiop.hcert.kotlin.chain.impl.DefaultCborService
import ehn.techiop.hcert.kotlin.chain.impl.DefaultCompressorService
import ehn.techiop.hcert.kotlin.chain.impl.DefaultContextIdentifierService
import ehn.techiop.hcert.kotlin.chain.impl.DefaultCoseService
import ehn.techiop.hcert.kotlin.chain.impl.DefaultTwoDimCodeService
import ehn.techiop.hcert.kotlin.chain.impl.FileBasedCryptoService
import ehn.techiop.hcert.kotlin.chain.impl.RandomRsaKeyCryptoService
import eu.europa.ec.dgc.gateway.connector.DgcGatewayDownloadConnector
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ResourceLoader

@Configuration
@EnableConfigurationProperties(ConfigurationProperties::class)
class ServiceConfiguration {

    @Autowired
    lateinit var resourceLoader: ResourceLoader

    @Autowired
    lateinit var properties: ConfigurationProperties

    private fun loadFromResource(location: String) =
        resourceLoader.getResource(location).inputStream.readAllBytes().decodeToString()

    @Bean
    fun qrCodeService(): TwoDimCodeService {
        return DefaultTwoDimCodeService(350, BarcodeFormat.QR_CODE)
    }

    @Bean
    fun cryptoServiceTrustList(): CryptoService {
        val pemEncodedKeyPair = loadFromResource(properties.trustList.ecPrivate.toString())
        val pemEncodedCert = loadFromResource(properties.trustList.ecCert.toString())
        return FileBasedCryptoService(pemEncodedKeyPair, pemEncodedCert)
    }

    @Bean
    fun cryptoServiceEc(): CryptoService {
        val pemEncodedKeyPair = loadFromResource(properties.chain.ecPrivate.toString())
        val pemEncodedCert = loadFromResource(properties.chain.ecCert.toString())
        return FileBasedCryptoService(pemEncodedKeyPair, pemEncodedCert)
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
    fun trustListServiceAdapter(
        cryptoServiceTrustList: CryptoService,
        cryptoServiceList: Set<CryptoService>,
        downloadConnector: DgcGatewayDownloadConnector,
    ): TrustListServiceAdapter {
        return TrustListServiceAdapter(cryptoServiceTrustList, cryptoServiceList, downloadConnector)
    }

    @Bean
    fun chainEc(
        cborService: CborService,
        coseEcService: CoseService,
        contextIdentifierService: ContextIdentifierService,
        compressorService: CompressorService,
        base45Service: Base45Service
    ): ChainAdapter {
        return ChainAdapter(
            "EC 256 Key",
            Chain(cborService, coseEcService, contextIdentifierService, compressorService, base45Service),
            qrCodeService()
        )
    }

    @Bean
    fun chainRsa2048(
        cborService: CborService,
        coseRsaService: CoseService,
        contextIdentifierService: ContextIdentifierService,
        compressorService: CompressorService,
        base45Service: Base45Service
    ): ChainAdapter {
        return ChainAdapter(
            "RSA 2048 Key",
            Chain(
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
    fun chainRsa3072(
        cborService: CborService,
        contextIdentifierService: ContextIdentifierService,
        compressorService: CompressorService,
        base45Service: Base45Service
    ): ChainAdapter {
        return ChainAdapter(
            "RSA 3072 Key",
            Chain(
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
    fun chainFaultyCbor(
        coseEcService: CoseService,
        contextIdentifierService: ContextIdentifierService,
        compressorService: CompressorService,
        base45Service: Base45Service
    ): ChainAdapter {
        return ChainAdapter(
            "Faulty CBOR (expect: FAIL)",
            Chain(
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
    fun chainNonVerifiableCose(
        cborService: CborService,
        contextIdentifierService: ContextIdentifierService,
        compressorService: CompressorService,
        base45Service: Base45Service
    ): ChainAdapter {
        return ChainAdapter(
            "Faulty COSE (non-verifiable signature) (expect: FAIL)",
            Chain(
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
    fun chainUnprotectedCose(
        cborService: CborService,
        contextIdentifierService: ContextIdentifierService,
        compressorService: CompressorService,
        base45Service: Base45Service
    ): ChainAdapter {
        return ChainAdapter(
            "Unprotected COSE (KID in unprotected header) (expect: GOOD)",
            Chain(
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
    fun chainFaultyCose(
        cborService: CborService,
        contextIdentifierService: ContextIdentifierService,
        compressorService: CompressorService,
        base45Service: Base45Service
    ): ChainAdapter {
        return ChainAdapter(
            "Faulty COSE (not a valid COSE) (expect: FAIL)",
            Chain(
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
    fun chainFaultyContextIdentifier(
        cborService: CborService,
        coseEcService: CoseService,
        compressorService: CompressorService,
        base45Service: Base45Service
    ): ChainAdapter {
        return ChainAdapter(
            "Faulty ContextIdentifier (HC2:) (expect: FAIL)",
            Chain(
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
    fun chainNoopContextIdentifier(
        cborService: CborService,
        coseEcService: CoseService,
        compressorService: CompressorService,
        base45Service: Base45Service
    ): ChainAdapter {
        return ChainAdapter(
            "No-op ContextIdentifier (expect: GOOD)",
            Chain(
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
    fun chainFaultyBase45(
        cborService: CborService,
        coseEcService: CoseService,
        contextIdentifierService: ContextIdentifierService,
        compressorService: CompressorService
    ): ChainAdapter {
        return ChainAdapter(
            "Faulty Base45 (expect: FAIL)",
            Chain(
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
    fun chainFaultyCompressor(
        cborService: CborService,
        coseEcService: CoseService,
        contextIdentifierService: ContextIdentifierService,
        base45Service: Base45Service
    ): ChainAdapter {
        return ChainAdapter(
            "Faulty Compressor (expect: FAIL)",
            Chain(
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
    fun chainNoopCompressor(
        cborService: CborService,
        coseEcService: CoseService,
        contextIdentifierService: ContextIdentifierService,
        base45Service: Base45Service
    ): ChainAdapter {
        return ChainAdapter(
            "No-op Compressor (expect: GOOD)",
            Chain(
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
