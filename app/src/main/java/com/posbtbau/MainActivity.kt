package com.posbtbau

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var productListView: ListView
    private lateinit var cartListView: ListView
    private lateinit var cartTotalText: TextView
    private lateinit var reportSummary: TextView
    private lateinit var printerStatus: TextView
    private lateinit var printerSpinner: Spinner
    private lateinit var dataStore: DataStore
    private lateinit var printerHelper: BluetoothPrinterHelper

    private val productCatalog = listOf(
        Product("Coffee", 3.50),
        Product("Tea", 2.75),
        Product("Sandwich", 6.90),
        Product("Cake slice", 4.20),
        Product("Bottle water", 1.50)
    )

    private val cartItems = mutableListOf<CartItem>()
    private var lastReceipt = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dataStore = DataStore(getSharedPreferences("pos_data", MODE_PRIVATE))
        printerHelper = BluetoothPrinterHelper(this)

        productListView = findViewById(R.id.productListView)
        cartListView = findViewById(R.id.cartListView)
        cartTotalText = findViewById(R.id.textCartTotal)
        reportSummary = findViewById(R.id.textReportSummary)
        printerStatus = findViewById(R.id.textPrinterStatus)
        printerSpinner = findViewById(R.id.printerSpinner)

        findViewById<Button>(R.id.buttonProducts).setOnClickListener { showSection(R.id.sectionProducts) }
        findViewById<Button>(R.id.buttonCart).setOnClickListener { showSection(R.id.sectionCart) }
        findViewById<Button>(R.id.buttonReports).setOnClickListener { showSection(R.id.sectionReports) }
        findViewById<Button>(R.id.buttonPrinter).setOnClickListener { showSection(R.id.sectionPrinter) }

        findViewById<Button>(R.id.buttonCheckout).setOnClickListener { checkout() }
        findViewById<Button>(R.id.buttonPrintReport).setOnClickListener { printDailyReport() }
        findViewById<Button>(R.id.buttonRefreshPrinters).setOnClickListener { refreshPairedPrinters() }
        findViewById<Button>(R.id.buttonConnectPrinter).setOnClickListener { connectSelectedPrinter() }
        findViewById<Button>(R.id.buttonPrintLastReceipt).setOnClickListener { printLastReceipt() }

        productListView.setOnItemClickListener { _, _, position, _ -> addProductToCart(productCatalog[position]) }
        cartListView.setOnItemLongClickListener { _, _, position, _ -> removeCartItem(position); true }

        refreshProductCatalog()
        refreshCartView()
        refreshReportView()
        refreshPairedPrinters()
        requestBluetoothPermissions()
    }

    private fun requestBluetoothPermissions() {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )
        val missing = permissions.filter { ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 101)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            if (grantResults.any { it != PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Bluetooth permissions are required for printer connection.", Toast.LENGTH_LONG).show()
            } else {
                refreshPairedPrinters()
            }
        }
    }

    private fun showSection(sectionId: Int) {
        val sections = listOf(
            R.id.sectionProducts,
            R.id.sectionCart,
            R.id.sectionReports,
            R.id.sectionPrinter
        )
        sections.forEach { findViewById<View>(it).visibility = if (it == sectionId) View.VISIBLE else View.GONE }
    }

    private fun refreshProductCatalog() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, productCatalog.map { "${it.name} - $${"%.2f".format(it.price)}" })
        productListView.adapter = adapter
    }

    private fun refreshCartView() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, cartItems.map { "${it.product.name} x${it.quantity} - $${"%.2f".format(it.totalPrice())}" })
        cartListView.adapter = adapter
        cartTotalText.text = "Total: $${"%.2f".format(cartItems.sumOf { it.totalPrice() })}"
    }

    private fun refreshReportView() {
        val todaySales = dataStore.getTodaySales()
        if (todaySales.isEmpty()) {
            reportSummary.text = "No sales recorded today."
            return
        }

        val total = todaySales.sumOf { it.total }
        reportSummary.text = buildString {
            append("Sales today: ${todaySales.size}\n")
            append("Total revenue: $${"%.2f".format(total)}\n\n")
            append("Recent receipts:\n")
            todaySales.takeLast(5).forEach { append("- ${it.time} • $${"%.2f".format(it.total)}\n") }
        }
    }

    private fun refreshPairedPrinters() {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        val paired = adapter?.bondedDevices?.toList().orEmpty()
        val labels = paired.map { "${it.name} (${it.address})" }.ifEmpty { listOf("No paired printers found") }
        printerSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
    }

    private fun addProductToCart(product: Product) {
        val quantities = arrayOf("1", "2", "3", "4", "5")
        AlertDialog.Builder(this)
            .setTitle("Add ${product.name}")
            .setItems(quantities) { _, which ->
                val quantity = quantities[which].toInt()
                val existing = cartItems.find { it.product.name == product.name }
                if (existing != null) {
                    existing.quantity += quantity
                } else {
                    cartItems.add(CartItem(product, quantity))
                }
                refreshCartView()
                Toast.makeText(this, "Added $quantity x ${product.name} to cart", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun removeCartItem(position: Int) {
        if (position in cartItems.indices) {
            val removed = cartItems.removeAt(position)
            refreshCartView()
            Toast.makeText(this, "Removed ${removed.product.name}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkout() {
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Cart is empty", Toast.LENGTH_SHORT).show()
            return
        }

        val total = cartItems.sumOf { it.totalPrice() }
        val receipt = buildReceipt(cartItems, total)
        lastReceipt = receipt
        dataStore.addSale(SaleRecord(currentTime(), total, receipt))
        cartItems.clear()
        refreshCartView()
        refreshReportView()
        Toast.makeText(this, "Sale recorded. Receipt ready to print.", Toast.LENGTH_LONG).show()
    }

    private fun buildReceipt(items: List<CartItem>, total: Double): String {
        return buildString {
            append("POS BTB AU\n")
            append("==============================\n")
            append("Date: ${currentTime()}\n")
            append("------------------------------\n")
            items.forEach { append("${it.product.name} x${it.quantity}  $${"%.2f".format(it.totalPrice())}\n") }
            append("------------------------------\n")
            append("Total: $${"%.2f".format(total)}\n")
            append("Thank you for your purchase!\n")
        }
    }

    private fun printDailyReport() {
        val todaySales = dataStore.getTodaySales()
        if (todaySales.isEmpty()) {
            Toast.makeText(this, "No sales to print today.", Toast.LENGTH_SHORT).show()
            return
        }
        val reportText = buildString {
            append("POS BTB AU - Daily Report\n")
            append("==============================\n")
            append("Date: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}\n")
            append("Total receipts: ${todaySales.size}\n")
            append("Total revenue: $${"%.2f".format(todaySales.sumOf { it.total })}\n")
            append("------------------------------\n")
            todaySales.forEach { append("${it.time} - $${"%.2f".format(it.total)}\n") }
            append("==============================\n")
        }
        lastReceipt = reportText
        printerHelper.printText(reportText)
        Toast.makeText(this, "Printing daily report...", Toast.LENGTH_SHORT).show()
    }

    private fun connectSelectedPrinter() {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        val pairedDevices = adapter?.bondedDevices?.toList().orEmpty()
        val index = printerSpinner.selectedItemPosition
        if (pairedDevices.isEmpty() || index !in pairedDevices.indices) {
            Toast.makeText(this, "Select a paired printer first.", Toast.LENGTH_SHORT).show()
            return
        }
        val device = pairedDevices[index]
        printerStatus.text = "Connecting to ${device.name}..."
        Thread {
            val connected = printerHelper.connect(device)
            runOnUiThread {
                printerStatus.text = if (connected) "Connected to ${device.name}" else "Failed to connect"
            }
        }.start()
    }

    private fun printLastReceipt() {
        if (lastReceipt.isBlank()) {
            Toast.makeText(this, "No receipt available to print.", Toast.LENGTH_SHORT).show()
            return
        }
        printerHelper.printText(lastReceipt)
        Toast.makeText(this, "Sending receipt to printer...", Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("SimpleDateFormat")
    private fun currentTime(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
    }
}
