package codingEmpires.maizedisesedetection

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import codingEmpires.maizedisesedetection.databinding.ActivityDiseaseDetectionBinding
import com.bumptech.glide.Glide

class ActivityDiseaseDetection : AppCompatActivity() {
    lateinit var binding:ActivityDiseaseDetectionBinding
    private lateinit var mContext: Context
    private var currentCameraId = 0


    companion object {
        private const val REQUEST_PICK_IMAGE = 1
        const val REQUEST_IMAGE_CAPTURE = 2

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_disease_detection)
        binding = ActivityDiseaseDetectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mContext=this@ActivityDiseaseDetection
        openCamera()



        binding.galleryImg.setOnClickListener {
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(galleryIntent, REQUEST_PICK_IMAGE)
        }

        binding.flipCamera.setOnClickListener { flipCamera() }
        binding.clickImg.setOnClickListener { captureImage() }
        binding.galleryImg.setOnClickListener { pickImageFromGallery() }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            // Get the selected image URI
            val imageUri = data.data

            // Set the selected image to the ImageView directly
            binding.displayedImage.setImageURI(imageUri)
        } else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK && data != null) {
            // For camera capture, set the image to the ImageView directly
            val imageBitmap = data.extras?.get("data") as Bitmap
            binding.displayedImage.setImageBitmap(imageBitmap)
        }
    }
    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE)
    }

    private fun flipCamera() {
        // Change the camera id to switch between front and back cameras
        currentCameraId = if (currentCameraId == 0) 1 else 0
        openCamera()
    }

    private fun captureImage() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE)
    }
    private fun pickImageFromGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, REQUEST_PICK_IMAGE)
    }

}