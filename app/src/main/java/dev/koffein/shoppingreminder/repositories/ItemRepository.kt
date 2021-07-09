package dev.koffein.shoppingreminder.repositories

import dev.koffein.shoppingreminder.models.Item
import kotlin.random.Random

interface ItemRepository {
    suspend fun getItem(id: String): Item
    suspend fun getItems(): List<Item>
}

class MockRepository : ItemRepository {
    override suspend fun getItem(id: String): Item = Item("dummy item", "dummy description")

    override suspend fun getItems(): List<Item> {
        val size = Random.nextInt(1, 30)

        return (0..size).map { Item("dummy item$it", "dummy description$it") }
            .toList()
    }

    companion object {
        const val TAG = "MockRepository"
    }

}

// class FirebaseItemRepository: ItemRepository {
//
// }