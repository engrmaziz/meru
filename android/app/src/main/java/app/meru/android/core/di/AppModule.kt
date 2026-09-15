package app.meru.android.core.di

import android.content.Context
import androidx.room.Room
import app.meru.android.core.database.MeruDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): MeruDatabase =
        Room.databaseBuilder(context, MeruDatabase::class.java, "meru.db")
            .fallbackToDestructiveMigration()
            .build()
}
