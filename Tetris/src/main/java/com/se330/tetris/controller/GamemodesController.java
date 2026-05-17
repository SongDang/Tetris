package com.se330.tetris.controller;

import com.se330.tetris.service.SoundManager;
import com.se330.tetris.util.SoundType;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import com.se330.tetris.service.GameContext;
import com.se330.tetris.service.SceneManager;

public class GamemodesController {

    @FXML
    private AnchorPane gamemodesPane;

    @FXML
    private ImageView standardCard;

    @FXML
    private ImageView timeCard;

    @FXML
    private ImageView hardCard;

    @FXML
    private Button backCard;

    private SceneManager sceneManager;
    private GameContext gameContext;

    @FXML
    private void initialize() {
        sceneManager = SceneManager.getInstance();
        gameContext = GameContext.getInstance();
        // Play hover on footer/back button
        if (backCard != null) backCard.setOnMouseEntered(e -> SoundManager.getInstance().playSE(SoundType.HOVER));

        // Setup image variant swapping for full-card ImageViews (default, hover -> var2, pressed -> var3)
        setupImageVariants(standardCard, this::onStandardClicked,
                "/assets/default_standard.png",
                "/assets/var2_standard.png",
                "/assets/var3_standard.png");

        setupImageVariants(timeCard, this::onTimeAttackClicked,
                "/assets/default_time.png",
                "/assets/var2_time.png",
                "/assets/var3_time.png");

        setupImageVariants(hardCard, this::onHardModeClicked,
                "/assets/default_hard.png",
                "/assets/var2_hard.png",
                "/assets/var3_hard.png");
    }

    private void setupImageVariants(ImageView iv, Runnable onClick, String defaultPath, String hoverPath, String pressedPath) {
        if (iv == null) return;

        Image imgDefault = loadAsset(defaultPath);
        Image imgHover = loadAsset(hoverPath);
        Image imgPressed = loadAsset(pressedPath);

        if (imgDefault != null) iv.setImage(imgDefault);

        iv.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            if (imgHover != null) iv.setImage(imgHover);
            SoundManager.getInstance().playSE(SoundType.HOVER);
        });
        iv.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            if (imgDefault != null) iv.setImage(imgDefault);
        });

        iv.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            if (imgPressed != null) iv.setImage(imgPressed);
        });
        iv.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            if (iv.isHover() && imgHover != null) iv.setImage(imgHover);
            else if (imgDefault != null) iv.setImage(imgDefault);
        });

        if (onClick != null) {
            iv.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> onClick.run());
        }
    }

    private Image loadAsset(String resourcePath) {
        try {
            return new Image(getClass().getResourceAsStream(resourcePath));
        } catch (Exception ex) {
            return null;
        }
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
        SoundManager.getInstance().playMusic(SoundType.TIME_MODE_MUSIC);
        gameContext.setGameMode(GameContext.GameMode.TIME_ATTACK);
        gameContext.reset();
        sceneManager.clearSceneCache();
        sceneManager.switchToScene(SceneManager.GAME_SCENE);
    }

    @FXML
    private void onHardModeClicked() {
        SoundManager.getInstance().playSE(SoundType.CLICK);
        SoundManager.getInstance().playMusic(SoundType.HARD_THEME);
        gameContext.setGameMode(GameContext.GameMode.HARD_MODE);
        gameContext.reset();
        sceneManager.clearSceneCache();
        sceneManager.switchToScene(SceneManager.HARD_GAME_SCENE);
    }

    @FXML
    private void onBackClicked() {
        SoundManager.getInstance().playSE(SoundType.CLICK);
        sceneManager.switchToScene(SceneManager.MAIN_MENU_SCENE);
    }
}
