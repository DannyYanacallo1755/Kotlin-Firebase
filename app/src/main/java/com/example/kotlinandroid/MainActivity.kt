package com.example.kotlinandroid

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.InputStream

class MainActivity : ComponentActivity() {

    private lateinit var imageView: ImageView
    private lateinit var selectImageButton: Button
    private lateinit var uploadImageButton: Button

    private var imageUri: Uri? = null

    private val storageReference: StorageReference by lazy {
        FirebaseStorage.getInstance().reference
    }

    private val databaseReference: DatabaseReference by lazy {
        FirebaseDatabase.getInstance().getReference("images")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView = findViewById(R.id.image_view)
        selectImageButton = findViewById(R.id.btn_select_image)
        uploadImageButton = findViewById(R.id.btn_upload_image)

        selectImageButton.setOnClickListener {
            openFileChooser()
        }

        uploadImageButton.setOnClickListener {
            uploadImage()
        }
    }

    private fun openFileChooser() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            imageUri = data.data

            val inputStream: InputStream? = contentResolver.openInputStream(imageUri!!)
            val bitmap: Bitmap = BitmapFactory.decodeStream(inputStream)
            imageView.setImageBitmap(bitmap)
        }
    }

    private fun uploadImage() {
        if (imageUri != null) {
            val fileReference = storageReference.child("images/${System.currentTimeMillis()}.jpg")

            fileReference.putFile(imageUri!!)
                .addOnSuccessListener {
                    fileReference.downloadUrl.addOnSuccessListener { uri ->
                        val imageUrl = uri.toString()
                        saveImageUrlToDatabase(imageUrl)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MainActivity", "Upload failed: ${e.message}")
                }
        }
    }

    private fun saveImageUrlToDatabase(imageUrl: String) {
        val imageId = databaseReference.push().key ?: return
        databaseReference.child(imageId).setValue(imageUrl)
            .addOnSuccessListener {
                Log.d("MainActivity", "Image URL saved to database")
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Failed to save URL to database: ${e.message}")
            }
    }

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }
}
