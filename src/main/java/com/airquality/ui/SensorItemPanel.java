package com.airquality.ui;

import com.airquality.config.ThresholdConfig;
import com.airquality.model.SensorData;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.format.DateTimeFormatter;

public class SensorItemPanel extends HBox {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Label idLabel;
    private final Label locationLabel;
    private final Label formaldehydeLabel;
    private final Label pm25Label;
    private final Label timeLabel;

    public SensorItemPanel() {
        setSpacing(20);
        setStyle("-fx-background-color: #f8f9fa; -fx-padding: 10 15; -fx-background-radius: 8; -fx-border-color: #dee2e6; -fx-border-radius: 8;");

        idLabel = createLabel("", 100);
        idLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));

        locationLabel = createLabel("", 100);
        formaldehydeLabel = createLabel("", 100);
        formaldehydeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));

        pm25Label = createLabel("", 100);
        pm25Label.setFont(Font.font("Arial", FontWeight.BOLD, 13));

        timeLabel = createLabel("", 100);

        getChildren().addAll(idLabel, locationLabel, formaldehydeLabel, pm25Label, timeLabel);
    }

    private Label createLabel(String text, double width) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", 12));
        label.setTextFill(Color.DARKSLATEGRAY);
        label.setPrefWidth(width);
        return label;
    }

    public void updateData(SensorData data) {
        idLabel.setText(data.getSensorId());
        locationLabel.setText(data.getLocation());
        formaldehydeLabel.setText(String.format("甲醛: %.3f mg/m³", data.getFormaldehyde()));
        pm25Label.setText(String.format("PM2.5: %.1f μg/m³", data.getPm25()));
        timeLabel.setText(data.getTimestamp().format(FORMATTER));

        if (data.getFormaldehyde() >= ThresholdConfig.FORMALDEHYDE_THRESHOLD) {
            formaldehydeLabel.setTextFill(Color.RED);
        } else if (data.getFormaldehyde() >= ThresholdConfig.FORMALDEHYDE_THRESHOLD * 0.7) {
            formaldehydeLabel.setTextFill(Color.ORANGE);
        } else {
            formaldehydeLabel.setTextFill(Color.FORESTGREEN);
        }

        if (data.getPm25() >= ThresholdConfig.PM25_THRESHOLD) {
            pm25Label.setTextFill(Color.RED);
        } else if (data.getPm25() >= ThresholdConfig.PM25_THRESHOLD * 0.7) {
            pm25Label.setTextFill(Color.ORANGE);
        } else {
            pm25Label.setTextFill(Color.FORESTGREEN);
        }
    }
}
