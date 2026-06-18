package com.airquality.ui;

import com.airquality.config.ThresholdConfig;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

public class Gauge extends StackPane {
    private static final double SIZE = 320;
    private final String title;
    private final String unit;
    private final double maxValue;
    private final double threshold;

    private final Label valueLabel;
    private final Label unitLabel;
    private final Line needle;
    private final Rotate needleRotate;
    private final Pane dial;
    private final Circle centerCircle;
    private final Label statusLabel;

    private double currentValue = 0;
    private boolean isWarning = false;

    public Gauge(String title, String unit, double maxValue, double threshold) {
        this.title = title;
        this.unit = unit;
        this.maxValue = maxValue;
        this.threshold = threshold;

        setPrefSize(SIZE, SIZE);
        setMinSize(SIZE, SIZE);
        setMaxSize(SIZE, SIZE);

        dial = new Pane();
        dial.setPrefSize(SIZE, SIZE);

        drawDial();

        valueLabel = new Label("0.00");
        valueLabel.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        valueLabel.setTextFill(Color.DARKSLATEGRAY);

        unitLabel = new Label(unit);
        unitLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 16));
        unitLabel.setTextFill(Color.GRAY);

        VBox valueBox = new VBox(2, valueLabel, unitLabel);
        valueBox.setAlignment(Pos.CENTER);
        valueBox.setTranslateY(50);

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        titleLabel.setTextFill(Color.DARKSLATEGRAY);
        titleLabel.setTranslateY(-110);

        statusLabel = new Label("正常");
        statusLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        statusLabel.setTextFill(Color.FORESTGREEN);
        statusLabel.setTranslateY(105);

        needle = new Line();
        needle.setStartX(SIZE / 2);
        needle.setStartY(SIZE / 2);
        needle.setEndX(SIZE / 2);
        needle.setEndY(SIZE / 2 - 115);
        needle.setStrokeWidth(4);
        needle.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        needle.setStroke(Color.rgb(50, 50, 50));

        needleRotate = new Rotate(0, SIZE / 2, SIZE / 2);
        needle.getTransforms().add(needleRotate);

        centerCircle = new Circle(SIZE / 2, SIZE / 2, 12);
        centerCircle.setFill(Color.rgb(60, 60, 60));

        Circle innerCircle = new Circle(SIZE / 2, SIZE / 2, 6);
        innerCircle.setFill(Color.LIGHTGRAY);

        dial.getChildren().addAll(needle, centerCircle, innerCircle);

        getChildren().addAll(dial, titleLabel, valueBox, statusLabel);
        setPadding(new Insets(10));

        setStyleNormal();
    }

    private void drawDial() {
        double centerX = SIZE / 2;
        double centerY = SIZE / 2;
        double radius = 135;

        Circle background = new Circle(centerX, centerY, radius + 25);
        RadialGradient bgGradient = new RadialGradient(
                0, 0, centerX, centerY, radius + 30, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.WHITE),
                new Stop(0.85, Color.LIGHTGRAY),
                new Stop(1, Color.DARKGRAY)
        );
        background.setFill(bgGradient);
        dial.getChildren().add(background);

        Circle outerRing = new Circle(centerX, centerY, radius + 15);
        outerRing.setFill(null);
        outerRing.setStroke(Color.DARKGRAY);
        outerRing.setStrokeWidth(3);
        dial.getChildren().add(outerRing);

        int totalTicks = 50;
        double startAngle = 225;
        double endAngle = -45;
        double angleRange = 270;

        int thresholdTick = (int) (totalTicks * (threshold / maxValue));

        for (int i = 0; i <= totalTicks; i++) {
            double angle = startAngle - (angleRange * i / totalTicks);
            double rad = Math.toRadians(angle);

            boolean isMajor = (i % 10 == 0);
            double innerR = isMajor ? radius - 20 : radius - 10;
            double outerR = radius;

            double x1 = centerX + innerR * Math.cos(rad);
            double y1 = centerY + innerR * Math.sin(rad);
            double x2 = centerX + outerR * Math.cos(rad);
            double y2 = centerY + outerR * Math.sin(rad);

            Line tick = new Line(x1, y1, x2, y2);
            tick.setStrokeWidth(isMajor ? 2.5 : 1.5);

            if (i >= thresholdTick) {
                tick.setStroke(Color.RED);
            } else if (i >= thresholdTick * 0.7) {
                tick.setStroke(Color.ORANGE);
            } else {
                tick.setStroke(Color.DARKSLATEGRAY);
            }
            dial.getChildren().add(tick);

            if (isMajor) {
                double labelR = radius - 35;
                double lx = centerX + labelR * Math.cos(rad);
                double ly = centerY + labelR * Math.sin(rad);
                double val = maxValue * i / totalTicks;
                Label numLabel;
                if (maxValue < 1) {
                    numLabel = new Label(String.format("%.2f", val));
                } else {
                    numLabel = new Label(String.format("%.0f", val));
                }
                numLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 11));
                numLabel.setTextFill(Color.DARKSLATEGRAY);
                numLabel.setLayoutX(lx - 15);
                numLabel.setLayoutY(ly - 8);
                numLabel.setPrefWidth(30);
                numLabel.setAlignment(Pos.CENTER);
                dial.getChildren().add(numLabel);
            }
        }

        double warningStartAngle = startAngle - (angleRange * thresholdTick / totalTicks);
        Arc warningArc = new Arc(centerX, centerY, radius - 5, radius - 5,
                -45, -(warningStartAngle - (-45)));
        warningArc.setType(ArcType.OPEN);
        warningArc.setFill(null);
        warningArc.setStroke(Color.RED);
        warningArc.setStrokeWidth(8);
        warningArc.setOpacity(0.3);
        dial.getChildren().add(warningArc);
    }

    public void setValue(double value) {
        this.currentValue = value;
        double clampedValue = Math.max(0, Math.min(value, maxValue));
        double angle = -135 + (270 * clampedValue / maxValue);

        RotateTransition rt = new RotateTransition(Duration.millis(500), needle);
        rt.setFromAngle(needleRotate.getAngle());
        rt.setToAngle(angle);
        rt.setInterpolator(Interpolator.EASE_OUT);
        rt.play();

        if (maxValue < 1) {
            valueLabel.setText(String.format("%.3f", value));
        } else {
            valueLabel.setText(String.format("%.1f", value));
        }

        if (value >= threshold) {
            if (!isWarning) {
                isWarning = true;
                setStyleWarning();
            }
        } else {
            if (isWarning) {
                isWarning = false;
                setStyleNormal();
            }
        }
    }

    private void setStyleNormal() {
        statusLabel.setText("正常");
        statusLabel.setTextFill(Color.FORESTGREEN);
        valueLabel.setTextFill(Color.DARKSLATEGRAY);
        centerCircle.setFill(Color.rgb(60, 60, 60));
        setBackground(new Background(new BackgroundFill(
                Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY)));
    }

    private void setStyleWarning() {
        statusLabel.setText("超标警告");
        statusLabel.setTextFill(Color.RED);
        valueLabel.setTextFill(Color.RED);
        centerCircle.setFill(Color.RED);
    }

    public double getCurrentValue() {
        return currentValue;
    }

    public boolean isWarning() {
        return isWarning;
    }
}
