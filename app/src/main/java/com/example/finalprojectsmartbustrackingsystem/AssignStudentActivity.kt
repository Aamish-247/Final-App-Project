package com.example.finalprojectsmartbustrackingsystem

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.*
import kotlin.random.Random

class AssignStudentActivity : AppCompatActivity() {

    private lateinit var dbRef: DatabaseReference

    // UI Elements
    private lateinit var etStudentName: TextInputEditText
    private lateinit var spinnerParent: AutoCompleteTextView
    private lateinit var spinnerBus: AutoCompleteTextView
    private lateinit var spinnerDriver: AutoCompleteTextView
    private lateinit var btnAssign: MaterialButton

    // Data Maps (Naam dikhane ke liye aur ID save karne ke liye)
    private val parentMap = HashMap<String, String>() // Name -> ParentId
    private val busMap = HashMap<String, String>()    // Name -> BusId
    private val driverMap = HashMap<String, String>() // Name -> DriverId

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assign_student)

        dbRef = FirebaseDatabase.getInstance().reference

        etStudentName = findViewById(R.id.et_student_name)
        spinnerParent = findViewById(R.id.spinner_select_parent)
        spinnerBus = findViewById(R.id.spinner_select_bus)
        spinnerDriver = findViewById(R.id.spinner_select_driver)
        btnAssign = findViewById(R.id.btn_assign_student)

        // Dropdowns mein Firebase se data load karein
        loadParents()
        loadBuses()
        loadDrivers()

        btnAssign.setOnClickListener {
            assignStudentToSystem()
        }
    }

    private fun loadParents() {
        dbRef.child("users").orderByChild("role").equalTo("parent")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val parentNames = ArrayList<String>()
                    if (snapshot.exists()) {
                        for (parentSnap in snapshot.children) {
                            val name = parentSnap.child("name").value.toString()
                            val id = parentSnap.key.toString() // Firebase UID
                            parentNames.add(name)
                            parentMap[name] = id
                        }
                        val adapter = ArrayAdapter(this@AssignStudentActivity, android.R.layout.simple_dropdown_item_1line, parentNames)
                        spinnerParent.setAdapter(adapter)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun loadBuses() {
        dbRef.child("buses").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val busNames = ArrayList<String>()
                if (snapshot.exists()) {
                    for (busSnap in snapshot.children) {
                        val name = busSnap.child("busName").value.toString()
                        val id = busSnap.key.toString() // BUS-XXXX ID
                        busNames.add(name)
                        busMap[name] = id
                    }
                    val adapter = ArrayAdapter(this@AssignStudentActivity, android.R.layout.simple_dropdown_item_1line, busNames)
                    spinnerBus.setAdapter(adapter)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadDrivers() {
        dbRef.child("users").orderByChild("role").equalTo("driver")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val driverNames = ArrayList<String>()
                    if (snapshot.exists()) {
                        for (driverSnap in snapshot.children) {
                            val name = driverSnap.child("name").value.toString()
                            val id = driverSnap.key.toString() // Firebase UID
                            driverNames.add(name)
                            driverMap[name] = id
                        }
                        val adapter = ArrayAdapter(this@AssignStudentActivity, android.R.layout.simple_dropdown_item_1line, driverNames)
                        spinnerDriver.setAdapter(adapter)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun assignStudentToSystem() {
        val studentName = etStudentName.text.toString().trim()
        val selectedParent = spinnerParent.text.toString()
        val selectedBus = spinnerBus.text.toString()
        val selectedDriver = spinnerDriver.text.toString()

        // Validation
        if (studentName.isEmpty() || selectedParent.isEmpty() || selectedBus.isEmpty() || selectedDriver.isEmpty()) {
            Toast.makeText(this, "Please fill all fields and select from dropdowns", Toast.LENGTH_SHORT).show()
            return
        }

        // IDs nikalna Map se (taake database linkage sahi bane)
        val parentId = parentMap[selectedParent]
        val busId = busMap[selectedBus]
        val driverId = driverMap[selectedDriver]

        if (parentId == null || busId == null || driverId == null) {
            Toast.makeText(this, "Invalid selection. Please select from the list.", Toast.LENGTH_SHORT).show()
            return
        }

        // Generate Student ID (STD-XXXX)
        val randomId = Random.nextInt(1000, 9999)
        val studentId = "STD-$randomId"

        // Data Object (Yahan IDs aur Names dono save ho rahe hain)
        val studentData = mapOf(
            "studentId" to studentId,
            "studentName" to studentName,
            "parentId" to parentId,
            "parentName" to selectedParent,
            "busId" to busId,
            "busName" to selectedBus,
            "driverId" to driverId,
            "driverName" to selectedDriver
        )

        // Save to Firebase 'students' node
        dbRef.child("students").child(studentId).setValue(studentData)
            .addOnSuccessListener {
                showSuccessDialog(studentName)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showSuccessDialog(studentName: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Assignment Successful")
        builder.setMessage("Successfully Assigned ")
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
            finish()
        }
        builder.setCancelable(false)
        builder.show()
    }
}