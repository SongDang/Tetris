package com.se330.tetris.controller;

import com.se330.tetris.service.SoundManager;
import com.se330.tetris.util.SoundType;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.AnchorPane;
import javafx.scene.Parent;
import com.se330.tetris.service.SceneManager;

public class SettingsController {

    @FXML
    private AnchorPane settingsPane;

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
    private Label settingsScoreLabel;

    @FXML
    private Label settingsScoreValue;

    @FXML
    private Button saveBtn;

    @FXML
    private Button cancelBtn;

    @FXML
    private Button musicIconBtn;

    @FXML
    private Button sfxIconBtn;

    private SceneManager sceneManager;

    @FXML
    private void initialize() {
        sceneManager = SceneManager.getInstance();
        loadSettings();

        musicToggle.selectedProperty().addListener((obs, oldVal, newVal) -> onMusicToggled(newVal));
        sfxToggle.selectedProperty().addListener((obs, oldVal, newVal) -> onSfxToggled(newVal));
        hardModeToggle.selectedProperty().addListener((obs, oldVal, newVal) -> onHardModeToggled(newVal));
        gridToggle.selectedProperty().addListener((obs, oldVal, newVal) -> onGridToggled(newVal));

        musicVolume.valueProperty().addListener((obs, oldVal, newVal) -> onMusicVolumeChanged(newVal.intValue()));
        sfxVolume.valueProperty().addListener((obs, oldVal, newVal) -> onSfxVolumeChanged(newVal.intValue()));

        syncAudioButtonState();
        setScoreVisible(true);
        for (Button btn : new Button[]{saveBtn, cancelBtn, musicIconBtn, sfxIconBtn})
            if (btn != null) btn.setOnMouseEntered(e -> SoundManager.getInstance().playSE(SoundType.HOVER));
    }

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

    private void onMusicToggled(boolean enabled) {
        musicVolume.setDisable(!enabled);
        syncAudioButtonState();
        System.out.println("Music: " + (enabled ? "enabled" : "disabled"));

        if (enabled) {
            SoundManager.getInstance().setMusicVolume((float) musicVolume.getValue() / 100.0f);
        } else {
            SoundManager.getInstance().setMusicVolume(0.0f);
        }
    }

    private void onSfxToggled(boolean enabled) {
        sfxVolume.setDisable(!enabled);
        syncAudioButtonState();
        System.out.println("SFX: " + (enabled ? "enabled" : "disabled"));

        if (enabled) {
            SoundManager.getInstance().setSEVolume((float) sfxVolume.getValue() / 100.0f);
        } else {
            SoundManager.getInstance().setSEVolume(0.0f);
        }
    }

    @FXML
    private void onMusicIconClicked() {
        SoundManager.getInstance().playSE(SoundType.CLICK);
        musicToggle.setSelected(!musicToggle.isSelected());
    }

    @FXML
    private void onSfxIconClicked() {
        SoundManager.getInstance().playSE(SoundType.CLICK);
        sfxToggle.setSelected(!sfxToggle.isSelected());
    }

    private void onMusicVolumeChanged(int volume) {
        musicVolumeLabel.setText(volume + "%");
        System.out.println("Music volume changed to: " + volume + "%");

        SoundManager.getInstance().setMusicVolume(volume / 100.0f);
    }

    private void onSfxVolumeChanged(int volume) {
        sfxVolumeLabel.setText(volume + "%");
        System.out.println("SFX volume changed to: " + volume + "%");

        SoundManager.getInstance().setSEVolume(volume / 100.0f);
    }

    private void onHardModeToggled(boolean enabled) {
        System.out.println("Hard mode: " + (enabled ? "enabled" : "disabled"));
    }

    private void onGridToggled(boolean enabled) {
        System.out.println("Grid display: " + (enabled ? "enabled" : "disabled"));
    }

    @FXML
    private void onSaveClicked() {
        SoundManager.getInstance().playSE(SoundType.CLICK);
        logSettings();
        if (!hideOverlayIfPresent()) {
            sceneManager.switchToScene(SceneManager.MAIN_MENU_SCENE);
        }
    }

    @FXML
    private void onCancelClicked() {
        SoundManager.getInstance().playSE(SoundType.CLICK);
        System.out.println("Settings changes discarded");
        loadSettings();
        if (!hideOverlayIfPresent()) {
            sceneManager.switchToScene(SceneManager.MAIN_MENU_SCENE);
        }
    }

    private void logSettings() {
        System.out.println("=== Settings Saved ===");
        System.out.println("  Music: " + musicToggle.isSelected() + " (Volume: " + (int)musicVolume.getValue() + "%)");
        System.out.println("  SFX: " + sfxToggle.isSelected() + " (Volume: " + (int)sfxVolume.getValue() + "%)");
        System.out.println("  Hard Mode: " + hardModeToggle.isSelected());
        System.out.println("  Show Grid: " + gridToggle.isSelected());
        System.out.println("======================");
    }

    private void syncAudioButtonState() {
        if (musicIconBtn != null) {
            musicIconBtn.setOpacity(musicToggle.isSelected() ? 1.0 : 0.45);
        }
        if (sfxIconBtn != null) {
            sfxIconBtn.setOpacity(sfxToggle.isSelected() ? 1.0 : 0.45);
        }
    }

    private boolean hideOverlayIfPresent() {
        Parent current = settingsPane;
        while (current != null) {
            if (current.getStyleClass().contains("settings-overlay-host")) {
                current.setVisible(false);
                current.setManaged(false);
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    public void setScoreVisible(boolean visible) {
        if (settingsScoreLabel != null) {
            settingsScoreLabel.setVisible(visible);
            settingsScoreLabel.setManaged(visible);
        }
        if (settingsScoreValue != null) {
            settingsScoreValue.setVisible(visible);
            settingsScoreValue.setManaged(visible);
        }
    }
}
