package dev.koffein.shoppingreminder.models

import java.util.*

data class Item(
    val name: String,
    val description: String,
    val place: String,
    val id: String
) {
    companion object {
        fun ofNullable(
            name: String? = null,
            description: String? = null,
            place: String? = null,
            id: String? = null
        ) = Item(
            name ?: "",
            description ?: "",
            place ?: "",
            id ?: UUID.randomUUID().toString()
        )
    }
}
