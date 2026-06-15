package com.se330.tetris.service;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
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
    private Scene appScene;
    private Group contentLayer;
    private Pane viewport;
    private final Map<String, Parent> roots = new HashMap<>();
    private boolean fullScreenRequested = false;
    private boolean stageConfigured = false;
    private boolean switchingScene = false;

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
        configureStage();
    }

    public void switchToScene(String sceneName) {
        if (primaryStage == null) {
            throw new IllegalStateException("Primary stage has not been set. Call setPrimaryStage() first.");
        }

        try {
            Parent root = getOrLoadRoot(sceneName);
            boolean restoreFullScreen = fullScreenRequested || primaryStage.isFullScreen();
            switchingScene = true;
            try {
                Scene scene = getOrCreateAppScene();
                contentLayer.getChildren().setAll(root);
                if (primaryStage.getScene() != scene) {
                    primaryStage.setScene(scene);
                }
                primaryStage.setResizable(true);
                if (!primaryStage.isShowing() && !restoreFullScreen) {
                    primaryStage.sizeToScene();
                }
                if (!primaryStage.isShowing()) {
                    primaryStage.show();
                }
                if (restoreFullScreen) {
                    fullScreenRequested = true;
                    primaryStage.setFullScreen(true);
                }
                root.requestFocus();
            } finally {
                switchingScene = false;
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load scene: " + sceneName, e);
        }
    }

    private void configureStage() {
        if (primaryStage == null || stageConfigured) return;
        stageConfigured = true;
        primaryStage.setFullScreenExitHint("");
        primaryStage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        primaryStage.fullScreenProperty().addListener((obs, wasFullScreen, isFullScreen) -> {
            if (switchingScene) return;
            fullScreenRequested = isFullScreen;
        });
        primaryStage.maximizedProperty().addListener((obs, wasMaximized, isMaximized) -> {
            if (!isMaximized) return;
            Platform.runLater(() -> setFullScreenMode(true));
        });
    }

    private void setFullScreenMode(boolean enabled) {
        if (primaryStage == null) return;
        fullScreenRequested = enabled;
        if (enabled) {
            primaryStage.setFullScreen(true);
            if (primaryStage.isMaximized()) {
                Platform.runLater(() -> primaryStage.setMaximized(false));
            }
        } else {
            primaryStage.setFullScreen(false);
        }
    }

    private Parent getOrLoadRoot(String sceneName) throws IOException {
        if (!roots.containsKey(sceneName)) {
            Parent root = loadRoot(sceneName);
            roots.put(sceneName, root);
        }
        return roots.get(sceneName);
    }

    private Parent loadRoot(String sceneName) throws IOException {
        String fxmlPath = getFxmlPath(sceneName);
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Parent root = loader.load();
        configureDesignRoot(root);
        return root;
    }

    private Scene getOrCreateAppScene() {
        if (appScene == null) {
            viewport = createResponsiveViewport();
            appScene = new Scene(viewport, Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT, Color.BLACK);
            applyStylesheet(appScene);
            installFullscreenShortcut(appScene);
        }
        return appScene;
    }

    private void configureDesignRoot(Parent root) {
        if (root instanceof Region region) {
            region.setMinSize(Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);
            region.setPrefSize(Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);
            region.setMaxSize(Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);
        }
    }

    private Pane createResponsiveViewport() {
        contentLayer = new Group();
        Pane designSurface = new Pane(contentLayer);
        designSurface.setMinSize(Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);
        designSurface.setPrefSize(Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);
        designSurface.setMaxSize(Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);

        StackPane responsiveViewport = new StackPane(designSurface);
        responsiveViewport.setStyle("-fx-background-color: #000000;");
        responsiveViewport.setMinSize(0, 0);
        responsiveViewport.setPrefSize(Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);
        responsiveViewport.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        NumberBinding scale = Bindings.min(
                responsiveViewport.widthProperty().divide(Constants.WINDOW_WIDTH),
                responsiveViewport.heightProperty().divide(Constants.WINDOW_HEIGHT));

        designSurface.scaleXProperty().bind(scale);
        designSurface.scaleYProperty().bind(scale);

        return responsiveViewport;
    }

    private void installFullscreenShortcut(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (primaryStage == null) return;
            boolean fullScreenShortcut = event.getCode() == KeyCode.F11
                    || (event.getCode() == KeyCode.ENTER && event.isAltDown());
            if (!fullScreenShortcut) return;

            setFullScreenMode(!primaryStage.isFullScreen());
            event.consume();
        });
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
        roots.clear();
    }

    public Scene getScene(String sceneName) {
        return isSceneLoaded(sceneName) ? appScene : null;
    }

    public boolean isSceneLoaded(String sceneName) {
        return roots.containsKey(sceneName);
    }
}
