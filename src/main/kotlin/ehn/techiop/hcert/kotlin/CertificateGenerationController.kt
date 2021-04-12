package ehn.techiop.hcert.kotlin

import ehn.techiop.hcert.kotlin.chain.SampleData
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam


@Controller
class CertificateGenerationController(private val cborViewAdapter: CborViewAdapter) {

    private val log = LoggerFactory.getLogger(this::class.java)

    @GetMapping("/")
    fun index(model: Model): String {
        log.info("index called")
        model.addAttribute("pastInfectedJson", SampleData.recovery)
        model.addAttribute("vaccinatedJson", SampleData.vaccination)
        model.addAttribute("testedJson", SampleData.test)
        return "index"
    }

    @PostMapping("/generate")
    fun generateCertificate(@RequestParam(name = "vaccinationData") input: String, model: Model): String {
        log.info("generateCertificate called")
        val cardViewModels = listOf(cborViewAdapter.process(input))
        model.addAllAttributes(mapOf("vaccinationData" to input, "cardViewModels" to cardViewModels))
        return "vaccinationCertificate"
    }

}

