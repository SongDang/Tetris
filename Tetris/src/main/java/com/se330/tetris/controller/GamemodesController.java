package com.se330.tetris.controller;

import com.se330.tetris.service.SoundManager;
import com.se330.tetris.util.SoundType;
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
        for (Button btn : new Button[]{standardBtn, timeAttackBtn, hardModeBtn, backBtn})
            if (btn != null) btn.setOnMouseEntered(e -> SoundManager.getInstance().playSE(SoundType.HOVER));
    }

    @FXML
    private void onStandardClicked() {
        SoundManager.getInstance().playSE(SoundType.ENTER_MODE);
        SoundManager.getInstance().playMusic(SoundType.GAMEPLAY_THEME);
        gameContext.setGameMode(GameContext.GameMode.STANDARD);
        gameContext.reset();
        sceneManager.clearSceneCache();
        sceneManager.switchToScene(SceneManager.GAME_SCENE);
    }

    @FXML
    private void onTimeAttackClicked() {
        SoundManager.getInstance().playSE(SoundType.CLICK);
        SoundManager.getInstance().playMusic(SoundType.GAMEPLAY_THEME);
        gameContext.setGameMode(GameContext.GameMode.TIME_ATTACK);
        gameContext.reset();
        sceneManager.clearSceneCache();
        sceneManager.switchToScene(SceneManager.GAME_SCENE);
    }

    @FXML
    private void onHardModeClicked() {
        SoundManager.getInstance().playSE(SoundType.CLICK);
        SoundManager.getInstance().playMusic(SoundType.GAMEPLAY_THEME);
        gameContext.setGameMode(GameContext.GameMode.HARD_MODE);
        gameContext.reset();
        sceneManager.clearSceneCache();
        sceneManager.switchToScene(SceneManager.GAME_SCENE);
    }

    @FXML
    private void onBackClicked() {
        SoundManager.getInstance().playSE(SoundType.CLICK);
        sceneManager.switchToScene(SceneManager.MAIN_MENU_SCENE);
    }
}
