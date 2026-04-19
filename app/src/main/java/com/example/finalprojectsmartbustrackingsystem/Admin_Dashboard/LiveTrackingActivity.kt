package com.example.finalprojectsmartbustrackingsystem.Admin_Dashboard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.finalprojectsmartbustrackingsystem.R
import com.google.android.material.card.MaterialCardView
import com.google.firebase.database.*
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import kotlin.math.max

class LiveTrackingActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private lateinit var btnRefresh: MaterialCardView

    // Database references alag alag kar diye hain safai ke liye
    private lateinit var dbRefBuses: DatabaseReference
    private lateinit var dbRefLive: DatabaseReference
    private lateinit var dbRefRoutes: DatabaseReference

    // HashMap to keep track of existing markers (BusID -> Marker)
    private val busMarkers = HashMap<String, Marker>()
    private var liveLocationListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // OSMDroid Configuration
        val sharedPrefs = getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        Configuration.getInstance().load(this, sharedPrefs)

        setContentView(R.layout.activity_live_tracking)

        map = findViewById(R.id.map_live_tracking)
        btnRefresh = findViewById(R.id.btn_refresh_map)

        // Firebase References Initialize karna
        dbRefBuses = FirebaseDatabase.getInstance().getReference("buses")
        dbRefLive = FirebaseDatabase.getInstance().getReference("live_locations")
        dbRefRoutes = FirebaseDatabase.getInstance().getReference("routes")

        setupMap()

        // LOGIC 1: Map load hote hi sab buses ko Last Location ya Stop 1 par lagana
        loadInitialBusPositions()

        // LOGIC 2: Live updates sunna
        startListeningToLiveLocations()

        btnRefresh.setOnClickListener {
            manualRefreshLocations()
        }
    }

    override fun onResume() {
        super.onResume()
        map.onResume() // Map ko refresh karne ke liye zaroori hai
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Listener remove karna zaroori hai warna background mein crash hoga
        liveLocationListener?.let { dbRefLive.removeEventListener(it) }
    }

    private fun setupMap() {
        map.setMultiTouchControls(true)
        val mapController = map.controller
        mapController.setZoom(14.0)

        val defaultPoint = GeoPoint(33.6844, 73.0479)
        mapController.setCenter(defaultPoint)
    }

    // --- PHASE 1: Khari hui (Idle / Parked) Buses Load Karna ---
    private fun loadInitialBusPositions() {
        dbRefBuses.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (busSnap in snapshot.children) {
                    val busId = busSnap.key.toString()
                    val routeStatus = busSnap.child("assignedRoute").value?.toString()

                    if (routeStatus == "Assigned") {
                        val lastLat = busSnap.child("lastLat").value?.toString()?.toDoubleOrNull()
                        val lastLng = busSnap.child("lastLng").value?.toString()?.toDoubleOrNull()

                        if (lastLat != null && lastLng != null) {

                            updateOrAddMarker(busId, GeoPoint(lastLat, lastLng))
                        }
                        else {
                            // Agar nayi bus hai jisne aaj tak trip nahi kiya, toh Stop 1 par laga do
                            fetchStop1AndPlaceMarker(busId)
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun fetchStop1AndPlaceMarker(busId: String) {
        dbRefRoutes.child(busId).child("stop_1")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val lat = snapshot.child("latitude").value.toString().toDouble()
                        val lng = snapshot.child("longitude").value.toString().toDouble()
                        val stop1Point = GeoPoint(lat, lng)

                        // Marker lagao agar live tracking se pehle aayi nahi
                        if (!busMarkers.containsKey(busId)) {
                            updateOrAddMarker(busId, stop1Point)
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // --- PHASE 2: Automatic Live Update (Realtime) ---
    private fun startListeningToLiveLocations() {
        liveLocationListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isFinishing && !isDestroyed) {
                    if (snapshot.exists()) {
                        for (busSnap in snapshot.children) {
                            val busId = busSnap.key.toString()
                            val latStr = busSnap.child("latitude").value?.toString()
                            val lngStr = busSnap.child("longitude").value?.toString()

                            if (latStr != null && lngStr != null) {
                                try {
                                    val geoPoint = GeoPoint(latStr.toDouble(), lngStr.toDouble())
                                    updateOrAddMarker(busId, geoPoint)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@LiveTrackingActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
        liveLocationListener?.let { dbRefLive.addValueEventListener(it) }
    }

    private fun manualRefreshLocations() {
        Toast.makeText(this, "Refreshing Map...", Toast.LENGTH_SHORT).show()
        loadInitialBusPositions()
        map.invalidate()
    }

    private fun updateOrAddMarker(busId: String, newPosition: GeoPoint) {
        runOnUiThread {
            if (busMarkers.containsKey(busId)) {
                val existingMarker = busMarkers[busId]
                existingMarker?.position = newPosition
            } else {
                val marker = Marker(map)
                marker.position = newPosition
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                marker.infoWindow = null
                marker.setOnMarkerClickListener { _, _ -> true }

                marker.icon = createBusMarkerWithLabel(this, busId)

                map.overlays.add(marker)
                busMarkers[busId] = marker
            }

            // Map ko foran refresh karne ka command
            map.invalidate()
        }
    }

    private fun createBusMarkerWithLabel(context: Context, busNumber: String): Drawable {
        val text = "$busNumber"

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 40f
            isAntiAlias = true
            isFakeBoldText = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val bgPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
            setShadowLayer(5f, 0f, 2f, Color.parseColor("#888888"))
        }

        val textBounds = Rect()
        textPaint.getTextBounds(text, 0, text.length, textBounds)
        val textWidth = textBounds.width() + 40
        val textHeight = textBounds.height() + 20

        val busIcon = ContextCompat.getDrawable(context, R.drawable.ic_bus_marker)!!.mutate()

        val iconWidth = busIcon.intrinsicWidth
        val iconHeight = busIcon.intrinsicHeight

        val bitmapWidth = max(textWidth, iconWidth) + 10
        val bitmapHeight = textHeight + iconHeight + 15

        val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgLeft = (bitmapWidth - textWidth) / 2f
        val bgRect = RectF(bgLeft, 0f, bgLeft + textWidth, textHeight.toFloat())
        canvas.drawRoundRect(bgRect, 15f, 15f, bgPaint)

        val textX = bitmapWidth / 2f
        val textY = (textHeight / 2f) + (textBounds.height() / 2f) - 2f
        canvas.drawText(text, textX, textY, textPaint)

        val iconLeft = (bitmapWidth - iconWidth) / 2
        val iconTop = textHeight + 10
        busIcon.setBounds(iconLeft, iconTop, iconLeft + iconWidth, iconTop + iconHeight)
        busIcon.draw(canvas)

        return BitmapDrawable(context.resources, bitmap)
    }
}