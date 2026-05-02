package com.example.finalprojectsmartbustrackingsystem.Parent_Dashboard

import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
    private var studentStopLocation: GeoPoint? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val ctx = applicationContext
        Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", MODE_PRIVATE))

        setContentView(R.layout.activity_parent_live_tracking)


        busId = intent.getStringExtra("BUS_ID") ?: ""


        map = findViewById(R.id.map_parent_live_tracking)
        tvLiveBusName = findViewById(R.id.tv_live_bus_name)
        tvLiveStatus = findViewById(R.id.tv_live_status)
        tvEtaTime = findViewById(R.id.tv_eta_time)
        tvDistanceLeft = findViewById(R.id.tv_distance_left)
        fabRecenter = findViewById(R.id.fab_recenter_map)

        tvLiveBusName.text = "Tracking Bus: $busId"

        setupMap()

        if (busId.isNotEmpty()) {
            fetchRouteStopsAndStart(busId)
        } else {
            Toast.makeText(this, "Bus ID Error!", Toast.LENGTH_SHORT).show()
        }

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
    }

    private fun fetchRouteStopsAndStart(busId: String) {
        val dbRef = FirebaseDatabase.getInstance().getReference("routes").child(busId)

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists() && snapshot.childrenCount > 0) {
                    val stopsList = snapshot.children.toList()

                    val firstStop = stopsList.first()
                    val lat1 = firstStop.child("latitude").value.toString().toDoubleOrNull()
                    val lng1 = firstStop.child("longitude").value.toString().toDoubleOrNull()

                    if (lat1 != null && lng1 != null) {
                        studentStopLocation = GeoPoint(lat1, lng1)
                        addColoredPin(studentStopLocation!!, "Pickup Stop", Color.BLUE)
                        map.controller.setCenter(studentStopLocation)
                    }


                    if (stopsList.size > 1) {
                        val lastStop = stopsList.last()
                        val lat2 = lastStop.child("latitude").value.toString().toDoubleOrNull()
                        val lng2 = lastStop.child("longitude").value.toString().toDoubleOrNull()

                        if (lat2 != null && lng2 != null) {
                            val dropOffLocation = GeoPoint(lat2, lng2)
                            addColoredPin(dropOffLocation, "Final Drop-off", Color.RED)
                        }
                    }

                    startLiveTracking()
                } else {
                    Toast.makeText(this@ParentLiveTrackingActivity, "Route not found in database!", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun addColoredPin(geoPoint: GeoPoint, title: String, pinColor: Int) {
        val marker = Marker(map)
        marker.position = geoPoint
        marker.title = title

        val icon = ContextCompat.getDrawable(this, R.drawable.ic_custom_pin)?.mutate()
        icon?.setTint(pinColor)
        marker.icon = icon

        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        map.overlays.add(marker)
        map.invalidate()
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

            val busIcon = ContextCompat.getDrawable(this, R.drawable.ic_bus_marker)
            if (busIcon != null) {
                busMarker?.icon = busIcon
            } else {
                busMarker?.icon = ContextCompat.getDrawable(this, R.drawable.ic_bus)
            }

            busMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            map.overlays.add(busMarker)

            map.controller.animateTo(geoPoint)
            map.controller.setZoom(18.0)
        }

        busMarker?.position = geoPoint
        map.invalidate()
    }

    private fun calculateETAAndDistance(busLoc: GeoPoint) {
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
                tvEtaTime.setTextColor(Color.GREEN)
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