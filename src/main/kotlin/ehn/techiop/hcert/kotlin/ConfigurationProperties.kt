package ehn.techiop.hcert.kotlin

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.net.URI

@ConfigurationProperties(prefix = "app")
@ConstructorBinding
data class ConfigurationProperties(
    val chain: ConfigurationPropertiesChain,
    val trustList: ConfigurationPropertiesChain
) {

    @ConstructorBinding
    data class ConfigurationPropertiesChain(
        val ecPrivate: URI,
        val ecCert: URI
    )

}
