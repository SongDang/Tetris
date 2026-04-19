package com.se330.tetris.game;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Random;

public class GlitchExplosionEffect {

    public static class CellSnap {
        public final double px, py, size;
        public final Color color;
        public CellSnap(double px, double py, double size, Color color) {
            this.px = px; this.py = py; this.size = size; this.color = color;
        }
    }

    // White-heavy palette — roughly 40% white
    private static final Color[] NEON = {
        Color.WHITE, Color.WHITE, Color.WHITE,
        Color.web("#ff00ff"), Color.web("#00ffff"), Color.web("#ffff00"),
        Color.web("#ff0044"), Color.web("#44ff00"), Color.web("#ff8800"),
        Color.WHITE
    };

    private static final double DURATION     = 0.58;
    private static final double P_CORRUPT    = 0.30;
    private static final double P_CHROMA_END = 0.65;

    private final List<CellSnap> cells;
    private final double cx, cy, blockSize;
    private double elapsed = 0;
    private final Random rng = new Random();

    // Pre-scheduled erratic white pulses (t values where a zone-wide white flash fires)
    private final double[] whitePulses;

    private static class Strip {
        double x, y, w, h;
        Color color;
        double dir;
        double speedMult; // each strip slides at different speed for chaos
    }
    private final Strip[] strips;

    public GlitchExplosionEffect(double cx, double cy, double blockSize, List<CellSnap> cells) {
        this.cx = cx; this.cy = cy; this.blockSize = blockSize;
        this.cells = cells;

        // Random white flash moments scattered across the duration
        whitePulses = new double[]{ rng.nextDouble() * 0.25, rng.nextDouble() * 0.25 + 0.05,
                                    rng.nextDouble() * 0.18 + 0.08 };

        double zoneLeft = cx - blockSize * 1.5;
        double zoneTop  = cy - blockSize * 1.5;
        double zoneW    = blockSize * 3;
        double zoneH    = blockSize * 3;
        // Irregular strip heights for a more chaotic look
        java.util.List<Strip> sl = new java.util.ArrayList<>();
        double y = zoneTop;
        while (y < zoneTop + zoneH) {
            Strip s    = new Strip();
            s.h        = Math.max(1, rng.nextInt((int)(blockSize / 4)) + 1);
            s.x        = zoneLeft;
            s.y        = y;
            s.w        = zoneW;
            s.color    = rng.nextDouble() < 0.45 ? Color.WHITE : NEON[rng.nextInt(NEON.length)];
            s.dir      = rng.nextBoolean() ? 1 : -1;
            s.speedMult = 0.5 + rng.nextDouble() * 1.2;
            sl.add(s);
            y += s.h;
        }
        strips = sl.toArray(new Strip[0]);
    }

    public void update(double dt) { elapsed += dt; }
    public boolean isDone()       { return elapsed >= DURATION; }

    public void render(GraphicsContext gc) {
        double t = elapsed / DURATION;
        if (t >= 1.0) return;

        if (t < P_CORRUPT) {
            renderCorruption(gc, t / P_CORRUPT);
        } else if (t < P_CHROMA_END) {
            renderChromatic(gc, (t - P_CORRUPT) / (P_CHROMA_END - P_CORRUPT));
        } else {
            renderDissolve(gc, (t - P_CHROMA_END) / (1.0 - P_CHROMA_END));
        }

        // Erratic white zone-flash pulses
        for (double pt : whitePulses) {
            double dist = Math.abs(t - pt);
            if (dist < 0.045) {
                double a = (1.0 - dist / 0.045) * 0.88;
                gc.setGlobalAlpha(a);
                gc.setFill(Color.WHITE);
                gc.fillRect(cx - blockSize * 1.6, cy - blockSize * 1.6,
                            blockSize * 3.2, blockSize * 3.2);
                gc.setGlobalAlpha(1.0);
            }
        }

        // Wide white horizontal streaks that fire out left/right — erratic count
        if (t < 0.70) {
            int streakBurst = rng.nextDouble() < 0.35 ? rng.nextInt(4) + 1 : 0;
            gc.setGlobalAlpha(0.55 * (1 - t));
            gc.setFill(Color.WHITE);
            for (int i = 0; i < streakBurst; i++) {
                double sy = cy + (rng.nextDouble() - 0.5) * blockSize * 4;
                double sh = rng.nextDouble() * 3 + 1;
                double sx = cx - blockSize * (3 + rng.nextDouble() * 4);
                double sw = blockSize * (6 + rng.nextDouble() * 5);
                gc.fillRect(sx, sy, sw, sh);
            }
            gc.setGlobalAlpha(1.0);
        }

        // Sporadic noise pixels — burst-based, not smooth
        if (t < 0.88) {
            // Fire in irregular bursts: sometimes 0, sometimes 50+
            int count = rng.nextDouble() < 0.6 ? (int)(rng.nextDouble() * 55) : 0;
            for (int i = 0; i < count; i++) {
                gc.setGlobalAlpha(0.5 + rng.nextDouble() * 0.5);
                gc.setFill(NEON[rng.nextInt(NEON.length)]);
                double nx = cx + (rng.nextDouble() - 0.5) * blockSize * 7;
                double ny = cy + (rng.nextDouble() - 0.5) * blockSize * 7;
                gc.fillRect(nx, ny, rng.nextDouble() * 4 + 1, rng.nextDouble() * 4 + 1);
            }
            gc.setGlobalAlpha(1.0);
        }
    }

