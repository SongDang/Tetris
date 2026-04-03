package com.se330.tetris.controller;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import com.se330.tetris.service.GameContext;
import com.se330.tetris.service.SceneManager;

public class GameController {

    @FXML
    private BorderPane gamePane;

    @FXML
    private Canvas gameCanvas;

    @FXML
    private Canvas nextBlockCanvas;

    @FXML
    private Label scoreLabel;

    @FXML
    private Label levelLabel;

    @FXML
    private Label linesLabel;

    private SceneManager sceneManager;
    private GameContext gameContext;
    private GraphicsContext gameGraphics;
    private GraphicsContext nextBlockGraphics;
    private AnimationTimer gameLoop;
    private boolean gamePaused;
    private long lastUpdateTime;

    @FXML
    private void initialize() {
        sceneManager = SceneManager.getInstance();
        gameContext = GameContext.getInstance();
        gameContext.reset();

        gameGraphics = gameCanvas.getGraphicsContext2D();
        nextBlockGraphics = nextBlockCanvas.getGraphicsContext2D();

        gamePaused = false;
        lastUpdateTime = System.currentTimeMillis();

        updateScore(0);
        updateLevel(1);
        updateLines(0);

        gamePane.setOnKeyPressed(this::handleKeyPressed);
        gamePane.requestFocus();

        startGameLoop();

        drawGameBoard();
        drawNextBlock();
    }

    private void startGameLoop() {
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!gamePaused) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastUpdateTime >= 100) {
                        lastUpdateTime = currentTime;
                        updateGameState();
                        drawGameBoard();
                        drawNextBlock();
                    }
                }
            }
        };
        gameLoop.start();
    }

    private void updateGameState() {
    }

    public void updateScore(int score) {
        gameContext.setScore(score);
        scoreLabel.setText(String.valueOf(score));
    }

    public void updateLevel(int level) {
        gameContext.setLevel(level);
        levelLabel.setText(String.valueOf(level));
    }

    public void updateLines(int lines) {
        gameContext.setLines(lines);
        linesLabel.setText(String.valueOf(lines));
    }

    public void drawGameBoard() {
        GraphicsContext gc = gameGraphics;

        gc.setFill(Color.web("#1a1a1a"));
        gc.fillRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());

        gc.setStroke(Color.web("#00ff00"));
        gc.setLineWidth(2);
        gc.strokeRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());

        gc.setStroke(Color.web("#333333"));
        gc.setLineWidth(0.5);
        int blockSize = 30;
        for (int i = 0; i <= 10; i++) {
            gc.strokeLine(i * blockSize, 0, i * blockSize, gameCanvas.getHeight());
        }
        for (int i = 0; i <= 20; i++) {
            gc.strokeLine(0, i * blockSize, gameCanvas.getWidth(), i * blockSize);
        }
    }

    public void drawNextBlock() {
        GraphicsContext gc = nextBlockGraphics;

        gc.setFill(Color.web("#1a1a1a"));
        gc.fillRect(0, 0, nextBlockCanvas.getWidth(), nextBlockCanvas.getHeight());

        gc.setStroke(Color.web("#00ff00"));
        gc.setLineWidth(2);
        gc.strokeRect(0, 0, nextBlockCanvas.getWidth(), nextBlockCanvas.getHeight());

        gc.setFill(Color.web("#00ff00"));
        gc.setFont(javafx.scene.text.Font.font("Courier New", 12));
        gc.fillText("Next Block", nextBlockCanvas.getWidth() / 2 - 40, nextBlockCanvas.getHeight() / 2);
    }

    @FXML
    private void handleKeyPressed(KeyEvent event) {
        KeyCode code = event.getCode();

        switch (code) {
            case LEFT:
                handleMoveLeft();
                event.consume();
                break;
            case RIGHT:
                handleMoveRight();
                event.consume();
                break;
            case UP:
                handleRotate();
                event.consume();
                break;
            case SPACE:
                handleDrop();
                event.consume();
                break;
            case P:
                handlePause();
                event.consume();
                break;
            default:
                break;
        }
    }

    private void handleMoveLeft() {
    }

    private void handleMoveRight() {
    }

    private void handleRotate() {
    }

    private void handleDrop() {
    }

    private void handlePause() {
        gamePaused = !gamePaused;
    }

    public void gameOver() {
        if (gameLoop != null) {
            gameLoop.stop();
        }
        sceneManager.switchToScene(SceneManager.RESULTS_SCENE);
    }
}
