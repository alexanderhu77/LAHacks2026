package com.lahacks2026.pretriage.data

data class InsurancePlan(
    val plan_name: String,
    val telehealth: TelehealthInfo,
    val urgent_care: ProviderInfo,
    val emergency: EmergencyInfo
)

data class TelehealthInfo(
    val provider: String,
    val copay: Int,
    val url: String
)

data class ProviderInfo(
    val provider: String,
    val copay: Int,
    val distance: String
)

data class EmergencyInfo(
    val copay: Int,
    val note: String
)
