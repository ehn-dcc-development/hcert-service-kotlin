package ehn.techiop.hcert.kotlin

import org.springframework.stereotype.Service

@Service
class CborViewAdapter(private val chains: Set<ChainAdapter>) {

    fun process(title: String, input: String): List<CardViewModel> {
        return chains.map { it.process(title, input) }
    }

}

