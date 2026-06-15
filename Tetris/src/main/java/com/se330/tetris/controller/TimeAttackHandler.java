package com.se330.tetris.controller;

import com.se330.tetris.service.GameContext;
import com.se330.tetris.util.SoundType;
import com.se330.tetris.service.SoundManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Manages time attack countdown and freeze power-up logic.
 */
class TimeAttackHandler {

    private static final int TIME_ATTACK_START_SECONDS = 120;
    private static final int FREEZE_DURATION_SECONDS = 5;

    private final GameContext gameContext;
    private final Label timeLabel;
    private final Label freezeLabel;
    private final Label bombLabel;
    private final VBox timePanel;
    private final VBox bombPanel;
    private final Runnable onGameOver;

    private double timeRemainingSeconds = 0;
    private Timeline timeAttackTimeline;

    boolean isFreezeActive = false;
    private int freezeRemainingSeconds = 0;
    private Timeline freezeTimeline;

    private int bombsRemaining = 3;

    private boolean gamePausedRef = false;
    private boolean isGameOverRef = false;
    private Runnable onFreezeEnd = null;

    TimeAttackHandler(GameContext gameContext, Label timeLabel, Label freezeLabel, Label bombLabel,
                      VBox timePanel, VBox bombPanel, Runnable onGameOver) {
        this.gameContext = gameContext;
        this.timeLabel = timeLabel;
        this.freezeLabel = freezeLabel;
        this.bombLabel = bombLabel;
        this.timePanel = timePanel;
        this.bombPanel = bombPanel;
        this.onGameOver = onGameOver;
    }

    void setup() {
        if (gameContext.getGameMode() != GameContext.GameMode.TIME_ATTACK) {
            if (timePanel != null) { timePanel.setVisible(false); timePanel.setManaged(false); }
            if (freezeLabel != null) { freezeLabel.setVisible(false); freezeLabel.setManaged(false); }
            return;
        }

        if (gameContext.getGameMode() == GameContext.GameMode.TIME_ATTACK) {
            if (bombPanel != null) {
                bombPanel.setVisible(true);
                bombPanel.setManaged(true);
                bombLabel.setText("💣 x" + bombsRemaining);
            }
        } else {
            if (bombPanel != null) {
                bombPanel.setVisible(false);
                bombPanel.setManaged(false);
            }
        }

        timeRemainingSeconds = TIME_ATTACK_START_SECONDS;
        updateTimeLabel();
        if (timePanel != null) { timePanel.setVisible(true); timePanel.setManaged(true); }
        if (freezeLabel != null) { freezeLabel.setVisible(false); freezeLabel.setManaged(false); }

        timeAttackTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> tickTimeAttack()));
        timeAttackTimeline.setCycleCount(Timeline.INDEFINITE);
        timeAttackTimeline.play();
    }

    void setGameState(boolean paused, boolean gameOver) {
        gamePausedRef = paused;
        isGameOverRef = gameOver;
    }

    void pause() {
        if (timeAttackTimeline != null) timeAttackTimeline.pause();
    }

    void resume() {
        if (timeAttackTimeline != null) timeAttackTimeline.play();
    }

    void applyBonus(int linesCleared) {
        if (gameContext.getGameMode() != GameContext.GameMode.TIME_ATTACK || linesCleared <= 0) return;
        timeRemainingSeconds += (1 << linesCleared);
        updateTimeLabel();
    }

    void triggerFreezeIfNeeded(boolean hasTimeBlock) {
        if (!hasTimeBlock || gameContext.getGameMode() != GameContext.GameMode.TIME_ATTACK) return;
        if (isFreezeActive) return;
        startFreeze();
    }

    private void startFreeze() {
        isFreezeActive = true;
        freezeRemainingSeconds = FREEZE_DURATION_SECONDS;
        updateFreezeLabel();
        if (timeAttackTimeline != null) timeAttackTimeline.pause();
        if (freezeTimeline == null) {
            freezeTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> tickFreeze()));
            freezeTimeline.setCycleCount(Timeline.INDEFINITE);
        }
        freezeTimeline.playFromStart();
    }

    private void tickFreeze() {
        if (!isFreezeActive) return;
        freezeRemainingSeconds = Math.max(0, freezeRemainingSeconds - 1);
        updateFreezeLabel();
        if (freezeRemainingSeconds <= 0) endFreeze();
    }

    void setOnFreezeEnd(Runnable callback) { this.onFreezeEnd = callback; }

    double getTimeRemaining() { return timeRemainingSeconds; }

    private void endFreeze() {
        isFreezeActive = false;
        if (onFreezeEnd != null) onFreezeEnd.run();
        if (freezeTimeline != null) freezeTimeline.stop();
        if (timeAttackTimeline != null && gameContext.getGameMode() == GameContext.GameMode.TIME_ATTACK)
            timeAttackTimeline.play();
        if (freezeLabel != null) { freezeLabel.setVisible(false); freezeLabel.setManaged(false); }
    }

    void stopAll() {
        isFreezeActive = false;
        if (timeAttackTimeline != null) timeAttackTimeline.stop();
        if (freezeTimeline != null) freezeTimeline.stop();
        if (freezeLabel != null) { freezeLabel.setVisible(false); freezeLabel.setManaged(false); }
    }

    private void tickTimeAttack() {
        if (gamePausedRef || isGameOverRef) return;
        timeRemainingSeconds = Math.max(0, timeRemainingSeconds - 1);
        updateTimeLabel();
        if (timeRemainingSeconds <= 0) handleTimeout();
    }

    private void handleTimeout() {
        if (isGameOverRef) return;
        isGameOverRef = true;
        stopAll();
        SoundManager.getInstance().playSE(SoundType.GAME_OVER);
        SoundManager.getInstance().stopMusic();
        onGameOver.run();
    }

    private void updateTimeLabel() {
        if (timeLabel == null) return;
        int total = (int) Math.max(0, Math.ceil(timeRemainingSeconds));
        timeLabel.setText(String.format("%02d:%02d", total / 60, total % 60));
    }

    private void updateFreezeLabel() {
        if (freezeLabel == null) return;
        freezeLabel.setVisible(true);
        freezeLabel.setManaged(true);
        freezeLabel.setText("FREEZE: " + freezeRemainingSeconds + "s");
    }
}
