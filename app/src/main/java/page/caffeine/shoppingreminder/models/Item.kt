package page.caffeine.shoppingreminder.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
data class Item(
    val name: String,
    val description: String,
    val place: String,
    val placeId: String,
    val id: String
) : Parcelable {
    companion object {
        fun ofNullable(
            name: String? = null,
            description: String? = null,
            place: String? = null,
            placeId: String? = null,
            id: String? = null
        ) = Item(
            name ?: "",
            description ?: "",
            place ?: "",
            placeId ?: "",
            id ?: UUID.randomUUID().toString()
        )
    }
}
