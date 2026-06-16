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
    private static final Color STROKE       = Color.web("#1c1c1c");
    private static final double SPECIAL_CHANCE = 0.20;
    private static final double MAX_ROTATION_DEG = 70;
    private static final int    TRAIL_STEPS = 3;
    private static final double TRAIL_SPACING = CELL * 0.5;

    private static final TetrominoType[] TYPES = {
        TetrominoType.I, TetrominoType.O, TetrominoType.T,
        TetrominoType.S, TetrominoType.Z, TetrominoType.J, TetrominoType.L
    };

    private enum SpecialKind { NONE, BOMB, GHOST, TIME, RANDOM }
    private static final SpecialKind[] SPECIALS = {
        SpecialKind.BOMB, SpecialKind.GHOST, SpecialKind.TIME, SpecialKind.RANDOM
    };

    private javafx.scene.image.Image bombSprite;
    private javafx.scene.image.Image ghostSprite;
    private javafx.scene.image.Image timeBlockSprite;
    private double bgElapsed = 0;

    private static final double EXIT_LINGER = 1.0;

    private static class Faller {
        double x, y, speed, alpha;
        double pulseTimer, pulseGlow;
        boolean canPulse, pulsing;
        int[][] shape;
        int maxRow;
        TetrominoType type;
        SpecialKind special;
        double rotationDeg;
        double exitTimer = -1;
    }

    @FXML
    private void initialize() {
        sceneManager = SceneManager.getInstance();
        for (Button btn : new Button[]{gamemodesBtn, settingsBtn, exitBtn})
            if (btn != null) btn.setOnMouseEntered(e -> SoundManager.getInstance().playSE(SoundType.HOVER));

        bgCanvas.widthProperty().bind(mainMenuPane.widthProperty());
        bgCanvas.heightProperty().bind(mainMenuPane.heightProperty());

        bombSprite = new javafx.scene.image.Image(getClass().getResourceAsStream("/assets/bomb.png"));
        ghostSprite = new javafx.scene.image.Image(getClass().getResourceAsStream("/assets/GhostBlock.png"));
        timeBlockSprite = new javafx.scene.image.Image(getClass().getResourceAsStream("/assets/timeblock.png"));

        if (titleLabel != null) {
            titleLabel.setOnMouseEntered(e -> titleHovered = true);
            titleLabel.setOnMouseExited(e -> { titleHovered = false; titleIdleTimer = 0.5; });
        }

        spawnFallers();
        startBgAnimation();
    }

    private void spawnFallers() {
        double w = 1440, h = 1024;
        for (int i = 0; i < FALLER_COUNT; i++) {
            double[] pos = pickSpawnXY(w, -h, 0, null);
            fallers.add(randomFaller(pos[0], pos[1]));
        }
    }

    /** Picks an x/y spawn position that doesn't overlap any other active faller's bounds. */
    private double[] pickSpawnXY(double w, double minY, double maxY, Faller exclude) {
        double size = CELL * 4;
        double x = 0, y = 0;
        for (int attempt = 0; attempt < 20; attempt++) {
            x = rng.nextDouble() * (w - size);
            y = minY + rng.nextDouble() * (maxY - minY);
            boolean clash = false;
            for (Faller other : fallers) {
                if (other == exclude) continue;
                if (x < other.x + size && x + size > other.x
                        && y < other.y + size && y + size > other.y) {
                    clash = true;
                    break;
                }
            }
            if (!clash) break;
        }
        return new double[]{x, y};
    }

    private Faller randomFaller(double x, double y) {
        Faller f = new Faller();
        f.x        = x;
        f.y        = y;
        f.speed    = 160 + rng.nextDouble() * 220;
        f.alpha    = 0.35 + rng.nextDouble() * 0.45;
        f.rotationDeg = (rng.nextDouble() * 2 - 1) * MAX_ROTATION_DEG;

        if (rng.nextDouble() < SPECIAL_CHANCE) {
            f.special = SPECIALS[rng.nextInt(SPECIALS.length)];
            if (f.special == SpecialKind.RANDOM) {
                f.type  = TYPES[rng.nextInt(TYPES.length)];
                f.shape = f.type.getShape(rng.nextInt(4));
            } else {
                f.type  = null;
                f.shape = TetrominoType.BOMB.getShape(0);
            }
        } else {
            f.special = SpecialKind.NONE;
            f.type    = TYPES[rng.nextInt(TYPES.length)];
            f.shape   = f.type.getShape(rng.nextInt(4));
        }

        f.canPulse = rng.nextDouble() < 0.40;
        f.pulseTimer = 1.5 + rng.nextDouble() * 5.0;

        f.maxRow = 0;
        for (int r = 0; r < 4; r++)
            for (int c = 0; c < 4; c++)
                if (f.shape[r][c] == 1) f.maxRow = Math.max(f.maxRow, r);

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
                    f.y += f.speed * dt;

                    if (f.exitTimer >= 0) {
                        f.exitTimer += dt;
                        if (f.exitTimer >= EXIT_LINGER) {
                            double[] pos = pickSpawnXY(w, -(300 + CELL * 4), -(CELL * 4), f);
                            Faller next = randomFaller(pos[0], pos[1]);
                            f.x = next.x; f.y = next.y; f.speed = next.speed;
                            f.alpha = next.alpha; f.shape = next.shape; f.canPulse = next.canPulse;
                            f.type = next.type; f.special = next.special; f.rotationDeg = next.rotationDeg;
                            f.maxRow = next.maxRow; f.exitTimer = -1;
                            f.pulseTimer = next.pulseTimer; f.pulsing = false; f.pulseGlow = 0;
                            continue;
                        }
                    } else if (f.y + (f.maxRow + 1) * CELL > h) {
                        f.exitTimer = 0;
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

                    Color baseColor = switch (f.special) {
                        case TIME -> Color.web("#2dd4d4");
                        case GHOST -> Color.web("#c9a7ff");
                        case BOMB -> TetrominoType.BOMB.getColor();
                        default -> f.type != null ? f.type.getColor() : Color.web("#2e2e2e");
                    };
                    Color drawColor = f.pulseGlow > 0
                        ? baseColor.interpolate(Color.WHITE, f.pulseGlow * 0.80)
                        : baseColor;
                    Color strokeColor = f.pulseGlow > 0
                        ? STROKE.interpolate(Color.WHITE, f.pulseGlow * 0.50)
                        : STROKE;

                    double drawAlpha = f.alpha;

                    gc.setLineWidth(1);

                    double cx = f.x + CELL * 2;
                    double cy = f.y + CELL * 2;

                    gc.setFill(baseColor);
                    for (int t = TRAIL_STEPS; t >= 1; t--) {
                        gc.setGlobalAlpha(drawAlpha * (1.0 - (double) t / (TRAIL_STEPS + 1)) * 0.55);
                        gc.save();
                        gc.translate(0, -t * TRAIL_SPACING);
                        gc.translate(cx, cy);
                        gc.rotate(f.rotationDeg);
                        gc.translate(-cx, -cy);
                        for (int r = 0; r < 4; r++)
                            for (int c = 0; c < 4; c++)
                                if (f.shape[r][c] == 1) {
                                    double px = f.x + c * CELL;
                                    double py = f.y + r * CELL;
                                    gc.fillRect(px, py, CELL, CELL);
                                }
                        gc.restore();
                    }

                    gc.save();
                    gc.translate(cx, cy);
                    gc.rotate(f.rotationDeg);
                    gc.translate(-cx, -cy);

                    gc.setGlobalAlpha(drawAlpha);
                    for (int r = 0; r < 4; r++)
                        for (int c = 0; c < 4; c++)
                            if (f.shape[r][c] == 1) {
                                double px = f.x + c * CELL;
                                double py = f.y + r * CELL;
                                switch (f.special) {
                                    case BOMB -> gc.drawImage(bombSprite, px, py, CELL, CELL);
                                    case GHOST -> gc.drawImage(ghostSprite, px, py, CELL, CELL);
                                    case TIME -> gc.drawImage(timeBlockSprite, px, py, CELL, CELL);
                                    case RANDOM -> {
                                        double erratic = Math.sin(bgElapsed * 23.0 + f.x)
                                                * Math.sin(bgElapsed * 11.7 + f.y)
                                                * Math.sin(bgElapsed * 5.3);
                                        double shift = 2.0 + Math.abs(erratic) * 6.0;
                                        gc.setFill(Color.color(1, 0, 0, 0.35));
                                        gc.fillRect(px - shift, py, CELL, CELL);
                                        gc.setFill(Color.color(0, 0.4, 1, 0.35));
                                        gc.fillRect(px + shift, py, CELL, CELL);
                                        gc.setFill(drawColor);
                                        gc.setStroke(strokeColor);
                                        gc.fillRect(px, py, CELL, CELL);
                                        gc.strokeRect(px, py, CELL, CELL);
                                    }
                                    default -> {
                                        gc.setFill(drawColor);
                                        gc.setStroke(strokeColor);
                                        gc.fillRect(px, py, CELL, CELL);
                                        gc.strokeRect(px, py, CELL, CELL);
                                    }
                                }
                            }
                    gc.restore();
                }
                gc.setGlobalAlpha(1.0);
                bgElapsed += dt;
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
