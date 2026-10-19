package com.cybersquad.murendeni.api

import android.content.Context
import com.cybersquad.murendeni.data.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// Builds and keeps the single connection to the website API.
object ApiClient {

    // The Azure address of the website with /api/ on the end. The final slash is required.
    const val BASE_URL =
        "https://murendeniconstruction20260902013858-f2c2cnaxcgdtcnhm.southafricanorth-01.azurewebsites.net/api/"

    private var service: ApiService? = null

    fun getService(context: Context): ApiService {
        service?.let { return it }

        val session = SessionManager(context)

        val client = OkHttpClient.Builder()
            // Adds the saved login token to every request, so each call shows who is asking.
            .addInterceptor { chain ->
                val builder = chain.request().newBuilder()
                val token = session.getToken()
                if (token != null) {
                    builder.addHeader("Authorization", "Bearer $token")
                }
                chain.proceed(builder.build())
            }
            // BASIC logs only the method, address and result. It never logs passwords or tokens.
            .addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
            )
            // The free Azure plan can take up to a minute to wake up, so the timeouts are long.
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            // Uploads send large files, so a slow phone connection needs a longer write timeout.
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()

        val created = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)

        service = created
        return created
    }
}