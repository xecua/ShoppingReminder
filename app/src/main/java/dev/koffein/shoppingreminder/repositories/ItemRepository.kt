package dev.koffein.shoppingreminder.repositories

import dev.koffein.shoppingreminder.models.Item

interface ItemRepository {
    suspend fun getItem(id: String): Item
    suspend fun getItems(): Array<Item>
}

class MockRepository: ItemRepository {
    override suspend fun getItem(id: String): Item = Item("dummy item", "dummy description")

    override suspend fun getItems(): Array<Item> = arrayOf(
        Item("dummy item1", "dummy description1"),
        Item("dummy item2", "dummy description2")
    )

}

// class FirebaseItemRepository: ItemRepository {
//
// }