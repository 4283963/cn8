module com.airquality {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.sql;
    requires com.fazecast.jSerialComm;

    opens com.airquality to javafx.fxml, javafx.graphics;
    exports com.airquality;
    exports com.airquality.model;
    exports com.airquality.ui;
    exports com.airquality.service;
    exports com.airquality.dao;
    exports com.airquality.config;
    exports com.airquality.database;
}
