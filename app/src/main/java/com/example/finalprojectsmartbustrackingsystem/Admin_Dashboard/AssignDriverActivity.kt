package com.example.finalprojectsmartbustrackingsystem.Admin_Dashboard

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.finalprojectsmartbustrackingsystem.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.*

class AssignDriverActivity : AppCompatActivity() {

    private lateinit var dbRef: DatabaseReference

    private lateinit var spinnerDriver: AutoCompleteTextView
    private lateinit var spinnerBus: AutoCompleteTextView

    private val driverMap = HashMap<String, String>() // Driver Name -> Driver ID (UID)
    private val busMap = HashMap<String, String>() // Bus Name -> Bus ID

    private var selectedDriverId: String? = null
    private var selectedBusId: String? = null
    private var selectedDriverName: String? = null
    private var selectedBusName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assign_driver)

        dbRef = FirebaseDatabase.getInstance().reference

        spinnerDriver = findViewById(R.id.spinner_select_driver)
        spinnerBus = findViewById(R.id.spinner_select_bus_for_driver)

        // Data Load Karo
        loadDrivers()
        loadBuses()

        // Driver Selection Listener
        spinnerDriver.setOnItemClickListener { _, _, position, _ ->
            selectedDriverName = spinnerDriver.adapter.getItem(position).toString()
            selectedDriverId = driverMap[selectedDriverName]
        }

        // Bus Selection Listener
        spinnerBus.setOnItemClickListener { _, _, position, _ ->
            selectedBusName = spinnerBus.adapter.getItem(position).toString()
            selectedBusId = busMap[selectedBusName]
        }

        // Save Button Logic
        findViewById<MaterialButton>(R.id.btn_save_driver_assignment).setOnClickListener {
            if (selectedDriverId != null && selectedBusId != null) {
                saveAssignmentToDatabase()
            } else {
                Toast.makeText(this, "Please select both Driver and Bus", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // UPDATED: Fetch Drivers from 'users' node where role is 'driver'
    private fun loadDrivers() {
        dbRef.child("users").orderByChild("role").equalTo("driver")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val driverNames = ArrayList<String>()
                    driverMap.clear() // Purana data clear karein

                    if (snapshot.exists()) {
                        for (snap in snapshot.children) {
                            val name = snap.child("name").value.toString()
                            val id = snap.key.toString() // Yeh UID uthayega

                            driverNames.add(name)
                            driverMap[name] = id
                        }
                        val adapter = ArrayAdapter(this@AssignDriverActivity, android.R.layout.simple_dropdown_item_1line, driverNames)
                        spinnerDriver.setAdapter(adapter)
                    } else {
                        Toast.makeText(this@AssignDriverActivity, "No drivers found in database", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@AssignDriverActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // Fetch Buses
    private fun loadBuses() {
        dbRef.child("buses").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val busNames = ArrayList<String>()
                busMap.clear()

                if (snapshot.exists()) {
                    for (snap in snapshot.children) {
                        val name = snap.child("busName").value.toString()
                        val id = snap.key.toString()

                        busNames.add(name)
                        busMap[name] = id
                    }
                    val adapter = ArrayAdapter(this@AssignDriverActivity, android.R.layout.simple_dropdown_item_1line, busNames)
                    spinnerBus.setAdapter(adapter)
                } else {
                    Toast.makeText(this@AssignDriverActivity, "No buses found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // UPDATED: Save data properly to both nodes using multipath updates
    private fun saveAssignmentToDatabase() {
        val updates = HashMap<String, Any>()

        // 1. Driver ki profile (users node) mein bus ka data save karna
        updates["users/${selectedDriverId}/assignedBusId"] = selectedBusId!!
        updates["users/${selectedDriverId}/assignedBusName"] = selectedBusName!!

        // 2. Bus ke node mein driver ka data save karna
        updates["buses/${selectedBusId}/assignedDriverId"] = selectedDriverId!!
        updates["buses/${selectedBusId}/assignedDriverName"] = selectedDriverName!!

        dbRef.updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Driver Assigned Successfully!", Toast.LENGTH_LONG).show()
                finish() // Screen close ho jayegi
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}