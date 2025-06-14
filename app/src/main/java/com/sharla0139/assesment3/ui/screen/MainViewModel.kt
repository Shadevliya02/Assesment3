package com.sharla0139.assesment3.ui.screen

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sharla0139.assesment3.model.MenuItem
import com.sharla0139.assesment3.network.ApiStatus
import com.sharla0139.assesment3.network.MenuApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

class MainViewModel : ViewModel() {
    private val _data = mutableStateOf<List<MenuItem>>(emptyList())
    val data: State<List<MenuItem>> = _data

    private val _status = mutableStateOf(ApiStatus.LOADING)
    val status: State<ApiStatus> = _status

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    private val _menuItemToEdit = mutableStateOf<MenuItem?>(null)
    val menuItemToEdit: State<MenuItem?> = _menuItemToEdit

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    fun retrieveMenu() {
        viewModelScope.launch(Dispatchers.IO) {
            _status.value = ApiStatus.LOADING
            try {
                val result = MenuApi.service.getAllMenu()
                if (result.success) {
                    _data.value = result.data
                    _status.value = ApiStatus.SUCCESS
                } else {
                    throw Exception("Failed to retrieve menu data")
                }
            } catch (e: Exception) {
                Log.d("MainViewModel", "Failure: ${e.message}")
                _status.value = ApiStatus.FAILED
            }
        }
    }

    fun createMenuItem(nama: String, deskripsi: String, harga: String, bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val result = MenuApi.service.createMenuItem(
                    nama.toRequestBody("text/plain".toMediaTypeOrNull()),
                    deskripsi.toRequestBody("text/plain".toMediaTypeOrNull()),
                    harga.toRequestBody("text/plain".toMediaTypeOrNull()),
                    bitmap.toMultipartBody()
                )
                if (result.success) retrieveMenu()
                else throw Exception("Failed to create menu item")
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateMenuItem(id: Int, nama: String, deskripsi: String, harga: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val body = mapOf("nama" to nama, "deskripsi" to deskripsi, "harga" to harga)
                val result = MenuApi.service.updateMenuItem(id, body)
                if (result.success) {
                    retrieveMenu()
                } else {
                    throw Exception("Failed to update menu item")
                }
            } catch (e: Exception) {
                Log.d("MainViewModel", "Update failed: ${e.message}")
                _errorMessage.value = "Error updating: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteMenuItem(itemId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val result = MenuApi.service.deleteMenuItem(itemId)
                if (result.success) {
                    retrieveMenu()
                } else {
                    throw Exception(result.message)
                }
            } catch (e: Exception) {
                Log.d("MainViewModel", "Delete failed: ${e.message}")
                _errorMessage.value = "Error deleting: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onEditClick(menuItem: MenuItem) {
        _menuItemToEdit.value = menuItem
    }

    fun onDialogDismiss() {
        _menuItemToEdit.value = null
    }

    private fun Bitmap.toMultipartBody(): MultipartBody.Part {
        val stream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 80, stream)
        val byteArray = stream.toByteArray()
        val requestBody = byteArray.toRequestBody(
            "image/jpeg".toMediaTypeOrNull(), 0, byteArray.size)
        return MultipartBody.Part.createFormData("foto", "image.jpg", requestBody)
    }

    fun clearMessage() {
        _errorMessage.value = null
    }
}