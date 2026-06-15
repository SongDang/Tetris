package com.se330.tetris.service;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.se330.tetris.util.Constants;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SceneManager {

    public static final String MAIN_MENU_SCENE = "main";
    public static final String GAMEMODES_SCENE = "gamemodes";
    public static final String GAME_SCENE = "game";
    public static final String HARD_GAME_SCENE  = "hardgame";
    public static final String HARD_INTRO_SCENE     = "hardintro";
    public static final String TIME_INTRO_SCENE     = "timeintro";
    public static final String STANDARD_INTRO_SCENE = "standardintro";
    public static final String RESULTS_SCENE = "results";
    public static final String SETTINGS_SCENE = "settings";

    private static SceneManager instance;

    private Stage primaryStage;
    private final Map<String, Scene> scenes = new HashMap<>();

    private SceneManager() {
    }

    public static synchronized SceneManager getInstance() {
        if (instance == null) {
            instance = new SceneManager();
        }
        return instance;
    }

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    public void switchToScene(String sceneName) {
        if (primaryStage == null) {
            throw new IllegalStateException("Primary stage has not been set. Call setPrimaryStage() first.");
        }

        try {
            Scene scene = getOrLoadScene(sceneName);
            primaryStage.setScene(scene);
            primaryStage.sizeToScene();
            primaryStage.show();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load scene: " + sceneName, e);
        }
    }

    private Scene getOrLoadScene(String sceneName) throws IOException {
        if (!scenes.containsKey(sceneName)) {
            Scene newScene = loadScene(sceneName);
            scenes.put(sceneName, newScene);
        }
        return scenes.get(sceneName);
    }

    private Scene loadScene(String sceneName) throws IOException {
        String fxmlPath = getFxmlPath(sceneName);
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Parent root = loader.load();

        Scene scene = new Scene(root, Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);
        applyStylesheet(scene);

        return scene;
    }

    private String getFxmlPath(String sceneName) {
        return switch (sceneName) {
            case MAIN_MENU_SCENE -> Constants.FXML_MAIN;
            case GAMEMODES_SCENE -> Constants.FXML_GAMEMODES;
            case GAME_SCENE -> Constants.FXML_GAME;
            case HARD_GAME_SCENE  -> Constants.FXML_HARDGAME;
            case HARD_INTRO_SCENE     -> Constants.FXML_HARD_INTRO;
            case TIME_INTRO_SCENE     -> Constants.FXML_TIME_INTRO;
            case STANDARD_INTRO_SCENE -> Constants.FXML_STANDARD_INTRO;
            case RESULTS_SCENE -> Constants.FXML_RESULTS;
            case SETTINGS_SCENE -> Constants.FXML_SETTINGS;
            default -> throw new IllegalArgumentException("Unknown scene name: " + sceneName);
        };
    }

    private void applyStylesheet(Scene scene) {
        String cssResource = getClass().getResource(Constants.CSS_DARK_THEME).toExternalForm();
        scene.getStylesheets().add(cssResource);
    }

    public void clearSceneCache() {
        scenes.clear();
    }

    public Scene getScene(String sceneName) {
        return scenes.get(sceneName);
    }

    public boolean isSceneLoaded(String sceneName) {
        return scenes.containsKey(sceneName);
    }
}
