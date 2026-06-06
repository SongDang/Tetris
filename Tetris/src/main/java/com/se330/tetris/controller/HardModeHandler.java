package com.se330.tetris.controller;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Manages hard mode mechanics: blackout state machine and flavour text.
 */
public class HardModeHandler {

    public enum BlackoutState { NORMAL, FLICKER, PRE_BLACKOUT, BLACKOUT, POST_FLICKER }

    private static final double BLACKOUT_AUTO_DROP = 1.5;
    private static final double FLAVOUR_BASE_CD = 7.0;
    private static final double FLAVOUR_CHAR_DELAY = 0.055;

    private static final String[] FLAVOUR_PLACE = {
            "Bold choice. truly.", "That's one way to do it.",
            "You sure about that one.", "The audacity."
    };
    private static final String[] FLAVOUR_CLEAR = {
            "Fine. you get one.", "Don't let it go to your head.",
            "Was that on purpose? be honest."
    };
    private static final String[] FLAVOUR_TETRIS = {
            "Okay. i see you.", "Don't make it weird by celebrating.",
            "Four lines. i'll allow it."
    };
    private static final String[] FLAVOUR_STACK = {
            "You did this to yourself.", "This is a you problem.",
            "I'd look away but i can't.", "Are you okay? genuinely asking."
    };
    private static final String[] FLAVOUR_COMBO = {
            "Oh so you can do this.", "Where was this ten moves ago.",
            "Keep going. i dare you.", "Now you're showing off."
    };
    private static final String[] FLAVOUR_POST_BLACKOUT = {
            "The lights return. assess the damage.",
            "Welcome back. the stack didn't wait.",
            "Darkness lifted. problems haven't.",
            "You survived the blackout. barely counts.",
            "The board kept going without you.",
            "Light's back. make it count this time."
    };

    // Blackout state
    public BlackoutState blackoutState = BlackoutState.NORMAL;
    private double blackoutTimer = 0;
    private double blackoutDuration = 0;
    public double blackoutFlickerTimer = 0;
    public double blackoutDropTimer = 0;

    // Flavour text state
    public boolean lightsOutMode = false;
    public boolean darknessLoomsMode = false;
    public double flavourWaveTime = 0;
    public double flavourTypeElapsed = 0;
    private double flavourCooldown = 0;
    public int placesSinceFlavour = 0;
    public final List<Label> flavourChars = new ArrayList<>();
    public HBox flavourHBox;
    private StackPane flavourPane;

    private final Random rng;
    private final Runnable onMusicPause;
    private final Runnable onMusicResume;

    public HardModeHandler(Random rng, Runnable onMusicPause, Runnable onMusicResume) {
        this.rng = rng;
        this.onMusicPause = onMusicPause;
        this.onMusicResume = onMusicResume;
    }

    // --- Initialization ---

    public void buildFlavourChars(Label flavourLabel) {
        if (flavourLabel == null) return;
        flavourLabel.setVisible(false);
        flavourPane = (StackPane) flavourLabel.getParent();
        if (flavourPane == null) return;
        flavourHBox = new HBox(0);
        flavourHBox.setAlignment(Pos.CENTER);
        flavourPane.getChildren().add(flavourHBox);
        rebuildFlavourChars(flavourLabel.getText());
    }

    // --- Per-frame update ---

    public void update(double dt) {
        flavourWaveTime += dt;
        flavourTypeElapsed += dt;
        flavourCooldown -= dt;
        updateBlackout(dt);
    }

    /** Returns true if auto hard-drop should fire this frame. */
    public boolean tickBlackoutDrop(double dt) {
        if (blackoutState != BlackoutState.BLACKOUT) return false;
        blackoutDropTimer += dt;
        if (blackoutDropTimer >= BLACKOUT_AUTO_DROP) {
            blackoutDropTimer = 0;
            return true;
        }
        return false;
    }

