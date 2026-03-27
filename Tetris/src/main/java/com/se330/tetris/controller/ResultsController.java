package com.se330.tetris.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import com.se330.tetris.service.GameContext;
import com.se330.tetris.service.SceneManager;

/**
 * Controller for the results/game over screen.
 * Displays final score, game statistics, and allows player to retry or return to menu.
 */
public class ResultsController {

    // ========== FXML Components ==========
    @FXML
    private BorderPane resultsPane;

    @FXML
    private Label finalScoreLabel;

    @FXML
    private Label gameModeLabel;

    @FXML
    private Label timeLabel;

    @FXML
    private Label linesLabel;

    @FXML
    private Label levelLabel;

    @FXML
    private Label blocksLabel;

    @FXML
    private Button saveBtn;

    @FXML
    private Button retryBtn;

    @FXML
    private Button menuBtn;

    // ========== Instance Variables ==========
    private SceneManager sceneManager;
    private GameContext gameContext;

    /**
     * Initializes the controller after the FXML file has been loaded.
     * Populates the results display with demo game statistics.
     */
    @FXML
    private void initialize() {
        sceneManager = SceneManager.getInstance();
        gameContext = GameContext.getInstance();

        // Display game statistics (demo data)
        populateResults();
    }

    /**
     * Populates all statistics with demo data for UI demonstration.
     */
    private void populateResults() {
        // Use static demo data for display
        int demoScore = 366767;
        int demoLevel = 12;
        int demoLines = 45;
        int demoBlocks = 156;
        long demoTime = 332000; // 5:32 in milliseconds

        // Update labels with demo data
        finalScoreLabel.setText(String.valueOf(demoScore));
        gameModeLabel.setText(gameContext.getGameMode().getDisplayName());
        levelLabel.setText(String.valueOf(demoLevel));
        linesLabel.setText(String.valueOf(demoLines));
        blocksLabel.setText(String.valueOf(demoBlocks));

        // Format and display time
        long totalSeconds = demoTime / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        timeLabel.setText(String.format("Time: %d:%02d", minutes, seconds));

        System.out.println("Results displayed:");
        System.out.println("  Final Score: " + demoScore);
        System.out.println("  Mode: " + gameContext.getGameMode().getDisplayName());
        System.out.println("  Level: " + demoLevel);
        System.out.println("  Lines: " + demoLines);
        System.out.println("  Time: " + String.format("%d:%02d", minutes, seconds));
    }

    /**
     * Handles the click event for the Save button.
     * Logs the score for demo purposes.
     */
    @FXML
    private void onSaveClicked() {
        int score = Integer.parseInt(finalScoreLabel.getText());
        System.out.println("Saving score: " + score + " (" + gameModeLabel.getText() + ")");
    }

    /**
     * Handles the click event for the Retry button.
     * Returns to game screen with same gamemode.
     */
    @FXML
    private void onRetryClicked() {
        System.out.println("Retrying with mode: " + gameContext.getGameMode().getDisplayName());
        gameContext.reset();
        sceneManager.switchToScene(SceneManager.GAME_SCENE);
    }

    /**
     * Handles the click event for the Menu button.
     * Returns to the main menu screen.
     */
    @FXML
    private void onMenuClicked() {
        System.out.println("Returning to main menu");
        gameContext.reset();
        sceneManager.switchToScene(SceneManager.MAIN_MENU_SCENE);
    }
}
