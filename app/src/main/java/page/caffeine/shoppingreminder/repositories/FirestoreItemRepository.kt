package page.caffeine.shoppingreminder.repositories

import android.os.Build
import android.util.Log
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import page.caffeine.shoppingreminder.BuildConfig
import page.caffeine.shoppingreminder.models.Item
import javax.inject.Inject

class FirestoreItemRepository @Inject constructor() : ItemRepository {
    private val firestore = FirebaseFirestore.getInstance()

    private fun getDocumentId(): String? {
        return Firebase.auth.currentUser?.uid
    }

    init {
        if (BuildConfig.BUILD_TYPE == "debug" && Build.MODEL.contains("Emulator")) {
            // 10.0.0.2: host machine address visible from emulator
            firestore.useEmulator("10.0.2.2", 8080)
            val settings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build()
            firestore.firestoreSettings = settings
        }
    }

    override suspend fun getItem(id: String): Item? {
        Log.d(TAG, "getItem: $id")
        return try {
            getDocumentId()?.let {
                firestore.collection(ROOT_COLLECTION_ID).document(it).collection(ITEM_COLLECTION_ID)
                    .document(id).get().await().toObject<Item>()
            }
        } catch (e: FirebaseFirestoreException) {
            Log.e(TAG, "Failed to get item $id", e)
            return null
        }
    }


    override suspend fun getItems(): List<Item> {
        Log.d(TAG, "getItems")
        val snapshot = try {
            getDocumentId()?.let {
                firestore.collection(ROOT_COLLECTION_ID).document(it).collection(ITEM_COLLECTION_ID)
                    .get().await()
            }
        } catch (e: FirebaseFirestoreException) {
            Log.e(TAG, "Failed to get items", e)
            return listOf()
        } ?: return listOf()

        return snapshot.documents.mapNotNull { it.toObject<Item>() }
    }

    override suspend fun setItem(id: String, item: Item) {
        Log.d(TAG, "setItem: $id -> $item")
        // assert id == item.id?
        val data = hashMapOf<String, Any>(
            "name" to item.name,
            "description" to item.description,
            "place" to item.place,
            "placeId" to item.placeId,
            "id" to item.id
        )
        getDocumentId()?.let {
            firestore.collection(ROOT_COLLECTION_ID).document(it).collection(
                ITEM_COLLECTION_ID
            ).document(item.id).update(data)
        }
    }

    override suspend fun addItem(item: Item) {
        Log.d(TAG, "addItem")
        getDocumentId()?.let {
            firestore.collection(ROOT_COLLECTION_ID).document(it).collection(
                ITEM_COLLECTION_ID
            ).document(item.id).set(item)
        }
    }

    override suspend fun delItem(id: String) {
        Log.d(TAG, "delItem")
        getDocumentId()?.let {
            firestore.collection(ROOT_COLLECTION_ID).document(it).collection(ITEM_COLLECTION_ID)
                .document(id).delete()
        }
    }

    companion object {
        const val TAG = "FirestoreRepository"
        const val ROOT_COLLECTION_ID = "users"
        const val ITEM_COLLECTION_ID = "items"
    }
}
