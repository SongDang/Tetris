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

    private static final double CARD_FIT_WIDTH = 349.0;
    private static final double CARD_FIT_HEIGHT = 664.0;
    private static final double CARD_SOURCE_WIDTH = 403.0;
    private static final double CARD_WIDTH_SCALE = CARD_FIT_WIDTH / CARD_SOURCE_WIDTH;

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

        configureCardImageView(iv);
        if (imgDefault != null) setCardImage(iv, imgDefault);

        iv.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            if (imgHover != null) setCardImage(iv, imgHover);
            SoundManager.getInstance().playSE(SoundType.HOVER);
        });
        iv.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            if (imgDefault != null) setCardImage(iv, imgDefault);
        });

        iv.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            if (imgPressed != null) setCardImage(iv, imgPressed);
        });
        iv.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            if (iv.isHover() && imgHover != null) setCardImage(iv, imgHover);
            else if (imgDefault != null) setCardImage(iv, imgDefault);
        });

        if (onClick != null) {
            iv.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> onClick.run());
        }
    }

    private void configureCardImageView(ImageView iv) {
        iv.setFitWidth(CARD_FIT_WIDTH);
        iv.setFitHeight(CARD_FIT_HEIGHT);
        iv.setPreserveRatio(false);
        iv.setSmooth(false);
        iv.setManaged(false);
        iv.setPickOnBounds(true);
        iv.setLayoutX(0);
        iv.setLayoutY(0);
    }

    private void setCardImage(ImageView iv, Image image) {
        iv.setImage(image);
        iv.setViewport(null);
        double fitWidth = image.getWidth() * CARD_WIDTH_SCALE;
        iv.setFitWidth(fitWidth);
        iv.setFitHeight(CARD_FIT_HEIGHT);
        iv.setPreserveRatio(false);
        iv.setSmooth(false);
        iv.setManaged(false);
        iv.setPickOnBounds(true);
        iv.setLayoutX((CARD_FIT_WIDTH - fitWidth) / 2.0);
        iv.setLayoutY(0);
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
        sceneManager.switchToScene(SceneManager.HARD_INTRO_SCENE);
    }

    @FXML
    private void onBackClicked() {
        SoundManager.getInstance().playSE(SoundType.CLICK);
        sceneManager.switchToScene(SceneManager.MAIN_MENU_SCENE);
    }
}
