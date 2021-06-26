package dev.koffein.shoppingreminder.models

import com.google.android.gms.location.places.Place

class Item(
    name: String = "",
    description: String = "",
    place: Place? = null
) {
    var name: String = name
        private set
    var description: String = description
        private set
    var place: Place? = place
        private set
}