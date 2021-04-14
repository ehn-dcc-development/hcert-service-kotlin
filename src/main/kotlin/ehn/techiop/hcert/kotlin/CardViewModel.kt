package ehn.techiop.hcert.kotlin

data class CardViewModel(
    val title: String = "",
    val input: String = "",
    val base64Items: List<Base64Item> = listOf(),
    val codeResources: List<CodeResource> = listOf()
)

data class Base64Item(
    val title: String,
    val value: String
)

data class CodeResource(
    val title: String,
    val image: String
)

