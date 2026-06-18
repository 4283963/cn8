package com.airquality;

import com.airquality.database.DatabaseManager;
import com.airquality.service.DataAggregationService;
import com.airquality.service.MockSensorService;
import com.airquality.service.NetworkSensorService;
import com.airquality.service.SerialSensorService;
import com.airquality.ui.MainView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.PrintWriter;
import java.io.StringWriter;

public class AirQualityMonitorApp extends Application {

    private DataAggregationService aggregationService;
    private SerialSensorService serialService;
    private NetworkSensorService networkService;
    private MockSensorService mockService;

    public static void main(String[] args) {
        setupGlobalExceptionHandler();
        launch(args);
    }

    private static void setupGlobalExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            logError("未捕获异常 (线程: " + thread.getName() + ")", throwable);
        });

        System.setProperty("javafx.exception.handler", "");
    }

    private static void logError(String message, Throwable throwable) {
        System.err.println("=== " + message + " ===");
        System.err.println("错误类型: " + throwable.getClass().getName());
        System.err.println("错误信息: " + throwable.getMessage());
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        System.err.println("堆栈跟踪:\n" + sw.toString());
        System.err.println("===========================");
    }

    @Override
    public void init() {
        try {
            Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
                logError("初始化线程未捕获异常", throwable);
            });

            DatabaseManager.initialize();

            aggregationService = new DataAggregationService();
            serialService = new SerialSensorService();
            networkService = new NetworkSensorService();
            mockService = new MockSensorService();

            serialService.addListener(aggregationService);
            networkService.addListener(aggregationService);
            mockService.addListener(aggregationService);

            aggregationService.loadInitialData();
        } catch (Exception e) {
            logError("应用初始化失败", e);
        }
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
                logError("JavaFX UI线程未捕获异常", throwable);
                showErrorDialog(throwable);
            });

            MainView mainView = new MainView(aggregationService, serialService, networkService, mockService);

            Scene scene = new Scene(mainView, 1280, 1100);
            scene.getStylesheets().add("data:text/css,.root{font-family:'Arial';}");

            primaryStage.setTitle("室内空气质量监测系统");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1100);
            primaryStage.setMinHeight(900);

            primaryStage.setOnCloseRequest(e -> {
                try {
                    shutdown();
                } catch (Exception ex) {
                    logError("关闭应用时异常", ex);
                }
            });

            primaryStage.show();
        } catch (Exception e) {
            logError("启动界面失败", e);
            showErrorDialog(e);
        }
    }

    private void showErrorDialog(Throwable throwable) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> showErrorDialog(throwable));
            return;
        }
        try {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("系统异常");
            alert.setHeaderText("发生了一个非致命错误，系统将继续运行");
            alert.setContentText("错误信息: " + (throwable.getMessage() != null ? throwable.getMessage() : throwable.getClass().getSimpleName()));

            Label label = new Label("详细错误信息:");
            label.setTextFill(Color.DARKRED);

            StringWriter sw = new StringWriter();
            throwable.printStackTrace(new PrintWriter(sw));
            TextArea textArea = new TextArea(sw.toString());
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);

            VBox expContent = new VBox(10, label, textArea);
            expContent.setMaxWidth(Double.MAX_VALUE);
            alert.getDialogPane().setExpandableContent(expContent);

            alert.showAndWait();
        } catch (Exception ex) {
            logError("显示错误对话框失败", ex);
        }
    }

    private void shutdown() {
        System.out.println("正在关闭应用...");

        try {
            if (serialService != null) {
                serialService.disconnect();
            }
        } catch (Exception e) {
            System.err.println("关闭串口服务异常: " + e.getMessage());
        }

        try {
            if (networkService != null) {
                networkService.stopAll();
            }
        } catch (Exception e) {
            System.err.println("关闭网络服务异常: " + e.getMessage());
        }

        try {
            if (mockService != null) {
                mockService.stop();
            }
        } catch (Exception e) {
            System.err.println("关闭模拟服务异常: " + e.getMessage());
        }

        try {
            DatabaseManager.shutdown();
        } catch (Exception e) {
            System.err.println("关闭数据库异常: " + e.getMessage());
        }

        System.out.println("应用已安全关闭");
    }
}
