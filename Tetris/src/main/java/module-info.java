module com.se330.tetris {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    exports com.se330.tetris.core;
    exports com.se330.tetris.controller;
    exports com.se330.tetris.service;

    opens com.se330.tetris.core to javafx.fxml;
    opens com.se330.tetris.controller to javafx.fxml;
}