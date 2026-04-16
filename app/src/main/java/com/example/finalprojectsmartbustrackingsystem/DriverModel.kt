package com.example.finalprojectsmartbustrackingsystem

data class DriverModel(
    val uid: String? = "",
    val name: String? = "",
    val driverId: String? = "",
    val email: String? = "",
    val phone: String? = "",
    val assignedBusId: String? = null,
    val assignedBusName: String? = null,
    val shiftStart: String? = "--:--",
    val shiftEnd: String? = "--:--"
)