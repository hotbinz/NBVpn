package com.github.nbvpn.net;

import android.util.Log;

import org.pcap4j.packet.IllegalRawDataException;
import org.pcap4j.packet.IpPacket;
import org.pcap4j.packet.IpSelector;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.UdpPacket;
import org.xbill.DNS.Message;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;

public class Packets {

    String TAG = "Packet";
    IpPacket parsedPacket = null;

    public Packets(ByteBuffer packet, int length) {
        byte[] packetData = new byte[length];
        packet.get(packetData, 0, length);
        try {
            parsedPacket = (IpPacket) IpSelector.newPacket(packetData, 0, packetData.length);
        } catch (Exception e) {
            Log.i(TAG, "handleDnsRequest: Discarding invalid IP packet", e);
        }
    }

    public InetAddress getInetAddress() {
        InetAddress destAddr = translateDestinationAdress(parsedPacket);
        return destAddr;
    }

    public String getTCP() {
        String filter = "ip and tcp and (dst host 127.0.0.1 and dst port 80)";
        TcpPacket tcpPacket = null;
        if (!(parsedPacket.getPayload() instanceof TcpPacket)) {
            try {
                tcpPacket = TcpPacket.newPacket(parsedPacket.getPayload().getRawData(),0,parsedPacket.getPayload().getRawData().length);
            } catch (IllegalRawDataException e) {
                e.printStackTrace();
            }
            Log.e(TAG, "VpnTrafficMonitorThread: getTCP " + tcpPacket.toString());
        }
        return "";
    }

    public String getRequestHostName() {


        if (!(parsedPacket.getPayload() instanceof UdpPacket)) {
            Log.e(TAG, "handleDnsRequest: Discarding unknown packet type " + parsedPacket.getPayload());
            return "";
        }

        UdpPacket parsedUdp = (UdpPacket) parsedPacket.getPayload();

        if (parsedUdp.getPayload() == null) {
            Log.e(TAG, "handleDnsRequest: Sending UDP packet without payload: " + parsedUdp);
            return "";
        }

        byte[] dnsRawData = (parsedUdp).getPayload().getRawData();
        Message dnsMsg;
        try {
            dnsMsg = new Message(dnsRawData);
        } catch (IOException e) {
            Log.e(TAG, "handleDnsRequest: Discarding non-DNS or invalid packet", e);
            return "";
        }
        if (dnsMsg.getQuestion() == null) {
            Log.e(TAG, "handleDnsRequest: Discarding DNS packet with no query " + dnsMsg);
            return "";
        }
        String dnsQueryName = dnsMsg.getQuestion().getName().toString(true);
        return dnsQueryName;
    }

    /**
     * Translates the destination address in the packet to the real one. In
     * case address translation is not used, this just returns the original one.
     *
     * @param parsedPacket Packet to get destination address for.
     * @return The translated address or null on failure.
     */
    private InetAddress translateDestinationAdress(IpPacket parsedPacket) {
        InetAddress destAddr = null;
//        if (upstreamDnsServers.size() > 0) {
//            byte[] addr = parsedPacket.getHeader().getDstAddr().getAddress();
//            int index = addr[addr.length - 1] - 2;
//
//            try {
//                destAddr = upstreamDnsServers.get(index);
//            } catch (Exception e) {
//                Log.e(TAG, "handleDnsRequest: Cannot handle packets to" + parsedPacket.getHeader().getDstAddr().getHostAddress(), e);
//                return null;
//            }
//            Log.d(TAG, String.format("handleDnsRequest: Incoming packet to %s AKA %d AKA %s", parsedPacket.getHeader().getDstAddr().getHostAddress(), index, destAddr));
//        } else
        {
            destAddr = parsedPacket.getHeader().getDstAddr();
            Log.d(TAG, String.format("handleDnsRequest: Incoming packet to %s - is upstream", parsedPacket.getHeader().getDstAddr().getHostAddress()));
        }
        return destAddr;
    }
}
