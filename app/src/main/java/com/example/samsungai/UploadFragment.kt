package com.example.samsungai

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject


private const val REQUEST_CODE = 4

private const val GALLERY_PICK = 5

@AndroidEntryPoint
class UploadFragment : Fragment() {

    @Inject
    lateinit var fedonApi: api

    lateinit var root: View

    lateinit var currentPhotoPath: String

    private var photoCheck = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {


        // Inflate the layout for this fragment
        root = inflater.inflate(R.layout.fragment_upload, container, false)


        val button = root.findViewById<Button>(R.id.btn_upload_img)

        button.setOnClickListener {

            if (!::currentPhotoPath.isInitialized) {
                Toast.makeText(context, "Выберите изображение", Toast.LENGTH_SHORT).show()
            } else {
                val file = File(currentPhotoPath)
                //Toast.makeText(context, currentPhotoPath, Toast.LENGTH_SHORT).show()
                val requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file)

                val body = MultipartBody.Part.createFormData("image", file.name, requestFile)


                val resp = fedonApi.uploadPic(body).enqueue(object : Callback<Response> {
                    override fun onResponse(
                        call: Call<Response>,
                        response: retrofit2.Response<Response>
                    ) {
                        val textView = root.findViewById<TextView>(R.id.tv_calories)

                        response.body()?.calories?.let { it1 -> textView.setText("$it1 ккал") }
                        photoCheck = false
                    }

                    override fun onFailure(call: Call<Response>, t: Throwable) {
                        //Toast.makeText(context, t.stackTraceToString(), Toast.LENGTH_LONG).show()
                    }
                })
            }

        }

        val btn_take_img = root.findViewById<Button>(R.id.btn_take_image)

        btn_take_img.setOnClickListener {
            dispatchTakePictureIntent()
        }

        val btn_pick_img = root.findViewById<Button>(R.id.btn_select_img)

        btn_pick_img.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, GALLERY_PICK)
        }

        return root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val imageView = root.findViewById<ImageView>(R.id._iv_uploaded_image)

        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            val image = BitmapFactory.decodeFile(currentPhotoPath)
            imageView.setImageBitmap(image)
            photoCheck = true
        }
        if (requestCode == GALLERY_PICK && resultCode == RESULT_OK) {
            imageView.setImageURI(data?.data)

            val selectedImage = data?.data
            val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)

            val cursor = context?.contentResolver?.query(selectedImage!!, filePathColumn, null, null, null)
            assert(cursor != null)

            cursor!!.moveToFirst()

            val columnIndex = cursor.getColumnIndex(filePathColumn[0])

            val mediaPath = cursor.getString(columnIndex)

            cursor.close()


            currentPhotoPath = mediaPath

            photoCheck = true
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            context?.let {
                takePictureIntent.resolveActivity(it.packageManager)?.also {
                    // Create the File where the photo should go
                    val photoFile: File? = try {
                        createImageFile()
                    } catch (ex: IOException) {
                        // Error occurred while creating the File
                        null
                    }
                    // Continue only if the File was successfully created
                    photoFile?.also {
                        val photoURI: Uri = FileProvider.getUriForFile(
                            requireContext(),
                            "com.example.android.fileprovider",
                            it
                        )
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                        startActivityForResult(takePictureIntent, REQUEST_CODE)
                    }
                }
            }
        }
    }

}