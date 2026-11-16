package com.joni.motionalert.data

import com.joni.motionalert.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** Talks to the motion-alerts relay Worker. All calls are best effort: a
 *  failed upload is simply lost, the next motion sends again. */
object MotionApi {
    private val http = OkHttpClient.Builder().build()
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    /** The bike phone reports one motion event. Returns true on HTTP 2xx. */
    suspend fun postMotion(deviceId: String, timeMs: Long): Boolean {
        if (BuildConfig.MOTION_SECRET.isBlank()) return false
        val payload = buildJsonObject {
            put("secret", BuildConfig.MOTION_SECRET)
            put("device_id", deviceId)
            put("time", timeMs)
        }.toString()
        val req = Request.Builder()
            .url("${BuildConfig.RELAY_URL}/motion")
            .post(payload.toRequestBody(jsonType))
            .build()
        return runCatching {
            withContext(Dispatchers.IO) { http.newCall(req).execute().use { it.isSuccessful } }
        }.getOrDefault(false)
    }

    /** The owner phone registers its FCM token so motion events can reach it. */
    suspend fun registerToken(token: String): Boolean {
        val payload = buildJsonObject {
            put("token", token)
            put("role", "owner")
        }.toString()
        val req = Request.Builder()
            .url("${BuildConfig.RELAY_URL}/register")
            .post(payload.toRequestBody(jsonType))
            .build()
        return runCatching {
            withContext(Dispatchers.IO) { http.newCall(req).execute().use { it.isSuccessful } }
        }.getOrDefault(false)
    }
}
