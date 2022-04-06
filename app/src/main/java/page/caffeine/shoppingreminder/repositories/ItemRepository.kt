package page.caffeine.shoppingreminder.repositories

import page.caffeine.shoppingreminder.models.Item

interface ItemRepository {
    suspend fun getItem(id: String): Item?
    suspend fun getItem(index: Int): Item?
    suspend fun getItems(): List<Item> // sorted

    suspend fun addItem(item: Item)
    suspend fun insertItem(index: Int, item: Item)
    suspend fun setItem(id: String, item: Item)
    suspend fun setItem(index: Int, item: Item)
    suspend fun delItem(id: String)
    suspend fun delItem(index: Int)

    // ordering
    suspend fun swapItem(leftIndex: Int, rightIndex: Int)
    suspend fun swapItem(leftId: String, rightId: String)
}

