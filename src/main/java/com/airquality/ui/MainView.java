package com.airquality.ui;

import com.airquality.config.ThresholdConfig;
import com.airquality.model.SensorData;
import com.airquality.service.DataAggregationService;
import com.airquality.service.MockSensorService;
import com.airquality.service.NetworkSensorService;
import com.airquality.service.SerialSensorService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;

public class MainView extends BorderPane {
    private final DataAggregationService aggregationService;
    private final SerialSensorService serialService;
    private final NetworkSensorService networkService;
    private final MockSensorService mockService;

    private Gauge formaldehydeGauge;
    private Gauge pm25Gauge;

    private Label statusLabel;
    private Label sensorCountLabel;
    private VBox sensorListContainer;
    private final Map<String, SensorItemPanel> sensorPanels = new HashMap<>();

    private ComboBox<String> serialPortCombo;
    private Spinner<Integer> tcpPortSpinner;
    private Spinner<Integer> udpPortSpinner;
    private CheckBox mockModeCheckBox;

    public MainView(DataAggregationService aggregationService,
                    SerialSensorService serialService,
                    NetworkSensorService networkService,
                    MockSensorService mockService) {
        this.aggregationService = aggregationService;
        this.serialService = serialService;
        this.networkService = networkService;
        this.mockService = mockService;

        initUI();
        bindData();
        setupRefreshTimer();
    }

    private void initUI() {
        setStyle("-fx-background-color: linear-gradient(to bottom, #f0f4f8, #e2e8f0);");

        VBox topPanel = createTopPanel();
        setTop(topPanel);

        HBox centerPanel = createCenterPanel();
        setCenter(centerPanel);

        VBox rightPanel = createRightPanel();
        setRight(rightPanel);

        HBox bottomPanel = createBottomPanel();
        setBottom(bottomPanel);
    }

