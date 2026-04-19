package com.smarttour360.app.utils

import android.content.Context
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest

object DestinationImagePrefetcher {
    fun prefetch(context: Context, imageUrls: List<String>) {
        imageUrls
            .filter { it.isNotBlank() }
            .distinct()
            .take(12)
            .forEach { imageUrl ->
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.DISABLED)
                    .networkCachePolicy(CachePolicy.ENABLED)
                    .build()
                context.imageLoader.enqueue(request)
            }
    }
}
