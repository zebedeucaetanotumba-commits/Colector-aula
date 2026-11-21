package com.aula.coletor

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object Uploader {
    private val client = OkHttpClient()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    fun send(data: JSONObject): Boolean {
        return try {
            val body = data.toString().toRequestBody(JSON)
            val req = Request.Builder()
                .url(Config.ENDPOINT)
                .post(body)
                .build()
            client.newCall(req).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }
}
