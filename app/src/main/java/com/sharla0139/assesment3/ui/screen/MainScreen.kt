package com.sharla0139.assesment3.ui.screen

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.sharla0139.assesment3.BuildConfig
import com.sharla0139.assesment3.R
import com.sharla0139.assesment3.model.MenuItem
import com.sharla0139.assesment3.model.User
import com.sharla0139.assesment3.network.ApiStatus
import com.sharla0139.assesment3.network.UserDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Locale

enum class DisplayMode { LIST, GRID }
enum class AppScreen { MAIN, ABOUT }

@RequiresApi(Build.VERSION_CODES.N)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onAboutClick: () -> Unit) {
    val context = LocalContext.current
    val dataStore = UserDataStore(context)
    val user by dataStore.userFlow.collectAsState(User())
    val viewModel: MainViewModel = viewModel()
    val errorMessage by viewModel.errorMessage

    var showMenuDialog by remember { mutableStateOf(false) }
    var showHapusDialog by remember { mutableStateOf(false) }
    var showProfilDialog by remember { mutableStateOf(false) }
    var currentDisplayMode by remember { mutableStateOf(DisplayMode.GRID) }

    var menuItemToDelete by remember { mutableStateOf<MenuItem?>(null) }
    var bitmap: Bitmap? by remember { mutableStateOf(null) }

    val launcher = rememberLauncherForActivityResult(CropImageContract()) {
        bitmap = getCroppedImage(context.contentResolver, it)
        if (bitmap != null) {
            viewModel.onDialogDismiss()
            showMenuDialog = true
        }
    }

    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var currentLocationName by remember { mutableStateOf<String?>(null) }
    // NEW: State for showing location detail dialog
    var showLocationDetailDialog by remember { mutableStateOf(false) }


    val fusedLocationClient: FusedLocationProviderClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    currentLocation = location
                    Log.d("LOCATION", "Lat: ${location.latitude}, Lng: ${location.longitude}")
                    CoroutineScope(Dispatchers.IO).launch {
                        currentLocationName = getLocationName(context, location.latitude, location.longitude)
                    }
                }
            }
        }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                Toast.makeText(context, "Izin lokasi akurat diberikan", Toast.LENGTH_SHORT).show()
                startLocationUpdates(fusedLocationClient, locationCallback, context)
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                Toast.makeText(context, "Izin lokasi perkiraan diberikan", Toast.LENGTH_SHORT).show()
                startLocationUpdates(fusedLocationClient, locationCallback, context)
            }
            else -> {
                Toast.makeText(context, "Izin lokasi ditolak", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                startLocationUpdates(fusedLocationClient, locationCallback, context)
            }
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                startLocationUpdates(fusedLocationClient, locationCallback, context)
            }
            else -> {
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    DisposableEffect(fusedLocationClient) {
        onDispose {
            stopLocationUpdates(fusedLocationClient, locationCallback)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = stringResource(id = R.string.app_name))
                        currentLocationName?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .padding(start = 0.dp)
                                    // NEW: Make location text clickable to show full details
                                    .clickable { showLocationDetailDialog = true }
                            )
                        } ?: run {
                            Text(
                                text = stringResource(R.string.getting_location),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .padding(start = 0.dp)
                                    // NEW: Make getting location text clickable too
                                    .clickable { showLocationDetailDialog = true }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    IconButton(onClick = {
                        currentDisplayMode = if (currentDisplayMode == DisplayMode.GRID) {
                            DisplayMode.LIST
                        } else {
                            DisplayMode.GRID
                        }
                    }) {
                        Icon(
                            painter = painterResource(
                                id = if (currentDisplayMode == DisplayMode.GRID) R.drawable.ic_list_view else R.drawable.ic_grid_view
                            ),
                            contentDescription = stringResource(R.string.toggle_display_mode),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onAboutClick) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = stringResource(R.string.info_app),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = {
                        if (user.email.isEmpty()) {
                            CoroutineScope(Dispatchers.IO).launch { signIn(context, dataStore) }
                        } else {
                            showProfilDialog = true
                        }
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.account_circle_24),
                            contentDescription = stringResource(R.string.profil),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (user.email.isNotEmpty()) {
                        val options = CropImageContractOptions(
                            null, CropImageOptions(
                                imageSourceIncludeGallery = true,
                                imageSourceIncludeCamera = true,
                                fixAspectRatio = true
                            )
                        )
                        launcher.launch(options)
                    } else {
                        Toast.makeText(context, context.getString(R.string.not_logged_in_message), Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(id = R.string.tambah_menu)
                )
            }
        }
    ) { innerPadding ->
        ScreenContent(
            viewModel = viewModel,
            user = user,
            displayMode = currentDisplayMode,
            onDeleteClick = { menuItem ->
                menuItemToDelete = menuItem
                showHapusDialog = true
            },
            onEditClick = { menuItem ->
                viewModel.onEditClick(menuItem)
                showMenuDialog = true
            },
            modifier = Modifier.padding(innerPadding)
        )

        if (showMenuDialog) {
            MenuDialog(
                menuItem = viewModel.menuItemToEdit.value,
                bitmap = bitmap,
                onDismissRequest = {
                    showMenuDialog = false
                    viewModel.onDialogDismiss()
                    bitmap = null
                },
                onConfirmation = { id, nama, deskripsi, harga ->
                    if (id != null) {
                        viewModel.updateMenuItem(id, nama, deskripsi, harga)
                    } else {
                        bitmap?.let { viewModel.createMenuItem(nama, deskripsi, harga, it) }
                    }
                    showMenuDialog = false
                    viewModel.onDialogDismiss()
                    bitmap = null
                }
            )
        }

        if (showProfilDialog) {
            ProfilDialog(
                user = user,
                onDismissRequest = { showProfilDialog = false }) {
                CoroutineScope(Dispatchers.IO).launch { signOut(context, dataStore) }
                showProfilDialog = false
            }
        }

        if (showHapusDialog) {
            HapusDialog(
                onDismissRequest = {
                    showHapusDialog = false
                    menuItemToDelete = null
                },
                onConfirmation = {
                    menuItemToDelete?.let { viewModel.deleteMenuItem(it.id) }
                    showHapusDialog = false
                    menuItemToDelete = null
                }
            )
        }

        // NEW: Location Detail Dialog
        if (showLocationDetailDialog) {
            LocationDetailDialog(
                locationName = currentLocationName,
                latitude = currentLocation?.latitude,
                longitude = currentLocation?.longitude,
                onDismissRequest = { showLocationDetailDialog = false }
            )
        }


        if (viewModel.isLoading.value) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearMessage()
        }
    }
}

