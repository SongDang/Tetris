package com.se330.tetris.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import com.se330.tetris.service.SceneManager;
import com.se330.tetris.service.SoundManager;
import com.se330.tetris.util.SoundType;

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
        for (Button btn : new Button[]{gamemodesBtn, settingsBtn, exitBtn})
            if (btn != null) btn.setOnMouseEntered(e -> SoundManager.getInstance().playSE(SoundType.HOVER));
    }

    @FXML
    private void onGamemodesClicked() {
        SoundManager.getInstance().playSE(SoundType.CLICK);
        sceneManager.switchToScene(SceneManager.GAMEMODES_SCENE);
    }

    @FXML
    private void onSettingsClicked() {
        SoundManager.getInstance().playSE(SoundType.CLICK);
        if (settingsPopupController != null) {
            settingsPopupController.setScoreVisible(false);
        }
        settingsOverlay.setManaged(true);
        settingsOverlay.setVisible(true);
    }

    @FXML
    private void onSettingsOverlayBackdropClicked() {
        SoundManager.getInstance().playSE(SoundType.CLICK);
        settingsOverlay.setVisible(false);
        settingsOverlay.setManaged(false);
    }

    @FXML
    private void onExitClicked() {
        SoundManager.getInstance().playSE(SoundType.CLICK);
        System.exit(0);
    }
}
