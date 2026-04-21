package com.example.finalprojectsmartbustrackingsystem.Driver_Dashboard

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.finalprojectsmartbustrackingsystem.R
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.config.Configuration
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.Marker
import androidx.core.content.ContextCompat

class ActiveTripActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private lateinit var dbRef: DatabaseReference
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var busId: String? = null
    private val driverUid = FirebaseAuth.getInstance().currentUser?.uid
    private var currentDriverPoint: GeoPoint? = null
    private val routePolyline = Polyline()

    private var routeStartPoint: GeoPoint? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // OSMDroid Configuration load karna
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        setContentView(R.layout.activity_active_trip)

        // Intent se Bus ID lena
        busId = intent.getStringExtra("BUS_ID")
        dbRef = FirebaseDatabase.getInstance().reference
        map = findViewById(R.id.mapview)

        setupMap()

        // 1. Database se Route fetch karke line draw karna
        if (busId != null) {
            fetchRouteAndDraw(busId!!)
        }

        // 2. Location Client Setup
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupLocationCallback()
        startLocationUpdates()
        updateLocationInFirebase(33.6844, 73.0479)

        // 3. Recenter Button Logic (Aapke XML mein btn_recenter ID honi chahiye)
        findViewById<View>(R.id.btn_recenter).setOnClickListener {
            routeStartPoint?.let { point ->
                map.controller.animateTo(point)
                map.controller.setZoom(15.0)
            } ?: Toast.makeText(this, "Route not found", Toast.LENGTH_SHORT).show()
        }

        // 4. End Trip Button
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_end_trip).setOnClickListener {
            showEndConfirmation()
        }

        val btnAttendance = findViewById<View>(R.id.btn_active_attendance)
        btnAttendance.setOnClickListener {
            if (busId != null) {
                // Agar bus ID mojood hai toh Attendance Activity kholo
                val intent = Intent(this, DriverAttendanceActivity::class.java)
                intent.putExtra("BUS_ID", busId)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Wait for GPS or Route to load...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupMap() {
        map.setMultiTouchControls(true)
        map.controller.setZoom(15.0)

        val startPoint = GeoPoint(33.6844, 73.0479)
        map.controller.setCenter(startPoint)
    }

    private fun fetchRouteAndDraw(busId: String) {
        dbRef.child("routes").child(busId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val points = ArrayList<GeoPoint>()

                    for (stopSnap in snapshot.children) {
                        val lat = stopSnap.child("latitude").value.toString().toDouble()
                        val lng = stopSnap.child("longitude").value.toString().toDouble()
                        points.add(GeoPoint(lat, lng))
                    }

                    if (points.isNotEmpty()) {
                        // 1. Blue Line (Polyline) Draw Karna
                        routePolyline.setPoints(points)
                        routePolyline.outlinePaint.color = Color.BLUE
                        routePolyline.outlinePaint.strokeWidth = 12f
                        map.overlays.add(routePolyline)

                        for (i in points.indices) {
                            val marker = Marker(map)
                            marker.position = points[i]
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                            when (i) {
                                0 -> {
                                    // START POINT: Green Marker
                                    marker.title = "Start (Stop 1)"
                                    val startIcon = ContextCompat.getDrawable(this@ActiveTripActivity, R.drawable.ic_custom_pin)?.mutate()
                                    startIcon?.setTint(Color.GREEN)

                                    routeStartPoint = points[i]
                                }
                                points.size - 1 -> {
                                    // END POINT: Red Marker
                                    marker.title = "Last Stop"
                                    val endIcon = ContextCompat.getDrawable(this@ActiveTripActivity, R.drawable.ic_custom_pin)?.mutate()
                                    endIcon?.setTint(Color.RED)
                                    marker.icon = endIcon
                                }
                                else -> {
                                    marker.title = "Stop ${i + 1}"
                                    val middleIcon = ContextCompat.getDrawable(this@ActiveTripActivity, R.drawable.ic_custom_pin)?.mutate()
                                    middleIcon?.setTint(Color.BLUE)
                                    marker.icon = middleIcon
                                }
                            }
                            map.overlays.add(marker)
                        }

                        routeStartPoint?.let {
                            map.controller.animateTo(it)
                        }
                    }
                    map.invalidate()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    currentDriverPoint = GeoPoint(location.latitude, location.longitude)

                    // Firebase mein real-time location update bhejna
                    updateLocationInFirebase(location.latitude, location.longitude)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(1000)
            .build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun updateLocationInFirebase(lat: Double, lng: Double) {
        if (busId == null) return

        val locationData = mapOf(
            "latitude" to lat,
            "longitude" to lng,
            "lastUpdated" to ServerValue.TIMESTAMP
        )
        dbRef.child("live_locations").child(busId!!).setValue(locationData)
    }

    private fun showEndConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("End Trip?")
            .setMessage("Are you sure you want to end this trip?")
            .setPositiveButton("Yes, End Trip") { _, _ -> releaseResources() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun releaseResources() {
        fusedLocationClient.removeLocationUpdates(locationCallback)

        val updates = HashMap<String, Any?>()

        // Bus aur Driver ko free karna
        updates["buses/$busId/isAssigned"] = false
        updates["buses/$busId/assignedDriverId"] = "Not Assigned"
        updates["buses/$busId/assignedDriverName"] = "Not Assigned"
        updates["buses/$busId/isTripActive"] = false

        updates["users/$driverUid/isAvailable"] = true
        updates["users/$driverUid/assignedBusId"] = null
        updates["users/$driverUid/assignedBusName"] = "Not Assigned"
        updates["users/$driverUid/shiftStart"] = "--:--"
        updates["users/$driverUid/shiftEnd"] = "--:--"


        if (currentDriverPoint != null) {
            updates["buses/$busId/lastLat"] = currentDriverPoint!!.latitude
            updates["buses/$busId/lastLng"] = currentDriverPoint!!.longitude
        }

        // Live location ko remove karna
        dbRef.child("live_locations").child(busId!!).removeValue()

        dbRef.updateChildren(updates).addOnSuccessListener {
            Toast.makeText(this, "Trip Completed!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}