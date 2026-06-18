package com.airquality.service;

import com.airquality.model.SensorData;
import com.fazecast.jSerialComm.SerialPort;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SerialSensorService {
    private final List<SensorDataListener> listeners = new CopyOnWriteArrayList<>();
    private SerialPort serialPort;
    private volatile boolean running = false;
    private Thread readThread;
    private final StringBuilder buffer = new StringBuilder();

    public void addListener(SensorDataListener listener) {
        listeners.add(listener);
    }

    public void removeListener(SensorDataListener listener) {
        listeners.remove(listener);
    }

    public SerialPort[] getAvailablePorts() {
        return SerialPort.getCommPorts();
    }

    public void connect(String portName, int baudRate) {
        if (running) {
            disconnect();
        }
        serialPort = SerialPort.getCommPort(portName);
        serialPort.setBaudRate(baudRate);
        serialPort.setNumDataBits(8);
        serialPort.setNumStopBits(1);
        serialPort.setParity(SerialPort.NO_PARITY);

        if (serialPort.openPort()) {
            running = true;
            startReading();
            System.out.println("串口连接成功: " + portName);
        } else {
            System.err.println("串口连接失败: " + portName);
        }
    }

    private void startReading() {
        readThread = new Thread(() -> {
            while (running && serialPort != null && serialPort.isOpen()) {
                try {
                    if (serialPort.bytesAvailable() > 0) {
                        byte[] readBuffer = new byte[serialPort.bytesAvailable()];
                        int numRead = serialPort.readBytes(readBuffer, readBuffer.length);
                        if (numRead > 0) {
                            String data = new String(readBuffer, 0, numRead, StandardCharsets.UTF_8);
                            processSerialData(data);
                        }
                    }
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("串口读取错误: " + e.getMessage());
                }
            }
        }, "Serial-Reader");
        readThread.setDaemon(true);
        readThread.start();
    }

    private void processSerialData(String data) {
        buffer.append(data);
        String content = buffer.toString();
        String[] lines = content.split("\n");
        for (int i = 0; i < lines.length - 1; i++) {
            parseAndNotify(lines[i].trim());
        }
        if (content.endsWith("\n")) {
            buffer.setLength(0);
        } else {
            buffer.setLength(0);
            buffer.append(lines[lines.length - 1]);
        }
    }

    private void parseAndNotify(String line) {
        try {
            String[] parts = line.split(",");
            if (parts.length >= 4) {
                String sensorId = parts[0].trim();
                String location = parts[1].trim();
                double formaldehyde = Double.parseDouble(parts[2].trim());
                double pm25 = Double.parseDouble(parts[3].trim());
                SensorData sensorData = new SensorData(sensorId, location, formaldehyde, pm25, LocalDateTime.now());
                notifyListeners(sensorData);
            }
        } catch (NumberFormatException e) {
            System.err.println("解析串口数据失败: " + line);
        }
    }

    private void notifyListeners(SensorData data) {
        for (SensorDataListener listener : listeners) {
            try {
                listener.onSensorDataReceived(data);
            } catch (Exception e) {
                System.err.println("监听器处理串口数据异常: " + e.getMessage());
            }
        }
    }

    public void disconnect() {
        running = false;
        if (readThread != null) {
            readThread.interrupt();
        }
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
        }
        System.out.println("串口已断开");
    }

    public boolean isConnected() {
        return running && serialPort != null && serialPort.isOpen();
    }
}
