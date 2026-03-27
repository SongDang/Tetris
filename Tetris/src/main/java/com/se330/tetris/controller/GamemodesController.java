package com.se330.tetris.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import com.se330.tetris.service.GameContext;
import com.se330.tetris.service.SceneManager;

/**
 * Controller for the game modes selection screen.
 * Allows players to choose between different game modes and start a game.
 */
public class GamemodesController {

    // ========== FXML Components ==========
    @FXML
    private BorderPane gamemodesPane;

    @FXML
    private Button standardBtn;

    @FXML
    private Button timeAttackBtn;

    @FXML
    private Button hardModeBtn;

    @FXML
    private Button backBtn;

    // ========== Instance Variables ==========
    private SceneManager sceneManager;
    private GameContext gameContext;

    /**
     * Initializes the controller after the FXML file has been loaded.
     * Sets up the scene manager and game context.
     */
    @FXML
    private void initialize() {
        sceneManager = SceneManager.getInstance();
        gameContext = GameContext.getInstance();
    }

    /**
     * Handles the click event for the Standard Mode button.
     * Sets the game mode to STANDARD and starts the game.
     */
    @FXML
    private void onStandardClicked() {
        gameContext.setGameMode(GameContext.GameMode.STANDARD);
        sceneManager.switchToScene(SceneManager.GAME_SCENE);
    }

    /**
     * Handles the click event for the Time Attack Mode button.
     * Sets the game mode to TIME_ATTACK and starts the game.
     */
    @FXML
    private void onTimeAttackClicked() {
        gameContext.setGameMode(GameContext.GameMode.TIME_ATTACK);
        sceneManager.switchToScene(SceneManager.GAME_SCENE);
    }

    /**
     * Handles the click event for the Hard Mode button.
     * Sets the game mode to HARD_MODE and starts the game.
     */
    @FXML
    private void onHardModeClicked() {
        gameContext.setGameMode(GameContext.GameMode.HARD_MODE);
        sceneManager.switchToScene(SceneManager.GAME_SCENE);
    }

    /**
     * Handles the click event for the Back button.
     * Returns to the main menu screen.
     */
    @FXML
    private void onBackClicked() {
        sceneManager.switchToScene(SceneManager.MAIN_MENU_SCENE);
    }
}
