package com.example.finalprojectsmartbustrackingsystem.Admin_Dashboard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.finalprojectsmartbustrackingsystem.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.*
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import androidx.appcompat.app.AlertDialog

class DefineRouteActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private lateinit var dbRef: DatabaseReference
    private lateinit var spinnerBus: AutoCompleteTextView
    private lateinit var tvInstruction: TextView
    private lateinit var btnClear: MaterialButton
    private lateinit var btnSave: MaterialButton

    private val busMap = HashMap<String, String>()
    private val routePoints = ArrayList<GeoPoint>()

    private var selectedBusId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPrefs = getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        Configuration.getInstance().load(this, sharedPrefs)

        setContentView(R.layout.activity_define_route)

        dbRef = FirebaseDatabase.getInstance().reference

        spinnerBus = findViewById(R.id.spinner_select_bus_route)
        tvInstruction = findViewById(R.id.tv_instruction)
        map = findViewById(R.id.mapview)
        btnClear = findViewById(R.id.btn_clear_route)
        btnSave = findViewById(R.id.btn_save_route)

        setupMap()
        loadBuses()


        spinnerBus.setOnItemClickListener { _, _, position, _ ->
            val selectedBusName = spinnerBus.adapter.getItem(position).toString()
            selectedBusId = busMap[selectedBusName]

            clearMapData()
            loadExistingRoute(selectedBusId!!)
        }

        btnClear.setOnClickListener {
            if (selectedBusId != null) {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Delete Route")
                builder.setMessage("Do you wanna delete this route?")

                builder.setPositiveButton("Yes, Delete") { dialog, _ ->
                    val updates = HashMap<String, Any?>()
                    updates["routes/${selectedBusId!!}"] = null

                    updates["buses/${selectedBusId!!}/assignedRoute"] = "Not Assigned"

                    dbRef.updateChildren(updates)
                        .addOnSuccessListener {
                            clearMapData()
                            tvInstruction.text = "Route deleted. Tap on map to create a new one!"
                            Toast.makeText(this, "Route Deleted from Database!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                    dialog.dismiss()
                }

                builder.setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }

                builder.show()

            } else {
                Toast.makeText(this, "Please select a Bus first", Toast.LENGTH_SHORT).show()
            }
        }

        btnSave.setOnClickListener {
            if (selectedBusId == null) {
                Toast.makeText(this, "Please select a Bus first", Toast.LENGTH_SHORT).show()
            } else if (routePoints.isEmpty()) {
                Toast.makeText(this, "Please add at least one stop on the map", Toast.LENGTH_SHORT).show()
            } else {
                saveRouteToFirebase()
            }
        }
    }

    private fun loadExistingRoute(busId: String) {
        dbRef.child("routes").child(busId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    tvInstruction.visibility = View.VISIBLE
                    tvInstruction.text = "Existing route loaded. Tap to add more stops or Clear All."

                    var stopIndex = 1
                    for (stopSnap in snapshot.children) {
                        val lat = stopSnap.child("latitude").value.toString().toDouble()
                        val lng = stopSnap.child("longitude").value.toString().toDouble()
                        val point = GeoPoint(lat, lng)

                        addStopMarker(point, stopIndex)
                        stopIndex++
                    }
                    if (routePoints.isNotEmpty()) {
                        map.controller.animateTo(routePoints[0])
                    }
                } else {
                    tvInstruction.visibility = View.VISIBLE
                    tvInstruction.text = "No route found. Tap on map to create a new one!"
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun clearMapData() {
        routePoints.clear()
        map.overlays.clear()
        setupMap() 
        map.invalidate() 
    }

    private fun setupMap() {
        map.setMultiTouchControls(true)
        val mapController = map.controller
        mapController.setZoom(15.0)

        val startPoint = GeoPoint(33.6844, 73.0479)
        mapController.setCenter(startPoint)

        val mReceive = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                if (selectedBusId != null) {
                    addStopMarker(p, routePoints.size + 1)
                } else {
                    Toast.makeText(this@DefineRouteActivity, "Select a Bus first!", Toast.LENGTH_SHORT).show()
                }
                return true
            }
            override fun longPressHelper(p: GeoPoint): Boolean = false
        }

        map.overlays.add(MapEventsOverlay(mReceive))
    }

    private fun loadBuses() {
        dbRef.child("buses").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val busNames = ArrayList<String>()
                if (snapshot.exists()) {
                    for (busSnap in snapshot.children) {
                        val name = busSnap.child("busName").value.toString()
                        val id = busSnap.key.toString()
                        busNames.add(name)
                        busMap[name] = id
                    }
                    val adapter = ArrayAdapter(this@DefineRouteActivity, android.R.layout.simple_dropdown_item_1line, busNames)
                    spinnerBus.setAdapter(adapter)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun addStopMarker(p: GeoPoint, stopNumber: Int) {
        val marker = Marker(map)
        marker.position = p
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = "Stop $stopNumber"
        marker.icon = getNumberedIcon(marker, stopNumber)

        map.overlays.add(marker)
        map.invalidate() 
        routePoints.add(p)
    }

    private fun getNumberedIcon(marker: Marker, number: Int): Drawable {
        val defaultIcon = marker.icon
        val bitmap = Bitmap.createBitmap(defaultIcon.intrinsicWidth, defaultIcon.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        defaultIcon.setBounds(0, 0, canvas.width, canvas.height)
        defaultIcon.draw(canvas)

        val paint = Paint().apply {
            color = Color.BLACK 
            textSize = 40f 
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }

        val xPos = canvas.width / 2f
        val yPos = (canvas.height / 3f) + 15f

        canvas.drawText(number.toString(), xPos, yPos, paint)
        return BitmapDrawable(resources, bitmap)
    }

    private fun saveRouteToFirebase() {
        val updates = HashMap<String, Any?>()
        val routeData = HashMap<String, Any>()

        for ((index, point) in routePoints.withIndex()) {
            val stopData = mapOf(
                "latitude" to point.latitude,
                "longitude" to point.longitude
            )
            routeData["stop_${index + 1}"] = stopData
        }


        updates["routes/${selectedBusId!!}"] = routeData

        updates["buses/${selectedBusId!!}/assignedRoute"] = "Assigned"

        dbRef.updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Route saved successfully for this Bus!", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error saving route: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}