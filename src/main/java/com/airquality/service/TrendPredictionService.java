package com.airquality.service;

import com.airquality.dao.SensorDataDao;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TrendPredictionService {
    private final SensorDataDao sensorDataDao = new SensorDataDao();
    private static final int HISTORY_HOURS = 24;
    private static final int PREDICT_HOURS = 3;
    private static final int PREDICT_INTERVAL_MINUTES = 30;

    public static class TrendResult {
        public final List<SensorDataDao.TimePoint> historyData;
        public final List<PredictedPoint> predictedData;
        public final LinearModel formaldehydeModel;
        public final LinearModel pm25Model;
        public final String formaldehydeTrend;
        public final String pm25Trend;

        public TrendResult(List<SensorDataDao.TimePoint> historyData,
                           List<PredictedPoint> predictedData,
                           LinearModel formaldehydeModel,
                           LinearModel pm25Model) {
            this.historyData = historyData;
            this.predictedData = predictedData;
            this.formaldehydeModel = formaldehydeModel;
            this.pm25Model = pm25Model;
            this.formaldehydeTrend = describeTrend(formaldehydeModel.slope);
            this.pm25Trend = describeTrend(pm25Model.slope);
        }

        private String describeTrend(double slope) {
            if (Math.abs(slope) < 1e-6) {
                return "平稳";
            } else if (slope > 0) {
                return "上升 ⚠";
            } else {
                return "下降 ✓";
            }
        }
    }

    public static class PredictedPoint {
        public final LocalDateTime time;
        public final double formaldehyde;
        public final double pm25;
        public final boolean isPrediction;

        public PredictedPoint(LocalDateTime time, double formaldehyde, double pm25, boolean isPrediction) {
            this.time = time;
            this.formaldehyde = formaldehyde;
            this.pm25 = pm25;
            this.isPrediction = isPrediction;
        }
    }

    public static class LinearModel {
        public final double slope;
        public final double intercept;
        public final double correlation;

        public LinearModel(double slope, double intercept, double correlation) {
            this.slope = slope;
            this.intercept = intercept;
            this.correlation = correlation;
        }

        public double predict(double x) {
            return slope * x + intercept;
        }
    }

    public TrendResult calculateTrend() {
        try {
            List<SensorDataDao.TimePoint> history = sensorDataDao.findHourlyAverages(HISTORY_HOURS);

            if (history.size() < 3) {
                return new TrendResult(history, new ArrayList<>(),
                        new LinearModel(0, 0, 0),
                        new LinearModel(0, 0, 0));
            }

            long nowMinutes = toMinutes(LocalDateTime.now());
            double[] x = new double[history.size()];
            double[] yF = new double[history.size()];
            double[] yP = new double[history.size()];

            for (int i = 0; i < history.size(); i++) {
                SensorDataDao.TimePoint point = history.get(i);
                x[i] = toMinutes(point.time) - nowMinutes;
                yF[i] = point.formaldehyde;
                yP[i] = point.pm25;
            }

            LinearModel formaldehydeModel = linearRegression(x, yF);
            LinearModel pm25Model = linearRegression(x, yP);

            List<PredictedPoint> predicted = generatePredictions(formaldehydeModel, pm25Model, nowMinutes);

            return new TrendResult(history, predicted, formaldehydeModel, pm25Model);
        } catch (Exception e) {
            System.err.println("计算趋势预测失败: " + e.getMessage());
            return new TrendResult(new ArrayList<>(), new ArrayList<>(),
                    new LinearModel(0, 0, 0),
                    new LinearModel(0, 0, 0));
        }
    }

    private List<PredictedPoint> generatePredictions(LinearModel fModel, LinearModel pModel, long nowMinutes) {
        List<PredictedPoint> points = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        int intervals = (PREDICT_HOURS * 60) / PREDICT_INTERVAL_MINUTES;

        for (int i = 1; i <= intervals; i++) {
            LocalDateTime predTime = now.plusMinutes((long) i * PREDICT_INTERVAL_MINUTES);
            double x = (toMinutes(predTime) - nowMinutes);
            double predF = Math.max(0, fModel.predict(x));
            double predP = Math.max(0, pModel.predict(x));
            points.add(new PredictedPoint(predTime, predF, predP, true));
        }
        return points;
    }

    private LinearModel linearRegression(double[] x, double[] y) {
        int n = x.length;
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0, sumY2 = 0;

        for (int i = 0; i < n; i++) {
            sumX += x[i];
            sumY += y[i];
            sumXY += x[i] * y[i];
            sumX2 += x[i] * x[i];
            sumY2 += y[i] * y[i];
        }

        double denom = (n * sumX2 - sumX * sumX);
        double slope = (n * sumXY - sumX * sumY) / denom;
        double intercept = (sumY - slope * sumX) / n;

        double correlationNum = n * sumXY - sumX * sumY;
        double correlationDenom = Math.sqrt(denom * (n * sumY2 - sumY * sumY));
        double correlation = correlationDenom == 0 ? 0 : correlationNum / correlationDenom;

        return new LinearModel(slope, intercept, correlation);
    }

    private static long toMinutes(LocalDateTime time) {
        return time.toEpochSecond(java.time.ZoneOffset.UTC) / 60;
    }
}
