package com.opankcs.costa.data

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private var cachedKey: String? = null
    private var cachedApi: CostaApi? = null

    @Synchronized
    fun get(prefs: Prefs): CostaApi {
        val key = prefs.baseUrl + "|" + prefs.token
        cachedApi?.let { api ->
            if (cachedKey == key) return api
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Accept", "application/json")
                    .addHeader("Authorization", "Bearer ${prefs.token}")
                    .build()
                chain.proceed(request)
            }
            .build()

        val api = Retrofit.Builder()
            .baseUrl(prefs.baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CostaApi::class.java)

        cachedKey = key
        cachedApi = api
        return api
    }
}