    private void updateBlackout(double dt) {
        blackoutTimer += dt;
        switch (blackoutState) {
            case NORMAL -> {
                if (blackoutTimer >= 20.0) {
                    blackoutTimer = 0;
                    blackoutState = BlackoutState.FLICKER;
                    blackoutDuration = 0.3;
                    blackoutFlickerTimer = 0;
                    darknessLoomsMode = true;
                    rebuildFlavourChars("Darkness looms.", "#40A3FF");
                    flavourCooldown = FLAVOUR_BASE_CD;
                }
            }
            case FLICKER -> {
                blackoutDuration -= dt;
                blackoutFlickerTimer += dt * 10.0;
                if (blackoutDuration <= 0) {
                    blackoutState = BlackoutState.PRE_BLACKOUT;
                    blackoutDuration = 5.0;
                }
            }
            case PRE_BLACKOUT -> {
                blackoutDuration -= dt;
                if (blackoutDuration <= 0) {
                    blackoutState = BlackoutState.BLACKOUT;
                    blackoutDuration = 5.0;
                    blackoutDropTimer = 0;
                    onMusicPause.run();
                    setLightsOut();
                }
            }
            case BLACKOUT -> {
                blackoutDuration -= dt;
                if (blackoutDuration <= 0) {
                    blackoutState = BlackoutState.POST_FLICKER;
                    blackoutDuration = 0.3;
                    blackoutFlickerTimer = 0;
                    onMusicResume.run();
                    clearLightsOut();
                }
            }
            case POST_FLICKER -> {
                blackoutDuration -= dt;
                blackoutFlickerTimer += dt * 10.0;
                if (blackoutDuration <= 0) {
                    blackoutState = BlackoutState.NORMAL;
                    blackoutTimer = 0;
                }
            }
        }
    }

    /** Đã sửa đổi logic: Chỉ cho phép kết thúc trạng thái Blackout sớm nếu đã bước vào giai đoạn hồi đèn cuối cùng */
    public void resetBlackoutOnSpawn() {
        if (blackoutState == BlackoutState.POST_FLICKER) {
            blackoutState = BlackoutState.NORMAL;
            blackoutDuration = 0;
            blackoutFlickerTimer = 0;
            blackoutTimer = 0;
        }
    }

    // --- Flavour text ---

    public boolean trySetFlavourPlace(int stackTopRow) {
        boolean isHigh = stackTopRow < 7;
        return trySetFlavour(isHigh ? FLAVOUR_STACK : FLAVOUR_PLACE, 0.0);
    }

    public boolean trySetFlavourClear() { return trySetFlavour(FLAVOUR_CLEAR, 0.0); }

    public boolean trySetFlavourTetris() { return trySetFlavour(FLAVOUR_TETRIS, FLAVOUR_BASE_CD); }

    public boolean trySetFlavourCombo() { return trySetFlavour(FLAVOUR_COMBO, 2.0); }

    private boolean trySetFlavour(String[] pool, double threshold) {
        if (lightsOutMode || flavourCooldown > threshold || flavourHBox == null) return false;
        rebuildFlavourChars(pool[rng.nextInt(pool.length)]);
        flavourCooldown = FLAVOUR_BASE_CD;
        placesSinceFlavour = 0;
        return true;
    }

    private void setLightsOut() {
        if (flavourHBox == null) return;
        lightsOutMode = true;
        darknessLoomsMode = false;
        flavourCooldown = Double.MAX_VALUE;
        flavourChars.clear();
        flavourHBox.getChildren().clear();
        for (char c : "LIGHTS OUT".toCharArray()) {
            Label l = new Label(String.valueOf(c));
            l.setStyle("-fx-font-family: 'VT323'; -fx-font-size: 40; -fx-text-fill: #FF0000;");
            l.setOpacity(1.0);
            flavourChars.add(l);
            flavourHBox.getChildren().add(l);
        }
    }

    private void clearLightsOut() {
        lightsOutMode = false;
        if (flavourHBox != null) {
            flavourHBox.setTranslateX(0);
            flavourHBox.setTranslateY(0);
        }
        rebuildFlavourChars(FLAVOUR_POST_BLACKOUT[rng.nextInt(FLAVOUR_POST_BLACKOUT.length)]);
        flavourCooldown = FLAVOUR_BASE_CD;
    }

