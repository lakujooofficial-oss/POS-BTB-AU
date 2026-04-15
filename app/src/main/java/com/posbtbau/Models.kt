package com.posbtbau

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class Product(val name: String, val price: Double)

data class Promotion(val name: String, val discountPercent: Double)

data class CartItem(val product: Product, var quantity: Int) {
    fun totalPrice(): Double = product.price * quantity
}

data class SaleRecord(val time: String, val total: Double, val receipt: String)

class DataStore(private val preferences: SharedPreferences) {
    private val keySales = "sales_records"

    fun addSale(record: SaleRecord) {
        val sales = loadSales().toMutableList()
        sales.add(record)
        preferences.edit().putString(keySales, salesToJson(sales)).apply()
    }

    fun getTodaySales(): List<SaleRecord> {
        val formatter = SimpleDateFormat("yyyy-MM-dd")
        val today = formatter.format(Date())
        return loadSales().filter { it.time.startsWith(today) }
    }

    fun getWeeklySales(): List<SaleRecord> {
        val endDate = Date()
        val calendar = Calendar.getInstance().apply { time = endDate; add(Calendar.DAY_OF_YEAR, -7) }
        val cutoff = calendar.time
        return loadSales().filter { parseTime(it.time)?.after(cutoff) == true }
    }

    fun getAllSales(): List<SaleRecord> = loadSales()

    private fun loadSales(): List<SaleRecord> {
        val raw = preferences.getString(keySales, "[]") ?: "[]"
        val array = JSONArray(raw)
        val sales = mutableListOf<SaleRecord>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            sales.add(SaleRecord(item.optString("time"), item.optDouble("total"), item.optString("receipt")))
        }
        return sales
    }

    private fun salesToJson(sales: List<SaleRecord>): String {
        val array = JSONArray()
        sales.forEach {
            val obj = JSONObject()
            obj.put("time", it.time)
            obj.put("total", it.total)
            obj.put("receipt", it.receipt)
            array.put(obj)
        }
        return array.toString()
    }

    private fun parseTime(value: String): Date? {
        return try {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(value)
        } catch (e: Exception) {
            null
        }
    }
}
