package com.example.finalprojectsmartbustrackingsystem.Admin_Dashboard

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.finalprojectsmartbustrackingsystem.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AdminDashboard : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        val btnUser = findViewById<MaterialButton>(R.id.btn_manage_users)
        val btnBus = findViewById<MaterialButton>(R.id.btn_manage_fleet)
        val btnMap = findViewById<MaterialButton>(R.id.btn_live_tracking)
        val btnNotif = findViewById<MaterialButton>(R.id.btn_send_alerts)
        val btnLogout = findViewById<MaterialButton>(R.id.btn_logout)

        // 🔥 Start listening for emergencies
        listenForEmergencies()

        // 1. User Management
        btnUser.setOnClickListener {
            startActivity(Intent(this, ManageUsersActivity::class.java))
        }

        // 2. Manage Fleet
        btnBus.setOnClickListener {
            startActivity(Intent(this, ManageFleetActivity::class.java))
        }

        // 3. Live Tracking
        btnMap.setOnClickListener {
            startActivity(Intent(this, LiveTrackingActivity::class.java))
        }

        // 4. Notifications
        btnNotif.setOnClickListener {
            startActivity(Intent(this, BroadcastAlertsActivity::class.java))
        }

        // 5. Logout
        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            Toast.makeText(this, "Logged Out Successfully", Toast.LENGTH_SHORT).show()
        }
    }


    private fun listenForEmergencies() {
        val emergencyRef = FirebaseDatabase.getInstance().getReference("emergencies")

        emergencyRef.orderByChild("status").equalTo("Active")
            .addChildEventListener(object : ChildEventListener {

                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {

                    val busId = snapshot.child("busId").value?.toString() ?: "Unknown Bus"
                    val driverId = snapshot.child("driverId").value?.toString() ?: "Unknown Driver"
                    val issue = snapshot.child("issueType").value?.toString() ?: "Unknown Issue"
                    val alertId = snapshot.key.toString()

                    fetchNamesAndShowAlert(busId, driverId, issue, alertId)
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun fetchNamesAndShowAlert(busId: String, driverId: String, issue: String, alertId: String) {
        val dbRef = FirebaseDatabase.getInstance().reference

        dbRef.child("users").child(driverId).child("name").get()
            .addOnSuccessListener { driverSnap ->
                val driverName = driverSnap.value?.toString() ?: "Unknown Driver"


                dbRef.child("buses").child(busId).child("busName").get()
                    .addOnSuccessListener { busSnap ->
                        val busName = busSnap.value?.toString() ?: busId

                        showAdminSOSDialog(driverName, busName, issue, alertId)
                    }
            }
    }

    private fun showAdminSOSDialog(driverName: String, busName: String, issue: String, alertId: String) {

        AlertDialog.Builder(this)
            .setTitle("🚨 EMERGENCY ALERT 🚨")
            .setMessage("Driver: $driverName\nBus: $busName\nIssue: $issue\n\nPlease Note This Issue!")
            .setCancelable(false)
            .setPositiveButton("Acknowledge") { _, _ ->

                FirebaseDatabase.getInstance().getReference("emergencies")
                    .child(alertId).child("status").setValue("Resolved")
            }
            .show()
    }
}