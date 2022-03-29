package page.caffeine.shoppingreminder.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import page.caffeine.shoppingreminder.models.Item
import page.caffeine.shoppingreminder.repositories.FirestoreRepository
import page.caffeine.shoppingreminder.repositories.ItemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivityViewModel : ViewModel() {
    private val _items: MutableLiveData<List<Item>> by lazy {
        MutableLiveData()
    }
    val items: LiveData<List<Item>> = _items

    // Hilt使うべき?
    // private val itemRepository: ItemRepository = MockRepository()
    private val itemRepository: ItemRepository = FirestoreRepository()

    init {
        viewModelScope.launch(Dispatchers.IO) { updateItems() }
    }

    fun addItem(item: Item) {
        Log.d(TAG, "add item $item")
        viewModelScope.launch(Dispatchers.IO) {
            itemRepository.addItem(item)
            updateItems()
        }
    }

    fun setItem(id: String, item: Item) {
        Log.d(TAG, "update item $id by $item")
        // repositoryのsubscribeをしたい(整合性のため)
        viewModelScope.launch(Dispatchers.IO) {
            itemRepository.setItem(id, item)
            updateItems()
        }
    }

    fun delItem(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            itemRepository.delItem(id)
            updateItems()
        }
    }

    // 自動的に毎回呼べると良い?
    private suspend fun updateItems() {
        // 効率悪そう……
        // val newItems = itemRepository.getItems().sortedBy { i -> i.name }
        val newItems = itemRepository.getItems()
        _items.postValue(newItems)

    }

    companion object {
        const val TAG = "MainActivityViewModel"
    }
}