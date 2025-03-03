package de.tomcory.heimdall.scanner.traffic.connection.transportLayer

import android.net.VpnService
import android.os.Handler
import android.system.OsConstants
import de.tomcory.heimdall.scanner.traffic.components.ComponentManager
import de.tomcory.heimdall.scanner.traffic.components.DeviceWriteThread
import de.tomcory.heimdall.scanner.traffic.connection.inetLayer.IpPacketBuilder
import org.pcap4j.packet.Packet
import org.pcap4j.packet.UdpPacket
import org.pcap4j.packet.UnknownPacket
import org.pcap4j.packet.namednumber.UdpPort
import timber.log.Timber
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.BufferOverflowException
import java.nio.channels.DatagramChannel
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.util.Arrays

/**
 * Represents a transport-layer connection using UDP.
 *
 * @param componentManager
 * @param deviceWriter
 * @param initialPacket UDP datagram from which the necessary metadata is extracted to create the instance (ideally the very first datagram of a new socket).
 * @param ipPacketBuilder
 */
class UdpConnection internal constructor(
    componentManager: ComponentManager,
    deviceWriter: Handler,
    initialPacket: UdpPacket,
    ipPacketBuilder: IpPacketBuilder,
    remoteHost: String?
) : TransportLayerConnection(
    deviceWriter = deviceWriter,
    componentManager = componentManager,
    localPort = initialPacket.header.srcPort.valueAsInt(),
    remotePort = initialPacket.header.dstPort.valueAsInt(),
    remoteHost = remoteHost,
    ipPacketBuilder = ipPacketBuilder
) {
    override val protocol = "UDP"
    override val selectableChannel: DatagramChannel = openChannel(ipPacketBuilder.remoteAddress, componentManager.vpnService)
    override val selectionKey = connectChannel(componentManager.selector)
    override val appId: Int? = componentManager.appFinder.getAppId(ipPacketBuilder.localAddress, ipPacketBuilder.remoteAddress, localPort, remotePort, OsConstants.IPPROTO_UDP)
    override val appPackage: String? = componentManager.appFinder.getAppPackage(appId)
    override val id = createDatabaseEntity()

    init {
        if(id > 0) {
            Timber.d("udp$id Creating UDP Connection to ${ipPacketBuilder.remoteAddress.hostAddress}:${remotePort} ($remoteHost)")
        }
    }

    private fun openChannel(remoteAddress: InetAddress, vpnService: VpnService?): DatagramChannel {
        // open the channel now, but connect it asynchronously for better performance
        state = TransportLayerState.CONNECTING
        val selectableChannel = DatagramChannel.open()
        vpnService?.protect(selectableChannel.socket())
        selectableChannel.configureBlocking(false)
        selectableChannel.socket().soTimeout = 0
        selectableChannel.socket().receiveBufferSize = componentManager.maxPacketSize
        selectableChannel.connect(InetSocketAddress(remoteAddress, remotePort))
        state = TransportLayerState.CONNECTED
        return selectableChannel
    }

    private fun connectChannel(selector: Selector): SelectionKey? {
        // register OP_READ interest for the channel
        synchronized(ComponentManager.selectorMonitor) {
            selector.wakeup()
            val selectionKey = try {
                selectableChannel.register(selector, SelectionKey.OP_READ)
            } catch (e: Exception) {
                Timber.e(e, "udp$id Error registering SelectableChannel")
                null
            }
            selectionKey?.attach(this)
            return selectionKey
        }
    }

    override fun buildPayload(rawPayload: ByteArray): UdpPacket.Builder {
        return UdpPacket.Builder()
            .srcAddr(ipPacketBuilder.remoteAddress)
            .dstAddr(ipPacketBuilder.localAddress)
            .srcPort(UdpPort(remotePort.toShort(), ""))
            .dstPort(UdpPort(localPort.toShort(), ""))
            .correctChecksumAtBuild(true)
            .correctLengthAtBuild(true)
            .payloadBuilder(UnknownPacket.newPacket(rawPayload, 0, rawPayload.size).builder)
    }

    override fun wrapOutbound(payload: ByteArray) {
        // if the application layer returned anything, write it to the connection's outward-facing channel
        if (payload.isNotEmpty()) {
            outBuffer.clear()
            outBuffer.put(payload)
            outBuffer.flip()
            while (outBuffer.hasRemaining()) {
                try {
                    selectableChannel.write(outBuffer)
                } catch (e: IOException) {
                    Timber.e(e, "udp$id Error writing to DatagramChannel, closing connection")
                    closeHard()
                    break
                } catch (e: BufferOverflowException) {
                    Timber.e(e, "udp$id Error writing to DatagramChannel, closing connection")
                    closeHard()
                    break
                }
            }
        }
    }

    override fun wrapInbound(payload: ByteArray) {
        val forwardPacket = ipPacketBuilder.buildPacket(buildPayload(payload))
        deviceWriter.sendMessage(deviceWriter.obtainMessage(DeviceWriteThread.WRITE_UDP, forwardPacket))
    }

    override fun unwrapOutbound(outgoingPacket: Packet) {
        passOutboundToEncryptionLayer(outgoingPacket.payload)
    }

    override fun unwrapInbound() {
        if(selectionKey == null) {
            Timber.e("udp$id SelectionKey is null")
            state = TransportLayerState.ABORTED
            return
        }

        if (selectionKey.isReadable) {
            var bytesRead: Int
            do {
                try {
                    // read and forward the incoming data chunk by chunk
                    inBuffer.clear()
                    bytesRead = selectableChannel.read(inBuffer)
                    if (bytesRead > 0) {
                        inBuffer.flip()
                        val rawData = Arrays.copyOf(inBuffer.array(), bytesRead)

                        // pass the payload to the application layer for further processing
                        passInboundToEncryptionLayer(rawData)
                    }
                } catch (e: IOException) {
                    Timber.e(e, "udp$id Error reading data from DatagramChannel")
                    bytesRead = -1
                }
            } while (bytesRead > 0) //TODO: improve

            // no need to keep DNS connections open after the first and only packet
            if (remotePort == 53) {
                try {
                    selectableChannel.close()
                } catch (e: IOException) {
                    Timber.e(e, "udp$id Error closing DatagramChannel")
                }
                bytesRead = -1
            }

            // DatagramChannel is closed, do the same for the connection
            if (bytesRead == -1) {
                selectionKey.cancel()
                closeHard()
            }
        } else {
            Timber.e("udp$id UDP connection's channel triggered an event that isn't OP_READ")
        }
    }
}