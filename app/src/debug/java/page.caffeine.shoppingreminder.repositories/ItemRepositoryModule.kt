package page.caffeine.shoppingreminder.repositories

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ItemRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindItemRepository(
        firestoreRepository: FirestoreItemRepository
    ): ItemRepository
}

