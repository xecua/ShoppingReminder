package dev.koffein.shoppingreminder.repositories

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import dev.koffein.shoppingreminder.BuildConfig
import dev.koffein.shoppingreminder.models.Item
import kotlinx.coroutines.tasks.await
import java.util.*

interface ItemRepository {
    suspend fun getItem(id: String): Item?
    suspend fun getItems(): List<Item> // 順序を保証しない(使う側でソートすることを期待)

    // わける必要があるかは怪しい(itemがidを内包しているため)
    suspend fun setItem(id: String, item: Item)
    suspend fun addItem(item: Item)
}

class MockRepository : ItemRepository {
    private val data = mutableMapOf(*(0..10).map {
        val id = UUID.randomUUID().toString()
        Pair(id, Item("dummy item$it", "dummy description$it", "", id))
    }.toTypedArray())

    override suspend fun getItem(id: String): Item? = data[id]

    override suspend fun getItems(): List<Item> {
        return data.values.toList()
    }

    override suspend fun setItem(id: String, item: Item) {
        Log.d(TAG, "$id is set to $item")
        data[id] = item
    }

    override suspend fun addItem(item: Item) {
        Log.d(TAG, "new item $item added")
        data[item.id] = item
    }

    companion object {
        const val TAG = "MockRepository"
    }

}

class FirestoreRepository : ItemRepository {
    private val firestore = FirebaseFirestore.getInstance()

    // とりあえず……
    private val documentId = "aaaaa"

    init {
        if (BuildConfig.BUILD_TYPE == "debug") {
            // 10.0.0.2: host machine address visible from emulator
            firestore.useEmulator("10.0.2.2", 8080)
            val settings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build()
            firestore.firestoreSettings = settings
        }
    }

    override suspend fun getItem(id: String): Item? {
        Log.d(TAG, "getItem: $id")
        val snapshot = try {
            firestore.collection(ROOT_COLLECTION_ID).document(documentId).collection(COLLECTION_ID)
                .document(id).get().await()
        } catch (e: FirebaseFirestoreException) {
            Log.e(TAG, "Failed to get item $id", e)
            return null
        }

        Log.d(TAG, "$snapshot.data")

        return Item(
            name = snapshot.getString("name") ?: "",
            description = snapshot.getString("description") ?: "",
            place = snapshot.getString("place") ?: "",
            id = snapshot.id
        )
    }

    override suspend fun getItems(): List<Item> {
        Log.d(TAG, "getItems")
        val snapshot = try {
            firestore.collection(ROOT_COLLECTION_ID).document(documentId).collection(COLLECTION_ID)
                .get().await()
        } catch (e: FirebaseFirestoreException) {
            Log.e(TAG, "Failed to get items", e)
            return listOf()
        }
        return snapshot.documents.map {
            Log.d(TAG, "$it")
            Item(
                name = it.getString("name") ?: "",
                description = it.getString("description") ?: "",
                place = it.getString("place") ?: "",
                id = it.id
            )
        }
    }

    override suspend fun setItem(id: String, item: Item) {
        Log.d(TAG, "setItem: $id -> $item")
        // assert id == item.id?
        val data = hashMapOf<String, Any>(
            "name" to item.name,
            "description" to item.description,
            "place" to item.place,
            "id" to item.id
        )
        firestore.collection(ROOT_COLLECTION_ID).document(documentId).collection(
            COLLECTION_ID
        ).document(item.id).update(data)
    }

    override suspend fun addItem(item: Item) {
        Log.d(TAG, "addItem")
        setItem(item.id, item)
        val data = hashMapOf<String, Any>(
            "name" to item.name,
            "description" to item.description,
            "place" to item.place,
            "id" to item.id
        )
        firestore.collection(ROOT_COLLECTION_ID).document(documentId).collection(
            COLLECTION_ID
        ).document(item.id).set(data)
    }

    companion object {
        const val TAG = "FirestoreRepository"
        const val ROOT_COLLECTION_ID = "items"
        const val COLLECTION_ID = "items"
    }
}
