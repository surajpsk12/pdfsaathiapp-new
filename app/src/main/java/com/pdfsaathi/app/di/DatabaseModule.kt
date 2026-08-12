package com.pdfsaathi.app.di

import android.content.Context
import androidx.room.Room
import com.pdfsaathi.app.data.local.dao.BookmarkDao
import com.pdfsaathi.app.data.local.dao.PdfDocumentDao
import com.pdfsaathi.app.data.local.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "pdf_saathi_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun providePdfDocumentDao(database: AppDatabase): PdfDocumentDao {
        return database.pdfDocumentDao()
    }

    @Provides
    fun provideBookmarkDao(database: AppDatabase): BookmarkDao {
        return database.bookmarkDao()
    }
}
