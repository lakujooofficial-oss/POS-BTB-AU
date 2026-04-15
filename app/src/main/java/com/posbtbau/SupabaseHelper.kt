package com.posbtbau

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

private const val SUPABASE_URL = "https://qpsfrsxymboivxkxjrto.supabase.co"
private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InFwc2Zyc3h5eW1ib2l2eGt4anJ0byIsInJvbGUiOiJhbm9uIiwiaWF0IjoxNzc2MTUyMTU1LCJleHAiOjIwOTE3MjgxNTV9.8WQQbyh84TdrmE94GwhOlVVuJcFz90U3eU0XSz5Yqlk"
private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

class SupabaseHelper {
    private val client = OkHttpClient()

    fun syncProducts(products: List<Product>, callback: (Boolean, String) -> Unit) {
        val payload = JSONArray().apply {
            products.forEach {
                put(JSONObject().apply {
                    put("name", it.name)
                    put("price", it.price)
                })
            }
        }
        postJson("/rest/v1/products", payload.toString(), callback)
    }

    fun syncPromotions(promotions: List<Promotion>, callback: (Boolean, String) -> Unit) {
        val payload = JSONArray().apply {
            promotions.forEach {
                put(JSONObject().apply {
                    put("name", it.name)
                    put("discount_percent", it.discountPercent)
                })
            }
        }
        postJson("/rest/v1/promotions", payload.toString(), callback)
    }

    fun postSale(record: SaleRecord, callback: (Boolean, String) -> Unit) {
        val payload = JSONArray().apply {
            put(JSONObject().apply {
                put("time", record.time)
                put("total", record.total)
                put("receipt", record.receipt)
            })
        }
        postJson("/rest/v1/sales", payload.toString(), callback)
    }

    private fun postJson(path: String, jsonBody: String, callback: (Boolean, String) -> Unit) {
        val request = Request.Builder()
            .url(SUPABASE_URL + path)
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_KEY")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "resolution=merge-duplicates")
            .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                callback(false, e.message ?: "Network error")
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    callback(true, body)
                } else {
                    callback(false, "${response.code}: $body")
                }
            }
        })
    }
}
