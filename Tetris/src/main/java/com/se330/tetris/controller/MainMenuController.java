package com.se330.tetris.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import com.se330.tetris.service.SceneManager;

public class MainMenuController {

    @FXML
    private AnchorPane mainMenuPane;

    @FXML
    private Button gamemodesBtn;

    @FXML
    private Button settingsBtn;

    @FXML
    private AnchorPane mainContent;

    @FXML
    private AnchorPane settingsOverlay;

    @FXML
    private SettingsController settingsPopupController;

    @FXML
    private Button exitBtn;

    private SceneManager sceneManager;

    @FXML
    private void initialize() {
        sceneManager = SceneManager.getInstance();
    }

    @FXML
    private void onGamemodesClicked() {
        sceneManager.switchToScene(SceneManager.GAMEMODES_SCENE);
    }

    @FXML
    private void onSettingsClicked() {
        if (settingsPopupController != null) {
            settingsPopupController.setScoreVisible(false);
        }
        settingsOverlay.setManaged(true);
        settingsOverlay.setVisible(true);
    }

    @FXML
    private void onSettingsOverlayBackdropClicked() {
        settingsOverlay.setVisible(false);
        settingsOverlay.setManaged(false);
    }

    @FXML
    private void onExitClicked() {
        System.exit(0);
    }
}
