package page.caffeine.shoppingreminder.repositories

import javax.inject.Singleton
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ItemRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindItemRepository(
        firestoreRepository: FirestoreItemRepository
    ): ItemRepository
}

