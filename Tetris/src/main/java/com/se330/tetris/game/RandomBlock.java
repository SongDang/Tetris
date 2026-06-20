package com.se330.tetris.game;

import com.se330.tetris.service.SoundManager;
import com.se330.tetris.util.SoundType;
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
    private volatile long lastChangeMs = System.currentTimeMillis();
    private volatile TetrominoType nextType;
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
        this.nextType = pickPreview(initialType);
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

    public TetrominoType getPreviewType() {
        return nextType;
    }

    public double getMorphProgress() {
        if (!transforming) return 0;
        double elapsed = System.currentTimeMillis() - lastChangeMs;
        return Math.min(1.0, elapsed / intervalMs);
    }

    private TetrominoType pickPreview(TetrominoType exclude) {
        List<TetrominoType> candidates = new ArrayList<>(ALLOWED_TYPES);
        candidates.remove(exclude);
        java.util.Collections.shuffle(candidates, rng);
        return candidates.isEmpty() ? exclude : candidates.get(0);
    }

    private void changeTypeIfPossible() {
        if (!transforming) {
            return;
        }
        // Transform to the pre-determined nextType if valid, else pick a new one
        TetrominoType target = nextType;
        if (target == null || (typeValidator != null && !typeValidator.test(this, target))) {
            List<TetrominoType> candidates = new ArrayList<>(ALLOWED_TYPES);
            candidates.remove(getType());
            java.util.Collections.shuffle(candidates, rng);
            target = null;
            for (TetrominoType candidate : candidates) {
                if (typeValidator == null || typeValidator.test(this, candidate)) {
                    target = candidate;
                    break;
                }
            }
        }
        if (target != null) {
            setType(target);
            lastChangeMs = System.currentTimeMillis();
            nextType = pickPreview(target);

            SoundManager.getInstance().playSE(SoundType.RANDOM_PIECE);
        }
    }
}
