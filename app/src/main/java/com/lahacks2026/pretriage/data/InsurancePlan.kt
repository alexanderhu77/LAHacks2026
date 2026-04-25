package com.lahacks2026.pretriage.data

import android.content.Context
import com.google.gson.Gson

data class InsurancePlan(
    val id: String,
    val name: String,
    val telehealthDeepLink: String,
    val urgentCareNetworkQuery: String,
    val copayBySeverity: Map<String, Int>
) {
    fun copayFor(severity: SeverityLevel): Int? = copayBySeverity[severity.name]
}

object InsurancePlanLoader {
    private val gson = Gson()

    fun load(context: Context, planId: String): InsurancePlan {
        val path = "insurance_plans/$planId.json"
        val json = context.assets.open(path).bufferedReader().use { it.readText() }
        return gson.fromJson(json, InsurancePlan::class.java)
    }

    fun loadAll(context: Context): List<InsurancePlan> {
        return context.assets.list("insurance_plans")
            ?.filter { it.endsWith(".json") }
            ?.map { fileName ->
                val json = context.assets.open("insurance_plans/$fileName")
                    .bufferedReader().use { it.readText() }
                gson.fromJson(json, InsurancePlan::class.java)
            }
            ?: emptyList()
    }
}
