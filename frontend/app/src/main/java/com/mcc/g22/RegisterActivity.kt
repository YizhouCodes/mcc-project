package com.mcc.g22

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.mcc.g22.utils.checkFormatEmail
import com.mcc.g22.utils.login
import kotlinx.android.synthetic.main.activity_register.*
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.util.*
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
        // logout()
        signUp_register_button.setOnClickListener {
            registerUser()
        }

        back_login_register.setOnClickListener{
            startActivity(Intent(this@RegisterActivity , RegisterActivity :: class.java))
        }

        // select photo for profile
        profile_image.setOnClickListener{

            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 0)

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0 && resultCode == Activity.RESULT_OK && data != null){

            profilePhoto = data.data
            val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, profilePhoto)
            profile_image.setImageBitmap(bitmap)

        }
    }

    private fun registerUser() {

        val email = email_register_editText.text.toString()
        val password = password_register_editText.text.toString()
        val username = displayName_register_editText.text.toString()
        if (!checkFormatEmail(email, email_register_editText))
            return

        if (password.isEmpty() || password.length < 6) {
            password_register_editText.error = resources.getString(R.string.password_check_login)
            password_register_editText.requestFocus()
            return
        }

        checkUsernameUnique(username)
        val uid = FirebaseAuth.getInstance().uid ?: ""
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")

        val user = User(uid, username, profilePhoto)

        ref.setValue(user)
            .addOnSuccessListener {
                progressbar_register.visibility = View.VISIBLE

                registerAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        progressbar_register.visibility = View.GONE

                        if (task.isSuccessful) {
                            Toast.makeText(this, resources.getString(R.string.create_user), Toast.LENGTH_SHORT).show()
                            login()

                            if (!uploadProfileImageToFirebase())
                                saveUserToFirebase(null)

                        } else {
                            Toast.makeText(this, resources.getString(R.string.failed_create_user), Toast.LENGTH_SHORT ).show()
                            return@addOnCompleteListener
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this,"Failed to create user: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
    }

    // check username is unique, if not suggest 3 other alternatives
    private fun checkUsernameUnique(displayName: String):List<String> {

        val recommendedUsername = mutableListOf<String>()
        database.orderByChild("username").equalTo(displayName).addValueEventListener(object: ValueEventListener {

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if(dataSnapshot.exists()){
                    for (i in 0..3){
                        recommendedUsername.add(displayName + createRandomAlphanumeric())
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        })
        return recommendedUsername
      }

    private fun createRandomAlphanumeric(): String{

        val chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        var randomText = ""

        for (i in 0..4)
               randomText += chars[floor(Math.random() * chars.length).toInt()]

        return randomText

    }

    private fun saveUserToFirebase(profilePhotoUrl: String?) {


    }


    private fun uploadProfileImageToFirebase(): Boolean {
        //no photo selected
        if (profilePhoto == null)
            return false

        val filename = UUID.randomUUID().toString()
        val ref = FirebaseStorage.getInstance().getReference("/images/$filename")

        ref.putFile(profilePhoto!!)
            .addOnSuccessListener {

                ref.downloadUrl.addOnSuccessListener {
                }
            }

        return true
    }

    override fun onStart() {
        super.onStart()
        registerAuth.currentUser?.let {
            login()
        }

    }
}