package com.se330.tetris.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import com.se330.tetris.service.SceneManager;

/**
 * Controller for the settings configuration screen.
 * Manages audio settings, gameplay options, and preferences (demo version).
 */
public class SettingsController {

    // ========== FXML Components ==========
    @FXML
    private BorderPane settingsPane;

    @FXML
    private CheckBox musicToggle;

    @FXML
    private CheckBox sfxToggle;

    @FXML
    private CheckBox hardModeToggle;

    @FXML
    private CheckBox gridToggle;

    @FXML
    private Slider musicVolume;

    @FXML
    private Slider sfxVolume;

    @FXML
    private Label musicVolumeLabel;

    @FXML
    private Label sfxVolumeLabel;

    @FXML
    private Button saveBtn;

    @FXML
    private Button cancelBtn;

    // ========== Instance Variables ==========
    private SceneManager sceneManager;

    /**
     * Initializes the controller after the FXML file has been loaded.
     * Loads default settings and sets up event listeners.
     */
    @FXML
    private void initialize() {
        sceneManager = SceneManager.getInstance();

        // Load default settings
        loadSettings();

        // Set up event listeners for settings changes
        musicToggle.selectedProperty().addListener((obs, oldVal, newVal) -> onMusicToggled(newVal));
        sfxToggle.selectedProperty().addListener((obs, oldVal, newVal) -> onSfxToggled(newVal));
        hardModeToggle.selectedProperty().addListener((obs, oldVal, newVal) -> onHardModeToggled(newVal));
        gridToggle.selectedProperty().addListener((obs, oldVal, newVal) -> onGridToggled(newVal));
        
        musicVolume.valueProperty().addListener((obs, oldVal, newVal) -> onMusicVolumeChanged(newVal.intValue()));
        sfxVolume.valueProperty().addListener((obs, oldVal, newVal) -> onSfxVolumeChanged(newVal.intValue()));
    }

    /**
     * Loads the default settings.
     */
    private void loadSettings() {
        musicToggle.setSelected(true);
        musicVolume.setValue(70);
        musicVolumeLabel.setText("70%");

        sfxToggle.setSelected(true);
        sfxVolume.setValue(80);
        sfxVolumeLabel.setText("80%");

        hardModeToggle.setSelected(false);
        gridToggle.setSelected(true);

        System.out.println("Settings loaded (default values)");
    }

    /**
     * Handles enabling/disabling background music.
     *
     * @param enabled true if music should be enabled
     */
    private void onMusicToggled(boolean enabled) {
        musicVolume.setDisable(!enabled);
        System.out.println("Music: " + (enabled ? "enabled" : "disabled"));
    }

    /**
     * Handles enabling/disabling sound effects.
     *
     * @param enabled true if SFX should be enabled
     */
    private void onSfxToggled(boolean enabled) {
        sfxVolume.setDisable(!enabled);
        System.out.println("SFX: " + (enabled ? "enabled" : "disabled"));
    }

    /**
     * Handles music volume changes and updates the label.
     *
     * @param volume the new volume level (0-100)
     */
    private void onMusicVolumeChanged(int volume) {
        musicVolumeLabel.setText(volume + "%");
        System.out.println("Music volume changed to: " + volume + "%");
    }

    /**
     * Handles SFX volume changes and updates the label.
     *
     * @param volume the new volume level (0-100)
     */
    private void onSfxVolumeChanged(int volume) {
        sfxVolumeLabel.setText(volume + "%");
        System.out.println("SFX volume changed to: " + volume + "%");
    }

    /**
     * Handles hard mode toggle.
     *
     * @param enabled true if hard mode should be enabled
     */
    private void onHardModeToggled(boolean enabled) {
        System.out.println("Hard mode: " + (enabled ? "enabled" : "disabled"));
    }

    /**
     * Handles grid display toggle.
     *
     * @param enabled true if grid should be displayed
     */
    private void onGridToggled(boolean enabled) {
        System.out.println("Grid display: " + (enabled ? "enabled" : "disabled"));
    }

    /**
     * Handles the click event for the Save button.
     * Logs all settings and returns to main menu.
     */
    @FXML
    private void onSaveClicked() {
        logSettings();
        sceneManager.switchToScene(SceneManager.MAIN_MENU_SCENE);
    }

    /**
     * Handles the click event for the Cancel button.
     * Resets settings and returns to main menu without saving.
     */
    @FXML
    private void onCancelClicked() {
        System.out.println("Settings changes discarded");
        loadSettings();
        sceneManager.switchToScene(SceneManager.MAIN_MENU_SCENE);
    }

    /**
     * Logs all current settings to console.
     */
    private void logSettings() {
        System.out.println("=== Settings Saved ===");
        System.out.println("  Music: " + musicToggle.isSelected() + " (Volume: " + (int)musicVolume.getValue() + "%)");
        System.out.println("  SFX: " + sfxToggle.isSelected() + " (Volume: " + (int)sfxVolume.getValue() + "%)");
        System.out.println("  Hard Mode: " + hardModeToggle.isSelected());
        System.out.println("  Show Grid: " + gridToggle.isSelected());
        System.out.println("======================");
    }
}