@Composable
fun ScreenContent(
    viewModel: MainViewModel,
    user: User,
    displayMode: DisplayMode,
    onDeleteClick: (MenuItem) -> Unit,
    onEditClick: (MenuItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val data by viewModel.data
    val status by viewModel.status
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.retrieveMenu()
    }

    when (status) {
        ApiStatus.LOADING -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        ApiStatus.SUCCESS -> {
            if (data.isEmpty()) {
                Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.empty_menu_message),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                if (displayMode == DisplayMode.GRID) {
                    LazyVerticalGrid(
                        modifier = modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp),
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(data, key = { it.id }) { menuItem ->
                            MenuItemGridCard(
                                menuItem = menuItem,
                                isUserLoggedIn = user.email.isNotEmpty(),
                                onDeleteClick = {
                                    if (user.email.isNotEmpty()) {
                                        onDeleteClick(menuItem)
                                    } else {
                                        Toast.makeText(context, context.getString(R.string.not_logged_in_message), Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onEditClick = {
                                    if (user.email.isNotEmpty()) {
                                        onEditClick(menuItem)
                                    } else {
                                        Toast.makeText(context, context.getString(R.string.not_logged_in_message), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(data, key = { it.id }) { menuItem ->
                            MenuItemListCard(
                                menuItem = menuItem,
                                isUserLoggedIn = user.email.isNotEmpty(),
                                onDeleteClick = {
                                    if (user.email.isNotEmpty()) {
                                        onDeleteClick(menuItem)
                                    } else {
                                        Toast.makeText(context, context.getString(R.string.not_logged_in_message), Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onEditClick = {
                                    if (user.email.isNotEmpty()) {
                                        onEditClick(menuItem)
                                    } else {
                                        Toast.makeText(context, context.getString(R.string.not_logged_in_message), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
        ApiStatus.FAILED -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleMedium
                )
                Button(
                    onClick = { viewModel.retrieveMenu() },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(text = stringResource(id = R.string.coba_lagi))
                }
            }
        }
    }
}

@Composable
fun MenuItemGridCard(
    menuItem: MenuItem,
    isUserLoggedIn: Boolean,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val isExcludedItem = remember(menuItem.nama) {
        menuItem.nama.equals("Nasi Goreng Spesial", ignoreCase = true) ||
                menuItem.nama.equals("Mie Ayam Komplit", ignoreCase = true) ||
                menuItem.nama.equals("Es Kopi Susu Gula Aren", ignoreCase = true)
    }

    val showActions = isUserLoggedIn && !isExcludedItem

    Box(
        modifier = Modifier
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(menuItem.foto)
                .crossfade(true)
                .build(),
            contentDescription = stringResource(R.string.gambar, menuItem.nama),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(id = R.drawable.loading_img),
            error = painterResource(id = R.drawable.broken_img),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f))
                .padding(8.dp)
        ) {
            Text(
                text = menuItem.nama,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = menuItem.deskripsi,
                fontStyle = FontStyle.Normal,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Rp ${"%,d".format(Locale.GERMAN, menuItem.harga)}",
                fontStyle = FontStyle.Italic,
                fontSize = 14.sp,
                color = Color(0xFF8BC34A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (showActions) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(35.dp)
                        .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
                        .clickable { onEditClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_edit),
                        contentDescription = stringResource(R.string.edit),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(35.dp)
                        .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
                        .clickable { onDeleteClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.hapus),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MenuItemListCard(
    menuItem: MenuItem,
    isUserLoggedIn: Boolean,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val isExcludedItem = remember(menuItem.nama) {
        menuItem.nama.equals("Nasi Goreng Spesial", ignoreCase = true) ||
                menuItem.nama.equals("Mie Ayam Komplit", ignoreCase = true) ||
                menuItem.nama.equals("Es Kopi Susu Gula Aren", ignoreCase = true)
    }

    val showActions = isUserLoggedIn && !isExcludedItem

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(menuItem.foto)
                    .crossfade(true)
                    .build(),
                contentDescription = stringResource(R.string.gambar, menuItem.nama),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.loading_img),
                error = painterResource(id = R.drawable.broken_img),
                modifier = Modifier
                    .size(96.dp)
                    .clip(MaterialTheme.shapes.small)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 8.dp)
            ) {
                Text(
                    text = menuItem.nama,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = menuItem.deskripsi,
                    fontStyle = FontStyle.Normal,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = "Rp ${"%,d".format(Locale.GERMAN, menuItem.harga)}",
                    fontStyle = FontStyle.Italic,
                    fontSize = 14.sp,
                    color = Color(0xFF4CAF50),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (showActions) {
                Column(
                    modifier = Modifier.padding(end = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(35.dp)
                            .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
                            .clickable { onEditClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_edit),
                            contentDescription = stringResource(R.string.edit),
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .size(35.dp)
                            .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
                            .clickable { onDeleteClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.hapus),
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}


private suspend fun signIn(context: Context, dataStore: UserDataStore) {
    val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(BuildConfig.API_KEY)
        .build()

    val request: GetCredentialRequest = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()
    try {
        val credentialManager = CredentialManager.create(context)
        val result = credentialManager.getCredential(context, request)
        handleSignIn(result, dataStore)
    } catch (e: GetCredentialException) {
        Log.e("SIGN-IN", "Error: ${e.errorMessage}")
    }
}

private suspend fun handleSignIn(
    result: GetCredentialResponse,
    dataStore: UserDataStore
) {
    val credential = result.credential
    if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        try {
            val googleId = GoogleIdTokenCredential.createFrom(credential.data)
            val nama = googleId.displayName ?: ""
            val email = googleId.id
            val photoUrl = googleId.profilePictureUri.toString()
            dataStore.saveData(User(nama, email, photoUrl))
        } catch (e: GoogleIdTokenParsingException) {
            Log.e("SIGN-IN", "Error: ${e.message}")
        }
    } else {
        Log.e("SIGN-IN", "Error: unrecognized custom credential type.")
    }
}

private suspend fun signOut(context: Context, dataStore: UserDataStore) {
    try {
        val credentialManager = CredentialManager.create(context)
        credentialManager.clearCredentialState(
            ClearCredentialStateRequest()
        )
        dataStore.saveData(User())
    } catch (e: ClearCredentialException) {
        Log.e("SIGN-OUT", "Error: ${e.message}")
    }
}

private fun getCroppedImage(
    resolver: ContentResolver,
    result: CropImageView.CropResult
): Bitmap? {
    if (!result.isSuccessful) {
        Log.e("IMAGE", "Error: ${result.error?.message}")
        return null
    }

    val uri = result.uriContent ?: return null

    return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.getBitmap(resolver, uri)
    } else {
        val source = ImageDecoder.createSource(resolver, uri)
        ImageDecoder.decodeBitmap(source)
    }
}

private fun startLocationUpdates(
    fusedLocationClient: FusedLocationProviderClient,
    locationCallback: LocationCallback,
    context: Context
) {
    val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        10000
    )
        .setMinUpdateIntervalMillis(5000)
        .build()

    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    ) {
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            Log.d("LOCATION", "Memulai pembaruan lokasi")
        } catch (e: SecurityException) {
            Log.e("LOCATION", "Izin lokasi tidak diberikan: ${e.message}")
            Toast.makeText(context, "Izinkan lokasi agar fitur berfungsi", Toast.LENGTH_LONG).show()
        }
    } else {
        Log.e("LOCATION", "Izin lokasi tidak tersedia untuk memulai pembaruan")
    }
}

private fun stopLocationUpdates(
    fusedLocationClient: FusedLocationProviderClient,
    locationCallback: LocationCallback
) {
    fusedLocationClient.removeLocationUpdates(locationCallback)
    Log.d("LOCATION", "Menghentikan pembaruan lokasi")
}

@Suppress("DEPRECATION")
private fun getLocationName(context: Context, latitude: Double, longitude: Double): String? {
    val geocoder = Geocoder(context, Locale.getDefault())
    return try {
        val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(latitude, longitude, 1)
        } else {
            geocoder.getFromLocation(latitude, longitude, 1)
        }

        if (!addresses.isNullOrEmpty()) {
            val address = addresses[0]
            val fullAddress = StringBuilder()
            // Loop through address lines to get a more complete address
            for (i in 0..address.maxAddressLineIndex) {
                fullAddress.append(address.getAddressLine(i)).append("\n")
            }
            fullAddress.toString().trim() // Remove trailing newline if any
        } else {
            null
        }
    } catch (e: IOException) {
        Log.e("LOCATION_NAME", "Geocoder service not available or network error: ${e.message}")
        null
    } catch (e: IllegalArgumentException) {
        Log.e("LOCATION_NAME", "Invalid lat/long: ${e.message}")
        null
    }
}

// NEW: Composable for Location Detail Dialog
@Composable
fun LocationDetailDialog(
    locationName: String?,
    latitude: Double?,
    longitude: Double?,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(R.string.your_current_location)) },
        text = {
            Column {
                if (locationName != null && locationName.isNotBlank()) {
                    Text(text = stringResource(R.string.full_address), fontWeight = FontWeight.Bold)
                    Text(text = locationName)
                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    Text(text = stringResource(R.string.address_not_found))
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(text = stringResource(R.string.coordinates), fontWeight = FontWeight.Bold)
                Text(text = "Latitude: ${latitude ?: "N/A"}")
                Text(text = "Longitude: ${longitude ?: "N/A"}")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.tutup))
            }
        }
    )
}