module com.se330.tetris {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.desktop;

    exports com.se330.tetris.core;
    exports com.se330.tetris.controller;
    exports com.se330.tetris.service;

    opens com.se330.tetris.model to javafx.base;
    opens com.se330.tetris.core to javafx.fxml;
    opens com.se330.tetris.controller to javafx.fxml;
}