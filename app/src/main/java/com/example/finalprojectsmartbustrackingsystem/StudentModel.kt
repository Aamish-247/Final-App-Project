package com.example.finalprojectsmartbustrackingsystem

data class StudentModel(
    val studentId: String? = "",
    val studentName: String? = "",
    val parentId: String? = "",
    val parentName: String? = "",
    val busId: String? = "",
    val busName: String? = "",
    val driverId: String? = "",
    val driverName: String? = "",
    val pickupStop: String? = null,
    val dropoffStop: String? = null,
    var attendanceStatus: String? = "Pending"
)