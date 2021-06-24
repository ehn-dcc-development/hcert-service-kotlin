package ehn.techiop.hcert.kotlin

import com.google.zxing.BarcodeFormat
import ehn.techiop.hcert.kotlin.chain.Base45Service
import ehn.techiop.hcert.kotlin.chain.CborService
import ehn.techiop.hcert.kotlin.chain.Chain
import ehn.techiop.hcert.kotlin.chain.CompressorService
import ehn.techiop.hcert.kotlin.chain.ContextIdentifierService
import ehn.techiop.hcert.kotlin.chain.CoseService
import ehn.techiop.hcert.kotlin.chain.CryptoService
import ehn.techiop.hcert.kotlin.chain.CwtService
import ehn.techiop.hcert.kotlin.chain.HigherOrderValidationService
import ehn.techiop.hcert.kotlin.chain.SchemaValidationService
import ehn.techiop.hcert.kotlin.chain.TwoDimCodeService
import ehn.techiop.hcert.kotlin.chain.faults.FaultyBase45Service
import ehn.techiop.hcert.kotlin.chain.faults.FaultyCompressorService
import ehn.techiop.hcert.kotlin.chain.faults.FaultyCoseService
import ehn.techiop.hcert.kotlin.chain.faults.FaultyCwtService
import ehn.techiop.hcert.kotlin.chain.faults.NonVerifiableCoseService
import ehn.techiop.hcert.kotlin.chain.faults.NoopCompressorService
import ehn.techiop.hcert.kotlin.chain.faults.NoopContextIdentifierService
import ehn.techiop.hcert.kotlin.chain.faults.UnprotectedCoseService
import ehn.techiop.hcert.kotlin.chain.impl.DefaultBase45Service
import ehn.techiop.hcert.kotlin.chain.impl.DefaultCborService
import ehn.techiop.hcert.kotlin.chain.impl.DefaultCompressorService
import ehn.techiop.hcert.kotlin.chain.impl.DefaultContextIdentifierService
import ehn.techiop.hcert.kotlin.chain.impl.DefaultCoseService
import ehn.techiop.hcert.kotlin.chain.impl.DefaultCwtService
import ehn.techiop.hcert.kotlin.chain.impl.DefaultHigherOrderValidationService
import ehn.techiop.hcert.kotlin.chain.impl.DefaultSchemaValidationService
import ehn.techiop.hcert.kotlin.chain.impl.DefaultTwoDimCodeService
import ehn.techiop.hcert.kotlin.chain.impl.FileBasedCryptoService
import ehn.techiop.hcert.kotlin.chain.impl.RandomRsaKeyCryptoService
import eu.europa.ec.dgc.gateway.connector.DgcGatewayDownloadConnector
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ResourceLoader
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableScheduling
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
    fun higherOrderValidationService(): HigherOrderValidationService {
        return DefaultHigherOrderValidationService()
    }

    @Bean
    fun schemaValidationService(): SchemaValidationService {
        return DefaultSchemaValidationService()
    }

    @Bean
    fun cborService(): CborService {
        return DefaultCborService()
    }

    @Bean
    fun cwtService(): CwtService {
        return DefaultCwtService()
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
        return TrustListServiceAdapter(cryptoServiceTrustList, cryptoServiceList, downloadConnector, properties)
    }

    @Bean
    fun chainEc(
        higherOrderValidationService: HigherOrderValidationService,
        schemaValidationService: SchemaValidationService,
        cborService: CborService,
        cwtService: CwtService,
        coseEcService: CoseService,
        contextIdentifierService: ContextIdentifierService,
        compressorService: CompressorService,
        base45Service: Base45Service
    ): ChainAdapter {
        return ChainAdapter(
            "EC 256 Key",
            Chain(
                higherOrderValidationService,
                schemaValidationService,
                cborService,
                cwtService,
                coseEcService,
                compressorService,
                base45Service,
                contextIdentifierService
            ),
            qrCodeService()
        )
    }

    @Bean
    fun chainRsa2048(
        higherOrderValidationService: HigherOrderValidationService,
        schemaValidationService: SchemaValidationService,
        cborService: CborService,
        cwtService: CwtService,
        coseRsaService: CoseService,
        contextIdentifierService: ContextIdentifierService,
        compressorService: CompressorService,
        base45Service: Base45Service
    ): ChainAdapter {
        return ChainAdapter(
            "RSA 2048 Key",
            Chain(
                higherOrderValidationService,
                schemaValidationService,
                cborService,
                cwtService,
                coseRsaService,
                compressorService,
                base45Service,
                contextIdentifierService
            ),
            qrCodeService()
        )
    }

    @Bean
    fun chainRsa3072(
        higherOrderValidationService: HigherOrderValidationService,
        schemaValidationService: SchemaValidationService,
        cborService: CborService,
        cwtService: CwtService,
        contextIdentifierService: ContextIdentifierService,
        compressorService: CompressorService,
        base45Service: Base45Service
    ): ChainAdapter {
        return ChainAdapter(
            "RSA 3072 Key",
            Chain(
                higherOrderValidationService,
                schemaValidationService,
                cborService,
                cwtService,
                DefaultCoseService(cryptoServiceRsa3072()),
                compressorService,
                base45Service,
                contextIdentifierService
            ),
            qrCodeService()
        )
    }

    @Bean
    fun chainFaultyCbor(
        higherOrderValidationService: HigherOrderValidationService,
        schemaValidationService: SchemaValidationService,
        cborService: CborService,
        coseEcService: CoseService,
        contextIdentifierService: ContextIdentifierService,
        compressorService: CompressorService,
        base45Service: Base45Service
    ): ChainAdapter {
        return ChainAdapter(
            "Faulty CWT (expect: FAIL)",
            Chain(
                higherOrderValidationService,
                schemaValidationService,
                cborService,
                FaultyCwtService(),
                coseEcService,
                compressorService,
                base45Service,
                contextIdentifierService,
            ),
            qrCodeService()
        )
    }

    @Bean
    fun chainNonVerifiableCose(
        higherOrderValidationService: HigherOrderValidationService,
        schemaValidationService: SchemaValidationService,
        cborService: CborService,
        cwtService: CwtService,
        contextIdentifierService: ContextIdentifierService,
        compressorService: CompressorService,
        base45Service: Base45Service
    ): ChainAdapter {
        return ChainAdapter(
            "Faulty COSE (non-verifiable signature) (expect: FAIL)",
            Chain(
                higherOrderValidationService,
                schemaValidationService,
                cborService,
                cwtService,
                NonVerifiableCoseService(cryptoServiceEc()),
                compressorService,
                base45Service,
                contextIdentifierService
            ),
            qrCodeService()
        )
    }

    @Bean
    fun chainUnprotectedCose(
        higherOrderValidationService: HigherOrderValidationService,
        schemaValidationService: SchemaValidationService,
        cborService: CborService,
        cwtService: CwtService,
        contextIdentifierService: ContextIdentifierService,
        compressorService: CompressorService,
        base45Service: Base45Service
    ): ChainAdapter {
        return ChainAdapter(
            "Unprotected COSE (KID in unprotected header) (expect: GOOD)",
            Chain(
                higherOrderValidationService,
                schemaValidationService,
                cborService,
                cwtService,
                UnprotectedCoseService(cryptoServiceEc()),
                compressorService,
                base45Service,
                contextIdentifierService
            ),
            qrCodeService()
        )
    }

    @Bean
    fun chainFaultyCose(
        higherOrderValidationService: HigherOrderValidationService,
        schemaValidationService: SchemaValidationService,
        cborService: CborService,
        cwtService: CwtService,
        contextIdentifierService: ContextIdentifierService,
        compressorService: CompressorService,
        base45Service: Base45Service
    ): ChainAdapter {
        return ChainAdapter(
            "Faulty COSE (not a valid COSE) (expect: FAIL)",
            Chain(
                higherOrderValidationService,
                schemaValidationService,
                cborService,
                cwtService,
                FaultyCoseService(cryptoServiceEc()),
                compressorService,
                base45Service,
                contextIdentifierService
            ),
            qrCodeService()
        )
    }

    @Bean
    fun chainFaultyContextIdentifier(
        higherOrderValidationService: HigherOrderValidationService,
        schemaValidationService: SchemaValidationService,
        cborService: CborService,
        cwtService: CwtService,
        coseEcService: CoseService,
        compressorService: CompressorService,
        base45Service: Base45Service
    ): ChainAdapter {
        return ChainAdapter(
            "Faulty ContextIdentifier (HC2:) (expect: FAIL)",
            Chain(
                higherOrderValidationService,
                schemaValidationService,
                cborService,
                cwtService,
                coseEcService,
                compressorService,
                base45Service,
                DefaultContextIdentifierService("HC2:")
            ),
            qrCodeService()
        )
    }

    @Bean
    fun chainNoopContextIdentifier(
        higherOrderValidationService: HigherOrderValidationService,
        schemaValidationService: SchemaValidationService,
        cborService: CborService,
        cwtService: CwtService,
        coseEcService: CoseService,
        compressorService: CompressorService,
        base45Service: Base45Service
    ): ChainAdapter {
        return ChainAdapter(
            "No-op ContextIdentifier (expect: GOOD)",
            Chain(
                higherOrderValidationService,
                schemaValidationService,
                cborService,
                cwtService,
                coseEcService,
                compressorService,
                base45Service,
                NoopContextIdentifierService()
            ),
            qrCodeService()
        )
    }

    @Bean
    fun chainFaultyBase45(
        higherOrderValidationService: HigherOrderValidationService,
        schemaValidationService: SchemaValidationService,
        cborService: CborService,
        cwtService: CwtService,
        coseEcService: CoseService,
        contextIdentifierService: ContextIdentifierService,
        compressorService: CompressorService
    ): ChainAdapter {
        return ChainAdapter(
            "Faulty Base45 (expect: FAIL)",
            Chain(
                higherOrderValidationService,
                schemaValidationService,
                cborService,
                cwtService,
                coseEcService,
                compressorService,
                FaultyBase45Service(),
                contextIdentifierService
            ),
            qrCodeService()
        )
    }

    @Bean
    fun chainFaultyCompressor(
        higherOrderValidationService: HigherOrderValidationService,
        schemaValidationService: SchemaValidationService,
        cborService: CborService,
        cwtService: CwtService,
        coseEcService: CoseService,
        contextIdentifierService: ContextIdentifierService,
        base45Service: Base45Service
    ): ChainAdapter {
        return ChainAdapter(
            "Faulty Compressor (expect: FAIL)",
            Chain(
                higherOrderValidationService,
                schemaValidationService,
                cborService,
                cwtService,
                coseEcService,
                FaultyCompressorService(),
                base45Service,
                contextIdentifierService,
            ),
            qrCodeService()
        )
    }

    @Bean
    fun chainNoopCompressor(
        higherOrderValidationService: HigherOrderValidationService,
        schemaValidationService: SchemaValidationService,
        cborService: CborService,
        cwtService: CwtService,
        coseEcService: CoseService,
        contextIdentifierService: ContextIdentifierService,
        base45Service: Base45Service
    ): ChainAdapter {
        return ChainAdapter(
            "No-op Compressor (expect: FAIL)",
            Chain(
                higherOrderValidationService,
                schemaValidationService,
                cborService,
                cwtService,
                coseEcService,
                NoopCompressorService(),
                base45Service,
                contextIdentifierService
            ),
            qrCodeService()
        )
    }


}
