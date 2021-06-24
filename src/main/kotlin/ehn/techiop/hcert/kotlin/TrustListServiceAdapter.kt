package ehn.techiop.hcert.kotlin

import ehn.techiop.hcert.kotlin.chain.CryptoService
import ehn.techiop.hcert.kotlin.chain.fromBase64
import ehn.techiop.hcert.kotlin.crypto.CertificateAdapter
import ehn.techiop.hcert.kotlin.trust.TrustListV2EncodeService
import eu.europa.ec.dgc.gateway.connector.DgcGatewayDownloadConnector
import org.springframework.scheduling.annotation.Scheduled
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

class TrustListServiceAdapter(
    signingService: CryptoService,
    cryptoServices: Set<CryptoService>,
    private val downloadConnector: DgcGatewayDownloadConnector,
    properties: ConfigurationProperties
) {
    private val certificateFactory = CertificateFactory.getInstance("X.509")
    private val trustListV2Service = TrustListV2EncodeService(signingService)
    private val internalCertificates: Set<CertificateAdapter> = cryptoServices.map { it.getCertificate() }.toSet()
    private val externalCertificates: Set<CertificateAdapter> = properties.trustListExt
        .map { certificateFactory.generateCertificate(it.openStream()) as X509Certificate }
        .map { CertificateAdapter(it) }
        .toSet()

    private fun loadGatewayCerts() = downloadConnector.trustedCertificates
        .map { it.rawData.fromBase64() }
        .map { certificateFactory.generateCertificate(it.inputStream()) as X509Certificate }
        .map { CertificateAdapter(it) }
        .toSet()

    private fun loadTrustListV2Sig() = trustListV2Service.encodeSignature(trustListV2Content)

    private fun loadTrustListV2Content() =
        trustListV2Service.encodeContent(internalCertificates + externalCertificates + loadGatewayCerts())

    var trustListV2Content: ByteArray = loadTrustListV2Content()
        private set

    var trustListV2Sig: ByteArray = loadTrustListV2Sig()
        private set

    @Scheduled(fixedDelayString = "PT5M", initialDelayString = "PT1M")
    fun generateTrustList() {
        trustListV2Content = loadTrustListV2Content()
        trustListV2Sig = loadTrustListV2Sig()
    }

}