    private void renderCorruption(GraphicsContext gc, double t) {
        // Each cell: high chance of white or neon flash
        for (CellSnap cell : cells) {
            double roll = rng.nextDouble();
            Color c;
            if      (roll < 0.30) c = Color.WHITE;
            else if (roll < 0.58) c = NEON[rng.nextInt(NEON.length)];
            else if (roll < 0.68) c = Color.BLACK;
            else                  c = cell.color;
            gc.setFill(c);
            gc.fillRect(cell.px, cell.py, cell.size, cell.size);
        }

        // Occasional full white cell burst (30% chance any frame)
        if (rng.nextDouble() < 0.30 && !cells.isEmpty()) {
            gc.setGlobalAlpha(0.75);
            gc.setFill(Color.WHITE);
            for (CellSnap cell : cells) {
                if (rng.nextBoolean())
                    gc.fillRect(cell.px, cell.py, cell.size, cell.size);
            }
            gc.setGlobalAlpha(1.0);
        }

        // Bands: wider, more white, irregular height
        double zoneX = cx - blockSize * 1.5;
        double zoneW = blockSize * 3;
        int bandCount = 4 + rng.nextInt(5);
        for (int i = 0; i < bandCount; i++) {
            double bandY = cy - blockSize * 1.8 + rng.nextDouble() * blockSize * 3.6;
            double bandH = rng.nextDouble() * blockSize * 0.5 + 2;
            Color  bc    = rng.nextDouble() < 0.50 ? Color.WHITE : NEON[rng.nextInt(NEON.length)];
            gc.setGlobalAlpha(0.4 + rng.nextDouble() * 0.45);
            gc.setFill(bc);
            gc.fillRect(zoneX - blockSize * rng.nextDouble(), bandY, zoneW + blockSize * rng.nextDouble() * 2, bandH);
        }
        gc.setGlobalAlpha(1.0);
    }

    private void renderChromatic(GraphicsContext gc, double t) {
        double offset = t * blockSize * 2.1;
        double alpha  = 0.80 - t * 0.30;

        for (CellSnap cell : cells) {
            double r = cell.color.getRed();
            double g = cell.color.getGreen();
            double b = cell.color.getBlue();

            gc.setGlobalAlpha(alpha * 0.80);
            gc.setFill(Color.color(r, 0, 0));
            gc.fillRect(cell.px - offset, cell.py, cell.size, cell.size);

            gc.setFill(Color.color(0, 0, b));
            gc.fillRect(cell.px + offset, cell.py, cell.size, cell.size);

            gc.setFill(Color.color(0, g, 0));
            gc.fillRect(cell.px, cell.py - offset * 0.35, cell.size, cell.size);

            // Random white flicker on individual cells (25% chance)
            if (rng.nextDouble() < 0.25) {
                gc.setGlobalAlpha(alpha * 0.65);
                gc.setFill(Color.WHITE);
                gc.fillRect(cell.px + (rng.nextDouble() - 0.5) * offset * 0.5,
                            cell.py + (rng.nextDouble() - 0.5) * offset * 0.2,
                            cell.size, cell.size);
            }
        }
        gc.setGlobalAlpha(1.0);

        // Scan-lines: white-heavy, erratic spread
        double zoneX = cx - blockSize * 1.5;
        double zoneW = blockSize * 3;
        int lineCount = 3 + rng.nextInt(5);
        for (int i = 0; i < lineCount; i++) {
            double bandY  = cy - blockSize * 1.5 + rng.nextDouble() * blockSize * 3;
            double shiftX = (rng.nextDouble() - 0.5) * offset * 1.2;
            Color  lc     = rng.nextDouble() < 0.55 ? Color.WHITE : NEON[rng.nextInt(NEON.length)];
            gc.setGlobalAlpha(0.20 + rng.nextDouble() * 0.40);
            gc.setFill(lc);
            gc.fillRect(zoneX + shiftX, bandY, zoneW + Math.abs(shiftX), rng.nextDouble() * 3 + 1);
        }
        gc.setGlobalAlpha(1.0);
    }

    private void renderDissolve(GraphicsContext gc, double t) {
        double maxShift = blockSize * 4.5;
        for (Strip s : strips) {
            double shift = s.dir * t * maxShift * s.speedMult;
            double a     = (1.0 - t) * (s.color == Color.WHITE ? 0.80 : 0.60);
            if (a <= 0) continue;
            gc.setGlobalAlpha(a);
            gc.setFill(s.color);
            gc.fillRect(s.x + shift, s.y, s.w, s.h);
        }
        // White ghost fade over the zone
        gc.setGlobalAlpha((1 - t) * 0.20);
        gc.setFill(Color.WHITE);
        gc.fillRect(cx - blockSize * 1.5, cy - blockSize * 1.5, blockSize * 3, blockSize * 3);
        gc.setGlobalAlpha(1.0);
    }
}
