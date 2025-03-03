package com.ErikV121.network_monitoring_system.service;

import com.ErikV121.network_monitoring_system.model.ServerMessage;
import org.pcap4j.core.PacketListener;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.packet.EthernetPacket;
import org.pcap4j.util.MacAddress;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class NetworkMonitorService {

    private final SimpMessagingTemplate template;
    private static final int SNAPSHOT_LENGTH = 65535; // Maximum packet size
    private static final int READ_TIMEOUT = 50; // Timeout in milliseconds
    private static final long SAMPLING_PERIOD = 1000; // 1 second in milliseconds
    private static final long SEND_RATE = 500; // Send messages every 500ms

    private final AtomicLong uploadBytes = new AtomicLong(0);
    private final AtomicLong downloadBytes = new AtomicLong(0);
    private final AtomicLong packetCount = new AtomicLong(0);

    private final AtomicLong totalUploadBytes = new AtomicLong(0); // Track total upload
    private final AtomicLong totalDownloadBytes = new AtomicLong(0); // Track total download

    private final AtomicLong packetsSent = new AtomicLong(0); // Track packets sent
    private final AtomicLong packetsReceived = new AtomicLong(0); // Track packets received

    private MacAddress interfaceMac;
    private final ConcurrentLinkedQueue<ServerMessage> messageQueue = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService messageDispatcher = Executors.newSingleThreadScheduledExecutor();

    public NetworkMonitorService(SimpMessagingTemplate template) {
        this.template = template;

        // Select network interface and set MAC address
        PcapNetworkInterface device = getNetworkInterface();
        if (device != null) {
            this.interfaceMac = (MacAddress) device.getLinkLayerAddresses().get(0);
            startPacketCapture(device);
        } else {
            System.err.println("No network interface selected. Bandwidth monitoring disabled.");
        }

        // Start message dispatcher
        startMessageDispatcher();
    }

    private void startMessageDispatcher() {
        messageDispatcher.scheduleAtFixedRate(() -> {
            ServerMessage message = messageQueue.poll(); // Fetch the next message from the queue
            if (message != null) {
                template.convertAndSend("/main/test1", message);
            }
        }, 0, SEND_RATE, TimeUnit.MILLISECONDS);
    }

    private void startPacketCapture(PcapNetworkInterface device) {
        new Thread(() -> {
            try (PcapHandle handle = device.openLive(
                    SNAPSHOT_LENGTH,
                    PcapNetworkInterface.PromiscuousMode.NONPROMISCUOUS,
                    READ_TIMEOUT)) {

                PacketListener packetListener = packet -> {
                    try {
                        packetsReceived.incrementAndGet(); // Increment received packets
                        EthernetPacket ethernetPacket = packet.get(EthernetPacket.class);
                        if (ethernetPacket != null) {
                            MacAddress srcMac = ethernetPacket.getHeader().getSrcAddr();
                            MacAddress dstMac = ethernetPacket.getHeader().getDstAddr();

                            int payloadSize = packet.getRawData().length;

                            if (srcMac.equals(interfaceMac)) {
                                uploadBytes.addAndGet(payloadSize);
                                totalUploadBytes.addAndGet(payloadSize); // Increment total upload bytes
                                packetCount.incrementAndGet();
                            } else if (dstMac.equals(interfaceMac) || dstMac.equals(MacAddress.ETHER_BROADCAST_ADDRESS)) {
                                downloadBytes.addAndGet(payloadSize);
                                totalDownloadBytes.addAndGet(payloadSize); // Increment total download bytes
                                packetCount.incrementAndGet();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                };

                while (true) {
                    handle.loop(1, packetListener);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private PcapNetworkInterface getNetworkInterface() {
        try {
            return new org.pcap4j.util.NifSelector().selectNetworkInterface();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Scheduled(fixedRate = SAMPLING_PERIOD)
    public void sendPacketLossData() {
        long sent = packetsSent.get();
        long received = packetsReceived.get();
        double packetLossPercentage = (sent > 0)
                ? ((double) (sent - received) / sent) * 100
                : 0.0;

        ServerMessage serverMessage = ServerMessage.builder()
                .content(String.format(
                        "Upload: %.2f Mbps, Download: %.2f Mbps, Packet Loss: %.2f%%",
                        (uploadBytes.get() * 8.0) / (SAMPLING_PERIOD * 1000.0),
                        (downloadBytes.get() * 8.0) / (SAMPLING_PERIOD * 1000.0),
                        packetLossPercentage
                ))
                .build();

        // Add the message to the queue
        messageQueue.offer(serverMessage);

        // Reset counters for next interval
        packetsSent.set(0);
        packetsReceived.set(0);
        uploadBytes.set(0);
        downloadBytes.set(0);
    }

    @Scheduled(fixedRate = 1000) // Simulate packets being sent
    public void sendPackets() {
        packetsSent.incrementAndGet(); // Increment packets sent
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
}

