package com.example.demoapp.userAuthentication

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.StrictMode
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.demoapp.R
import com.example.demoapp.ui.setup.BluetoothSetupActivity
import com.example.demoapp.utilities.CloseKeyboard
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException

class LoginActivity : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        setContentView(R.layout.activity_login)

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        //Validation check to ensure app is connected to network
        if (isConnectedToNetwork(this)) {
            firebaseAuth = FirebaseAuth.getInstance()
            progressBar = findViewById(R.id.progressBar_login)
            progressBar.visibility = View.INVISIBLE

            if (firebaseAuth.currentUser != null) {
                startActivity(Intent(this@LoginActivity, BluetoothSetupActivity::class.java))
                finish()
            }
        }
    }

    private fun isConnectedToNetwork(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val nw = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
        return when {
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    }

    //Perform validation check if there are email and password strings. Email and Password formatting are handled by Firebase.
    fun getLoginValidationError(email: String, password: String): String? {
        if (email == "") {
            return "Please enter a valid email address"
        }
        if (password == "") {
            return "Please enter a valid password"
        }
//        progressBar.visibility = View.INVISIBLE
        return null
    }

    fun loginUser(view: View) {
        //Obtain email and password from text fields and pass them to validation check function
        val email = findViewById<EditText>(R.id.editTextLoginEmail).text.toString()
        val password = findViewById<EditText>(R.id.editTextLoginPassword).text.toString()
        val validationError = getLoginValidationError(email, password)

        //Only allow the user to continue if the values pass validation check.
        if (validationError != null) {
            Toast.makeText(this, validationError, Toast.LENGTH_LONG).show()
            return
        }

        CloseKeyboard().hideKeyboard(view)
        progressBar.visibility = View.VISIBLE

        //Attempt sign in. If user does not exist or password/email are of poor format, user will not be logged in and cannot continue.
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this@LoginActivity) { task ->
                //checking if successful
                if (task.isSuccessful) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@LoginActivity, "Login Successful", Toast.LENGTH_SHORT)
                        .show()
                    startActivity(Intent(this@LoginActivity, BluetoothSetupActivity::class.java))
                    finish()
                } else {
                    val e = task.exception as FirebaseAuthException
                    Toast.makeText(
                        this@LoginActivity,
                        "Login Failed: " + e.message,
                        Toast.LENGTH_SHORT
                    ).show()
                    progressBar.visibility = View.INVISIBLE
                }
            }
    }

    fun goRegister(view: View) {
        startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
    }

    //Again check if there is network connection.
    override fun onResume() {
        super.onResume()
        if (isConnectedToNetwork(this)) {
            firebaseAuth = FirebaseAuth.getInstance()
            progressBar = findViewById(R.id.progressBar_login)
            progressBar.visibility = View.INVISIBLE

            if (firebaseAuth.currentUser != null) {
                startActivity(Intent(this@LoginActivity, BluetoothSetupActivity::class.java))
                finish()
            }
        } else {
            startActivity(Intent(this@LoginActivity, NoNetwork::class.java))
            finish()
        }
    }
}
