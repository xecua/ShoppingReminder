package page.caffeine.shoppingreminder.repositories

import page.caffeine.shoppingreminder.models.Item

interface ItemRepository {
    suspend fun getItem(id: String): Item?
    suspend fun getItems(): List<Item> // 順序を保証しない(使う側でソートすることを期待)

    // わける必要があるかは怪しい(itemがidを内包しているため)
    suspend fun setItem(id: String, item: Item)
    suspend fun addItem(item: Item)

    suspend fun delItem(id: String)
}

