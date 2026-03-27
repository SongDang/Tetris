package com.se330.tetris.service;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.se330.tetris.util.Constants;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * SceneManager is a singleton class responsible for managing scene transitions
 * throughout the Tetris application. It handles loading FXML files, applying
 * stylesheets, and switching between different scenes.
 */
public class SceneManager {

    // ========== Scene Constants ==========
    public static final String MAIN_MENU_SCENE = "main";
    public static final String GAMEMODES_SCENE = "gamemodes";
    public static final String GAME_SCENE = "game";
    public static final String RESULTS_SCENE = "results";
    public static final String SETTINGS_SCENE = "settings";

    // ========== Singleton Instance ==========
    private static SceneManager instance;

    // ========== Instance Variables ==========
    private Stage primaryStage;
    private final Map<String, Scene> scenes = new HashMap<>();

    /**
     * Private constructor to enforce singleton pattern
     */
    private SceneManager() {
    }

    /**
     * Gets the singleton instance of SceneManager
     *
     * @return the singleton instance
     */
    public static synchronized SceneManager getInstance() {
        if (instance == null) {
            instance = new SceneManager();
        }
        return instance;
    }

    /**
     * Sets the primary stage for the application
     *
     * @param stage the primary stage
     */
    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    /**
     * Switches to the specified scene by name.
     * Loads the FXML file, applies dark theme CSS, and displays the scene.
     *
     * @param sceneName the name of the scene to switch to
     * @throws IllegalArgumentException if the scene name is invalid
     * @throws RuntimeException if there's an error loading the FXML file
     */
    public void switchToScene(String sceneName) {
        if (primaryStage == null) {
            throw new IllegalStateException("Primary stage has not been set. Call setPrimaryStage() first.");
        }

        try {
            Scene scene = getOrLoadScene(sceneName);
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load scene: " + sceneName, e);
        }
    }

    /**
     * Gets a scene from the cache or loads it if not already loaded
     *
     * @param sceneName the name of the scene
     * @return the loaded scene
     * @throws IOException if the FXML file cannot be loaded
     */
    private Scene getOrLoadScene(String sceneName) throws IOException {
        if (!scenes.containsKey(sceneName)) {
            Scene newScene = loadScene(sceneName);
            scenes.put(sceneName, newScene);
        }
        return scenes.get(sceneName);
    }

    /**
     * Loads a scene from the corresponding FXML file
     *
     * @param sceneName the name of the scene
     * @return the loaded scene
     * @throws IOException if the FXML file cannot be loaded
     */
    private Scene loadScene(String sceneName) throws IOException {
        String fxmlPath = getFxmlPath(sceneName);
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Parent root = loader.load();

        Scene scene = new Scene(root);
        applyStylesheet(scene);

        return scene;
    }

    /**
     * Gets the FXML file path for the given scene name
     *
     * @param sceneName the name of the scene
     * @return the FXML file path
     * @throws IllegalArgumentException if the scene name is invalid
     */
    private String getFxmlPath(String sceneName) {
        return switch (sceneName) {
            case MAIN_MENU_SCENE -> Constants.FXML_MAIN;
            case GAMEMODES_SCENE -> Constants.FXML_GAMEMODES;
            case GAME_SCENE -> Constants.FXML_GAME;
            case RESULTS_SCENE -> Constants.FXML_RESULTS;
            case SETTINGS_SCENE -> Constants.FXML_SETTINGS;
            default -> throw new IllegalArgumentException("Unknown scene name: " + sceneName);
        };
    }

    /**
     * Applies the dark theme stylesheet to the scene
     *
     * @param scene the scene to apply the stylesheet to
     */
    private void applyStylesheet(Scene scene) {
        String cssResource = getClass().getResource(Constants.CSS_DARK_THEME).toExternalForm();
        scene.getStylesheets().add(cssResource);
    }

    /**
     * Clears the scene cache to free up memory
     * Useful when reinitializing the application or changing themes
     */
    public void clearSceneCache() {
        scenes.clear();
    }

    /**
     * Gets a previously loaded scene without reloading it
     *
     * @param sceneName the name of the scene
     * @return the scene, or null if not loaded
     */
    public Scene getScene(String sceneName) {
        return scenes.get(sceneName);
    }

    /**
     * Checks if a scene is currently loaded in the cache
     *
     * @param sceneName the name of the scene
     * @return true if the scene is cached, false otherwise
     */
    public boolean isSceneLoaded(String sceneName) {
        return scenes.containsKey(sceneName);
    }
}