    private void rebuildFlavourChars(String text) {
        darknessLoomsMode = false;
        rebuildFlavourChars(text, null);
    }

    private void rebuildFlavourChars(String text, String hexColor) {
        if (flavourHBox == null) return;
        flavourChars.clear();
        flavourHBox.getChildren().clear();
        String baseColor = (hexColor != null) ? hexColor : "#00ff00"; // Mặc định xanh neon retro nếu không truyền màu
        for (char c : text.toCharArray()) {
            Label l = new Label(String.valueOf(c));
            l.setStyle("-fx-font-family: 'VT323'; -fx-font-size: 40; -fx-text-fill: " + baseColor + ";");
            l.setOpacity(0);
            flavourChars.add(l);
            flavourHBox.getChildren().add(l);
        }
        flavourTypeElapsed = 0;
    }

    // --- Render-time flavour update (called each frame during render) ---

    public void renderFlavourText(Random renderRng) {
        if (lightsOutMode) {
            renderLightsOutFlavour(renderRng);
        } else if (darknessLoomsMode) {
            renderDarknessLoomsFlavour(renderRng);
        } else {
            renderNormalFlavour(renderRng);
        }
    }

    private void renderLightsOutFlavour(Random renderRng) {
        if (flavourHBox != null) {
            flavourHBox.setTranslateX((renderRng.nextDouble() - 0.5) * 8);
            flavourHBox.setTranslateY((renderRng.nextDouble() - 0.5) * 5);
        }
        for (Label l : flavourChars) {
            if (renderRng.nextDouble() < 0.12) {
                l.setOpacity(renderRng.nextDouble() < 0.4 ? 0.0 : 1.0);
            } else {
                l.setOpacity(1.0);
            }
            String col = renderRng.nextDouble() < 0.06
                    ? (renderRng.nextBoolean() ? "#ffffff" : "#880000")
                    : "#FF0000";
            l.setStyle("-fx-font-family: 'VT323'; -fx-font-size: 40; -fx-text-fill: " + col + ";");
        }
    }

    private void renderDarknessLoomsFlavour(Random renderRng) {
        if (flavourHBox != null) { flavourHBox.setTranslateX(0); flavourHBox.setTranslateY(0); }
        int nVisible = Math.min(flavourChars.size(), (int) (flavourTypeElapsed / (FLAVOUR_CHAR_DELAY * 1.8)));
        for (int i = 0; i < flavourChars.size(); i++) {
            Label l = flavourChars.get(i);
            boolean visible = i < nVisible;
            if (visible && renderRng.nextDouble() < 0.04)
                l.setOpacity(0.4 + renderRng.nextDouble() * 0.3);
            else
                l.setOpacity(visible ? 1.0 : 0.0);
            if (visible) l.setTranslateY(Math.sin(flavourWaveTime * 1.2 + i * 0.45) * 5.0);
            l.setStyle("-fx-font-family: 'VT323'; -fx-font-size: 40; -fx-text-fill: #40A3FF;");
        }
    }

    private void renderNormalFlavour(Random renderRng) {
        if (flavourHBox != null) { flavourHBox.setTranslateX(0); flavourHBox.setTranslateY(0); }
        int nVisible = Math.min(flavourChars.size(), (int) (flavourTypeElapsed / FLAVOUR_CHAR_DELAY));
        for (int i = 0; i < flavourChars.size(); i++) {
            Label l = flavourChars.get(i);
            boolean visible = i < nVisible;
            l.setOpacity(visible ? 1.0 : 0.0);
            if (visible) l.setTranslateY(Math.sin(flavourWaveTime * 2.5 + i * 0.45) * 2.5);
            // Đảm bảo ép font VT323 để không bị vỡ giao diện chữ châm biếm thông thường
            l.setStyle("-fx-font-family: 'VT323'; -fx-font-size: 40; -fx-text-fill: #00ff00;");
        }
    }
}