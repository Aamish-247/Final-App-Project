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
import android.widget.TextView
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
    private lateinit var tvActiveBuses: TextView
    private lateinit var btnRefresh: MaterialCardView
    private lateinit var dbRef: DatabaseReference

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
        tvActiveBuses = findViewById(R.id.tv_active_buses)
        btnRefresh = findViewById(R.id.btn_refresh_map)

        dbRef = FirebaseDatabase.getInstance().getReference("live_locations")

        setupMap()
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
        liveLocationListener?.let { dbRef.removeEventListener(it) }
    }

    private fun setupMap() {
        map.setMultiTouchControls(true)
        val mapController = map.controller
        mapController.setZoom(15.0)

        val defaultPoint = GeoPoint(33.6844, 73.0479)
        mapController.setCenter(defaultPoint)
    }

    // LOGIC 1: Automatic Live Update (Realtime)
    private fun startListeningToLiveLocations() {
        liveLocationListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isFinishing && !isDestroyed) {
                    processLocationData(snapshot, isManual = false)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@LiveTrackingActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
        liveLocationListener?.let { dbRef.addValueEventListener(it) }
    }

    // LOGIC 2: Manual Refresh Update (One-time fetch)
    private fun manualRefreshLocations() {
        Toast.makeText(this, "Refreshing Map...", Toast.LENGTH_SHORT).show()

        // get() use kiya hai taake sirf ek dafa data aaye
        dbRef.get().addOnSuccessListener { snapshot ->
            processLocationData(snapshot, isManual = true)
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to refresh", Toast.LENGTH_SHORT).show()
        }
    }

    // Helper Function: Data process karne ke liye (Taa ke code repeat na ho)
    private fun processLocationData(snapshot: DataSnapshot, isManual: Boolean) {
        if (snapshot.exists()) {
            var activeCount = 0

            for (busSnap in snapshot.children) {
                val busId = busSnap.key.toString()
                val latStr = busSnap.child("latitude").value?.toString()
                val lngStr = busSnap.child("longitude").value?.toString()

                if (latStr != null && lngStr != null) {
                    try {
                        val lat = latStr.toDouble()
                        val lng = lngStr.toDouble()
                        val geoPoint = GeoPoint(lat, lng)

                        updateOrAddMarker(busId, geoPoint)
                        activeCount++
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            tvActiveBuses.text = "Active Buses on Map: $activeCount"

            if (isManual) {
                Toast.makeText(this, "Map Updated!", Toast.LENGTH_SHORT).show()
            }
        } else {
            tvActiveBuses.text = "Active Buses on Map: 0"
        }
    }

    private fun updateOrAddMarker(busId: String, newPosition: GeoPoint) {
        // ASAL FIX: runOnUiThread lazmi hai taake screen hang ya block na ho
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

        val busIcon = ContextCompat.getDrawable(context, R.drawable.ic_bus_marker) ?:
        ContextCompat.getDrawable(context, org.osmdroid.library.R.drawable.marker_default)!!
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