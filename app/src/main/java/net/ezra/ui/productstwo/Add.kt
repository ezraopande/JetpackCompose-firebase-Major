package net.ezra.ui.productstwo

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import net.ezra.R
import java.io.ByteArrayOutputStream
import kotlin.random.Random

// Model class for product
data class Product(
    val name: String,
    val description: String,
    val imageUrls: List<String> = emptyList()
)

// Global variable for selected images
var selectedImages by mutableStateOf<List<Uri>>(emptyList())

@Composable
fun AddProductTwo(navController: NavHostController) {
    val context = LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            onImageSelected(uri)
        }
    }

    var productName by remember { mutableStateOf("") }
    var productDescription by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextField(
            value = productName,
            onValueChange = { productName = it },
            label = { Text("Product Name") },
            modifier = Modifier.fillMaxWidth()
        )
        TextField(
            value = productDescription,
            onValueChange = { productDescription = it },
            label = { Text("Product Description") },
            modifier = Modifier.fillMaxWidth()
        )
        // Display selected images
        selectedImages.forEach { uri ->
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = null,
                modifier = Modifier.size(100.dp)
            )
        }
        Button(
            onClick = { imagePicker.launch("image/*") },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Add Image")
        }
        Button(
            onClick = {
                val product = Product(
                    name = productName,
                    description = productDescription
                )
                addProduct(product)
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Add Product")
        }
    }
}

fun onImageSelected(uri: Uri) {
    // Convert URI to string and add it to the list of selected images
    selectedImages = selectedImages.toMutableList().apply { add(uri) }
}

fun addProduct(product: Product) {
    val db = Firebase.firestore
    val storage = Firebase.storage
    val productImages = mutableListOf<String>()

    // Upload images to Storage and get download URLs
    val productRef = db.collection("productstwo").document()
    val productId = productRef.id

    for (imageUri in selectedImages) {
        val imageRef = storage.reference.child("imagestwo/$productId/${Random.nextInt()}_${System.currentTimeMillis()}")
        val uploadTask = imageRef.putFile(imageUri)
        uploadTask.addOnSuccessListener { taskSnapshot ->
            taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                val imageUrl = uri.toString()
                productImages.add(imageUrl)
                // Check if all images are uploaded
                if (productImages.size == selectedImages.size) {
                    // All images uploaded, add product to Firestore
                    val productWithImages = product.copy(imageUrls = productImages)
                    addProductToFirestore(productRef, productWithImages)
                }
            }
        }
    }
}

fun addProductToFirestore(productRef: DocumentReference, product: Product) {
    // Add product to Firestore
    productRef.set(product)
        .addOnSuccessListener {
            println("Product added with ID: ${productRef.id}")
        }
        .addOnFailureListener { e ->
            println("Error adding product: $e")
        }
}
