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
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.toColorInt
import com.example.finalprojectsmartbustrackingsystem.Admin_Dashboard.LoginActivity

class DriverDashboard : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference

    private lateinit var tvWelcome: TextView
    private lateinit var tvAssignedBus: TextView
    private lateinit var tvShiftTiming: TextView
    private lateinit var btnStartTrip: MaterialButton
    private lateinit var btnSOS: MaterialButton
    private lateinit var btnLogout: MaterialButton

    private var currentBusId: String? = null
    private var isTripRunning: Boolean = false

    private val appStartTime = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.driver_dashboard)

        auth = FirebaseAuth.getInstance()
        dbRef = FirebaseDatabase.getInstance().getReference("users")

        tvWelcome = findViewById(R.id.tv_driver_welcome)
        tvAssignedBus = findViewById(R.id.tv_assigned_bus_dashboard)
        tvShiftTiming = findViewById(R.id.tv_shift_timing)
        btnStartTrip = findViewById(R.id.btn_start_trip)
        btnSOS = findViewById(R.id.btn_sos_emergency)
        btnLogout = findViewById(R.id.btn_driver_logout)

        fetchDriverDetails()
        listenForBroadcastAlerts()


        btnStartTrip.setOnClickListener {
            if (currentBusId != null && currentBusId != "Not Assigned") {
                val uid = auth.currentUser?.uid
                if (uid != null) {
                    FirebaseDatabase.getInstance().getReference("buses").child(currentBusId!!).child("isTripActive").setValue(true)
                }

                val intent = Intent(this, ActiveTripActivity::class.java)
                intent.putExtra("BUS_ID", currentBusId)
                startActivity(intent)
            } else {
                Toast.makeText(this, "No Bus Assigned!", Toast.LENGTH_SHORT).show()
            }
        }

        btnSOS.setOnClickListener {
            if (isTripRunning) {
                showSOSDialog()
            } else {
                Toast.makeText(this, "No Active Trip", Toast.LENGTH_LONG).show()
            }
        }
        btnLogout.setOnClickListener {
            auth.signOut()


            val intent = Intent(this, LoginActivity::class.java)

            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)
            finish()
        }
    }

    private fun fetchDriverDetails() {
        val uid = auth.currentUser?.uid ?: return
        dbRef.child(uid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {

                    val driverName = snapshot.child("name").value?.toString() ?: "Driver"
                    tvWelcome.text = "Welcome, $driverName"

                    val busId = snapshot.child("assignedBusId").value?.toString()

                    if (busId == null || busId == "null" || busId == "Not Assigned") {
                        currentBusId = null
                        tvAssignedBus.text = "No Bus Assigned"
                        tvShiftTiming.text = "Shift: --:-- to --:--"
                        btnStartTrip.text = "No Bus Assigned"
                        btnStartTrip.setBackgroundColor(android.graphics.Color.GRAY)
                        isTripRunning = false
                    }
                    else {
                        currentBusId = busId
                        tvAssignedBus.text = snapshot.child("assignedBusName").value?.toString() ?: "Unknown"
                        val sTime = snapshot.child("shiftStart").value?.toString() ?: "--"
                        val eTime = snapshot.child("shiftEnd").value?.toString() ?: "--"
                        tvShiftTiming.text = "Shift: $sTime to $eTime"

                        FirebaseDatabase.getInstance().getReference("buses").child(busId)
                            .addValueEventListener(object : ValueEventListener {
                                override fun onDataChange(busSnap: DataSnapshot) {
                                    if (busSnap.exists()) {
                                        val isTripActive = busSnap.child("isTripActive").value as? Boolean ?: false
                                        isTripRunning = isTripActive

                                        if (isTripActive) {
                                            btnStartTrip.text = "Resume Trip"
                                            btnStartTrip.setBackgroundColor("#FF9800".toColorInt())
                                        } else {
                                            btnStartTrip.text = "Start Trip"
                                            btnStartTrip.setBackgroundColor("#1976D2".toColorInt())
                                        }
                                    }
                                }
                                override fun onCancelled(error: DatabaseError) {}
                            })
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
    private fun showSOSDialog() {
        val options = arrayOf("Accident", "Bus Breakdown", "Medical Emergency", "Heavy Traffic/Stuck")

        AlertDialog.Builder(this)
            .setTitle("🚨 Send Emergency SOS")
            .setItems(options) { _, which ->
                sendEmergencyAlert(options[which])
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun sendEmergencyAlert(issueType: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show()
            return
        }

        val emergencyRef = FirebaseDatabase.getInstance().getReference("emergencies")
        val alertId = emergencyRef.push().key ?: return

        val finalBusId = currentBusId ?: "Parked/No Active Bus"

        val alertData = mapOf(
            "alertId" to alertId,
            "busId" to finalBusId,
            "driverId" to currentUser.uid,
            "issueType" to issueType,
            "timestamp" to ServerValue.TIMESTAMP,
            "status" to "Active"
        )

        emergencyRef.child(alertId).setValue(alertData)
            .addOnSuccessListener {
                Toast.makeText(this, "🚨 SOS Alert Sent to Admin!", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to send SOS: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
    private fun listenForBroadcastAlerts() {
        val alertsRef = FirebaseDatabase.getInstance().getReference("broadcast_alerts")
        alertsRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                val audience = snapshot.child("audience").value?.toString() ?: ""
                val type = snapshot.child("type").value?.toString() ?: "Alert"
                val message = snapshot.child("message").value?.toString() ?: ""

                if (timestamp > appStartTime) {
                    if (audience == "All" || audience == "Drivers Only")
                    {
                        sendBroadcastNotification(type, message)
                    }
                }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun sendBroadcastNotification(type: String, message: String) {
        val channelId = "SchoolBroadcasts"
        val notificationManager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(channelId, "School Broadcasts", android.app.NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val builder = androidx.core.app.NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("📢 $type Alert")
            .setContentText(message)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build()) // Har message ki alag ID
    }
}