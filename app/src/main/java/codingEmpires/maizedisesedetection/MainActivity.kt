package codingEmpires.maizedisesedetection

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import codingEmpires.maizedisesedetection.ml.ModelUnquant
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder


class MainActivity : AppCompatActivity() {

    private lateinit var camera: Button
    private lateinit var gallery: Button
    private lateinit var imageView: ImageView
    private lateinit var result: TextView
    private val imageSize = 32

    private val CAMERA_REQUEST_CODE = 3
    private val GALLERY_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val swipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.refreshLayout)

        // Refresh function for the layout
        swipeRefreshLayout.setOnRefreshListener{

            swipeRefreshLayout.isRefreshing = false
        }

        camera = findViewById(R.id.button)
        gallery = findViewById(R.id.button2)
        imageView = findViewById(R.id.imageView)
        result = findViewById(R.id.result)

        camera.setOnClickListener {
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
            } else {
                requestPermissions(arrayOf(Manifest.permission.CAMERA), 100)
            }
        }

        gallery.setOnClickListener {
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE)
        }
    }

    private fun classifyImage(image: Bitmap) {
        try {
            // Load the SVM model
            val svmModel = ModelUnquant.newInstance(applicationContext)

            // Convert Bitmap to ByteBuffer for the SVM model
            val inputShapeSVM = intArrayOf(1, 4)
            val byteBufferSVM = convertBitmapToByteBuffer(image, inputShapeSVM)

            // Create inputs for reference for the SVM model
            val inputFeature0SVM = TensorBuffer.createFixedSize(inputShapeSVM, DataType.FLOAT32)
            inputFeature0SVM.loadBuffer(byteBufferSVM)

            // Run model inference for the SVM model
            val outputsSVM = svmModel.process(inputFeature0SVM)
            val outputFeature0SVM = outputsSVM.outputFeature0AsTensorBuffer

            // Determine the predicted class index for the SVM model
            val maxIndexSVM = outputFeature0SVM.floatArray.indices.maxByOrNull { outputFeature0SVM.floatArray[it] }
            val predictedClassIndexSVM = maxIndexSVM ?: -1 // Default value if no maximum value is found

            // Close the SVM model
            svmModel.close()

            // Load the ModelUnquant model
            val modelUnquant = ModelUnquant.newInstance(applicationContext)

            // Convert Bitmap to ByteBuffer for the ModelUnquant model
            val inputShapeUnquant = intArrayOf(1, 224, 224, 3)
            val byteBufferUnquant = convertBitmapToByteBuffer(image, inputShapeUnquant)

            // Create inputs for reference for the ModelUnquant model
            val inputFeature0Unquant = TensorBuffer.createFixedSize(inputShapeUnquant, DataType.FLOAT32)
            inputFeature0Unquant.loadBuffer(byteBufferUnquant)

            // Run model inference for the ModelUnquant model
            val outputsUnquant = modelUnquant.process(inputFeature0Unquant)
            val outputFeature0Unquant = outputsUnquant.outputFeature0AsTensorBuffer

            // Determine the predicted class index for the ModelUnquant model
            val maxIndexUnquant = outputFeature0Unquant.floatArray.indices.maxByOrNull { outputFeature0Unquant.floatArray[it] }
            val predictedClassIndexUnquant = maxIndexUnquant ?: -1 // Default value if no maximum value is found

            // Close the ModelUnquant model
            modelUnquant.close()

            // Update the UI with the predicted classes for both models
            val classesUnquant = arrayOf("Blight", "Common_Rust", "Gray_Leaf_Spot", "Healthy")
            result.text = classesUnquant[predictedClassIndexSVM]

            // Optionally, update the UI with the predicted class for the ModelUnquant model
            // result.text = classesUnquant[predictedClassIndexUnquant]
        } catch (e: IOException) {
            // Handle the exception
        }
    }

    // Helper function to convert Bitmap to ByteBuffer
    private fun convertBitmapToByteBuffer(bitmap: Bitmap, inputShape: IntArray): ByteBuffer {
        // Ensure that inputShape contains at least three elements
        if (inputShape.size < 3) {
            throw IllegalArgumentException("Input shape array must have at least three elements")
        }
        // Calculate the input size based on the input shape
        val inputSize = inputShape.reduce { acc, i -> acc * i }

        // Allocate memory for the ByteBuffer
        val byteBuffer = ByteBuffer.allocateDirect(inputSize * 4) // 4 bytes per float
        byteBuffer.order(ByteOrder.nativeOrder())

        // Resize the bitmap to match the input shape's width and height
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputShape[1], inputShape[2], true)

        // Get the pixels of the resized bitmap
        val intValues = IntArray(inputShape[1] * inputShape[2])
        resizedBitmap.getPixels(intValues, 0, inputShape[1], 0, 0, inputShape[1], inputShape[2])

        // Convert pixel values to floating point and store them in the ByteBuffer
        for (pixelValue in intValues) {
            val r = (pixelValue shr 16) and 0xFF
            val g = (pixelValue shr 8) and 0xFF
            val b = pixelValue and 0xFF

            // Normalize pixel values to [0, 1] range and convert to float
            val rf = r.toFloat() / 255.0f
            val gf = g.toFloat() / 255.0f
            val bf = b.toFloat() / 255.0f

            byteBuffer.putFloat(rf)
            byteBuffer.putFloat(gf)
            byteBuffer.putFloat(bf)
        }

        // Rewind the ByteBuffer and return it
        byteBuffer.rewind()
        return byteBuffer
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, @Nullable data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == CAMERA_REQUEST_CODE) {
                val image = data?.extras?.get("data") as Bitmap?
                var dimension = image?.width ?: 0
                dimension = Math.min(dimension, image?.height ?: 0)
                val thumbnail = ThumbnailUtils.extractThumbnail(image!!, dimension, dimension)
                imageView.setImageBitmap(thumbnail)
                val scaledImage = Bitmap.createScaledBitmap(thumbnail, imageSize, imageSize, false)
                classifyImage(scaledImage)
            } else if (requestCode == GALLERY_REQUEST_CODE) {
                val uri = data?.data
                var image: Bitmap? = null
                try {
                    image = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                imageView.setImageBitmap(image)
                val scaledImage = Bitmap.createScaledBitmap(image!!, imageSize, imageSize, false)
                classifyImage(scaledImage)
            }
        }
    }
}
