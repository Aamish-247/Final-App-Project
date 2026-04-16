package com.example.finalprojectsmartbustrackingsystem.Admin_Dashboard

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.finalprojectsmartbustrackingsystem.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.FirebaseDatabase
import kotlin.random.Random

class AddBusActivity : AppCompatActivity() {

    private var isEditMode = false
    private var editBusId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_bus)

        val etBusName = findViewById<TextInputEditText>(R.id.et_bus_name)
        val etBusPlate = findViewById<TextInputEditText>(R.id.et_bus_plate)
        val etBusCapacity = findViewById<TextInputEditText>(R.id.et_bus_capacity)
        val btnSave = findViewById<MaterialButton>(R.id.btn_save_bus)

        // --- CHECK FOR EDIT MODE ---
        val action = intent.getStringExtra("action")
        if (action == "edit") {
            isEditMode = true
            editBusId = intent.getStringExtra("busId")

            // Form ko purane data se bhar dena
            etBusName.setText(intent.getStringExtra("busName"))
            etBusPlate.setText(intent.getStringExtra("licensePlate"))
            etBusCapacity.setText(intent.getStringExtra("capacity"))

            // Button ka text change karna
            btnSave.text = "UPDATE BUS"
        }

        btnSave.setOnClickListener {
            val name = etBusName.text.toString().trim()
            val plate = etBusPlate.text.toString().trim()
            val capacity = etBusCapacity.text.toString().trim()

            if (name.isEmpty() || plate.isEmpty() || capacity.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isEditMode) {
                updateBusInDatabase(name, plate, capacity)
            } else {
                saveBusToDatabase(name, plate, capacity)
            }
        }
    }

    private fun saveBusToDatabase(name: String, plate: String, capacity: String) {
        val randomId = Random.nextInt(1000, 9999)
        val busId = "BUS-$randomId"

        val dbRef = FirebaseDatabase.getInstance().getReference("buses")

        val busData = mapOf(
            "busId" to busId,
            "busName" to name,
            "licensePlate" to plate,
            "capacity" to capacity,
            "assignedRoute" to "Not Assigned",
            "assignedDriver" to "Not Assigned"
        )

        dbRef.child(busId).setValue(busData)
            .addOnSuccessListener {
                showSuccessDialog(busId, "Registered")
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateBusInDatabase(name: String, plate: String, capacity: String) {
        editBusId?.let { id ->
            val dbRef = FirebaseDatabase.getInstance().getReference("buses").child(id)

            // Sirf wahi cheezein update karein jo form mein hain
            val updates = mapOf(
                "busName" to name,
                "licensePlate" to plate,
                "capacity" to capacity
            )

            dbRef.updateChildren(updates)
                .addOnSuccessListener {
                    Toast.makeText(this, "Bus Updated Successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Update Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showSuccessDialog(busId: String, actionType: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Success")
        builder.setMessage("Bus has been $actionType successfully.\nBus ID: $busId")
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
            finish()
        }
        builder.setCancelable(false)
        builder.show()
    }
}