package com.sharla0139.assesment3.network

import com.sharla0139.assesment3.model.MenuItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

private const val BASE_URL = "https://menu-api-alpha.vercel.app/"

private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

private val retrofit = Retrofit.Builder()
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .baseUrl(BASE_URL)
    .build()

data class MenuListResponse(val success: Boolean, val data: List<MenuItem>)
data class MenuDetailResponse(val success: Boolean, val data: MenuItem)
data class GeneralResponse(val success: Boolean, val message: String)

interface MenuApiService {
    @GET("api/menu")
    suspend fun getAllMenu(): MenuListResponse

    @Multipart
    @POST("api/menu")
    suspend fun createMenuItem(
        @Part("nama") nama: RequestBody,
        @Part("deskripsi") deskripsi: RequestBody,
        @Part("harga") harga: RequestBody,
        @Part image: MultipartBody.Part
    ): MenuDetailResponse

    @PUT("api/menu/{id}")
    suspend fun updateMenuItem(
        @Path("id") itemId: Int,
        @Body menuUpdate: Map<String, String>
    ): MenuDetailResponse

    @DELETE("api/menu/{id}")
    suspend fun deleteMenuItem(@Path("id") itemId: Int): GeneralResponse
}

object MenuApi {
    val service: MenuApiService by lazy {
        retrofit.create(MenuApiService::class.java)
    }
}

enum class ApiStatus { LOADING, SUCCESS, FAILED }