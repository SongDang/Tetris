package com.se330.tetris.controller;

import com.se330.tetris.service.SoundManager;
import com.se330.tetris.util.SoundType;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import com.se330.tetris.service.GameContext;
import com.se330.tetris.service.SceneManager;

public class ResultsController {

    @FXML
    private AnchorPane resultsPane;

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

    private SceneManager sceneManager;
    private GameContext gameContext;

    @FXML
    private void initialize() {
        sceneManager = SceneManager.getInstance();
        gameContext = GameContext.getInstance();
        populateResults();
    }

    private void populateResults() {
        int demoScore = gameContext.getScore();
        ;
        int demoLevel = gameContext.getLevel();
        int demoLines = gameContext.getLines();
        int demoBlocks = 156;
        long demoTime = 332000;

        finalScoreLabel.setText(String.valueOf(demoScore));
        gameModeLabel.setText(gameContext.getGameMode().getDisplayName());
        levelLabel.setText(String.valueOf(demoLevel));
        linesLabel.setText(String.valueOf(demoLines));
        blocksLabel.setText(String.valueOf(demoBlocks));

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

    @FXML
    private void onSaveClicked() {
        int score = Integer.parseInt(finalScoreLabel.getText());
        System.out.println("Saving score: " + score + " (" + gameModeLabel.getText() + ")");
        System.out.println("Save completed, returning to main menu");
        gameContext.reset();
        sceneManager.switchToScene(SceneManager.MAIN_MENU_SCENE);
    }

    @FXML
    private void onRetryClicked() {
        System.out.println("Retrying with mode: " + gameContext.getGameMode().getDisplayName());
        gameContext.reset();
        // Clear cache to force GameController.initialize() to run again
        sceneManager.clearSceneCache();
        sceneManager.switchToScene(SceneManager.GAME_SCENE);
        SoundManager.getInstance().playMusic(SoundType.GAMEPLAY_THEME);
    }

    @FXML
    private void onMenuClicked() {
        System.out.println("Returning to main menu");
        gameContext.reset();
        sceneManager.switchToScene(SceneManager.MAIN_MENU_SCENE);
    }
}
