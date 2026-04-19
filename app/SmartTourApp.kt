package com.smarttour360.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.smarttour360.app.ui.chatbot.rag.KnowledgeBase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.io.File

class SmartTourApp : Application(), ImageLoaderFactory {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            KnowledgeBase(this@SmartTourApp).seedIfNeeded()
        }
    }

    override fun newImageLoader(): ImageLoader {
        val imageCacheDir = File(cacheDir, "images")
        return ImageLoader.Builder(this)
            .okHttpClient(
                OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                            .header("User-Agent", "SmartTour360-Android/1.0")
                            .header("Accept-Language", "en-IN,en;q=0.9")
                            .header("Referer", "https://en.wikipedia.org/")
                            .build()
                        chain.proceed(request)
                    }
                    .build()
            )
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.12)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(imageCacheDir)
                    .maxSizeBytes(12L * 1024L * 1024L)
                    .build()
            }
            .crossfade(true)
            .respectCacheHeaders(false)
            .build()
    }
}
