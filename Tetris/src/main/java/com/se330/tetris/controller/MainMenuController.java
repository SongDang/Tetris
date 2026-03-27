package com.se330.tetris.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import com.se330.tetris.service.SceneManager;
import com.se330.tetris.util.Constants;

/**
 * Controller for the main menu screen.
 * Handles user interactions on the main menu and navigation to other screens.
 */
public class MainMenuController {

    // ========== FXML Components ==========
    @FXML
    private BorderPane mainMenuPane;

    @FXML
    private Button gamemodesBtn;

    @FXML
    private Button settingsBtn;

    @FXML
    private Button exitBtn;

    // ========== Instance Variables ==========
    private SceneManager sceneManager;

    /**
     * Initializes the controller after the FXML file has been loaded.
     * Sets up the scene manager and configures button actions.
     */
    @FXML
    private void initialize() {
        sceneManager = SceneManager.getInstance();
    }

    /**
     * Handles the click event for the Gamemodes button.
     * Switches to the gamemodes selection screen.
     */
    @FXML
    private void onGamemodesClicked() {
        sceneManager.switchToScene(SceneManager.GAMEMODES_SCENE);
    }

    /**
     * Handles the click event for the Settings button.
     * Switches to the settings configuration screen.
     */
    @FXML
    private void onSettingsClicked() {
        sceneManager.switchToScene(SceneManager.SETTINGS_SCENE);
    }

    /**
     * Handles the click event for the Exit button.
     * Terminates the application.
     */
    @FXML
    private void onExitClicked() {
        System.exit(0);
    }
}
