package com.posbtbau

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.InputType
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
    private lateinit var promotionSpinner: Spinner
    private lateinit var dataStore: DataStore
    private lateinit var printerHelper: BluetoothPrinterHelper
    private lateinit var supabaseHelper: SupabaseHelper

    private val productCatalog = mutableListOf(
        Product("Coffee", 3.50),
        Product("Tea", 2.75),
        Product("Sandwich", 6.90),
        Product("Cake slice", 4.20),
        Product("Bottle water", 1.50)
    )
    private val promotions = mutableListOf<Promotion>()
    private val cartItems = mutableListOf<CartItem>()
    private var selectedPromotion: Promotion? = null
    private var lastReceipt = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dataStore = DataStore(getSharedPreferences("pos_data", MODE_PRIVATE))
        printerHelper = BluetoothPrinterHelper(this)
        supabaseHelper = SupabaseHelper()

        productListView = findViewById(R.id.productListView)
        cartListView = findViewById(R.id.cartListView)
        cartTotalText = findViewById(R.id.textCartTotal)
        reportSummary = findViewById(R.id.textReportSummary)
        printerStatus = findViewById(R.id.textPrinterStatus)
        printerSpinner = findViewById(R.id.printerSpinner)
        promotionSpinner = findViewById(R.id.promotionSpinner)

        findViewById<Button>(R.id.buttonProducts).setOnClickListener { showSection(R.id.sectionProducts) }
        findViewById<Button>(R.id.buttonCart).setOnClickListener { showSection(R.id.sectionCart) }
        findViewById<Button>(R.id.buttonReports).setOnClickListener { showSection(R.id.sectionReports) }
        findViewById<Button>(R.id.buttonPrinter).setOnClickListener { showSection(R.id.sectionPrinter) }

        findViewById<Button>(R.id.buttonAddProduct).setOnClickListener { showAddProductDialog() }
        findViewById<Button>(R.id.buttonAddPromotion).setOnClickListener { showAddPromotionDialog() }
        findViewById<Button>(R.id.buttonCheckout).setOnClickListener { checkout() }
        findViewById<Button>(R.id.buttonClearCart).setOnClickListener { clearCart() }
        findViewById<Button>(R.id.buttonViewTodaySales).setOnClickListener { displayDailySales() }
        findViewById<Button>(R.id.buttonViewWeeklySales).setOnClickListener { displayWeeklySales() }
        findViewById<Button>(R.id.buttonPrintReport).setOnClickListener { printDailyReport() }
        findViewById<Button>(R.id.buttonPrintWeeklyReport).setOnClickListener { printWeeklyReport() }
        findViewById<Button>(R.id.buttonSyncSupabase).setOnClickListener { syncSupabaseData() }
        findViewById<Button>(R.id.buttonRefreshPrinters).setOnClickListener { refreshPairedPrinters() }
        findViewById<Button>(R.id.buttonConnectPrinter).setOnClickListener { connectSelectedPrinter() }
        findViewById<Button>(R.id.buttonPrintLastReceipt).setOnClickListener { printLastReceipt() }

        productListView.setOnItemClickListener { _, _, position, _ -> addProductToCart(productCatalog[position]) }
        productListView.setOnItemLongClickListener { _, _, position, _ -> removeCatalogProduct(position); true }
        cartListView.setOnItemLongClickListener { _, _, position, _ -> removeCartItem(position); true }

        promotionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedPromotion = if (promotions.isEmpty() || position !in promotions.indices) null else promotions[position]
                refreshCartView()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedPromotion = null
                refreshCartView()
            }
        }

        refreshProductCatalog()
        refreshPromotionSpinner()
        refreshCartView()
        displayDailySales()
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
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, productCatalog.map { "${it.name} - Rp ${"%.2f".format(it.price)}" })
        productListView.adapter = adapter
    }

    private fun refreshPromotionSpinner() {
        val labels = promotions.map { "${it.name} (${it.discountPercent.toInt()}%)" }.ifEmpty { listOf("Tidak ada promo") }
        promotionSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun refreshCartView() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, cartItems.map { "${it.product.name} x${it.quantity} - Rp ${"%.2f".format(it.totalPrice())}" })
        cartListView.adapter = adapter
        val total = calculateCartTotal()
        val promoText = selectedPromotion?.let { " (-${it.discountPercent.toInt()}%)" } ?: ""
        cartTotalText.text = "Total: Rp ${"%.2f".format(total)}$promoText"
    }

    private fun calculateCartTotal(): Double {
        val rawTotal = cartItems.sumOf { it.totalPrice() }
        val discount = selectedPromotion?.discountPercent ?: 0.0
        return rawTotal * (1.0 - discount / 100.0)
    }

    private fun addProductToCart(product: Product) {
        val quantities = arrayOf("1", "2", "3", "4", "5")
        AlertDialog.Builder(this)
            .setTitle("Tambah ${product.name}")
            .setItems(quantities) { _, which ->
                val quantity = quantities[which].toInt()
                val existing = cartItems.find { it.product.name == product.name }
                if (existing != null) {
                    existing.quantity += quantity
                } else {
                    cartItems.add(CartItem(product, quantity))
                }
                refreshCartView()
                Toast.makeText(this, "Ditambahkan $quantity x ${product.name}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun removeCatalogProduct(position: Int) {
        if (position in productCatalog.indices) {
            val removed = productCatalog.removeAt(position)
            refreshProductCatalog()
            Toast.makeText(this, "Produk ${removed.name} dihapus dari katalog", Toast.LENGTH_SHORT).show()
        }
    }

    private fun removeCartItem(position: Int) {
        if (position in cartItems.indices) {
            val removed = cartItems.removeAt(position)
            refreshCartView()
            Toast.makeText(this, "Produk ${removed.product.name} dihapus dari keranjang", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearCart() {
        cartItems.clear()
        refreshCartView()
        Toast.makeText(this, "Keranjang dikosongkan", Toast.LENGTH_SHORT).show()
    }

    private fun showAddProductDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 16, 24, 0)
        }
        val nameInput = EditText(this).apply {
            hint = "Nama produk"
        }
        val priceInput = EditText(this).apply {
            hint = "Harga"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        layout.addView(nameInput)
        layout.addView(priceInput)

        AlertDialog.Builder(this)
            .setTitle("Tambah Produk Baru")
            .setView(layout)
            .setPositiveButton("Simpan") { _, _ ->
                val name = nameInput.text.toString().trim()
                val price = priceInput.text.toString().toDoubleOrNull() ?: 0.0
                if (name.isBlank() || price <= 0.0) {
                    Toast.makeText(this, "Isi nama dan harga dengan benar.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                productCatalog.add(Product(name, price))
                refreshProductCatalog()
                Toast.makeText(this, "Produk $name ditambahkan", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showAddPromotionDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 16, 24, 0)
        }
        val nameInput = EditText(this).apply {
            hint = "Nama promo"
        }
        val discountInput = EditText(this).apply {
            hint = "Diskon (%)"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        layout.addView(nameInput)
        layout.addView(discountInput)

        AlertDialog.Builder(this)
            .setTitle("Buat Promo Baru")
            .setView(layout)
            .setPositiveButton("Simpan") { _, _ ->
                val name = nameInput.text.toString().trim()
                val discount = discountInput.text.toString().toDoubleOrNull() ?: 0.0
                if (name.isBlank() || discount <= 0.0) {
                    Toast.makeText(this, "Isi nama dan persentase diskon dengan benar.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                promotions.add(Promotion(name, discount))
                refreshPromotionSpinner()
                Toast.makeText(this, "Promo $name ditambahkan", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun checkout() {
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Keranjang kosong", Toast.LENGTH_SHORT).show()
            return
        }
        val total = calculateCartTotal()
        val receipt = buildReceipt(cartItems, total, selectedPromotion)
        lastReceipt = receipt
        dataStore.addSale(SaleRecord(currentTime(), total, receipt))
        postSaleToSupabase(SaleRecord(currentTime(), total, receipt))
        cartItems.clear()
        refreshCartView()
        displayDailySales()
        Toast.makeText(this, "Transaksi berhasil. Resi siap dicetak.", Toast.LENGTH_LONG).show()
    }

    private fun buildReceipt(items: List<CartItem>, total: Double, promotion: Promotion?): String {
        return buildString {
            append("POS BTB AU\n")
            append("==============================\n")
            append("Tanggal: ${currentTime()}\n")
            append("------------------------------\n")
            items.forEach { append("${it.product.name} x${it.quantity}  Rp ${"%.2f".format(it.totalPrice())}\n") }
            if (promotion != null) {
                append("------------------------------\n")
                append("Promo: ${promotion.name} -${promotion.discountPercent.toInt()}%\n")
            }
            append("------------------------------\n")
            append("Total: Rp ${"%.2f".format(total)}\n")
            append("==============================\n")
            append("Terima kasih atas pembelian Anda!\n")
        }
    }

    private fun displayDailySales() {
        val todaySales = dataStore.getTodaySales()
        if (todaySales.isEmpty()) {
            reportSummary.text = "Tidak ada penjualan hari ini."
            return
        }
        val total = todaySales.sumOf { it.total }
        reportSummary.text = buildString {
            append("Ringkasan hari ini:\n")
            append("Total transaksi: ${todaySales.size}\n")
            append("Total pendapatan: Rp ${"%.2f".format(total)}\n\n")
            append("Transaksi terakhir:\n")
            todaySales.takeLast(5).forEach { append("- ${it.time} • Rp ${"%.2f".format(it.total)}\n") }
        }
    }

    private fun displayWeeklySales() {
        val weeklySales = dataStore.getWeeklySales()
        if (weeklySales.isEmpty()) {
            reportSummary.text = "Tidak ada penjualan dalam 7 hari terakhir."
            return
        }
        val total = weeklySales.sumOf { it.total }
        reportSummary.text = buildString {
            append("Ringkasan mingguan:\n")
            append("Total transaksi: ${weeklySales.size}\n")
            append("Total pendapatan: Rp ${"%.2f".format(total)}\n\n")
            append("Transaksi terakhir:\n")
            weeklySales.takeLast(5).forEach { append("- ${it.time} • Rp ${"%.2f".format(it.total)}\n") }
        }
    }

    private fun printDailyReport() {
        val todaySales = dataStore.getTodaySales()
        if (todaySales.isEmpty()) {
            Toast.makeText(this, "Tidak ada penjualan untuk dicetak hari ini.", Toast.LENGTH_SHORT).show()
            return
        }
        val reportText = buildReportText(todaySales, "Laporan Harian")
        lastReceipt = reportText
        printerHelper.printText(reportText)
        Toast.makeText(this, "Mencetak laporan harian...", Toast.LENGTH_SHORT).show()
    }

    private fun printWeeklyReport() {
        val weeklySales = dataStore.getWeeklySales()
        if (weeklySales.isEmpty()) {
            Toast.makeText(this, "Tidak ada penjualan untuk dicetak minggu ini.", Toast.LENGTH_SHORT).show()
            return
        }
        val reportText = buildReportText(weeklySales, "Laporan Mingguan")
        lastReceipt = reportText
        printerHelper.printText(reportText)
        Toast.makeText(this, "Mencetak laporan mingguan...", Toast.LENGTH_SHORT).show()
    }

    private fun buildReportText(sales: List<SaleRecord>, title: String): String {
        return buildString {
            append("POS BTB AU - $title\n")
            append("==============================\n")
            append("Tanggal: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}\n")
            append("Total transaksi: ${sales.size}\n")
            append("Total pendapatan: Rp ${"%.2f".format(sales.sumOf { it.total })}\n")
            append("------------------------------\n")
            sales.forEach { append("${it.time} - Rp ${"%.2f".format(it.total)}\n") }
            append("==============================\n")
        }
    }

    private fun syncSupabaseData() {
        Thread {
            supabaseHelper.syncProducts(productCatalog) { success, message ->
                runOnUiThread {
                    if (success) Toast.makeText(this, "Produk disinkronkan ke Supabase", Toast.LENGTH_SHORT).show()
                    else Toast.makeText(this, "Sinkron produk gagal: $message", Toast.LENGTH_LONG).show()
                }
            }
            supabaseHelper.syncPromotions(promotions) { success, message ->
                runOnUiThread {
                    if (success) Toast.makeText(this, "Promo disinkronkan ke Supabase", Toast.LENGTH_SHORT).show()
                    else Toast.makeText(this, "Sinkron promo gagal: $message", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun postSaleToSupabase(record: SaleRecord) {
        supabaseHelper.postSale(record) { success, message ->
            runOnUiThread {
                if (success) Toast.makeText(this, "Data penjualan tersimpan di Supabase", Toast.LENGTH_SHORT).show()
                else Toast.makeText(this, "Gagal simpan penjualan: $message", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun refreshPairedPrinters() {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        val paired = adapter?.bondedDevices?.toList().orEmpty()
        val labels = paired.map { "${it.name} (${it.address})" }.ifEmpty { listOf("No paired printers found") }
        printerSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
    }

    private fun connectSelectedPrinter() {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        val pairedDevices = adapter?.bondedDevices?.toList().orEmpty()
        val index = printerSpinner.selectedItemPosition
        if (pairedDevices.isEmpty() || index !in pairedDevices.indices) {
            Toast.makeText(this, "Pilih printer yang sudah dipasangkan terlebih dahulu.", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "Tidak ada resi terakhir untuk dicetak.", Toast.LENGTH_SHORT).show()
            return
        }
        printerHelper.printText(lastReceipt)
        Toast.makeText(this, "Mengirim resi ke printer...", Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("SimpleDateFormat")
    private fun currentTime(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
    }
}
