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
    private Runnable closeHandler;
    private Runnable quitHandler;
    private boolean loadingSettings = false;

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
        loadingSettings = true;
        SoundManager soundManager = SoundManager.getInstance();
        int musicPercent = Math.round(soundManager.getMusicVolume() * 100.0f);
        int sfxPercent = Math.round(soundManager.getSEVolume() * 100.0f);

        musicToggle.setSelected(musicPercent > 0);
        musicVolume.setValue(musicPercent);
        musicVolumeLabel.setText(musicPercent + "%");
        musicVolume.setDisable(musicPercent <= 0);

        sfxToggle.setSelected(sfxPercent > 0);
        sfxVolume.setValue(sfxPercent);
        sfxVolumeLabel.setText(sfxPercent + "%");
        sfxVolume.setDisable(sfxPercent <= 0);

        hardModeToggle.setSelected(false);
        gridToggle.setSelected(true);

        syncAudioButtonState();
        loadingSettings = false;
    }

    private void onMusicToggled(boolean enabled) {
        if (loadingSettings) return;
        musicVolume.setDisable(!enabled);
        syncAudioButtonState();

        if (enabled) {
            if (musicVolume.getValue() <= 0) {
                musicVolume.setValue(70);
            }
            SoundManager.getInstance().setMusicVolume((float) musicVolume.getValue() / 100.0f);
        } else {
            SoundManager.getInstance().setMusicVolume(0.0f);
        }
    }

    private void onSfxToggled(boolean enabled) {
        if (loadingSettings) return;
        sfxVolume.setDisable(!enabled);
        syncAudioButtonState();

        if (enabled) {
            if (sfxVolume.getValue() <= 0) {
                sfxVolume.setValue(80);
            }
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
        if (loadingSettings) return;
        musicVolumeLabel.setText(volume + "%");
        boolean enabled = volume > 0;
        if (musicToggle.isSelected() != enabled) {
            musicToggle.setSelected(enabled);
            return;
        }
        musicVolume.setDisable(!enabled);
        syncAudioButtonState();
        SoundManager.getInstance().setMusicVolume(enabled ? volume / 100.0f : 0.0f);
    }

    private void onSfxVolumeChanged(int volume) {
        if (loadingSettings) return;
        sfxVolumeLabel.setText(volume + "%");
        boolean enabled = volume > 0;
        if (sfxToggle.isSelected() != enabled) {
            sfxToggle.setSelected(enabled);
            return;
        }
        sfxVolume.setDisable(!enabled);
        syncAudioButtonState();
        SoundManager.getInstance().setSEVolume(enabled ? volume / 100.0f : 0.0f);
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
        if (closeHandler != null) {
            closeHandler.run();
            return;
        }
        if (!hideOverlayIfPresent()) {
            sceneManager.switchToScene(SceneManager.MAIN_MENU_SCENE);
        }
    }

    @FXML
    private void onCancelClicked() {
        SoundManager.getInstance().playSE(SoundType.CLICK);
        System.out.println("Settings changes discarded");
        if (quitHandler != null) {
            quitHandler.run();
            return;
        }
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

    public void setScoreValue(int score) {
        if (settingsScoreValue != null) {
            settingsScoreValue.setText(String.valueOf(score));
        }
    }

    public void setCloseHandler(Runnable closeHandler) {
        this.closeHandler = closeHandler;
    }

    public void setQuitHandler(Runnable quitHandler) {
        this.quitHandler = quitHandler;
    }
}
