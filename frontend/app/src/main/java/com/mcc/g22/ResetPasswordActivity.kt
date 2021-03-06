package com.mcc.g22

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.mcc.g22.utils.checkFormatEmail
import kotlinx.android.synthetic.main.activity_forgot_pass.*

class ResetPasswordActivity : AppCompatActivity() {

    lateinit var resetAuth : FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_pass)

        resetAuth = FirebaseAuth.getInstance()
        reset_password_button.setOnClickListener {
            resetPassword()
        }

        back_login_reset.setOnClickListener {
            startActivity(Intent(this@ResetPasswordActivity , LoginActivity :: class.java))
        }

    }

    private fun resetPassword(){
        val email = email_resetPW_editText.text.toString()

        if(!checkFormatEmail(email , email_resetPW_editText))
            return
        progressbar_reset_password.visibility = View.VISIBLE

        resetAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener{task ->
                progressbar_reset_password.visibility = View.GONE
                if(task.isSuccessful)
                    Toast.makeText(this, resources.getString(R.string.check_email),Toast.LENGTH_SHORT).show()
                else{
                    Toast.makeText(this, resources.getString(R.string.email_notFound_reset) , Toast.LENGTH_SHORT).show()
                }

            }
    }

}
