package com.example.finalprojectsmartbustrackingsystem

data class BusModel(
    val busId: String? = "",
    val busName: String? = "",
    val licensePlate: String? = "",
    val capacity: String? = "",
    val assignedRoute: String? = "",
    val assignedDriver: String? = ""
)