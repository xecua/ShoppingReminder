package dev.koffein.shoppingreminder.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import dev.koffein.shoppingreminder.models.Item

class MainActivityViewModel: ViewModel() {
    val places: LiveData<Array<Item>> = TODO()
}