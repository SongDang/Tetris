package com.se330.tetris;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.se330.tetris.service.SceneManager;
import com.se330.tetris.util.Constants;

import java.io.IOException;

/**
 * Main entry point for the Tetris application.
 * Initializes the JavaFX application, loads the main menu scene,
 * and applies the dark theme stylesheet.
 */
public class TetrisApp extends Application {

    /**
     * Starts the Tetris application
     *
     * @param primaryStage the primary stage for the application
     * @throws IOException if the FXML file cannot be loaded
     */
    @Override
    public void start(Stage primaryStage) throws IOException {
        // Initialize SceneManager with the primary stage
        SceneManager sceneManager = SceneManager.getInstance();
        sceneManager.setPrimaryStage(primaryStage);

        // Load the main FXML file
        FXMLLoader loader = new FXMLLoader(getClass().getResource(Constants.FXML_MAIN));
        Parent root = loader.load();

        // Create the scene
        Scene scene = new Scene(root, Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);

        // Apply dark theme stylesheet
        String cssResource = getClass().getResource(Constants.CSS_DARK_THEME).toExternalForm();
        scene.getStylesheets().add(cssResource);

        // Configure the primary stage
        primaryStage.setTitle(Constants.WINDOW_TITLE);
        primaryStage.setScene(scene);
        primaryStage.setWidth(Constants.WINDOW_WIDTH);
        primaryStage.setHeight(Constants.WINDOW_HEIGHT);

        // Show the application
        primaryStage.show();
    }

    /**
     * Main method to launch the Tetris application
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}
