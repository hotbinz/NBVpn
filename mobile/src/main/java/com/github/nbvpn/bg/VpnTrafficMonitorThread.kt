package com.github.nbvpn.bg

import android.os.ParcelFileDescriptor
import android.util.Log
import com.github.nbvpn.net.ByteBufferPool
import com.github.nbvpn.net.Packets
import java.io.FileInputStream
import java.nio.ByteBuffer


class VpnTrafficMonitorThread(var conn: ParcelFileDescriptor): Thread() {

    override fun run() {
        val vpnInput = FileInputStream(conn.fileDescriptor).channel
        var buffer2Net: ByteBuffer? = null
        var isDataSend = true

        while (true) {
            //数据发送出去了,就get 新的咯
            buffer2Net = ByteBufferPool.acquire()
            val inputSize = vpnInput.read(buffer2Net)
            if(inputSize > 0) {
                buffer2Net?.flip()
                var packets = Packets(buffer2Net, inputSize)
                Log.e("VpnTrafficMonitorThread", "RequestHostName" + packets.tcp)
            }

            //可减少内存抖动??
            Thread.sleep(50)
        }
    }
}