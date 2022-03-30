package page.caffeine.shoppingreminder.repositories

import android.util.Log
import page.caffeine.shoppingreminder.models.Item
import java.util.UUID
import javax.inject.Inject

class MockRepository @Inject constructor() : ItemRepository {
    private val data = mutableMapOf(*(0..10).map {
        val id = UUID.randomUUID().toString()
        Pair(id, Item("dummy item$it", "dummy description$it", "", "", id))
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

    override suspend fun delItem(id: String) {
        Log.d(TAG, "$id will be deleted")
        data.remove(id)
    }

    companion object {
        const val TAG = "MockRepository"
    }
}
