package com.scanforge

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.scanforge.data.model.ScannedModelDao
import com.scanforge.data.model.ScanForgeDatabase
import com.scanforge.processing.DepthProcessor
import com.scanforge.processing.ModelExporter
import com.scanforge.processing.PhotogrammetryProcessor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// ─── Application class ────────────────────────────────────────────────────────
@HiltAndroidApp
class ScanForgeApp : Application()

// ─── DI Module ────────────────────────────────────────────────────────────────
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ScanForgeDatabase {
        return Room.databaseBuilder(
            context,
            ScanForgeDatabase::class.java,
            ScanForgeDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideModelDao(db: ScanForgeDatabase): ScannedModelDao = db.modelDao()

    @Provides
    @Singleton
    fun providePhotogrammetryProcessor(): PhotogrammetryProcessor = PhotogrammetryProcessor()

    @Provides
    @Singleton
    fun provideDepthProcessor(): DepthProcessor = DepthProcessor()

    @Provides
    @Singleton
    fun provideModelExporter(): ModelExporter = ModelExporter()
}
