package com.se330.tetris.core;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import com.se330.tetris.controller.GameController;

public class TetrisApp extends Application {
    public static final int BOARD_WIDTH = 10;
    public static final int BOARD_HEIGHT = 20;
    public static final int CELL_SIZE = 28;

    @Override
    public void start(Stage stage) {
        Canvas canvas = new Canvas(BOARD_WIDTH * CELL_SIZE, BOARD_HEIGHT * CELL_SIZE);

        GameController controller = new GameController(canvas);
        controller.start();

        BorderPane root = new BorderPane(canvas);
        root.setStyle("-fx-background-color: #14121f;");

        Scene scene = new Scene(root, canvas.getWidth() + 40, canvas.getHeight() + 40, Color.BLACK);
        scene.setOnKeyPressed(controller::onKeyPressed);
        stage.setTitle("Tetris - Step 1");
        stage.setScene(scene);
        stage.show();

    }

    public static void main(String[] args) {
        launch(args);
    }
}
