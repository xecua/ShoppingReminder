package dev.koffein.shoppingreminder.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.koffein.shoppingreminder.models.Item
import dev.koffein.shoppingreminder.repositories.ItemRepository
import dev.koffein.shoppingreminder.repositories.MockRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivityViewModel : ViewModel() {
    private val _items: MutableLiveData<MutableList<Item>> by lazy {
        MutableLiveData()
    }
    val items: LiveData<out List<Item>> = _items

    // Hilt使うべき?
    private val itemRepository: ItemRepository = MockRepository()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _items.postValue(itemRepository.getItems().toMutableList())
        }
    }

    fun setItem(item: Item, index: Int?) {
        if (index == null) {
            // create
            _items.value?.add(item)
        } else {
            // update
            val values = _items.value
            values?.set(index, item)
            _items.postValue(values)
        }

    }

}