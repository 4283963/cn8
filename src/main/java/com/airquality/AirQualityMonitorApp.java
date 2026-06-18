package com.airquality;

import com.airquality.database.DatabaseManager;
import com.airquality.service.DataAggregationService;
import com.airquality.service.MockSensorService;
import com.airquality.service.NetworkSensorService;
import com.airquality.service.SerialSensorService;
import com.airquality.ui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class AirQualityMonitorApp extends Application {

    private DataAggregationService aggregationService;
    private SerialSensorService serialService;
    private NetworkSensorService networkService;
    private MockSensorService mockService;

    @Override
    public void init() {
        DatabaseManager.initialize();

        aggregationService = new DataAggregationService();
        serialService = new SerialSensorService();
        networkService = new NetworkSensorService();
        mockService = new MockSensorService();

        serialService.addListener(aggregationService);
        networkService.addListener(aggregationService);
        mockService.addListener(aggregationService);

        aggregationService.loadInitialData();
    }

    @Override
    public void start(Stage primaryStage) {
        MainView mainView = new MainView(aggregationService, serialService, networkService, mockService);

        Scene scene = new Scene(mainView, 1280, 820);
        scene.getStylesheets().add("data:text/css,.root{font-family:'Arial';}");

        primaryStage.setTitle("室内空气质量监测系统");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(720);

        primaryStage.setOnCloseRequest(e -> shutdown());

        primaryStage.show();
    }

    private void shutdown() {
        System.out.println("正在关闭应用...");

        if (serialService != null) {
            serialService.disconnect();
        }
        if (networkService != null) {
            networkService.stopAll();
        }
        if (mockService != null) {
            mockService.stop();
        }

        DatabaseManager.shutdown();
        System.out.println("应用已安全关闭");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
