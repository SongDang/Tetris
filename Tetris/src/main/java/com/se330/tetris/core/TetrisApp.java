package com.se330.tetris.core;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import com.se330.tetris.controller.GameController;

public class TetrisApp extends Application {
    public static final int BOARD_WIDTH = 10;
    public static final int BOARD_HEIGHT = 20;
    public static final int CELL_SIZE = 28;

    @Override
    public void start(Stage stage) {
        Canvas boardCanvas = new Canvas(BOARD_WIDTH * CELL_SIZE, BOARD_HEIGHT * CELL_SIZE);
        Canvas nextCanvas = new Canvas(6 * CELL_SIZE, 6 * CELL_SIZE);
        Canvas scoreCanvas = new Canvas(6 * CELL_SIZE, 6 * CELL_SIZE);

        HBox root = new HBox(20);
        root.setStyle("-fx-background-color: #14121f; -fx-padding: 20;");
        root.getChildren().addAll(boardCanvas, nextCanvas);

        GameController controller = new GameController(boardCanvas, nextCanvas, scoreCanvas);
        controller.start();

        Scene scene = new Scene(root,
                boardCanvas.getWidth() + nextCanvas.getWidth() + 60,
                boardCanvas.getHeight() + 40,
                Color.BLACK);

        scene.setOnKeyPressed(controller::onKeyPressed);

        stage.setTitle("Tetris");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
