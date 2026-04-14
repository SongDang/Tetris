package com.se330.tetris.core;

import com.se330.tetris.service.SoundManager;
import com.se330.tetris.util.SoundType;
import javafx.application.Application;
import javafx.stage.Stage;
import com.se330.tetris.service.SceneManager;
import com.se330.tetris.util.Constants;

public class TetrisApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        SceneManager sceneManager = SceneManager.getInstance();
        sceneManager.setPrimaryStage(primaryStage);

        primaryStage.setTitle(Constants.WINDOW_TITLE);
        primaryStage.setWidth(Constants.WINDOW_WIDTH);
        primaryStage.setHeight(Constants.WINDOW_HEIGHT);

        String requestedFromProperty = System.getProperty("tetris.startScene", "").trim();
        String requestedFromEnv = System.getenv("TETRIS_START_SCENE");
        String requested = !requestedFromProperty.isEmpty()
            ? requestedFromProperty.toLowerCase()
            : (requestedFromEnv == null ? SceneManager.MAIN_MENU_SCENE : requestedFromEnv.trim().toLowerCase());
        String initialScene = switch (requested) {
            case SceneManager.GAMEMODES_SCENE -> SceneManager.GAMEMODES_SCENE;
            case SceneManager.GAME_SCENE -> SceneManager.GAME_SCENE;
            case SceneManager.RESULTS_SCENE -> SceneManager.RESULTS_SCENE;
            case SceneManager.SETTINGS_SCENE -> SceneManager.SETTINGS_SCENE;
            default -> SceneManager.MAIN_MENU_SCENE;
        };

        System.out.println("Starting scene: " + initialScene);
        sceneManager.switchToScene(initialScene);

        SoundManager.getInstance().playMusic(SoundType.MAIN_THEME); //theme music
    }

    public static void main(String[] args) {
        launch(args);
    }
}
