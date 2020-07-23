package com.example.android.architecture.blueprints.todoapp.util

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.http.ContentType
import io.ktor.util.KtorExperimentalAPI
import okhttp3.logging.HttpLoggingInterceptor


@KtorExperimentalAPI
val httpClientAndroid = HttpClient(OkHttp) {
    install(JsonFeature) {
        serializer = GsonSerializer()
        accept(ContentType.Application.HalJson)

    }
    engine {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        addInterceptor(loggingInterceptor)
    }
}