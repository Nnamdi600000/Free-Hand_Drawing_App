package com.codennamdi.drawingapp

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var drawingView: DrawingView
    private lateinit var mImageButtonCurrentPaint: ImageButton
    private lateinit var progressDialog: Dialog

    private val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val imageBackground: ImageView = findViewById(R.id.image_view_background)
                imageBackground.setImageURI(result.data?.data)
            }
        }

    private val permissionRequest: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permission ->
            permission.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value

                if (isGranted) {
                    Toast.makeText(
                        this,
                        "Permission granted for External storage",
                        Toast.LENGTH_LONG
                    ).show()

                    val pickIntent =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    openGalleryLauncher.launch(pickIntent)

                } else {
                    if (permissionName == Manifest.permission.READ_EXTERNAL_STORAGE) {
                        Toast.makeText(
                            this,
                            "Permission denied for External storage",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawingView = findViewById(R.id.drawing_view)
        drawingView.setSizeForBrush(20.toFloat())

        val linearLayoutPaintColours = findViewById<LinearLayout>(R.id.linear_layout_brush_colors)
        mImageButtonCurrentPaint = linearLayoutPaintColours[1] as ImageButton
        mImageButtonCurrentPaint.setImageDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.pallet_selected
            )
        )
        setUpButtonClickListener()
    }

    private fun setUpButtonClickListener() {
        val imageBtnBrush: ImageButton = findViewById(R.id.image_button_brush)

        imageBtnBrush.setOnClickListener {
            showTheBrushSizePickerDialog()
        }

        findViewById<ImageButton>(R.id.image_button_import_images).setOnClickListener {
            requestStoragePermission()
        }

        findViewById<ImageButton>(R.id.image_button_undo_btn).setOnClickListener {
            drawingView.unClickUndo()
        }

        findViewById<ImageButton>(R.id.image_button_redo_btn).setOnClickListener {
            drawingView.unClickRedo()
        }

        findViewById<ImageButton>(R.id.image_button_save_image).setOnClickListener {
            if (isReadStorageAllowed()) {
                showProgressDialog()
                lifecycleScope.launch {
                    val frameLayoutDrawingView: FrameLayout =
                        findViewById(R.id.frame_layout_container)

                    saveBitmapFile(getBitmapFromView(frameLayoutDrawingView)) //Here we are calling the saveBitmapFile() and getBitmapFromView()
                }
            }
        }
    }

    private fun showTheBrushSizePickerDialog() {
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush size: ")

        val verySmallBtn: ImageButton = brushDialog.findViewById(R.id.image_button_very_small_brush)
        verySmallBtn.setOnClickListener {
            drawingView.setSizeForBrush(5.toFloat())
            brushDialog.dismiss()
        }

        val smallBtn: ImageButton = brushDialog.findViewById(R.id.image_button_small_brush)
        smallBtn.setOnClickListener {
            drawingView.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }

        val mediumBtn: ImageButton = brushDialog.findViewById(R.id.image_button_medium_brush)
        mediumBtn.setOnClickListener {
            drawingView.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }

        val largeBtn: ImageButton = brushDialog.findViewById(R.id.image_button_large_brush)
        largeBtn.setOnClickListener {
            drawingView.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }

        val xLargeBtn: ImageButton = brushDialog.findViewById(R.id.image_button_x_large_brush)
        xLargeBtn.setOnClickListener {
            drawingView.setSizeForBrush(50.toFloat())
            brushDialog.dismiss()
        }

        val xxLargeBtn: ImageButton = brushDialog.findViewById(R.id.image_button_xx_large_brush)
        xxLargeBtn.setOnClickListener {
            drawingView.setSizeForBrush(70.toFloat())
            brushDialog.dismiss()
        }
        brushDialog.show()
    }

    fun paintClicked(view: View) {
        if (view !== mImageButtonCurrentPaint) {
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawingView.setColor(colorTag)

            imageButton.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.pallet_selected
                )
            )
            mImageButtonCurrentPaint.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_normal)
            )
            mImageButtonCurrentPaint = view
        }
    }

    private fun requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {
            showRationaleDialog(
                "Drawing App",
                "Drawing app" + "Needs To Access Your External Storage"
            )
        } else {
            permissionRequest.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    private fun isReadStorageAllowed(): Boolean {
        val result =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) //We are checking if permission is granted or denied.

        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun showRationaleDialog(title: String, message: String) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()
    }

    private fun getBitmapFromView(view: View): Bitmap {
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val backgroundDrawable = view.background

        if (backgroundDrawable != null) {
            backgroundDrawable.draw(canvas)
        } else {
            canvas.drawColor(Color.WHITE)
        }

        view.draw(canvas)
        return returnedBitmap
    }

    /**
     * This function handles the saving of the Bitmap, which is the image that would be saved.
     */
    private suspend fun saveBitmapFile(mBitmap: Bitmap?): String {
        var result = ""
        withContext(Dispatchers.IO) {
            if (mBitmap != null) {
                try {
                    val bytes =
                        ByteArrayOutputStream() // The buffer capacity are usually 32 bytes but they can increase in size, if necessary.
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)

                    val file =
                        File(externalCacheDir?.absoluteFile.toString() + File.separator + "DrawingApp_" + System.currentTimeMillis() / 1000 + ".png")

                    val fileOutput = FileOutputStream(file)
                    fileOutput.write(bytes.toByteArray())
                    fileOutput.close()

                    result = file.absolutePath

                    runOnUiThread {
                        cancelProgressDialog()
                        if (result.isNotEmpty()) {
                            shareImage(result) // Where we called the share Function.
                            Toast.makeText(
                                this@MainActivity,
                                "File saved at: $result",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "Something went wrong saving the file",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return result
    }

    private fun cancelProgressDialog() {
        progressDialog.dismiss()
    }

    private fun showProgressDialog() {
        progressDialog = Dialog(this)

        progressDialog.setContentView(R.layout.dialog_progress)

        progressDialog.show()
    }

    /**
     * This function is responsible for calling the share panel by using Intent.
     */
    private fun shareImage(result: String) {
        MediaScannerConnection.scanFile(this, arrayOf(result), null) { _, url ->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM, url)
            shareIntent.type = "image/png"
            startActivity(Intent.createChooser(shareIntent, "Share"))
        }
    }
}