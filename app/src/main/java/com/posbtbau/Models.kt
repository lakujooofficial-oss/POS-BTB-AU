package com.posbtbau

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class Product(val name: String, val price: Double)

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
}
