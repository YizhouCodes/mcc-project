package com.mcc.g22

import android.Manifest.permission.CAMERA
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.activity_dashboard.drawer_layout
import kotlinx.android.synthetic.main.activity_edit_profile.*
import kotlinx.android.synthetic.main.activity_main.*

class EditProfileActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val PERMISSION_REQUEST_CODE: Int = 101
    val REQUEST_IMAGE_CAPTURE = 1
    val REQUEST_GALLERY = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)
        nav_view.setNavigationItemSelectedListener(this)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when(item.getItemId()) {
            R.id.nav_profile -> {
                showProfile()
                return true
            }
            R.id.nav_changePassword -> {
                changePassword()
                return true
            }
            R.id.nav_logOut -> {
                logOut()
                return true
            }

        }
        return true
    }

    fun showProfile() {
        intent = Intent(this, EditProfileActivity::class.java)
        startActivity(intent)
    }


    fun changePassword() {
        intent = Intent(this, ChangePasswordActivity::class.java)
        startActivity(intent)
    }

    fun logOut() {

    }

    fun toggleDrawer(view: View){
        if(drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        }
        else {
            drawer_layout.openDrawer(GravityCompat.START)
        }
    }

    fun returnHome(view: View) {
        intent = Intent(this, DashboardActivity::class.java)
        startActivity(intent)
    }

    fun editPicture(view: View) {
        val builder = AlertDialog.Builder(this)
        var options = arrayOf("Camera", "Gallery", "Cancel")

        with(builder){
            setTitle("Select Options")
            setItems(options) { dialog, which ->
                when(which) {
                    0 -> {
                        if (checkPermission()) cameraClick() else requestPermission()
                    }
                    1 -> {
                        galleryClick()
                    }
                    2 -> {

                    }
                }
            }
            show()
        }
    }

    fun cameraClick() {
        var i = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(i, REQUEST_IMAGE_CAPTURE)
    }

    fun galleryClick() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_GALLERY)
    }

    private fun checkPermission(): Boolean {
        return (ContextCompat.checkSelfPermission(this, CAMERA) ==
                PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
            READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(READ_EXTERNAL_STORAGE, CAMERA),
            PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {

                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                    cameraClick()

                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
                return
            }

            else -> {

            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            var bmp = data?.extras?.get("data") as Bitmap
            profileImage.setImageBitmap(bmp)
        } else if(requestCode == REQUEST_GALLERY && resultCode == Activity.RESULT_OK) {
            if (data != null)
            {
                profileImage.setScaleType(ImageView.ScaleType.CENTER)
                profileImage.setImageURI(data.data)
            }
        }
    }

}