    private VBox createTopPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(20, 30, 15, 30));

        Label titleLabel = new Label("室内空气质量监测系统");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        titleLabel.setTextFill(Color.rgb(30, 58, 95));

        HBox infoBar = new HBox(30);
        infoBar.setAlignment(Pos.CENTER_LEFT);

        statusLabel = new Label("● 系统运行中");
        statusLabel.setFont(Font.font("Arial", 14));
        statusLabel.setTextFill(Color.FORESTGREEN);

        sensorCountLabel = new Label("在线传感器: 0");
        sensorCountLabel.setFont(Font.font("Arial", 14));
        sensorCountLabel.setTextFill(Color.DARKSLATEGRAY);

        Label thresholdInfo = new Label(
                String.format("阈值: 甲醛 ≥ %.2f mg/m³ | PM2.5 ≥ %.0f μg/m³ 为超标",
                        ThresholdConfig.FORMALDEHYDE_THRESHOLD,
                        ThresholdConfig.PM25_THRESHOLD));
        thresholdInfo.setFont(Font.font("Arial", 13));
        thresholdInfo.setTextFill(Color.GRAY);

        infoBar.getChildren().addAll(statusLabel, sensorCountLabel, thresholdInfo);
        panel.getChildren().addAll(titleLabel, infoBar);
        return panel;
    }

    private HBox createCenterPanel() {
        HBox panel = new HBox(40);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(20, 30, 20, 30));

        formaldehydeGauge = new Gauge("甲醛 (HCHO)", "mg/m³",
                ThresholdConfig.FORMALDEHYDE_MAX_SCALE,
                ThresholdConfig.FORMALDEHYDE_THRESHOLD);

        pm25Gauge = new Gauge("PM2.5", "μg/m³",
                ThresholdConfig.PM25_MAX_SCALE,
                ThresholdConfig.PM25_THRESHOLD);

        panel.getChildren().addAll(formaldehydeGauge, pm25Gauge);
        return panel;
    }

    private VBox createRightPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(20, 20, 20, 10));
        panel.setPrefWidth(380);

        Label listTitle = new Label("各传感器实时数据");
        listTitle.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        listTitle.setTextFill(Color.DARKSLATEGRAY);

        sensorListContainer = new VBox(8);
        sensorListContainer.setPadding(new Insets(5));

        ScrollPane scrollPane = new ScrollPane(sensorListContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setPrefHeight(500);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        panel.getChildren().addAll(listTitle, scrollPane);
        return panel;
    }

    private HBox createBottomPanel() {
        HBox panel = new HBox(20);
        panel.setPadding(new Insets(15, 30, 20, 30));
        panel.setStyle("-fx-background-color: rgba(255,255,255,0.5); -fx-border-color: #cbd5e0; -fx-border-width: 1 0 0 0;");
        panel.setAlignment(Pos.CENTER_LEFT);

        Label serialLabel = new Label("串口:");
        serialLabel.setFont(Font.font("Arial", 13));

        serialPortCombo = new ComboBox<>();
        serialPortCombo.setPrefWidth(150);
        refreshSerialPorts();

        Button refreshPortsBtn = new Button("刷新端口");
        refreshPortsBtn.setOnAction(e -> refreshSerialPorts());

        Button connectSerialBtn = new Button("连接串口");
        connectSerialBtn.setOnAction(e -> connectSerial());

        Button disconnectSerialBtn = new Button("断开串口");
        disconnectSerialBtn.setOnAction(e -> disconnectSerial());

        Separator sep1 = new Separator();
        sep1.setOrientation(javafx.geometry.Orientation.VERTICAL);

        Label tcpLabel = new Label("TCP端口:");
        tcpLabel.setFont(Font.font("Arial", 13));
        tcpPortSpinner = new Spinner<>(1024, 65535, 9876);
        tcpPortSpinner.setPrefWidth(90);

        Button startTcpBtn = new Button("启动TCP");
        startTcpBtn.setOnAction(e -> startTcpServer());

        Button stopTcpBtn = new Button("停止TCP");
        stopTcpBtn.setOnAction(e -> stopTcpServer());

        Separator sep2 = new Separator();
        sep2.setOrientation(javafx.geometry.Orientation.VERTICAL);

        Label udpLabel = new Label("UDP端口:");
        udpLabel.setFont(Font.font("Arial", 13));
        udpPortSpinner = new Spinner<>(1024, 65535, 9877);
        udpPortSpinner.setPrefWidth(90);

        Button startUdpBtn = new Button("启动UDP");
        startUdpBtn.setOnAction(e -> startUdpServer());

        Button stopUdpBtn = new Button("停止UDP");
        stopUdpBtn.setOnAction(e -> stopUdpServer());

        Separator sep3 = new Separator();
        sep3.setOrientation(javafx.geometry.Orientation.VERTICAL);

        mockModeCheckBox = new CheckBox("模拟数据模式");
        mockModeCheckBox.setFont(Font.font("Arial", 13));
        mockModeCheckBox.selectedProperty().addListener((obs, old, val) -> {
            if (val) {
                startMockService();
            } else {
                stopMockService();
            }
        });

        panel.getChildren().addAll(
                serialLabel, serialPortCombo, refreshPortsBtn, connectSerialBtn, disconnectSerialBtn,
                sep1, tcpLabel, tcpPortSpinner, startTcpBtn, stopTcpBtn,
                sep2, udpLabel, udpPortSpinner, startUdpBtn, stopUdpBtn,
                sep3, mockModeCheckBox
        );

        return panel;
    }

    private void bindData() {
        aggregationService.avgFormaldehydeProperty().addListener((obs, old, val) -> {
            Platform.runLater(() -> formaldehydeGauge.setValue(val.doubleValue()));
        });
        aggregationService.avgPm25Property().addListener((obs, old, val) -> {
            Platform.runLater(() -> pm25Gauge.setValue(val.doubleValue()));
        });
    }

    private void setupRefreshTimer() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> refreshSensorList()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void refreshSensorList() {
        var allData = aggregationService.getLatestSensorData();
        for (Map.Entry<String, SensorData> entry : allData.entrySet()) {
            SensorItemPanel itemPanel = sensorPanels.get(entry.getKey());
            if (itemPanel == null) {
                itemPanel = new SensorItemPanel();
                sensorPanels.put(entry.getKey(), itemPanel);
                sensorListContainer.getChildren().add(itemPanel);
            }
            itemPanel.updateData(entry.getValue());
        }
        sensorCountLabel.setText("在线传感器: " + aggregationService.getActiveSensorCount());
    }

    private void refreshSerialPorts() {
        serialPortCombo.getItems().clear();
        var ports = serialService.getAvailablePorts();
        for (var port : ports) {
            serialPortCombo.getItems().add(port.getSystemPortName());
        }
        if (!serialPortCombo.getItems().isEmpty()) {
            serialPortCombo.getSelectionModel().selectFirst();
        }
    }

    private void connectSerial() {
        String port = serialPortCombo.getValue();
        if (port == null) {
            showAlert("请选择串口");
            return;
        }
        serialService.connect(port, 9600);
    }

    private void disconnectSerial() {
        serialService.disconnect();
    }

    private void startTcpServer() {
        networkService.setTcpPort(tcpPortSpinner.getValue());
        networkService.startTcpServer();
        statusLabel.setText("● TCP服务器已启动 (端口 " + tcpPortSpinner.getValue() + ")");
    }

    private void stopTcpServer() {
        networkService.stopTcpServer();
        if (!networkService.isUdpRunning()) {
            statusLabel.setText("● 系统运行中");
            statusLabel.setTextFill(Color.FORESTGREEN);
        }
    }

    private void startUdpServer() {
        networkService.setUdpPort(udpPortSpinner.getValue());
        networkService.startUdpServer();
        statusLabel.setText("● UDP服务器已启动 (端口 " + udpPortSpinner.getValue() + ")");
    }

    private void stopUdpServer() {
        networkService.stopUdpServer();
        if (!networkService.isTcpRunning()) {
            statusLabel.setText("● 系统运行中");
            statusLabel.setTextFill(Color.FORESTGREEN);
        }
    }

    private void startMockService() {
        mockService.start();
        statusLabel.setText("● 模拟数据模式运行中");
        statusLabel.setTextFill(Color.rgb(237, 137, 54));
    }

    private void stopMockService() {
        mockService.stop();
        statusLabel.setText("● 系统运行中");
        statusLabel.setTextFill(Color.FORESTGREEN);
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("提示");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
