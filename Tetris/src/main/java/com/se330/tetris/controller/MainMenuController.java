package com.se330.tetris.controller;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import com.se330.tetris.game.TetrominoType;
import com.se330.tetris.service.SceneManager;
import com.se330.tetris.service.SoundManager;
import com.se330.tetris.util.SoundType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainMenuController {

    @FXML private AnchorPane mainMenuPane;
    @FXML private AnchorPane mainContent;
    @FXML private AnchorPane settingsOverlay;
    @FXML private Canvas     bgCanvas;
    @FXML private Button     gamemodesBtn;
    @FXML private Button     settingsBtn;
    @FXML private Button     exitBtn;
    @FXML private Label      titleLabel;
    @FXML private SettingsController settingsPopupController;

    private SceneManager sceneManager;
    private AnimationTimer bgTimer;
    private long bgLastNano = 0;
    private final List<Faller> fallers = new ArrayList<>();
    private final Random rng = new Random();

    // title random-effect state
    private enum TitleFx { NONE, GLITCH, BLINK }
    private TitleFx titleFx        = TitleFx.NONE;
    private double  titleIdleTimer = 3.0;
    private double  titleFxRemain  = 0;
    private double  glitchTick     = 0;
    private double  blinkTick      = 0;
    private boolean titleHovered   = false;

    private static final int   CELL         = 26;
    private static final int   FALLER_COUNT = 28;
    private static final Color FILL         = Color.web("#2e2e2e");
    private static final Color STROKE       = Color.web("#1c1c1c");

    private static final TetrominoType[] TYPES = {
        TetrominoType.I, TetrominoType.O, TetrominoType.T,
        TetrominoType.S, TetrominoType.Z, TetrominoType.J, TetrominoType.L
    };

    private static class Faller {
        double x, y, speed, alpha, fadeRate;
        double pulseTimer, pulseGlow;
        boolean canPulse, pulsing;
        int[][] shape;
    }

    @FXML
    private void initialize() {
        sceneManager = SceneManager.getInstance();
        for (Button btn : new Button[]{gamemodesBtn, settingsBtn, exitBtn})
            if (btn != null) btn.setOnMouseEntered(e -> SoundManager.getInstance().playSE(SoundType.HOVER));

        bgCanvas.widthProperty().bind(mainMenuPane.widthProperty());
        bgCanvas.heightProperty().bind(mainMenuPane.heightProperty());

        if (titleLabel != null) {
            titleLabel.setOnMouseEntered(e -> titleHovered = true);
            titleLabel.setOnMouseExited(e -> { titleHovered = false; titleIdleTimer = 0.5; });
        }

        spawnFallers();
        startBgAnimation();
    }

    private void spawnFallers() {
        double w = 1440, h = 1024;
        for (int i = 0; i < FALLER_COUNT; i++)
            fallers.add(randomFaller(rng.nextDouble() * (w - CELL * 4),
                                     rng.nextDouble() * h - h));
    }

    private Faller randomFaller(double x, double y) {
        Faller f = new Faller();
        f.x        = x;
        f.y        = y;
        f.speed    = 90 + rng.nextDouble() * 120;
        f.alpha    = 0.35 + rng.nextDouble() * 0.45;
        f.fadeRate = 0.04 + rng.nextDouble() * 0.18;
        f.shape    = TYPES[rng.nextInt(TYPES.length)].getShape(rng.nextInt(4));
        f.canPulse = rng.nextDouble() < 0.40;
        f.pulseTimer = 1.5 + rng.nextDouble() * 5.0;
        return f;
    }

    private void startBgAnimation() {
        GraphicsContext gc = bgCanvas.getGraphicsContext2D();
        bgLastNano = System.nanoTime();
        bgTimer = new AnimationTimer() {
            @Override public void handle(long now) {
                double dt = Math.min((now - bgLastNano) / 1_000_000_000.0, 0.05);
                bgLastNano = now;

                double w = bgCanvas.getWidth();
                double h = bgCanvas.getHeight();
                gc.clearRect(0, 0, w, h);

                for (Faller f : fallers) {
                    f.y     += f.speed * dt;
                    f.alpha -= f.fadeRate * dt;

                    if (f.alpha <= 0) {
                        Faller next = randomFaller(rng.nextDouble() * (w - CELL * 4),
                                                   -(rng.nextDouble() * 300 + CELL * 4));
                        f.x = next.x; f.y = next.y; f.speed = next.speed;
                        f.alpha = next.alpha; f.fadeRate = next.fadeRate;
                        f.shape = next.shape; f.canPulse = next.canPulse;
                        f.pulseTimer = next.pulseTimer; f.pulsing = false; f.pulseGlow = 0;
                        continue;
                    }

                    if (f.canPulse) {
                        if (!f.pulsing) {
                            f.pulseTimer -= dt;
                            if (f.pulseTimer <= 0) { f.pulsing = true; f.pulseGlow = 1.0; }
                        } else {
                            f.pulseGlow -= dt * 2.8;
                            if (f.pulseGlow <= 0) {
                                f.pulsing = false; f.pulseGlow = 0;
                                f.pulseTimer = 1.5 + rng.nextDouble() * 5.0;
                            }
                        }
                    }

                    Color drawColor = f.pulseGlow > 0
                        ? FILL.interpolate(Color.WHITE, f.pulseGlow * 0.80)
                        : FILL;
                    Color strokeColor = f.pulseGlow > 0
                        ? STROKE.interpolate(Color.WHITE, f.pulseGlow * 0.50)
                        : STROKE;

                    gc.setGlobalAlpha(f.alpha);
                    gc.setFill(drawColor);
                    gc.setStroke(strokeColor);
                    gc.setLineWidth(1);
                    for (int r = 0; r < 4; r++)
                        for (int c = 0; c < 4; c++)
                            if (f.shape[r][c] == 1) {
                                double px = f.x + c * CELL;
                                double py = f.y + r * CELL;
                                gc.fillRect(px, py, CELL, CELL);
                                gc.strokeRect(px, py, CELL, CELL);
                            }
                }
                gc.setGlobalAlpha(1.0);
                updateTitleFx(dt);
            }
        };
        bgTimer.start();
    }

    @FXML
    private void onGamemodesClicked() {
        SoundManager.getInstance().playSE(SoundType.CLICK);
        sceneManager.switchToScene(SceneManager.GAMEMODES_SCENE);
    }

    @FXML
    private void onSettingsClicked() {
        SoundManager.getInstance().playSE(SoundType.CLICK);
        if (settingsPopupController != null)
            settingsPopupController.setScoreVisible(false);
        settingsOverlay.setManaged(true);
        settingsOverlay.setVisible(true);
    }

    @FXML
    private void onSettingsOverlayBackdropClicked() {
        SoundManager.getInstance().playSE(SoundType.CLICK);
        settingsOverlay.setVisible(false);
        settingsOverlay.setManaged(false);
    }

    @FXML
    private void onExitClicked() {
        SoundManager.getInstance().playSE(SoundType.CLICK);
        System.exit(0);
    }

    private void updateTitleFx(double dt) {
        if (titleLabel == null) return;
        if (titleFx == TitleFx.NONE) {
            if (titleHovered) {
                startTitleFx();
            } else {
                titleIdleTimer -= dt;
                if (titleIdleTimer <= 0) {
                    if (rng.nextDouble() < 0.55) startTitleFx();
                    else titleIdleTimer = 2.5 + rng.nextDouble() * 4.5;
                }
            }
            return;
        }
        titleFxRemain -= dt;
        if (titleFxRemain <= 0) { resetTitle(); return; }
        switch (titleFx) {
            case GLITCH -> tickGlitch(dt);
            case BLINK  -> { blinkTick += dt; titleLabel.setOpacity(Math.sin(blinkTick * 18) > 0 ? 1.0 : 0.05); }
            default -> {}
        }
    }

    private void startTitleFx() {
        TitleFx[] pool = { TitleFx.GLITCH, TitleFx.BLINK };
        titleFx = pool[rng.nextInt(pool.length)];
        titleFxRemain = switch (titleFx) {
            case GLITCH -> 0.15 + rng.nextDouble() * 0.55;
            case BLINK  -> 0.4 + rng.nextDouble() * 1.2;
            default     -> 1.0;
        };
        if (titleFx == TitleFx.BLINK) blinkTick = 0;
    }

    private void tickGlitch(double dt) {
        glitchTick -= dt;
        if (glitchTick <= 0) {
            glitchTick = 0.03 + rng.nextDouble() * 0.07;
            titleLabel.setTranslateX((rng.nextDouble() - 0.5) * 30);
            titleLabel.setTranslateY((rng.nextDouble() - 0.5) * 14);
            String[] cols = { "#00ff88", "#ff0033", "#00eeff", "#ffffff", "#ffff00" };
            titleLabel.setStyle(rng.nextDouble() < 0.6
                ? "-fx-text-fill:" + cols[rng.nextInt(cols.length)] + ";"
                : "");
        }
    }

    private void resetTitle() {
        titleFx = TitleFx.NONE;
        titleIdleTimer = 2.5 + rng.nextDouble() * 4.5;
        titleLabel.setTranslateX(0); titleLabel.setTranslateY(0);
        titleLabel.setScaleX(1);     titleLabel.setScaleY(1);
        titleLabel.setRotate(0);     titleLabel.setOpacity(1.0);
        titleLabel.setStyle("");
    }
}
