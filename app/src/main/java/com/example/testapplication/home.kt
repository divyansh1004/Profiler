package com.example.testapplication

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import com.example.testapplication.databinding.ActivityHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_home.*
import java.io.ByteArrayOutputStream
import java.io.File


class home : AppCompatActivity() {
    lateinit var auth: FirebaseAuth

    lateinit var binding: ActivityHomeBinding
    lateinit var database: DatabaseReference
    lateinit var storageReference: StorageReference
    lateinit var imageUri: Uri

    val REQUEST_IMAGE_CAPTURE = 1
    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)





        auth = FirebaseAuth.getInstance()
        val currentuser = auth.currentUser
        val id = currentuser?.uid

        val metadata = currentuser!!.metadata
        val sharedpreference = getSharedPreferences("$id", MODE_PRIVATE)

        if (metadata!!.creationTimestamp == metadata!!.lastSignInTimestamp) {
            Log.d("YES", "New user")
            // The user is new, show them a fancy intro screen!
            image_btn.setOnClickListener {
                Log.d("Main", "Photo selector")
                val popup = PopupMenu(this, image_btn)
                popup.inflate(R.menu.popup_menu)

                popup.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item: MenuItem? ->

                    when (item!!.itemId) {
                        R.id.camera -> {
                            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                            try {
                                @Suppress("DEPRECATION")
                                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                            } catch (e: ActivityNotFoundException) {
                                Log.d("Error", "errorcamera $e")
                            }
                        }


                        R.id.upload -> {
                            val intent = Intent(Intent.ACTION_PICK)
                            intent.type = "image/*"
                            @Suppress("DEPRECATION")
                            startActivityForResult(intent, 0)


                        }
                    }

                    true
                })

                popup.show()


            }
            @Suppress("DEPRECATION")


            val editor = sharedpreference.edit()
            binding.idSave.setOnClickListener {
                val name = binding.idName.text.toString()
                val email = binding.idEmail.text.toString()
                val phoneno = binding.idPhone.text.toString()

                editor.apply {
                    putString("name", name)
                    putString("email", email)
                    putString("phone", phoneno)
                    putString("image", encodetobse64string)
                    apply()

                }
                database = FirebaseDatabase.getInstance().getReference("Users")
                val User = User(name, email, phoneno)
                if (id != null) {
                    database.child(id).setValue(User).addOnSuccessListener {

                        uploadprofilepid()
                        Toast.makeText(this, "Succesful", Toast.LENGTH_SHORT).show()

                    }
                        .addOnFailureListener {
                            Log.d("WOW", "error is $it")
                            Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show()
                        }

                }

            }

        } else {
            Log.d("NO", "existing user")

            if (id != null) {
                if (id.isNotEmpty()) {
                    readData(id)
                } else {
                    Toast.makeText(this, "invaild name", Toast.LENGTH_SHORT).show()

                }
            }


            val name = sharedpreference.getString("name", null)
            val email = sharedpreference.getString("email", null)
            val phone = sharedpreference.getString("phone", null)
            val iiiage = sharedpreference.getString("image", null)


            id_name.setText(name)
            id_email.setText(email)
            id_phone.setText(phone)

            val baseimage = Base64.decode(iiiage, Base64.DEFAULT)
            val finalimage = BitmapFactory.decodeByteArray(baseimage, 0, baseimage.size)
            selectimage_register.setImageBitmap(finalimage)
            image_btn.alpha = 0f
            Log.d("No", "Existing user")
        }


        val logoutt = findViewById<Button>(R.id.idLogout)

        if (currentuser == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        logoutt.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

    }

    private fun readData(id: String) {
        database = FirebaseDatabase.getInstance().getReference("Users")
        database.child(id).get().addOnSuccessListener {
            if (it.exists()) {
                val namee = it.child("name").value
                val email = it.child("username").value
                val phone = it.child("phoneno").value
                Toast.makeText(this, "retreival sucessfull", Toast.LENGTH_SHORT).show()
                id_name.setText(namee.toString())
                id_email.setText(email.toString())
                id_phone.setText(phone.toString())

                storageReference = FirebaseStorage.getInstance().getReference("Users" + id)
                val localfile = File.createTempFile("tempimage", "png")
                storageReference.getFile(localfile).addOnSuccessListener {

                    val bitmap = BitmapFactory.decodeFile(localfile.absolutePath)
                    selectimage_register.setImageBitmap(bitmap)
                    image_btn.alpha = 0f
                    Log.d("HomeACTIVITY", "Finally image is being retrived")


                }.addOnFailureListener {
                    Toast.makeText(this, "Error in retreivia", Toast.LENGTH_SHORT).show()
                }


            } else {
                Toast.makeText(this, "invaild thing user does not exist", Toast.LENGTH_SHORT).show()

            }
        }.addOnFailureListener {
            Toast.makeText(this, "failed ", Toast.LENGTH_SHORT).show()

        }

    }


    var selectedphoto: Uri? = null
    var encodetobse64string: String? = null

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0 && resultCode == RESULT_OK && data != null) {
            Log.d("Register", "photo was selected")
            selectedphoto = data?.data
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedphoto)
            encodetobse64string =
                encodeTobase64(bitmap)
            selectimage_register.setImageBitmap(bitmap)
            image_btn.alpha = 0f

            Log.d("Register", "photo was displayed")

        }
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Log.d("Register", "photo was uploaded")
            val imageBitmap = data?.extras?.get("data") as Bitmap
            encodetobse64string =
                encodeTobase64(imageBitmap)
            selectimage_register.setImageBitmap(imageBitmap)
            image_btn.alpha = 0f

        }
    }

    private fun encodeTobase64(bm: Bitmap?): String? {
        val baos = ByteArrayOutputStream()
        if (bm != null) {
            bm.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        }
        val b = baos.toByteArray()
        Log.d("TT", "Image log ${Base64.encodeToString(b, Base64.DEFAULT)}")
        return Base64.encodeToString(b, Base64.DEFAULT)


    }

    private fun uploadprofilepid() {
        storageReference =
            FirebaseStorage.getInstance().getReference("Users" + auth.currentUser?.uid)
        selectedphoto?.let {
            storageReference.putFile(it).addOnSuccessListener {
                Toast.makeText(this, "Succesful uploaded profile pic", Toast.LENGTH_SHORT).show()

            }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed upload", Toast.LENGTH_SHORT).show()

                }
        }
    }

}