package ehn.techiop.hcert.kotlin

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["ehn.techiop.hcert.kotlin", "eu.europa.ec.dgc.gateway.connector"])
class CertificateGenerationServiceApplication

fun main(args: Array<String>) {
	runApplication<CertificateGenerationServiceApplication>(*args)
}
