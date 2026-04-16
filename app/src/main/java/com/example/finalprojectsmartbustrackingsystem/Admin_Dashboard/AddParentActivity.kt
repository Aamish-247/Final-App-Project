package com.example.finalprojectsmartbustrackingsystem.Admin_Dashboard

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.finalprojectsmartbustrackingsystem.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlin.random.Random

class AddParentActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private var isEditMode = false
    private var editUid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_parent)

        mAuth = FirebaseAuth.getInstance()

        val etName = findViewById<TextInputEditText>(R.id.et_parent_name)
        val etEmail = findViewById<TextInputEditText>(R.id.et_parent_email)
        val etPhone = findViewById<TextInputEditText>(R.id.et_parent_phone)
        val etPassword = findViewById<TextInputEditText>(R.id.et_parent_password)
        val btnSave = findViewById<MaterialButton>(R.id.btn_save_parent)
        val layoutPassword = findViewById<TextInputLayout>(R.id.layout_password_parent)

        // --- EDIT MODE CHECK ---
        val action = intent.getStringExtra("action")
        if (action == "edit") {
            isEditMode = true
            editUid = intent.getStringExtra("uid")

            etName.setText(intent.getStringExtra("name"))
            etEmail.setText(intent.getStringExtra("email"))
            etPhone.setText(intent.getStringExtra("phone"))

            etEmail.isEnabled = false
            layoutPassword?.visibility = View.GONE
            btnSave.text = "Update Parent"
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
                updateParentData(name, phone)
            } else {
                if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                registerParent(name, email, phone, password)
            }
        }
    }

    private fun updateParentData(name: String, phone: String) {
        editUid?.let { uid ->
            val dbRef = FirebaseDatabase.getInstance().getReference("users").child(uid)
            val updates = mapOf<String, Any>("name" to name, "phone" to phone)

            dbRef.updateChildren(updates)
                .addOnSuccessListener {
                    Toast.makeText(this, "Parent Updated Successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Update Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun registerParent(name: String, email: String, phone: String, password: String) {
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = mAuth.currentUser?.uid
                    val randomId = Random.nextInt(1000, 9999)
                    val parentCustomId = "PRN-$randomId" // PRN ID for Parents

                    val parentData = mapOf(
                        "uid" to uid,
                        "name" to name,
                        "email" to email,
                        "phone" to phone,
                        "parentId" to parentCustomId, // Key updated to parentId
                        "role" to "parent" // Role is now parent
                    )

                    FirebaseDatabase.getInstance().getReference("users")
                        .child(uid!!)
                        .setValue(parentData)
                        .addOnSuccessListener {
                            showSuccessDialog(parentCustomId)
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Database Error: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Auth Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun showSuccessDialog(parentId: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Registration Successful")
        builder.setMessage("Parent has been registered.\nGenerated ID: $parentId")
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
            finish()
        }
        builder.setCancelable(false)
        builder.show()
    }
}