package com.example.finalprojectsmartbustrackingsystem.Driver_Dashboard

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.finalprojectsmartbustrackingsystem.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class DriverDashboard : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference

    private lateinit var tvWelcome: TextView
    private lateinit var tvAssignedBus: TextView
    private lateinit var tvShiftTiming: TextView
    private lateinit var btnStartTrip: MaterialButton
    private lateinit var btnSOS: MaterialButton

    private var currentBusId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.driver_dashboard)

        auth = FirebaseAuth.getInstance()
        // Database node ka naam 'users' check kar lijiyega
        dbRef = FirebaseDatabase.getInstance().getReference("users")

        tvWelcome = findViewById(R.id.tv_driver_welcome)
        tvAssignedBus = findViewById(R.id.tv_assigned_bus_dashboard)
        tvShiftTiming = findViewById(R.id.tv_shift_timing)
        btnStartTrip = findViewById(R.id.btn_start_trip)
        btnSOS = findViewById(R.id.btn_sos_emergency)

        fetchDriverDetails()

        btnStartTrip.setOnClickListener {
            if (currentBusId != null) {
                Toast.makeText(this, "Trip Started!", Toast.LENGTH_SHORT).show()
                // Yahan ActiveTripActivity ka intent aayega
            } else {
                Toast.makeText(this, "No Bus Assigned!", Toast.LENGTH_SHORT).show()
            }
        }

        btnSOS.setOnClickListener {
            Toast.makeText(this, "Emergency SOS!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchDriverDetails() {
        val uid = auth.currentUser?.uid ?: return

        dbRef.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Driver ka naam fetch karna
                    val name = snapshot.child("name").value?.toString() ?: "Driver"
                    val busName = snapshot.child("assignedBusName").value?.toString() ?: "No Bus"
                    currentBusId = snapshot.child("assignedBusId").value?.toString()

                    // Shift timing fetch karna
                    val sTime = snapshot.child("shiftStart").value?.toString() ?: "--"
                    val eTime = snapshot.child("shiftEnd").value?.toString() ?: "--"

                    // UI Update
                    tvWelcome.text = "Welcome, $name!"
                    tvAssignedBus.text = busName
                    tvShiftTiming.text = "Shift: $sTime to $eTime"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DriverDashboard, "Database Error!", Toast.LENGTH_SHORT).show()
            }
        })
    }
}