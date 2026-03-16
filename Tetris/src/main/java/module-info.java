module com.se330.tetris {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.almasb.fxgl.all;

    exports com.se330.tetris.game;
    opens com.se330.tetris.game to javafx.fxml;
    exports com.se330.tetris.core;
    opens com.se330.tetris.core to javafx.fxml;
    exports com.se330.tetris.controller;
    opens com.se330.tetris.controller to javafx.fxml;
}