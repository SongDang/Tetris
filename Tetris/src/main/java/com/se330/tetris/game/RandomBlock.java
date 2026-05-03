package com.se330.tetris.game;

import javafx.application.Platform;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;

public class RandomBlock extends Piece {
    private static final EnumSet<TetrominoType> ALLOWED_TYPES = EnumSet.of(
            TetrominoType.I,
            TetrominoType.O,
            TetrominoType.T,
            TetrominoType.S,
            TetrominoType.Z,
            TetrominoType.L,
            TetrominoType.J);

    private final Random rng = new Random();
    private final long intervalMs;
    private final long indicatorPeriodMs;
    private ScheduledExecutorService scheduler;
    private volatile boolean transforming;
    private volatile long lastIndicatorToggleMs;
    private volatile boolean indicatorOn;
    private BiPredicate<RandomBlock, TetrominoType> typeValidator;

    public RandomBlock(TetrominoType initialType, int x, int y, long intervalMs) {
        this(initialType, x, y, intervalMs, 180L);
    }

    public RandomBlock(TetrominoType initialType, int x, int y, long intervalMs, long indicatorPeriodMs) {
        super(initialType, x, y);
        this.intervalMs = Math.max(250L, intervalMs);
        this.indicatorPeriodMs = Math.max(60L, indicatorPeriodMs);
        this.transforming = false;
        this.indicatorOn = true;
        this.lastIndicatorToggleMs = System.currentTimeMillis();
    }

    public void setTypeValidator(BiPredicate<RandomBlock, TetrominoType> typeValidator) {
        this.typeValidator = typeValidator;
    }

    public void startTimer() {
        if (transforming) {
            return;
        }
        transforming = true;
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> Platform.runLater(this::changeTypeIfPossible),
                intervalMs, intervalMs, TimeUnit.MILLISECONDS);
    }

    public void lockBlock() {
        transforming = false;
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    public boolean isTransforming() {
        return transforming;
    }

    public boolean isIndicatorOn() {
        if (!transforming) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now - lastIndicatorToggleMs >= indicatorPeriodMs) {
            indicatorOn = !indicatorOn;
            lastIndicatorToggleMs = now;
        }
        return indicatorOn;
    }

    private void changeTypeIfPossible() {
        if (!transforming) {
            return;
        }

        TetrominoType current = getType();
        List<TetrominoType> candidates = new ArrayList<>(ALLOWED_TYPES);
        candidates.remove(current);
        java.util.Collections.shuffle(candidates, rng);

        for (TetrominoType candidate : candidates) {
            if (typeValidator == null || typeValidator.test(this, candidate)) {
                setType(candidate);
                return;
            }
        }
    }
}
