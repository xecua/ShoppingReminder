package dev.koffein.shoppingreminder.models

import com.google.android.libraries.places.api.model.Place


data class Item(
    val name: String = "",
    val description: String = "",
    val place: Place? = null
)
