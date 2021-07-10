package dev.koffein.shoppingreminder.models

import java.util.*


data class Item(
    val name: String = "",
    val description: String = "",
    val place: String = "", // 名前だけでええか?
    val id: String = UUID.randomUUID().toString()
)
