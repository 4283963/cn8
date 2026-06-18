package com.airquality.ui;

import com.airquality.config.ThresholdConfig;
import com.airquality.dao.SensorDataDao;
import com.airquality.service.TrendPredictionService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TrendChartPanel extends VBox {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter HOUR_FORMATTER = DateTimeFormatter.ofPattern("H小时后");

    private final LineChart<Number, Number> formaldehydeChart;
    private final LineChart<Number, Number> pm25Chart;
    private final NumberAxis formaldehydeXAxis;
    private final NumberAxis formaldehydeYAxis;
    private final NumberAxis pm25XAxis;
    private final NumberAxis pm25YAxis;

    private final Label formaldehydeTrendLabel;
    private final Label pm25TrendLabel;
    private final Label suggestionLabel;
    private final Label lastUpdateLabel;

    private final TrendPredictionService predictionService = new TrendPredictionService();

    public TrendChartPanel() {
        setSpacing(10);
        setPadding(new Insets(10, 20, 10, 20));
        setStyle("-fx-background-color: rgba(255,255,255,0.7); -fx-border-color: #cbd5e0; -fx-border-width: 1 0 0 0;");

        VBox headerBox = new VBox(5);
        Label titleLabel = new Label("趋势预测（基于过去24小时，预测未来3小时）");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        titleLabel.setTextFill(Color.rgb(30, 58, 95));

        HBox statusBar = new HBox(30);
        statusBar.setAlignment(Pos.CENTER_LEFT);

        formaldehydeTrendLabel = new Label("甲醛趋势: --");
        formaldehydeTrendLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        formaldehydeTrendLabel.setTextFill(Color.DARKSLATEGRAY);

        pm25TrendLabel = new Label("PM2.5趋势: --");
        pm25TrendLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        pm25TrendLabel.setTextFill(Color.DARKSLATEGRAY);

        suggestionLabel = new Label("");
        suggestionLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        suggestionLabel.setTextFill(Color.FORESTGREEN);

        lastUpdateLabel = new Label("");
        lastUpdateLabel.setFont(Font.font("Arial", 12));
        lastUpdateLabel.setTextFill(Color.GRAY);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        statusBar.getChildren().addAll(formaldehydeTrendLabel, pm25TrendLabel, suggestionLabel, spacer, lastUpdateLabel);
        headerBox.getChildren().addAll(titleLabel, statusBar);

        formaldehydeXAxis = new NumberAxis();
        formaldehydeXAxis.setLabel("时间轴");
        formaldehydeXAxis.setTickLabelsVisible(true);
        formaldehydeXAxis.setAutoRanging(false);
        formaldehydeXAxis.setLowerBound(-24);
        formaldehydeXAxis.setUpperBound(3);
        formaldehydeXAxis.setTickUnit(3);

        formaldehydeYAxis = new NumberAxis();
        formaldehydeYAxis.setLabel("甲醛 (mg/m³)");
        formaldehydeYAxis.setAutoRanging(false);
        formaldehydeYAxis.setLowerBound(0);
        formaldehydeYAxis.setUpperBound(ThresholdConfig.FORMALDEHYDE_MAX_SCALE);
        formaldehydeYAxis.setTickUnit(0.05);

        formaldehydeChart = new LineChart<>(formaldehydeXAxis, formaldehydeYAxis);
        formaldehydeChart.setTitle("甲醛趋势");
        formaldehydeChart.setLegendVisible(true);
        formaldehydeChart.setAnimated(false);
        formaldehydeChart.setCreateSymbols(false);
        formaldehydeChart.setPrefHeight(220);
        formaldehydeChart.setHorizontalGridLinesVisible(true);
        formaldehydeChart.setVerticalGridLinesVisible(true);

        pm25XAxis = new NumberAxis();
        pm25XAxis.setLabel("时间轴");
        pm25XAxis.setTickLabelsVisible(true);
        pm25XAxis.setAutoRanging(false);
        pm25XAxis.setLowerBound(-24);
        pm25XAxis.setUpperBound(3);
        pm25XAxis.setTickUnit(3);

        pm25YAxis = new NumberAxis();
        pm25YAxis.setLabel("PM2.5 (μg/m³)");
        pm25YAxis.setAutoRanging(false);
        pm25YAxis.setLowerBound(0);
        pm25YAxis.setUpperBound(ThresholdConfig.PM25_MAX_SCALE);
        pm25YAxis.setTickUnit(25);

        pm25Chart = new LineChart<>(pm25XAxis, pm25YAxis);
        pm25Chart.setTitle("PM2.5 趋势");
        pm25Chart.setLegendVisible(true);
        pm25Chart.setAnimated(false);
        pm25Chart.setCreateSymbols(false);
        pm25Chart.setPrefHeight(220);
        pm25Chart.setHorizontalGridLinesVisible(true);
        pm25Chart.setVerticalGridLinesVisible(true);

        HBox chartsBox = new HBox(15, formaldehydeChart, pm25Chart);
        HBox.setHgrow(formaldehydeChart, Priority.ALWAYS);
        HBox.setHgrow(pm25Chart, Priority.ALWAYS);

        Label legendLabel = new Label("图例: ─ 历史数据  ┄┄ 预测趋势  ━ 超标阈值线");
        legendLabel.setFont(Font.font("Arial", 12));
        legendLabel.setTextFill(Color.GRAY);
        legendLabel.setPadding(new Insets(5, 0, 0, 0));

        getChildren().addAll(headerBox, chartsBox, legendLabel);
    }

    public void refreshData() {
        try {
            TrendPredictionService.TrendResult result = predictionService.calculateTrend();
            updateCharts(result);
            updateStatus(result);
            lastUpdateLabel.setText("更新时间: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        } catch (Exception e) {
            System.err.println("刷新趋势图失败: " + e.getMessage());
        }
    }

    private void updateCharts(TrendPredictionService.TrendResult result) {
        LocalDateTime now = LocalDateTime.now();
        long nowHours = now.toEpochSecond(java.time.ZoneOffset.UTC) / 3600;

        XYChart.Series<Number, Number> fHistory = new XYChart.Series<>();
        fHistory.setName("历史甲醛");

        XYChart.Series<Number, Number> fPredict = new XYChart.Series<>();
        fPredict.setName("预测甲醛");

        XYChart.Series<Number, Number> pHistory = new XYChart.Series<>();
        pHistory.setName("历史PM2.5");

        XYChart.Series<Number, Number> pPredict = new XYChart.Series<>();
        pPredict.setName("预测PM2.5");

        for (SensorDataDao.TimePoint point : result.historyData) {
            double hoursDiff = (point.time.toEpochSecond(java.time.ZoneOffset.UTC) / 3600.0) - nowHours;
            fHistory.getData().add(new XYChart.Data<>(hoursDiff, point.formaldehyde));
            pHistory.getData().add(new XYChart.Data<>(hoursDiff, point.pm25));
        }

        for (TrendPredictionService.PredictedPoint point : result.predictedData) {
            double hoursDiff = (point.time.toEpochSecond(java.time.ZoneOffset.UTC) / 3600.0) - nowHours;
            fPredict.getData().add(new XYChart.Data<>(hoursDiff, point.formaldehyde));
            pPredict.getData().add(new XYChart.Data<>(hoursDiff, point.pm25));
        }

        formaldehydeChart.getData().clear();
        formaldehydeChart.getData().addAll(fHistory, fPredict);
        addThresholdLine(formaldehydeChart, ThresholdConfig.FORMALDEHYDE_THRESHOLD, "阈值 " + ThresholdConfig.FORMALDEHYDE_THRESHOLD);

        pm25Chart.getData().clear();
        pm25Chart.getData().addAll(pHistory, pPredict);
        addThresholdLine(pm25Chart, ThresholdConfig.PM25_THRESHOLD, "阈值 " + (int) ThresholdConfig.PM25_THRESHOLD);

        styleSeries(fHistory, "#2563eb", 2, false);
        styleSeries(fPredict, "#f59e0b", 2.5, true);
        styleSeries(pHistory, "#059669", 2, false);
        styleSeries(pPredict, "#dc2626", 2.5, true);

        updateXAxisTicks(formaldehydeXAxis, now);
        updateXAxisTicks(pm25XAxis, now);
    }

    private void addThresholdLine(LineChart<Number, Number> chart, double threshold, String label) {
        XYChart.Series<Number, Number> thresholdSeries = new XYChart.Series<>();
        thresholdSeries.setName(label);
        thresholdSeries.getData().add(new XYChart.Data<>(-24, threshold));
        thresholdSeries.getData().add(new XYChart.Data<>(3, threshold));
        chart.getData().add(thresholdSeries);
        styleSeries(thresholdSeries, "#dc2626", 1.5, false);
    }

    private void styleSeries(XYChart.Series<Number, Number> series, String color, double width, boolean dashed) {
        String dashStyle = dashed ? "-fx-stroke-dash-array: 6 4;" : "";
        series.getNode().setStyle(
            "-fx-stroke: " + color + ";" +
            "-fx-stroke-width: " + width + ";" +
            dashStyle
        );
    }

    private void updateXAxisTicks(NumberAxis axis, LocalDateTime now) {
        axis.setTickLabelFormatter(new javafx.util.StringConverter<Number>() {
            @Override
            public String toString(Number object) {
                double hours = object.doubleValue();
                if (hours == 0) {
                    return "现在";
                } else if (hours < 0) {
                    int h = (int) Math.round(-hours);
                    if (h == 0) return "现在";
                    return h + "小时前";
                } else {
                    int h = (int) Math.round(hours);
                    if (h == 0) return "现在";
                    return h + "小时后";
                }
            }

            @Override
            public Number fromString(String string) {
                return 0;
            }
        });
    }

    private void updateStatus(TrendPredictionService.TrendResult result) {
        String fTrend = result.formaldehydeTrend;
        String pTrend = result.pm25Trend;

        formaldehydeTrendLabel.setText("甲醛趋势: " + fTrend);
        pm25TrendLabel.setText("PM2.5趋势: " + pTrend);

        if (fTrend.contains("上升") || pTrend.contains("上升")) {
            boolean fWillExceed = willExceed(result.predictedData, true);
            boolean pWillExceed = willExceed(result.predictedData, false);

            if (fWillExceed || pWillExceed) {
                StringBuilder sb = new StringBuilder("建议: ");
                if (fWillExceed) sb.append("甲醛预计将超标，");
                if (pWillExceed) sb.append("PM2.5预计将超标，");
                sb.append("请及时开窗通风！⚠");
                suggestionLabel.setText(sb.toString());
                suggestionLabel.setTextFill(Color.RED);
            } else {
                suggestionLabel.setText("提示: 数值呈上升趋势，请注意观察");
                suggestionLabel.setTextFill(Color.rgb(217, 119, 6));
            }
        } else if (fTrend.contains("下降") && pTrend.contains("下降")) {
            suggestionLabel.setText("✓ 空气质量正在改善，继续保持");
            suggestionLabel.setTextFill(Color.FORESTGREEN);
        } else {
            suggestionLabel.setText("✓ 空气质量稳定，状态良好");
            suggestionLabel.setTextFill(Color.FORESTGREEN);
        }
    }

    private boolean willExceed(List<TrendPredictionService.PredictedPoint> predictions, boolean isFormaldehyde) {
        for (TrendPredictionService.PredictedPoint p : predictions) {
            if (isFormaldehyde) {
                if (p.formaldehyde >= ThresholdConfig.FORMALDEHYDE_THRESHOLD) {
                    return true;
                }
            } else {
                if (p.pm25 >= ThresholdConfig.PM25_THRESHOLD) {
                    return true;
                }
            }
        }
        return false;
    }
}
