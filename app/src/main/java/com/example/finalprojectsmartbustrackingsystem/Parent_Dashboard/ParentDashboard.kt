package com.example.finalprojectsmartbustrackingsystem.Parent_Dashboard

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

    private var childBusId: String? = null
    private var isTripRunning: Boolean = false // Trip status variable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_dashboard)

        // Firebase Initializing
        auth = FirebaseAuth.getInstance()
        dbRefStudents = FirebaseDatabase.getInstance().getReference("students")
        dbRefBuses = FirebaseDatabase.getInstance().getReference("buses")
        dbRefRoutes = FirebaseDatabase.getInstance().getReference("routes")

        // UI Views Binding
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
                    Toast.makeText(this, "Trip is not active yet! Please wait for the driver to start.", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Bus not assigned yet!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchParentAndStudentData() {
        val parentId = auth.currentUser?.uid ?: return

        dbRefStudents.orderByChild("parentId").equalTo(parentId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(studentSnapshot: DataSnapshot) {
                    if (studentSnapshot.exists()) {
                        val childSnap = studentSnapshot.children.first()
                        val sName = childSnap.child("studentName").value?.toString() ?: "Student"
                        val busId = childSnap.child("busId").value?.toString()

                        tvChildInfo.text = "Student: $sName"

                        if (busId == null || busId == "Not Assigned" || busId == "" || busId == "null") {
                            showNoBusAssigned()
                        } else {
                            childBusId = busId
                            btnTrackBus.isEnabled = true
                            btnTrackBus.alpha = 1.0f
                            fetchBusAndRouteDetails(busId)
                        }
                    } else {
                        tvChildInfo.text = "No Student linked to this account"
                        showNoBusAssigned()
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun fetchBusAndRouteDetails(busId: String) {
        dbRefBuses.child(busId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(busSnap: DataSnapshot) {
                if (busSnap.exists()) {
                    val bID = busSnap.child("busID").value?.toString() ?: busId

                    // 🔥 FETCH REAL-TIME TRIP STATUS
                    isTripRunning = busSnap.child("isTripActive").getValue(Boolean::class.java) ?: false

                    tvBusDetails.text = "Assigned Bus: $bID"

                    val driverId = busSnap.child("assignedDriverId").value?.toString()

                    if (driverId != null && driverId.isNotEmpty()) {
                        // ID mil gayi! Ab 'users' node mein jao Driver ka data lene
                        val dbRefUsers = FirebaseDatabase.getInstance().getReference("users")
                        dbRefUsers.child(driverId).addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(driverSnap: DataSnapshot) {
                                if (driverSnap.exists()) {
                                    val dName = driverSnap.child("name").value?.toString() ?: "Driver not found"
                                    val dPhone = driverSnap.child("phone").value?.toString() ?: "No Phone"

                                    tvDriverDetails.text = "Driver: $dName\nPhone: $dPhone"
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

    private fun showNoBusAssigned() {
        childBusId = null
        isTripRunning = false
        tvBusDetails.text = "Bus: Not Assigned Yet"
        tvRouteDetails.text = "Route: --"
        tvDriverDetails.text = "Driver: Not Assigned"
        btnTrackBus.isEnabled = false
        btnTrackBus.alpha = 0.5f
    }
}