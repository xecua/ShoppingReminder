package dev.koffein.shoppingreminder.models

import com.google.android.gms.location.places.Place

data class Item(
    val name: String = "",
    val description: String = "",
    val place: Place? = null
)
