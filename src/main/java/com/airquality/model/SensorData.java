package com.airquality.model;

import java.time.LocalDateTime;

public class SensorData {
    private Long id;
    private String sensorId;
    private String location;
    private double formaldehyde;
    private double pm25;
    private LocalDateTime timestamp;

    public SensorData() {
    }

    public SensorData(String sensorId, String location, double formaldehyde, double pm25, LocalDateTime timestamp) {
        this.sensorId = sensorId;
        this.location = location;
        this.formaldehyde = formaldehyde;
        this.pm25 = pm25;
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSensorId() {
        return sensorId;
    }

    public void setSensorId(String sensorId) {
        this.sensorId = sensorId;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public double getFormaldehyde() {
        return formaldehyde;
    }

    public void setFormaldehyde(double formaldehyde) {
        this.formaldehyde = formaldehyde;
    }

    public double getPm25() {
        return pm25;
    }

    public void setPm25(double pm25) {
        this.pm25 = pm25;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "SensorData{" +
                "id=" + id +
                ", sensorId='" + sensorId + '\'' +
                ", location='" + location + '\'' +
                ", formaldehyde=" + formaldehyde +
                ", pm25=" + pm25 +
                ", timestamp=" + timestamp +
                '}';
    }
}
