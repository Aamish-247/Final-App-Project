package com.example.finalprojectsmartbustrackingsystem.Admin_Dashboard

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.finalprojectsmartbustrackingsystem.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlin.random.Random

class AddDriverActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private var isEditMode = false
    private var editUid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_driver)

        mAuth = FirebaseAuth.getInstance()

        val etName = findViewById<TextInputEditText>(R.id.et_driver_name)
        val etEmail = findViewById<TextInputEditText>(R.id.et_driver_email)
        val etPhone = findViewById<TextInputEditText>(R.id.et_driver_phone)
        val etPassword = findViewById<TextInputEditText>(R.id.et_driver_password)
        val btnSave = findViewById<MaterialButton>(R.id.btn_save_driver)

        val action = intent.getStringExtra("action")
        if (action == "edit") {
            isEditMode = true
            editUid = intent.getStringExtra("uid")

            etName.setText(intent.getStringExtra("name"))
            etEmail.setText(intent.getStringExtra("email"))
            etPhone.setText(intent.getStringExtra("phone"))

            etEmail.isEnabled = false
            etPassword.isEnabled = false
            etPassword.hint = "Password is lock for safety"

            btnSave.text = "Update Driver"
        }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (isEditMode) {
                if (name.isEmpty() || phone.isEmpty()) {
                    Toast.makeText(this, "Name and Phone are required", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                updateDriverData(name, phone)
            } else {
                if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                registerDriver(name, email, phone, password)
            }
        }
    }

    private fun updateDriverData(name: String, phone: String) {
        editUid?.let { uid ->
            val dbRef = FirebaseDatabase.getInstance().getReference("users").child(uid)
            val updates = mapOf<String, Any>(
                "name" to name,
                "phone" to phone
            )
            dbRef.updateChildren(updates).addOnSuccessListener {
                Toast.makeText(this, "Driver Updated Successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun registerDriver(name: String, email: String, phone: String, password: String) {
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = mAuth.currentUser?.uid
                    val randomId = Random.Default.nextInt(1000, 9999)
                    val driverCustomId = "DRV-$randomId"

                    // UPDATED: isAvailable flag add kar diya taake Driver list mein show ho
                    val driverData = mapOf(
                        "uid" to uid,
                        "name" to name,
                        "email" to email,
                        "phone" to phone,
                        "driverId" to driverCustomId,
                        "role" to "driver",
                        "isAvailable" to true // <--- YE ZAROORI HAI
                    )

                    FirebaseDatabase.getInstance().getReference("users")
                        .child(uid!!)
                        .setValue(driverData)
                        .addOnSuccessListener {
                            showSuccessDialog(driverCustomId)
                        }
                } else {
                    Toast.makeText(this, "Auth Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun showSuccessDialog(driverId: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Registration Successful")
        builder.setMessage("Driver has been registered.\nGenerated ID: $driverId")
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
            finish()
        }
        builder.setCancelable(false)
        builder.show()
    }
}