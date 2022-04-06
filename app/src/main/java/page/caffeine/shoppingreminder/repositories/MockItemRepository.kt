package page.caffeine.shoppingreminder.repositories

import android.util.Log
import page.caffeine.shoppingreminder.models.Item
import java.util.Collections
import java.util.UUID
import javax.inject.Inject

class MockRepository @Inject constructor() : ItemRepository {
    private val order = mutableListOf<String>()
    private val data = mutableMapOf(*(0..10).map {
        val id = UUID.randomUUID().toString()
        order.add(id)
        Pair(id, Item("dummy item$it", "dummy description$it", "", "", id))
    }.toTypedArray())

    override suspend fun getItem(id: String): Item? = data[id]
    override suspend fun getItem(index: Int): Item? = data[order[index]]
    override suspend fun getItems(): List<Item> = order.map { data[it]!! }.toList()

    override suspend fun setItem(id: String, item: Item) {
        Log.d(TAG, "$id is set to $item")
        data[id] = item
    }

    override suspend fun setItem(index: Int, item: Item) {
        Log.d(TAG, "$item is set to #$index")
        order[index] = item.id
        data[item.id] = item
    }

    override suspend fun addItem(item: Item) {
        Log.d(TAG, "new item $item added")
        order.add(item.id)
        data[item.id] = item
    }

    override suspend fun insertItem(index: Int, item: Item) {
        Log.d(TAG, "new item $item added into #$index")
        order.add(index, item.id)
        data[item.id] = item
    }

    override suspend fun delItem(id: String) {
        Log.d(TAG, "$id will be deleted")
        order.remove(id)
        data.remove(id)
    }

    override suspend fun delItem(index: Int) {
        Log.d(TAG, "item at #$index will be deleted")
        val itemId = order.removeAt(index)
        data.remove(itemId)
    }

    override suspend fun swapItem(leftIndex: Int, rightIndex: Int) {
        Log.d(TAG, "item at #$leftIndex and #$rightIndex will be swapped")
        Collections.swap(order, leftIndex, rightIndex)
    }

    override suspend fun swapItem(leftId: String, rightId: String) {
        Log.d(TAG, "item $leftId and $rightId will be swapped")
        val ids = order.filter { it in listOf(leftId, rightId) }
        val leftIndex = ids.indexOf(leftId)
        val rightIndex = 1 - leftIndex
        Collections.swap(order, leftIndex, rightIndex)
    }

    companion object {
        const val TAG = "MockRepository"
    }
}
