package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey
    val id: Int = 1,
    val name: String,
    val email: String,
    val salary: Double,
    val budget: Double,
    val currency: String,
    val country: String,
    val language: String
)
