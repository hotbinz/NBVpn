package com.github.nbvpn.bg

import android.os.ParcelFileDescriptor
import android.util.Log
import com.github.nbvpn.net.ByteBufferPool
import com.github.nbvpn.net.Packets
import okhttp3.internal.http.HttpHeaders
import java.io.FileInputStream
import java.nio.ByteBuffer
import org.pcap4j.packet.IpV4Packet
import org.pcap4j.packet.TcpPacket
import java.io.IOException
import java.net.Inet4Address
import java.nio.ByteOrder
import org.xbill.DNS.Address.getHostName
import java.net.InetAddress


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
                if(packets.tcp) {
                    val ipV4Packet = packets.parsedPacket.get(IpV4Packet::class.java)
                    val dstAddr: Inet4Address = ipV4Packet.getHeader().dstAddr
                    val addresses = InetAddress.getAllByName(dstAddr.hostAddress) // ip or DNS name
                    for (i in addresses.indices) {
                        val hostname = addresses[i].hostName
                        Log.e("VpnTrafficMonitorThread", "RequestHostName:"+hostname)
                    }
                    var tcpPacket: TcpPacket.TcpHeader = ipV4Packet.payload.header as TcpPacket.TcpHeader
                    Log.e("VpnTrafficMonitorThread", "RequestPort"+tcpPacket.dstPort)
                }
            }

            //可减少内存抖动??
            Thread.sleep(50)
        }
    }
}