package net.ezra.ui.productstwo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

// Model class for product
data class ProductTwo(
    val name: String? = "",
    val description: String = "",
    val imageUrls: List<String> = emptyList()
)

sealed class LoadedImageResult {
    object Loading : LoadedImageResult()
    data class Success(val bitmap: Bitmap) : LoadedImageResult()
    object Error : LoadedImageResult()
}

@Composable
fun ProductListScreenTwo(navController: NavController) {
    var products by remember { mutableStateOf(emptyList<ProductTwo>()) }

    // Fetch products from Firestore
    LaunchedEffect(true) {
        val firestore = Firebase.firestore
        val productsCollection = firestore.collection("productstwo")
        try {
            val snapshot = productsCollection.get().await()
            val productList = mutableListOf<ProductTwo>()
            for (doc in snapshot.documents) {
                val product = doc.toObject(ProductTwo::class.java)
                product?.let { productList.add(it) }
            }
            products = productList
        } catch (e: Exception) {
            // Handle error fetching products
            // For example, show a snackbar with an error message
            // or retry option
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Products")
        products.forEach { product ->
            ProductItemTwo(product)
        }
    }
}

@Composable
fun ProductItemTwo(product: ProductTwo) {
    val firstImageUrl = remember(product) { product.imageUrls.firstOrNull() }

    Row(verticalAlignment = Alignment.CenterVertically) {
        // Load and display the first image as product image
        LoadImage(url = firstImageUrl, modifier = Modifier.size(100.dp))

        Spacer(modifier = Modifier.width(16.dp))
        Column {
            product.name?.let { Text(text = it) }
            Text(text = product.description)
        }
    }
}

@Composable
fun LoadImage(url: String?, modifier: Modifier = Modifier) {
    val bitmapState = remember { mutableStateOf<Bitmap?>(null) }
    val errorState = remember { mutableStateOf(false) }

    LaunchedEffect(url) {
        if (!url.isNullOrEmpty()) {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    loadImageBitmap(url)
                }
                bitmapState.value = bitmap
            } catch (e: IOException) {
                errorState.value = true
            }
        }
    }

    Box(modifier = modifier) {
        when {
            bitmapState.value != null -> {
                Image(
                    bitmap = bitmapState.value!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            errorState.value -> {
                // Display error UI
                // For example, show an error image or message
                Text(text = "Error loading image", modifier = Modifier.fillMaxSize())
            }
            else -> {
                // Display loading UI
                // For example, show a progress indicator
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Throws(IOException::class)
suspend fun loadImageBitmap(url: String): Bitmap {
    // Use an image loading library like Coil or Glide for more efficient and cached image loading
    // For example:
    // return CoilImageLoader().loadBitmap(url)

    // For demonstration, using a simple method to load bitmap from URL
    val connection = URL(url).openConnection() as HttpURLConnection
    return try {
        connection.connect()
        BitmapFactory.decodeStream(connection.inputStream)
    } finally {
        connection.disconnect()
    }
}
