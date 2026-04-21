package com.example.finalprojectsmartbustrackingsystem.Parent_Dashboard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.finalprojectsmartbustrackingsystem.Admin_Dashboard.LoginActivity
import com.example.finalprojectsmartbustrackingsystem.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ParentDashboard : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var dbRefStudents: DatabaseReference
    private lateinit var dbRefBuses: DatabaseReference
    private lateinit var dbRefRoutes: DatabaseReference
    private lateinit var tvChildInfo: TextView
    private lateinit var tvBusDetails: TextView
    private lateinit var tvRouteDetails: TextView
    private lateinit var tvDriverDetails: TextView
    private lateinit var btnTrackBus: MaterialButton
    private lateinit var btnLogout: MaterialButton

    private var isNotificationSent = false
    private var stopLat: Double? = null
    private var stopLng: Double? = null

    private var childBusId: String? = null
    private var isTripRunning: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_dashboard)

        // 1. Request Notification Permission (Android 13+)
        checkNotificationPermission()

        auth = FirebaseAuth.getInstance()
        dbRefStudents = FirebaseDatabase.getInstance().getReference("students")
        dbRefBuses = FirebaseDatabase.getInstance().getReference("buses")
        dbRefRoutes = FirebaseDatabase.getInstance().getReference("routes")

        tvChildInfo = findViewById(R.id.tv_child_info)
        tvBusDetails = findViewById(R.id.tv_parent_bus_details)
        tvRouteDetails = findViewById(R.id.tv_parent_route_details)
        tvDriverDetails = findViewById(R.id.tv_parent_driver_details)
        btnTrackBus = findViewById(R.id.btn_track_child_bus)
        btnLogout = findViewById(R.id.btn_parent_logout)

        fetchParentAndStudentData()

        btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        btnTrackBus.setOnClickListener {
            if (childBusId != null) {
                if (isTripRunning) {
                    val intent = Intent(this, ParentLiveTrackingActivity::class.java)
                    intent.putExtra("BUS_ID", childBusId)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Trip is not active yet!", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    private fun fetchParentAndStudentData() {
        val parentId = auth.currentUser?.uid ?: return
        dbRefStudents.orderByChild("parentId").equalTo(parentId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val childSnap = snapshot.children.first()
                        val sName = childSnap.child("studentName").value?.toString() ?: "Student"
                        val busId = childSnap.child("busId").value?.toString()

                        tvChildInfo.text = "Student: $sName"

                        if (busId != null && busId != "Not Assigned") {
                            childBusId = busId
                            btnTrackBus.isEnabled = true
                            btnTrackBus.alpha = 1.0f
                            fetchBusAndRouteDetails(busId)
                        } else {
                            showNoBusAssigned()
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun fetchBusAndRouteDetails(busId: String) {
        dbRefBuses.child(busId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(busSnap: DataSnapshot) {
                if (busSnap.exists()) {
                    isTripRunning = busSnap.child("isTripActive").getValue(Boolean::class.java) ?: false

                    // 1. 🔥 FIX: Bus ID proper display with fallback
                    val bID = busSnap.child("busID").value?.toString() ?: busId
                    tvBusDetails.text = "Assigned Bus: $bID"

                    // Notification Logic
                    if (isTripRunning && !isNotificationSent) {
                        startDistanceMonitor(busId)
                    }

                    // 2. 🔥 FIX: Driver Info (Spelling theek ki hai - assigneddriverid)
                    val driverId = busSnap.child("assignedDriverId").value?.toString()
                    if (driverId != null && driverId.isNotEmpty()) {
                        FirebaseDatabase.getInstance().getReference("users").child(driverId)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(dSnap: DataSnapshot) {
                                    if (dSnap.exists()) {
                                        tvDriverDetails.text = "Driver: ${dSnap.child("name").value}\nPhone: ${dSnap.child("phone").value}"
                                    } else {
                                        tvDriverDetails.text = "Driver: Data Not Found\nPhone: --"
                                    }
                                }
                                override fun onCancelled(error: DatabaseError) {}
                            })
                    } else {
                        tvDriverDetails.text = "Driver: Not Assigned\nPhone: --"
                    }

                    dbRefRoutes.child(busId).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(routeSnap: DataSnapshot) {
                            if (routeSnap.exists() && routeSnap.childrenCount > 0) {
                                val stopsList = routeSnap.children.toList()

                                val firstStop = stopsList.first().key?.replaceFirstChar { it.uppercase() } ?: "Stop 1"
                                val lastStop = stopsList.last().key?.replaceFirstChar { it.uppercase() } ?: "Last Stop"

                                tvRouteDetails.text = "Route: $firstStop to $lastStop"
                            } else {
                                tvRouteDetails.text = "Route: Details not available"
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun startDistanceMonitor(busId: String) {
        // 🔥 IMPROVED: Stop_1 ki jagah pehla bacha (First stop) uthao
        dbRefRoutes.child(busId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists() && snapshot.childrenCount > 0) {
                    val firstStop = snapshot.children.first()
                    stopLat = firstStop.child("latitude").value.toString().toDoubleOrNull()
                    stopLng = firstStop.child("longitude").value.toString().toDoubleOrNull()

                    if (stopLat != null && stopLng != null) {
                        monitorLiveLocation(busId)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun monitorLiveLocation(busId: String) {
        val liveRef = FirebaseDatabase.getInstance().getReference("live_locations").child(busId)
        liveRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isNotificationSent && snapshot.exists() && stopLat != null) {
                    val busLat = snapshot.child("latitude").value.toString().toDoubleOrNull()
                    val busLng = snapshot.child("longitude").value.toString().toDoubleOrNull()

                    if (busLat != null && busLng != null) {
                        val busLoc = android.location.Location("Bus").apply { latitude = busLat; longitude = busLng }
                        val stopLoc = android.location.Location("Stop").apply { latitude = stopLat!!; longitude = stopLng!! }

                        val distance = busLoc.distanceTo(stopLoc)
                        if (distance < 500) { // 500 Meters
                            sendArrivalNotification()
                            isNotificationSent = true
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun sendArrivalNotification() {
        val channelId = "BusAlerts"
        val notificationManager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(channelId, "Bus Arrival", android.app.NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val builder = androidx.core.app.NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_bus)
            .setContentTitle("Bus is Arriving!")
            .setContentText("Your bus is within 500 meters of your stop.")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(1, builder.build())
    }

    private fun showNoBusAssigned() {
        tvBusDetails.text = "Bus: Not Assigned"
        btnTrackBus.isEnabled = false
        btnTrackBus.alpha = 0.5f
    }
}