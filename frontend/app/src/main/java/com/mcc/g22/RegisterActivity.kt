package com.mcc.g22

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.mcc.g22.utils.checkFormatEmail
import com.mcc.g22.utils.login
import kotlinx.android.synthetic.main.activity_register.*
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Semaphore
import kotlin.concurrent.thread
import kotlin.math.floor

class RegisterActivity : AppCompatActivity() {

    private lateinit var registerAuth : FirebaseAuth
    private var profilePhoto: Uri? = null

    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
    private val database = FirebaseDatabase.getInstance().getReference("/users")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        registerAuth = FirebaseAuth.getInstance()
        signUp_register_button.setOnClickListener {
            registerUser()
        }

        back_login_register.setOnClickListener{
            startActivity(Intent(this@RegisterActivity , LoginActivity :: class.java))
        }

        // select photo for profile
        upload_profile_pic_button.setOnClickListener{

            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 0)

        }

        displayName_register_editText.setOnFocusChangeListener { _ , hasFocus ->
            var username = displayName_register_editText.text.toString()

            if(hasFocus){
                recommended_textView.text = ""
                recommended_textView.visibility = View.GONE

                user_taken_textView.text = ""
                user_taken_textView.visibility = View.GONE
            }
            if (!hasFocus) {
                var recommendedUsername = "Try these"

                thread {
                    if(!checkUsernameUnique(username)){
                        for (i in 0..3){
                            var tmp = username + createRandomAlphanumeric()

                            while(!checkUsernameUnique(tmp)){
                                tmp = username + createRandomAlphanumeric()
                            }
                            recommendedUsername += " , $tmp"
                        }
                    }
                    if(recommendedUsername != "Try these") {

                        runOnUiThread {
                            recommended_textView.visibility = View.VISIBLE
                            recommended_textView.text = recommendedUsername

                            user_taken_textView.visibility = View.VISIBLE
                            user_taken_textView.text = resources.getString(R.string.usernameTaken)
                        }
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0 && resultCode == Activity.RESULT_OK && data != null){

            profilePhoto = data.data
            val path = getPathFromURI(profilePhoto)
            profile_pic_path_textView.text = path
        }
    }

    private fun registerUser() {

        val email = email_register_editText.text.toString()
        val password = password_register_editText.text.toString()

        if (!checkFormatEmail(email, email_register_editText))
            return

        if (password.isEmpty() || password.length < 6) {
            password_register_editText.error = resources.getString(R.string.password_check_login)
            password_register_editText.requestFocus()
            return
        }

        progressbar_register.visibility = View.VISIBLE

        registerAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                progressbar_register.visibility = View.GONE

                if (task.isSuccessful) {
                    Toast.makeText(this, resources.getString(R.string.create_user), Toast.LENGTH_SHORT).show()

                    if (!uploadProfileImageToFirebase())
                        saveUserToFirebaseDatabase(null)

                    login()

                } else {
                    Toast.makeText(this, resources.getString(R.string.failed_create_user), Toast.LENGTH_SHORT ).show()
                    return@addOnCompleteListener
                }
            }
            .addOnFailureListener {
                Toast.makeText(this,"Failed to create user: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }


    // check username is unique or not
    private fun checkUsernameUnique(displayName: String): Boolean {
        var result = true
        val latch = CountDownLatch(1)

       // database.child("username").equalTo(displayName).addListenerForSingleValueEvent(object: ValueEventListener {
        database.orderByChild("username").equalTo(displayName).addListenerForSingleValueEvent(object: ValueEventListener {

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                result = !dataSnapshot.exists()
                latch.countDown()
            }
            override fun onCancelled(error: DatabaseError) {
                latch.countDown()
            }
        })
        latch.await()
        return result
    }


    private fun createRandomAlphanumeric(): String{

        val chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        var randomText = ""

        for (i in 0..4)
               randomText += chars[floor(Math.random() * chars.length).toInt()]

        return randomText
    }

    private fun saveUserToFirebaseDatabase(profilePhotoUrl: String?) {

        val uid = registerAuth.uid ?: ""
        val username = displayName_register_editText.text.toString()
        val email = email_register_editText.text.toString()
        Log.d("" , "EMAIL IS $email")

        var profilePhoto = ""
        if (profilePhotoUrl != null){
            profilePhoto = profilePhotoUrl
        }
        val newUser = User(username , profilePhoto, email)

        val refUser = database.child(uid)
        refUser.setValue(newUser)
            .addOnCompleteListener{
                //Toast.makeText(this,"user added to database completely", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener{
                Toast.makeText(this,"Failed to add user to database${it.message}", Toast.LENGTH_SHORT).show()

            }

        /*val user = registerAuth.currentUser
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(username)
            .setPhotoUri(Uri.parse(profilePhotoUrl))
            .build()

        user?.updateProfile(profileUpdates)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this , "updateddddddddddd" , Toast.LENGTH_SHORT).show()
                } else {
                   Toast.makeText(this, "FAILED IN UPDATING", Toast.LENGTH_SHORT).show()
                }
            }

         */
    }


    private fun uploadProfileImageToFirebase(): Boolean {
        //no photo selected
        if (profilePhoto == null)
            return false

        val filename = UUID.randomUUID().toString()
        val ref = storage.getReference("/Images/$filename")

        ref.putFile(profilePhoto!!)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener{
                    saveUserToFirebaseDatabase(filename)
                }
            }
            .addOnFailureListener{
                Toast.makeText(this,"Failed to upload image${it.message}", Toast.LENGTH_SHORT).show()

            }

        return true
    }

    private fun getPathFromURI(contentUri: Uri?): String? {

        var res: String? = null
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(contentUri!!, proj, null, null, null)
        if (cursor!!.moveToFirst()) {
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            res = cursor.getString(columnIndex)
        }
        cursor.close()
        return res
    }

    override fun onStart() {
        super.onStart()
        registerAuth.currentUser?.let {
            login()
        }

    }
}