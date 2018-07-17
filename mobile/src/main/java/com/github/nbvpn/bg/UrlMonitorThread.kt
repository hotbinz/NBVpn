package com.github.nbvpn.bg

import android.net.LocalSocket
import android.util.Log
import com.github.nbvpn.App
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

class UrlMonitorThread : LocalSocketListener("UrlMonitorThread") {
    override val socketFile = File(App.app.deviceContext.filesDir, "logging_path")
    override fun accept(socket: LocalSocket) {
        try {
            val byteArrayOutputStream = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            var len = 0
            while (socket.inputStream.read(buffer).apply { len = this } != -1) {
                byteArrayOutputStream.write(buffer, 0, len)
            }
            var resultData = String(byteArrayOutputStream.toByteArray())
            if(!resultData.contains(":53")) {
                App.app.sendStatisInfo(resultData)
                Log.e(tag, resultData)
            }
            //TrafficMonitor.update(stat.getLong(0), stat.getLong(8))
        } catch (e: IOException) {
            Log.e(tag, "Error when recv traffic stat", e)
            App.app.track(e)
        }
    }
}