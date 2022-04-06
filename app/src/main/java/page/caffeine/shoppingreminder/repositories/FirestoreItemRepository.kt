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
import java.util.Collections
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

    override suspend fun getItem(index: Int): Item? {
        Log.d(TAG, "getItem at: $index")

        val userDocId = getDocumentId() ?: return null
        val itemMiscDocId = firestore.collection(ROOT_COLLECTION_ID).document(userDocId)
            .collection(MISC_COLLECTION_ID).document(MISC_ITEM_DOCUMENT_ID)
        val itemCollectionId =
            firestore.collection(ROOT_COLLECTION_ID).document(userDocId)
                .collection(ITEM_COLLECTION_ID)
        return try {
            firestore.runTransaction { transaction ->
                transaction
                    .get(itemMiscDocId).toObject<ItemMisc>()
                    ?.itemOrder
                    ?.get(index)
                    ?.let { key ->
                        transaction.get(itemCollectionId.document(key)).toObject<Item>()
                    }
            }.await()
        } catch (e: FirebaseFirestoreException) {
            Log.e(TAG, "Failed to get item at $index", e)
            return null
        }
    }

    override suspend fun getItems(): List<Item> {
        Log.d(TAG, "getItems")
        val snapshot = try {
            getDocumentId()?.let {
                firestore.collection(ROOT_COLLECTION_ID).document(it).collection(ITEM_COLLECTION_ID)
                    .get().await()
            } ?: return listOf()
        } catch (e: FirebaseFirestoreException) {
            Log.e(TAG, "Failed to get items", e)
            return listOf()
        }

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
        try {
            getDocumentId()?.let {
                firestore.collection(ROOT_COLLECTION_ID).document(it).collection(
                    ITEM_COLLECTION_ID
                ).document(item.id).update(data).await()
            }
        } catch (e: FirebaseFirestoreException) {
            Log.e(TAG, "Failed to update $id to $item", e)
        }
    }

    override suspend fun setItem(index: Int, item: Item) {
        Log.d(TAG, "setItem: $item at #$index")

        val userDocId = getDocumentId() ?: return
        val itemMiscDocId = firestore.collection(ROOT_COLLECTION_ID).document(userDocId)
            .collection(MISC_COLLECTION_ID).document(MISC_ITEM_DOCUMENT_ID)
        val itemDocId = firestore.collection(ROOT_COLLECTION_ID).document(userDocId)
            .collection(ITEM_COLLECTION_ID).document(item.id)

        try {
            firestore.runTransaction { transaction ->
                val itemMisc = transaction.get(itemMiscDocId).toObject<ItemMisc>()
                if (itemMisc == null) {
                    if (index != 0) {
                        throw FirebaseFirestoreException(
                            "Index out of range",
                            FirebaseFirestoreException.Code.INVALID_ARGUMENT
                        )
                    }
                    val itemMisc = ItemMisc(
                        itemOrder = mutableListOf(item.id)
                    )
                    transaction.set(itemDocId, item)
                    transaction.set(itemMiscDocId, itemMisc)
                } else {
                    if (index < 0 || itemMisc.itemOrder.size < index) {
                        throw FirebaseFirestoreException(
                            "Index out of range",
                            FirebaseFirestoreException.Code.INVALID_ARGUMENT
                        )
                    }
                    itemMisc.itemOrder[index] = item.id
                    transaction.set(itemDocId, item)
                    transaction.update(
                        itemMiscDocId, mapOf(
                            "itemOrder" to itemMisc.itemOrder
                        )
                    )
                }
                null
            }.await()
        } catch (e: FirebaseFirestoreException) {
            Log.e(TAG, "Failed to set $item", e)
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

    override suspend fun insertItem(index: Int, item: Item) {
        Log.d(TAG, "insert $item into #$index")

        val userDocId = getDocumentId() ?: return
        val itemMiscDocId = firestore.collection(ROOT_COLLECTION_ID).document(userDocId)
            .collection(MISC_COLLECTION_ID).document(MISC_ITEM_DOCUMENT_ID)
        val itemDocId =
            firestore.collection(ROOT_COLLECTION_ID).document(userDocId)
                .collection(ITEM_COLLECTION_ID).document(item.id)
        try {
            firestore.runTransaction { transaction ->
                val itemMisc = transaction.get(itemMiscDocId).toObject<ItemMisc>()
                if (itemMisc == null) {
                    // no item. create
                    if (index != 0) {
                        throw FirebaseFirestoreException(
                            "Index out of range",
                            FirebaseFirestoreException.Code.INVALID_ARGUMENT
                        )
                    }
                    val itemMisc = ItemMisc(
                        itemOrder = mutableListOf(item.id)
                    )

                    transaction.set(itemDocId, item)
                    transaction.set(itemMiscDocId, itemMisc)

                } else {
                    if (index < 0 || itemMisc.itemOrder.size < index) {
                        throw FirebaseFirestoreException(
                            "Index out of range",
                            FirebaseFirestoreException.Code.INVALID_ARGUMENT
                        )
                    }
                    itemMisc.itemOrder.add(index, item.id)

                    transaction.set(itemDocId, item)
                    transaction.update(
                        itemMiscDocId, mapOf(
                            "itemOrder" to itemMisc.itemOrder
                        )
                    )
                }
                null
            }.await()
        } catch (e: FirebaseFirestoreException) {
            Log.e(TAG, "Failed to insert $item into $index", e)
        }
    }

    override suspend fun delItem(id: String) {
        Log.d(TAG, "delItem")
        try {
            getDocumentId()?.let {
                firestore.collection(ROOT_COLLECTION_ID).document(it).collection(ITEM_COLLECTION_ID)
                    .document(id).delete().await()
            }
        } catch (e: FirebaseFirestoreException) {
            Log.e(TAG, "Failed to delete $id", e)
            return
        }
    }

    override suspend fun delItem(index: Int) {
        Log.d(TAG, "delItem at #$index")

        val userDocId = getDocumentId() ?: return
        val itemMiscDocId = firestore.collection(ROOT_COLLECTION_ID).document(userDocId)
            .collection(MISC_COLLECTION_ID).document(MISC_ITEM_DOCUMENT_ID)
        val itemCollectionId =
            firestore.collection(ROOT_COLLECTION_ID).document(userDocId)
                .collection(ITEM_COLLECTION_ID)
        try {
            firestore.runTransaction { transaction ->
                val itemMisc = transaction.get(itemMiscDocId).toObject<ItemMisc>()
                if (itemMisc == null || index < 0 || itemMisc.itemOrder.size < index) {
                    throw FirebaseFirestoreException(
                        "Index out of range",
                        FirebaseFirestoreException.Code.INVALID_ARGUMENT
                    )
                }

                val itemId = itemMisc.itemOrder.removeAt(index)
                transaction.delete(itemCollectionId.document(itemId))
                transaction.set(itemMiscDocId, itemMisc)

                null
            }.await()
        } catch (e: FirebaseFirestoreException) {
            Log.e(TAG, "Failed to delete item at $index", e)
            return
        }
    }

    override suspend fun swapItem(leftIndex: Int, rightIndex: Int) {
        Log.d(TAG, "swap item at #$leftIndex and #$rightIndex")

        val userDocId = getDocumentId() ?: return
        val itemMiscDocId = firestore.collection(ROOT_COLLECTION_ID).document(userDocId)
            .collection(MISC_COLLECTION_ID).document(MISC_ITEM_DOCUMENT_ID)
        try {
            firestore.runTransaction { transaction ->
                val itemMisc = transaction.get(itemMiscDocId).toObject<ItemMisc>()
                if (itemMisc == null || leftIndex < 0 || itemMisc.itemOrder.size < leftIndex ||
                    rightIndex < 0 || itemMisc.itemOrder.size < rightIndex
                ) {
                    throw FirebaseFirestoreException(
                        "Index out of range",
                        FirebaseFirestoreException.Code.INVALID_ARGUMENT
                    )
                }

                Collections.swap(itemMisc.itemOrder, leftIndex, rightIndex)
                transaction.update(
                    itemMiscDocId, mapOf(
                        "itemMisc" to itemMisc
                    )
                )

                null
            }.await()
        } catch (e: FirebaseFirestoreException) {
            Log.e(TAG, "Failed to swap item at #$leftIndex and #$rightIndex", e)
            return
        }

    }

    override suspend fun swapItem(leftId: String, rightId: String) {
        Log.d(TAG, "swap item $leftId and $rightId")

        val userDocId = getDocumentId() ?: return
        val itemMiscDocId = firestore.collection(ROOT_COLLECTION_ID).document(userDocId)
            .collection(MISC_COLLECTION_ID).document(MISC_ITEM_DOCUMENT_ID)
        try {
            firestore.runTransaction { transaction ->
                val itemMisc = transaction.get(itemMiscDocId).toObject<ItemMisc>()
                    ?: throw FirebaseFirestoreException(
                        "Index out of range",
                        FirebaseFirestoreException.Code.INVALID_ARGUMENT
                    )

                val ids = itemMisc.itemOrder.filter { it in listOf(leftId, rightId) }
                if (ids.size != 2) {
                    throw FirebaseFirestoreException(
                        "Index out of range",
                        FirebaseFirestoreException.Code.INVALID_ARGUMENT
                    )
                }
                val leftIndex = ids.indexOf(leftId)
                val rightIndex = 1 - leftIndex

                Collections.swap(itemMisc.itemOrder, leftIndex, rightIndex)
                transaction.update(
                    itemMiscDocId, mapOf(
                        "itemMisc" to itemMisc
                    )
                )

                null
            }.await()
        } catch (e: FirebaseFirestoreException) {
            Log.e(TAG, "Failed to swap item $leftId and $rightId", e)
            return
        }

    }

    companion object {
        const val TAG = "FirestoreRepository"
        const val ROOT_COLLECTION_ID = "users"
        const val ITEM_COLLECTION_ID = "items"
        const val MISC_COLLECTION_ID = "misc"
        const val MISC_ITEM_DOCUMENT_ID = "item"
    }
}

data class ItemMisc(
    val itemOrder: MutableList<String>
)
