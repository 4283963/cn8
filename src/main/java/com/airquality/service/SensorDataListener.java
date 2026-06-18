package com.airquality.service;

import com.airquality.model.SensorData;

public interface SensorDataListener {
    void onSensorDataReceived(SensorData data);
}
