package com.airquality.dao;

import com.airquality.database.DatabaseManager;
import com.airquality.model.SensorData;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SensorDataDao {

    public void insert(SensorData data) throws SQLException {
        String sql = "INSERT INTO sensor_data (sensor_id, location, formaldehyde, pm25, timestamp) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, data.getSensorId());
            pstmt.setString(2, data.getLocation());
            pstmt.setDouble(3, data.getFormaldehyde());
            pstmt.setDouble(4, data.getPm25());
            pstmt.setTimestamp(5, Timestamp.valueOf(data.getTimestamp()));
            pstmt.executeUpdate();
        }
    }

    public List<SensorData> findAll() throws SQLException {
        List<SensorData> list = new ArrayList<>();
        String sql = "SELECT * FROM sensor_data ORDER BY timestamp DESC";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<SensorData> findRecent(int minutes) throws SQLException {
        List<SensorData> list = new ArrayList<>();
        String sql = "SELECT * FROM sensor_data WHERE timestamp > ? ORDER BY timestamp DESC";
        LocalDateTime since = LocalDateTime.now().minusMinutes(minutes);
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setTimestamp(1, Timestamp.valueOf(since));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public double getAverageFormaldehyde(int minutes) throws SQLException {
        String sql = "SELECT AVG(formaldehyde) FROM sensor_data WHERE timestamp > ?";
        LocalDateTime since = LocalDateTime.now().minusMinutes(minutes);
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setTimestamp(1, Timestamp.valueOf(since));
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        }
        return 0.0;
    }

    public double getAveragePm25(int minutes) throws SQLException {
        String sql = "SELECT AVG(pm25) FROM sensor_data WHERE timestamp > ?";
        LocalDateTime since = LocalDateTime.now().minusMinutes(minutes);
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setTimestamp(1, Timestamp.valueOf(since));
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        }
        return 0.0;
    }

    public List<TimePoint> findHourlyAverages(int hours) throws SQLException {
        List<TimePoint> list = new ArrayList<>();
        String sql = "SELECT " +
                "FLOOR(HOUR(timestamp) * 4 + MINUTE(timestamp) / 15) AS time_slot, " +
                "MIN(timestamp) AS slot_start, " +
                "AVG(formaldehyde) AS avg_formaldehyde, " +
                "AVG(pm25) AS avg_pm25 " +
                "FROM sensor_data " +
                "WHERE timestamp > ? " +
                "GROUP BY time_slot " +
                "ORDER BY slot_start ASC";
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setTimestamp(1, Timestamp.valueOf(since));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("slot_start");
                    double formaldehyde = rs.getDouble("avg_formaldehyde");
                    double pm25 = rs.getDouble("avg_pm25");
                    if (ts != null) {
                        list.add(new TimePoint(ts.toLocalDateTime(), formaldehyde, pm25));
                    }
                }
            }
        }
        return list;
    }

    public static class TimePoint {
        public final LocalDateTime time;
        public final double formaldehyde;
        public final double pm25;

        public TimePoint(LocalDateTime time, double formaldehyde, double pm25) {
            this.time = time;
            this.formaldehyde = formaldehyde;
            this.pm25 = pm25;
        }

        @Override
        public String toString() {
            return "TimePoint{" +
                    "time=" + time +
                    ", formaldehyde=" + formaldehyde +
                    ", pm25=" + pm25 +
                    '}';
        }
    }

    private SensorData mapRow(ResultSet rs) throws SQLException {
        SensorData data = new SensorData();
        data.setId(rs.getLong("id"));
        data.setSensorId(rs.getString("sensor_id"));
        data.setLocation(rs.getString("location"));
        data.setFormaldehyde(rs.getDouble("formaldehyde"));
        data.setPm25(rs.getDouble("pm25"));
        data.setTimestamp(rs.getTimestamp("timestamp").toLocalDateTime());
        return data;
    }
}
