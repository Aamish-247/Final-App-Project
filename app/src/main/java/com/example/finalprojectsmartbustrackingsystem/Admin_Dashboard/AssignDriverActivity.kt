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

    private val driverMap = HashMap<String, String>()
    private val busMap = HashMap<String, String>()

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

        loadDrivers()
        loadBuses()

        spinnerDriver.setOnItemClickListener { _, _, position, _ ->
            selectedDriverName = spinnerDriver.adapter.getItem(position).toString()
            selectedDriverId = driverMap[selectedDriverName]
        }

        spinnerBus.setOnItemClickListener { _, _, position, _ ->
            selectedBusName = spinnerBus.adapter.getItem(position).toString()
            selectedBusId = busMap[selectedBusName]
        }

        findViewById<MaterialButton>(R.id.btn_save_driver_assignment).setOnClickListener {
            if (selectedDriverId != null && selectedBusId != null) {
                saveAssignmentToDatabase()
            } else {
                Toast.makeText(this, "Please select both Driver and Bus", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadDrivers() {

        dbRef.child("users").orderByChild("isAvailable").equalTo(true)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val driverNames = ArrayList<String>()
                    driverMap.clear()

                    if (snapshot.exists()) {
                        for (snap in snapshot.children) {
                            val name = snap.child("name").value.toString()
                            val id = snap.key.toString()
                            driverNames.add(name)
                            driverMap[name] = id
                        }
                        val adapter = ArrayAdapter(this@AssignDriverActivity, android.R.layout.simple_dropdown_item_1line, driverNames)
                        spinnerDriver.setAdapter(adapter)
                    } else {
                        Toast.makeText(this@AssignDriverActivity, "No available drivers!", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }


    private fun loadBuses() {

        dbRef.child("buses").orderByChild("isAssigned").equalTo(false)
            .addListenerForSingleValueEvent(object : ValueEventListener {
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
                        Toast.makeText(this@AssignDriverActivity, "No available buses found!", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@AssignDriverActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }


    private fun saveAssignmentToDatabase() {
        val updates = HashMap<String, Any>()


        updates["users/${selectedDriverId}/assignedBusId"] = selectedBusId!!
        updates["users/${selectedDriverId}/assignedBusName"] = selectedBusName!!
        updates["users/${selectedDriverId}/isAvailable"] = false


        updates["buses/${selectedBusId}/assignedDriverId"] = selectedDriverId!!
        updates["buses/${selectedBusId}/assignedDriverName"] = selectedDriverName!!
        updates["buses/${selectedBusId}/isAssigned"] = true

        dbRef.updateChildren(updates).addOnSuccessListener {
            Toast.makeText(this, "Assignment Successful! Driver and Bus are Busy Now.", Toast.LENGTH_LONG).show()
            finish()
        }
    }
}