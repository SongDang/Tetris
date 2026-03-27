module com.se330.tetris {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    exports com.se330.tetris;
    opens com.se330.tetris to javafx.fxml;
    
    exports com.se330.tetris.controller;
    opens com.se330.tetris.controller to javafx.fxml;
    
    exports com.se330.tetris.service;
    opens com.se330.tetris.service to javafx.fxml;
    
    exports com.se330.tetris.util;
    opens com.se330.tetris.util to javafx.fxml;
}