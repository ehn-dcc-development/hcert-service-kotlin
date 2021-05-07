package ehn.techiop.hcert.kotlin

import ehn.techiop.hcert.kotlin.chain.CryptoService
import ehn.techiop.hcert.kotlin.chain.SampleData
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam


@Controller
class CertificateGenerationController(
    private val cborViewAdapter: CborViewAdapter,
    private val chainEc: ChainAdapter,
    private val cryptoServiceEc: CryptoService,
    private val cryptoServiceTrustList: CryptoService
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    @GetMapping("/")
    fun index(model: Model): String {
        log.info("index called")
        model.addAttribute("pastInfectedJson", SampleData.recovery)
        model.addAttribute("vaccinatedJson", SampleData.vaccination)
        model.addAttribute("testedNaaJson", SampleData.testNaa)
        model.addAttribute("testedRatJson", SampleData.testRat)
        model.addAttribute("ecCertificatePem", cryptoServiceEc.exportCertificateAsPem())
        model.addAttribute("trustlistCertificatePem", cryptoServiceTrustList.exportCertificateAsPem())
        return "index"
    }

    @PostMapping("/generate")
    fun generateCertificate(@RequestParam(name = "vaccinationData") input: String, model: Model): String {
        log.info("generateCertificate called")
        val cardViewModels = listOf(chainEc.process("User Input", input))
        model.addAllAttributes(mapOf("cardViewModels" to cardViewModels))
        return "vaccinationCertificate"
    }

    @RequestMapping("/testsuite", method = [RequestMethod.GET, RequestMethod.POST])
    fun testSuite(model: Model): String {
        log.info("testsuite called")
        val cardViewModels = mutableListOf<CardViewModel>()
        //cborViewAdapter.process("Recovery statement", SampleData.recovery).forEach { cardViewModels.add(it) }
        cborViewAdapter.process("Vaccination statement", SampleData.vaccination).forEach { cardViewModels.add(it) }
        //cborViewAdapter.process("Test statement", SampleData.test).forEach { cardViewModels.add(it) }
        model.addAllAttributes(mapOf("cardViewModels" to cardViewModels))
        return "vaccinationCertificate"
    }

    @GetMapping("/qrc/vaccination")
    fun getQrCodeVaccination(): ResponseEntity<String> {
        log.info("getQrCodeVaccination")
        return ResponseEntity.ok(chainEc.processSingle(SampleData.vaccination))
    }

    @GetMapping("/qrc/recovery")
    fun getQrCodeRecovery(): ResponseEntity<String> {
        log.info("getQrCodeRecovery")
        return ResponseEntity.ok(chainEc.processSingle(SampleData.recovery))
    }

    @GetMapping("/qrc/test")
    fun getQrCodeTest(): ResponseEntity<String> {
        log.info("getQrCodeTest")
        return ResponseEntity.ok(chainEc.processSingle(SampleData.testNaa))
    }

}

