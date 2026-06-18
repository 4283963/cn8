package com.airquality.service;

import com.airquality.dao.SensorDataDao;
import com.airquality.model.SensorData;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class DataAggregationService implements SensorDataListener {
    private final SensorDataDao sensorDataDao = new SensorDataDao();
    private final Map<String, SensorData> latestSensorData = new HashMap<>();

    private final DoubleProperty avgFormaldehyde = new SimpleDoubleProperty(0.0);
    private final DoubleProperty avgPm25 = new SimpleDoubleProperty(0.0);

    public DataAggregationService() {
    }

    @Override
    public void onSensorDataReceived(SensorData data) {
        try {
            sensorDataDao.insert(data);
        } catch (SQLException e) {
            System.err.println("保存传感器数据失败: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("保存传感器数据发生未知错误: " + e.getMessage());
        }

        try {
            latestSensorData.put(data.getSensorId(), data);
            recalculateAverages();
        } catch (Exception e) {
            System.err.println("处理传感器数据异常: " + e.getMessage());
        }
    }

    private void recalculateAverages() {
        if (latestSensorData.isEmpty()) {
            runSafeOnFxThread(() -> {
                avgFormaldehyde.set(0.0);
                avgPm25.set(0.0);
            });
            return;
        }

        double totalFormaldehyde = 0;
        double totalPm25 = 0;
        int count = 0;

        try {
            LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
            for (SensorData data : latestSensorData.values()) {
                if (data.getTimestamp().isAfter(threshold)) {
                    totalFormaldehyde += data.getFormaldehyde();
                    totalPm25 += data.getPm25();
                    count++;
                }
            }
        } catch (Exception e) {
            System.err.println("计算平均值异常: " + e.getMessage());
            return;
        }

        if (count > 0) {
            final double avgF = totalFormaldehyde / count;
            final double avgP = totalPm25 / count;
            runSafeOnFxThread(() -> {
                avgFormaldehyde.set(Math.round(avgF * 1000.0) / 1000.0);
                avgPm25.set(Math.round(avgP * 10.0) / 10.0);
            });
        }
    }

    private void runSafeOnFxThread(Runnable action) {
        try {
            Platform.runLater(() -> {
                try {
                    action.run();
                } catch (Exception e) {
                    System.err.println("UI 更新异常: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            System.err.println("调度 UI 更新失败: " + e.getMessage());
        }
    }

    public double getAvgFormaldehyde() {
        return avgFormaldehyde.get();
    }

    public DoubleProperty avgFormaldehydeProperty() {
        return avgFormaldehyde;
    }

    public double getAvgPm25() {
        return avgPm25.get();
    }

    public DoubleProperty avgPm25Property() {
        return avgPm25;
    }

    public int getActiveSensorCount() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        return (int) latestSensorData.values().stream()
                .filter(d -> d.getTimestamp().isAfter(threshold))
                .count();
    }

    public Map<String, SensorData> getLatestSensorData() {
        return new HashMap<>(latestSensorData);
    }

    public void loadInitialData() {
        try {
            var recentData = sensorDataDao.findRecent(5);
            for (SensorData data : recentData) {
                latestSensorData.put(data.getSensorId(), data);
            }
            recalculateAverages();
        } catch (SQLException e) {
            System.err.println("加载初始数据失败: " + e.getMessage());
        }
    }
}
