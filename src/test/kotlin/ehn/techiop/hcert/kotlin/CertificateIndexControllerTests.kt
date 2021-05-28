package ehn.techiop.hcert.kotlin

import ehn.techiop.hcert.kotlin.chain.CryptoService
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

@SpringBootTest
@AutoConfigureMockMvc
class CertificateIndexControllerTests {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var cryptoServiceTrustList: CryptoService

    private val URL_PREFIX = "/cert"

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


