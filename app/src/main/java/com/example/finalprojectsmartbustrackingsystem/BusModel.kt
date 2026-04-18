package com.example.finalprojectsmartbustrackingsystem

data class BusModel(
    val busId: String? = "",
    val busName: String? = "",
    val licensePlate: String? = "",
    val capacity: String? = "",
    val assignedRoute: String? = "Not Assigned",
    val assignedDriverId: String? = "Not Assigned",
    val assignedDriverName: String? = "Not Assigned",
    val isAssigned: Boolean = false,
    val lastLat: Double? = null,
    val lastLng: Double? = null
)