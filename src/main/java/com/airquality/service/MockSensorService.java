package com.airquality.service;

import com.airquality.model.SensorData;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MockSensorService {
    private final List<SensorDataListener> listeners = new CopyOnWriteArrayList<>();
    private final Random random = new Random();
    private ScheduledExecutorService scheduler;
    private volatile boolean running = false;

    private final String[] sensorIds = {"SENSOR-001", "SENSOR-002", "SENSOR-003", "SENSOR-004", "SENSOR-005"};
    private final String[] locations = {"会议室A", "办公区B", "茶水间", "走廊", "经理办公室"};

    public void addListener(SensorDataListener listener) {
        listeners.add(listener);
    }

    public void removeListener(SensorDataListener listener) {
        listeners.remove(listener);
    }

    public void start() {
        if (running) return;
        running = true;
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::generateData, 0, 2, TimeUnit.SECONDS);
        System.out.println("模拟传感器服务已启动");
    }

    private void generateData() {
        for (int i = 0; i < sensorIds.length; i++) {
            double formaldehyde = 0.02 + random.nextDouble() * 0.15;
            double pm25 = 10 + random.nextDouble() * 60;

            if (random.nextInt(100) < 5) {
                formaldehyde = 0.12 + random.nextDouble() * 0.1;
                pm25 = 40 + random.nextDouble() * 50;
            }

            SensorData data = new SensorData(
                    sensorIds[i],
                    locations[i],
                    Math.round(formaldehyde * 1000.0) / 1000.0,
                    Math.round(pm25 * 10.0) / 10.0,
                    LocalDateTime.now()
            );
            notifyListeners(data);
        }
    }

    private void notifyListeners(SensorData data) {
        for (SensorDataListener listener : listeners) {
            listener.onSensorDataReceived(data);
        }
    }

    public void stop() {
        running = false;
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        System.out.println("模拟传感器服务已停止");
    }

    public boolean isRunning() {
        return running;
    }
}
