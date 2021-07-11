package dev.koffein.shoppingreminder.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
data class Item(
    val name: String,
    val description: String,
    val place: String,
    val id: String
) : Parcelable {
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
