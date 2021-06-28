package com.example.samsungai

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface api {

    @Multipart
    @POST("mobile")
    fun uploadPic(
        @Part image: MultipartBody.Part
    ): Call<Response>
}