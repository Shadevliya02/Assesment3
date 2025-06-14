package com.sharla0139.assesment3.ui.screen

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sharla0139.assesment3.R
import com.sharla0139.assesment3.model.MenuItem
import androidx.compose.material3.CardDefaults

@Composable
fun MenuDialog(
    menuItem: MenuItem?,
    bitmap: Bitmap?,
    onDismissRequest: () -> Unit,
    onConfirmation: (Int?, String, String, String) -> Unit
) {
    var nama by remember(menuItem) { mutableStateOf(menuItem?.nama ?: "") }
    var deskripsi by remember(menuItem) { mutableStateOf(menuItem?.deskripsi ?: "") }
    var harga by remember(menuItem) { mutableStateOf(menuItem?.harga?.toString() ?: "") }
    val isEditMode = menuItem != null

    Dialog(onDismissRequest = { onDismissRequest() }) {
        Card(
            modifier = Modifier.padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (isEditMode) stringResource(R.string.edit_menu) else stringResource(R.string.tambah_menu),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (isEditMode) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(menuItem?.foto).crossfade(true).build(),
                        placeholder = painterResource(id = R.drawable.loading_img),
                        error = painterResource(id = R.drawable.broken_img),
                        contentDescription = stringResource(R.string.gambar_menu),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .padding(bottom = 8.dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    bitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = stringResource(R.string.gambar_menu),
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .padding(bottom = 8.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = nama,
                    onValueChange = { nama = it },
                    label = { Text(text = stringResource(id = R.string.nama)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = deskripsi,
                    onValueChange = { deskripsi = it },
                    label = { Text(text = stringResource(R.string.deskripsi)) },
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
                )
                OutlinedTextField(
                    value = harga,
                    onValueChange = { harga = it },
                    label = { Text(text = stringResource(id = R.string.harga)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = { onDismissRequest() },
                        modifier = Modifier.padding(end = 8.dp)
                    ) { Text(text = stringResource(R.string.batal)) }
                    OutlinedButton(
                        onClick = { onConfirmation(menuItem?.id, nama, deskripsi, harga) },
                        enabled = nama.isNotEmpty() && deskripsi.isNotEmpty() && harga.isNotEmpty(),
                    ) { Text(text = stringResource(R.string.simpan)) }
                }
            }
        }
    }
}
