package com.airquality.service;

import com.airquality.model.SensorData;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NetworkSensorService {
    private final List<SensorDataListener> listeners = new CopyOnWriteArrayList<>();
    private volatile boolean tcpRunning = false;
    private volatile boolean udpRunning = false;
    private ServerSocket tcpServerSocket;
    private DatagramSocket udpSocket;
    private Thread tcpThread;
    private Thread udpThread;
    private final ExecutorService clientPool = Executors.newCachedThreadPool();

    private int tcpPort = 9876;
    private int udpPort = 9877;

    public void addListener(SensorDataListener listener) {
        listeners.add(listener);
    }

    public void removeListener(SensorDataListener listener) {
        listeners.remove(listener);
    }

    public void setTcpPort(int port) {
        this.tcpPort = port;
    }

    public void setUdpPort(int port) {
        this.udpPort = port;
    }

    public void startTcpServer() {
        if (tcpRunning) return;
        tcpThread = new Thread(() -> {
            try {
                tcpServerSocket = new ServerSocket(tcpPort);
                tcpRunning = true;
                System.out.println("TCP 服务器已启动，端口: " + tcpPort);
                while (tcpRunning && !tcpServerSocket.isClosed()) {
                    try {
                        Socket clientSocket = tcpServerSocket.accept();
                        clientPool.submit(() -> handleTcpClient(clientSocket));
                    } catch (Exception e) {
                        if (tcpRunning) {
                            System.err.println("TCP 连接错误: " + e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("TCP 服务器启动失败: " + e.getMessage());
            }
        }, "TCP-Server");
        tcpThread.setDaemon(true);
        tcpThread.start();
    }

    private void handleTcpClient(Socket socket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                parseAndNotify(line.trim());
            }
        } catch (Exception e) {
            System.err.println("TCP 客户端处理错误: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (Exception ignored) {
            }
        }
    }

    public void startUdpServer() {
        if (udpRunning) return;
        udpThread = new Thread(() -> {
            try {
                udpSocket = new DatagramSocket(udpPort);
                udpRunning = true;
                System.out.println("UDP 服务器已启动，端口: " + udpPort);
                byte[] receiveBuffer = new byte[1024];
                while (udpRunning && !udpSocket.isClosed()) {
                    try {
                        DatagramPacket packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                        udpSocket.receive(packet);
                        String data = new String(packet.getData(), 0, packet.getLength(), "UTF-8").trim();
                        parseAndNotify(data);
                    } catch (Exception e) {
                        if (udpRunning) {
                            System.err.println("UDP 接收错误: " + e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("UDP 服务器启动失败: " + e.getMessage());
            }
        }, "UDP-Server");
        udpThread.setDaemon(true);
        udpThread.start();
    }

    private void parseAndNotify(String data) {
        try {
            String[] parts = data.split(",");
            if (parts.length >= 4) {
                String sensorId = parts[0].trim();
                String location = parts[1].trim();
                double formaldehyde = Double.parseDouble(parts[2].trim());
                double pm25 = Double.parseDouble(parts[3].trim());
                SensorData sensorData = new SensorData(sensorId, location, formaldehyde, pm25, LocalDateTime.now());
                for (SensorDataListener listener : listeners) {
                    try {
                        listener.onSensorDataReceived(sensorData);
                    } catch (Exception e) {
                        System.err.println("监听器处理网络数据异常: " + e.getMessage());
                    }
                }
            }
        } catch (NumberFormatException e) {
            System.err.println("解析网络数据失败: " + data);
        } catch (Exception e) {
            System.err.println("处理网络数据异常: " + e.getMessage());
        }
    }

    public void stopTcpServer() {
        tcpRunning = false;
        try {
            if (tcpServerSocket != null && !tcpServerSocket.isClosed()) {
                tcpServerSocket.close();
            }
        } catch (Exception e) {
            System.err.println("关闭 TCP 服务器失败: " + e.getMessage());
        }
        System.out.println("TCP 服务器已停止");
    }

    public void stopUdpServer() {
        udpRunning = false;
        try {
            if (udpSocket != null && !udpSocket.isClosed()) {
                udpSocket.close();
            }
        } catch (Exception e) {
            System.err.println("关闭 UDP 服务器失败: " + e.getMessage());
        }
        System.out.println("UDP 服务器已停止");
    }

    public void stopAll() {
        stopTcpServer();
        stopUdpServer();
        clientPool.shutdownNow();
    }

    public boolean isTcpRunning() {
        return tcpRunning;
    }

    public boolean isUdpRunning() {
        return udpRunning;
    }
}
