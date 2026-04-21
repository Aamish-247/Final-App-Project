package com.example.finalprojectsmartbustrackingsystem.Parent_Dashboard

import android.location.Location
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.finalprojectsmartbustrackingsystem.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.*
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.Locale

class ParentLiveTrackingActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private lateinit var tvLiveBusName: TextView
    private lateinit var tvLiveStatus: TextView
    private lateinit var tvEtaTime: TextView
    private lateinit var tvDistanceLeft: TextView
    private lateinit var fabRecenter: FloatingActionButton
    private lateinit var ivBack: ImageView

    private lateinit var dbRefLocation: DatabaseReference
    private var busId: String = ""

    private var busMarker: Marker? = null
    private var currentBusLocation: GeoPoint? = null
    private var studentStopLocation: GeoPoint? = null // Default shuru mein Null hoga

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // OSMDroid Configuration Fix
        val ctx = applicationContext
        Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", MODE_PRIVATE))

        setContentView(R.layout.activity_parent_live_tracking)

        // Intent se Bus ID lena
        busId = intent.getStringExtra("BUS_ID") ?: ""

        // UI Binding
        map = findViewById(R.id.map_parent_live_tracking)
        tvLiveBusName = findViewById(R.id.tv_live_bus_name)
        tvLiveStatus = findViewById(R.id.tv_live_status)
        tvEtaTime = findViewById(R.id.tv_eta_time)
        tvDistanceLeft = findViewById(R.id.tv_distance_left)
        fabRecenter = findViewById(R.id.fab_recenter_map)

        tvLiveBusName.text = "Tracking Bus: $busId"

        // 🔥 FIX: Sirf map setup karo, Marker abhi nahi lagana kyunke location nahi aayi
        setupMap()

        if (busId.isNotEmpty()) {
            fetchStopOneLocation(busId)
        } else {
            Toast.makeText(this, "Bus ID Error!", Toast.LENGTH_SHORT).show()
        }

        // Floating Recenter Button Logic
        fabRecenter.setOnClickListener {
            currentBusLocation?.let {
                map.controller.animateTo(it)
                map.controller.setZoom(18.0)
            } ?: Toast.makeText(this, "Waiting for location...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupMap() {
        map.setMultiTouchControls(true)
        map.controller.setZoom(15.0)
        // 🔥 FIX: Yahan se null center logic hata di hai
    }

    private fun fetchStopOneLocation(busId: String) {
        val dbRef = FirebaseDatabase.getInstance().getReference("routes").child(busId).child("stop_1")

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val lat = snapshot.child("latitude").value.toString().toDoubleOrNull()
                    val lng = snapshot.child("longitude").value.toString().toDoubleOrNull()

                    if (lat != null && lng != null) {
                        studentStopLocation = GeoPoint(lat, lng)

                        // 🔥 FIX: Jab real location aa jaye, TAB marker lagao aur center karo
                        addStudentStopMarker()
                        map.controller.setCenter(studentStopLocation)

                        // Phir bus ki tracking shuru karo
                        startLiveTracking()
                    }
                } else {
                    Toast.makeText(this@ParentLiveTrackingActivity, "Stop 1 not found in routes!", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun addStudentStopMarker() {
        // 🔥 FIX: Safe Call. Agar location null nahi hai toh hi marker lagao
        studentStopLocation?.let { location ->
            val stopMarker = Marker(map)
            stopMarker.position = location
            stopMarker.title = "Pickup Stop"
            map.overlays.add(stopMarker)
            map.invalidate()
        }
    }

    private fun startLiveTracking() {
        dbRefLocation = FirebaseDatabase.getInstance().getReference("live_locations").child(busId)

        dbRefLocation.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val lat = snapshot.child("latitude").value.toString().toDoubleOrNull()
                    val lng = snapshot.child("longitude").value.toString().toDoubleOrNull()

                    if (lat != null && lng != null) {
                        val newLocation = GeoPoint(lat, lng)
                        currentBusLocation = newLocation
                        updateBusMarker(newLocation)
                        calculateETAAndDistance(newLocation)
                    }
                } else {
                    tvLiveStatus.text = "Status: Location Data Unavailable"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ParentLiveTrackingActivity, "Network Error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateBusMarker(geoPoint: GeoPoint) {
        if (busMarker == null) {
            busMarker = Marker(map)
            busMarker?.title = "Bus is here"

            busMarker?.icon = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.ic_bus)

            busMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

            map.overlays.add(busMarker)

            map.controller.animateTo(geoPoint)
            map.controller.setZoom(18.0)
        }


        busMarker?.position = geoPoint
        map.invalidate()
    }

    private fun calculateETAAndDistance(busLoc: GeoPoint) {
        // 🔥 FIX: Agar internet masle ki wajah se Stop location abhi tak null hai toh Crash na ho, wapis chala jaye
        val stopLoc = studentStopLocation ?: return

        val busLocationObj = Location("Bus").apply {
            latitude = busLoc.latitude
            longitude = busLoc.longitude
        }
        val stopLocationObj = Location("Stop").apply {
            latitude = stopLoc.latitude
            longitude = stopLoc.longitude
        }

        val distanceInMeters = busLocationObj.distanceTo(stopLocationObj)
        val distanceInKm = distanceInMeters / 1000

        val speedMetersPerSecond = 8.33
        val timeInSeconds = distanceInMeters / speedMetersPerSecond
        val timeInMinutes = (timeInSeconds / 60).toInt()

        tvDistanceLeft.text = String.format(Locale.US, "%.1f km", distanceInKm)

        when {
            distanceInMeters < 100 -> {
                tvEtaTime.text = "Arrived!"
                tvEtaTime.setTextColor(android.graphics.Color.GREEN)
                tvLiveStatus.text = "Bus is at your stop."
            }
            timeInMinutes <= 0 -> {
                tvEtaTime.text = "< 2 min"
                tvLiveStatus.text = "Bus is arriving very soon!"
            }
            else -> {
                tvEtaTime.text = "$timeInMinutes mins"
                tvLiveStatus.text = "Status: On the way"
            }
        }
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }
}