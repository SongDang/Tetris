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

/**
 * Controller for the main game screen.
 * Manages the game loop, rendering, user input, and score updates.
 */
public class GameController {

    // ========== FXML Components ==========
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

    // ========== Instance Variables ==========
    private SceneManager sceneManager;
    private GameContext gameContext;
    private GraphicsContext gameGraphics;
    private GraphicsContext nextBlockGraphics;
    private AnimationTimer gameLoop;
    private boolean gamePaused;
    private long lastUpdateTime;

    /**
     * Initializes the controller after the FXML file has been loaded.
     * Sets up the game services, canvas graphics, and game loop.
     */
    @FXML
    private void initialize() {
        sceneManager = SceneManager.getInstance();
        gameContext = GameContext.getInstance();
        gameContext.reset();

        // Initialize graphics contexts
        gameGraphics = gameCanvas.getGraphicsContext2D();
        nextBlockGraphics = nextBlockCanvas.getGraphicsContext2D();

        // Initialize game state
        gamePaused = false;
        lastUpdateTime = System.currentTimeMillis();

        // Update UI labels
        updateScore(0);
        updateLevel(1);
        updateLines(0);

        // Set up key input handling
        gamePane.setOnKeyPressed(this::handleKeyPressed);
        gamePane.requestFocus();

        // Start the game loop
        startGameLoop();

        // Draw initial game board
        drawGameBoard();
        drawNextBlock();
    }

    /**
     * Starts the game loop animation.
     * Updates game state and renders at regular intervals.
     */
    private void startGameLoop() {
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!gamePaused) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastUpdateTime >= 100) { // Update every 100ms
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

    /**
     * Updates the game state (called every game tick).
     * This includes moving blocks, checking for collisions, clearing lines, etc.
     */
    private void updateGameState() {
        // Game state update logic will be implemented by game service
    }

    /**
     * Updates the score display label.
     *
     * @param score the current score
     */
    public void updateScore(int score) {
        gameContext.setScore(score);
        scoreLabel.setText(String.valueOf(score));
    }

    /**
     * Updates the level display label.
     *
     * @param level the current level
     */
    public void updateLevel(int level) {
        gameContext.setLevel(level);
        levelLabel.setText(String.valueOf(level));
    }

    /**
     * Updates the lines cleared display label.
     *
     * @param lines the number of lines cleared
     */
    public void updateLines(int lines) {
        gameContext.setLines(lines);
        linesLabel.setText(String.valueOf(lines));
    }

    /**
     * Draws the game board on the canvas.
     */
    public void drawGameBoard() {
        GraphicsContext gc = gameGraphics;

        // Clear canvas
        gc.setFill(Color.web("#1a1a1a"));
        gc.fillRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());

        // Draw border
        gc.setStroke(Color.web("#00ff00"));
        gc.setLineWidth(2);
        gc.strokeRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());

        // Draw grid
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

    /**
     * Draws the next block preview on the preview canvas.
     */
    public void drawNextBlock() {
        GraphicsContext gc = nextBlockGraphics;

        // Clear canvas
        gc.setFill(Color.web("#1a1a1a"));
        gc.fillRect(0, 0, nextBlockCanvas.getWidth(), nextBlockCanvas.getHeight());

        // Draw border
        gc.setStroke(Color.web("#00ff00"));
        gc.setLineWidth(2);
        gc.strokeRect(0, 0, nextBlockCanvas.getWidth(), nextBlockCanvas.getHeight());

        // Draw placeholder text
        gc.setFill(Color.web("#00ff00"));
        gc.setFont(javafx.scene.text.Font.font("Courier New", 12));
        gc.fillText("Next Block", nextBlockCanvas.getWidth() / 2 - 40, nextBlockCanvas.getHeight() / 2);
    }

    /**
     * Handles key press events during gameplay.
     * Processes movement (arrows), rotation (up), drop (space), and pause (P) commands.
     *
     * @param event the key event
     */
    @FXML
    private void handleKeyPressed(KeyEvent event) {
        KeyCode code = event.getCode();

        switch (code) {
            case LEFT:
                // Move piece left
                handleMoveLeft();
                event.consume();
                break;
            case RIGHT:
                // Move piece right
                handleMoveRight();
                event.consume();
                break;
            case UP:
                // Rotate piece
                handleRotate();
                event.consume();
                break;
            case SPACE:
                // Drop piece
                handleDrop();
                event.consume();
                break;
            case P:
                // Toggle pause
                handlePause();
                event.consume();
                break;
            default:
                break;
        }
    }

    /**
     * Handles moving the current piece to the left.
     */
    private void handleMoveLeft() {
        // Movement logic will be implemented by game service
    }

    /**
     * Handles moving the current piece to the right.
     */
    private void handleMoveRight() {
        // Movement logic will be implemented by game service
    }

    /**
     * Handles rotating the current piece.
     */
    private void handleRotate() {
        // Rotation logic will be implemented by game service
    }

    /**
     * Handles dropping the current piece to the bottom.
     */
    private void handleDrop() {
        // Drop logic will be implemented by game service
    }

    /**
     * Handles toggling pause state.
     */
    private void handlePause() {
        gamePaused = !gamePaused;
    }

    /**
     * Ends the game and transitions to the results screen.
     */
    public void gameOver() {
        if (gameLoop != null) {
            gameLoop.stop();
        }
        sceneManager.switchToScene(SceneManager.RESULTS_SCENE);
    }
}
