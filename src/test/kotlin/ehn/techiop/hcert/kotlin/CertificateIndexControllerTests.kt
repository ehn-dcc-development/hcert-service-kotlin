package ehn.techiop.hcert.kotlin

import COSE.HeaderKeys
import ehn.techiop.hcert.kotlin.chain.CryptoService
import ehn.techiop.hcert.kotlin.chain.fromBase64
import ehn.techiop.hcert.kotlin.chain.impl.PrefilledCertificateRepository
import ehn.techiop.hcert.kotlin.chain.impl.TrustListCertificateRepository
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.security.cert.CertificateFactory
import java.util.Base64

@SpringBootTest
@AutoConfigureMockMvc
class CertificateIndexControllerTests {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var cryptoServiceEc: CryptoService

    @Autowired
    lateinit var cryptoServiceTrustList: CryptoService

    private val URL_PREFIX = "/cert"

    @Test
    fun certificateAsText() {
        val kid = cryptoServiceEc.getCborHeaders().first { it.first == HeaderKeys.KID }.second.GetByteString()
        val key = Base64.getUrlEncoder().encodeToString(kid)
        val certificate = mockMvc.get("$URL_PREFIX/$key") {
            accept(MediaType.TEXT_PLAIN)
        }.andExpect {
            status { isOk() }
            content { contentType("${MediaType.TEXT_PLAIN};charset=UTF-8") }
        }.andReturn().response.contentAsString

        val parsedCertificate =
            CertificateFactory.getInstance("X.509").generateCertificate(certificate.fromBase64().inputStream())

        assertNotNull(parsedCertificate)
    }

    @Test
    fun certificateAsBinary() {
        val kid = cryptoServiceEc.getCborHeaders().first { it.first == HeaderKeys.KID }.second.GetByteString()
        val key = Base64.getUrlEncoder().encodeToString(kid)
        val certificate = mockMvc.get("$URL_PREFIX/$key") {
            accept(MediaType.APPLICATION_OCTET_STREAM)
        }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_OCTET_STREAM) }
        }.andReturn().response.contentAsByteArray

        val parsedCertificate =
            CertificateFactory.getInstance("X.509").generateCertificate(certificate.inputStream())

        assertNotNull(parsedCertificate)
    }

    @Test
    fun trustListV2() {
        val trustListContent = mockMvc.get("$URL_PREFIX/listv2") {
            accept(MediaType.APPLICATION_OCTET_STREAM)
        }.andExpect {
            status { isOk() }
        }.andReturn().response.contentAsByteArray

        val trustListSignature = mockMvc.get("$URL_PREFIX/sigv2") {
            accept(MediaType.APPLICATION_OCTET_STREAM)
        }.andExpect {
            status { isOk() }
        }.andReturn().response.contentAsByteArray

        val repository = TrustListCertificateRepository(
            trustListSignature,
            trustListContent,
            PrefilledCertificateRepository(cryptoServiceTrustList.getCertificate())
        )

        assertNotNull(repository)
    }
}


