package com.example.finalprojectsmartbustrackingsystem.Admin_Dashboard

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.finalprojectsmartbustrackingsystem.Driver_Dashboard.DriverDashboard
import com.example.finalprojectsmartbustrackingsystem.Parent_Dashboard.ParentDashboard
import com.example.finalprojectsmartbustrackingsystem.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : AppCompatActivity() {


    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()


        val loginBtn = findViewById<Button>(R.id.btn_login)

        loginBtn.setOnClickListener {
            performLogin()
        }
    }

    private fun performLogin() {

        val emailET = findViewById<EditText>(R.id.et_email)
        val passwordET = findViewById<EditText>(R.id.et_password)

        val email = emailET.text.toString().trim()
        val password = passwordET.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid

                    if (uid == null) {
                        Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
                        return@addOnCompleteListener
                    }

                    // FIX 1: Node name 'users' hona chahiye
                    val dbRef = FirebaseDatabase.getInstance().getReference("users").child(uid)

                    dbRef.get().addOnSuccessListener { snapshot ->
                        if (snapshot.exists()) {
                            val role = snapshot.child("role").value?.toString()

                            if (role == null) {
                                Toast.makeText(this, "Role not found", Toast.LENGTH_SHORT).show()
                                return@addOnSuccessListener
                            }

                            // FIX 2: Sahi class names ka use
                            when (role.lowercase()) {
                                "admin" -> startActivity(Intent(this, AdminDashboard::class.java))
                                "driver" -> startActivity(Intent(this, DriverDashboard::class.java))
                                "parent" -> startActivity(Intent(this, ParentDashboard::class.java))
                                else -> Toast.makeText(this, "Unknown role: $role", Toast.LENGTH_SHORT).show()
                            }
                            finish()
                        } else {
                            Toast.makeText(this, "User data not found in database", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Login Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}