package com.se330.tetris.controller;

import com.se330.tetris.service.SoundManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import com.se330.tetris.service.GameContext;
import com.se330.tetris.service.SceneManager;

public class GamemodesController {

    @FXML
    private AnchorPane gamemodesPane;

    @FXML
    private Button standardBtn;

    @FXML
    private Button timeAttackBtn;

    @FXML
    private Button hardModeBtn;

    @FXML
    private Button backBtn;

    private SceneManager sceneManager;
    private GameContext gameContext;

    @FXML
    private void initialize() {
        sceneManager = SceneManager.getInstance();
        gameContext = GameContext.getInstance();
    }

    @FXML
    private void onStandardClicked() {
        SoundManager.getInstance().playSE(1);
        SoundManager.getInstance().playMusic(2); //gameplay theme
        gameContext.setGameMode(GameContext.GameMode.STANDARD);
        sceneManager.switchToScene(SceneManager.GAME_SCENE);
    }

    @FXML
    private void onTimeAttackClicked() {
        gameContext.setGameMode(GameContext.GameMode.TIME_ATTACK);
        sceneManager.switchToScene(SceneManager.GAME_SCENE);
    }

    @FXML
    private void onHardModeClicked() {
        gameContext.setGameMode(GameContext.GameMode.HARD_MODE);
        sceneManager.switchToScene(SceneManager.GAME_SCENE);
    }

    @FXML
    private void onBackClicked() {
        sceneManager.switchToScene(SceneManager.MAIN_MENU_SCENE);
    }
}
