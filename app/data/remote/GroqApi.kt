package com.smarttour360.app.data.remote

import com.smarttour360.app.dto.GroqChatRequest
import com.smarttour360.app.dto.GroqChatResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface GroqApi {
    @POST("openai/v1/chat/completions")
    suspend fun chat(
        @Header("Authorization") authorization: String,
        @Body request: GroqChatRequest
    ): GroqChatResponse

    @POST("openai/v1/embeddings")
    suspend fun embeddings(
        @Header("Authorization") authorization: String,
        @Body request: GroqEmbeddingRequest
    ): GroqEmbeddingResponse
}
