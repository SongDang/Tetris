package com.se330.tetris.game;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LevelUpEffect {

    private enum Phase { SLAM, CRACK, SHATTER, DONE }

    // Phase durations
    private static final double SLAM_DUR   = 0.15;
    private static final double OVER_DUR   = 0.08;
    private static final double SETTL_DUR  = 0.05;
    private static final double HOLD_DUR   = 0.20;
    private static final double CRACK_DUR  = 0.15;
    private static final double SHARD_DUR  = 1.10;

    // Absolute phase start times
    private static final double T_IMPACT  = SLAM_DUR;
    private static final double T_BOUNCE  = T_IMPACT + OVER_DUR;
    private static final double T_SETTLE  = T_BOUNCE + SETTL_DUR;
    private static final double T_CRACK   = T_SETTLE + HOLD_DUR;
    private static final double T_SHATTER = T_CRACK  + CRACK_DUR;
    private static final double T_DONE    = T_SHATTER + SHARD_DUR;

    private static final int    BASE_FONT  = 90;
    // Approximate bounds of "LV X" rendered at BASE_FONT in VT323
    private static final double TEXT_W     = 150;
    private static final double TEXT_H     = 62;

    private final int      level;
    private final Runnable onShake;
    private final Random   rng = new Random();

    private double  elapsed     = 0;
    private Phase   phase       = Phase.SLAM;
    private boolean shakeFired  = false;

    // Crack paths: list of (x,y) points per crack, generated lazily with canvas coords
    private final List<List<double[]>> cracks = new ArrayList<>();

    // Shatter shards
    private static class Shard {
        double x, y, vx, vy, alpha, w, h;
        Color  color;
    }
    private final List<Shard> shards = new ArrayList<>();

    public LevelUpEffect(int level, Runnable onShake) {
        this.level   = level;
        this.onShake = onShake;
    }

    // ── update ────────────────────────────────────────────────────────────────

    public void update(double dt) {
        if (phase == Phase.DONE) return;
        elapsed += dt;

        if (!shakeFired && elapsed >= T_IMPACT) {
            shakeFired = true;
            onShake.run();
        }

        if      (elapsed < T_CRACK)   phase = Phase.SLAM;
        else if (elapsed < T_SHATTER) phase = Phase.CRACK;
        else if (elapsed < T_DONE)    phase = Phase.SHATTER;
        else                          phase = Phase.DONE;

        if (phase == Phase.SHATTER) {
            for (Shard s : shards) {
                if (s.alpha <= 0) continue;
                s.x  += s.vx * dt;
                s.y  += s.vy * dt;
                s.vy += 350 * dt;
                s.alpha -= dt / SHARD_DUR;
            }
        }
    }

    public boolean isDone() { return phase == Phase.DONE; }

    // ── render ────────────────────────────────────────────────────────────────

    public void render(GraphicsContext gc, double cw, double ch) {
        if (phase == Phase.DONE) return;

        double cx = cw / 2.0;
        double cy = ch / 2.0;

        initCracks(cx, cy);
        if (phase == Phase.SHATTER && shards.isEmpty()) initShards(cx, cy);

        // Text (slam + crack phases)
        if (phase != Phase.SHATTER) {
            double scale    = getScale();
            int    fontSize = (int) (BASE_FONT * scale);
            gc.save();
            gc.setFont(Font.font("VT323", FontWeight.BOLD, fontSize));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setFill(Color.web("#00ffff"));
            gc.fillText("LV " + level, cx, cy + fontSize * 0.33);
            gc.restore();
        }

        // Crack lines
        if (phase == Phase.CRACK) {
            double prog = Math.min(1.0, (elapsed - T_CRACK) / (CRACK_DUR * 0.65));
            gc.save();
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(1.5);
            for (List<double[]> path : cracks) {
                int steps = Math.max(2, (int) (path.size() * prog));
                gc.beginPath();
                gc.moveTo(path.get(0)[0], path.get(0)[1]);
                for (int i = 1; i < steps; i++) gc.lineTo(path.get(i)[0], path.get(i)[1]);
                gc.stroke();
            }
            gc.restore();
        }

        // Shatter shards
        if (phase == Phase.SHATTER) {
            for (Shard s : shards) {
                if (s.alpha <= 0) continue;
                gc.save();
                gc.setGlobalAlpha(Math.max(0, s.alpha));
                gc.setFill(s.color);
                gc.fillRect(s.x - s.w / 2, s.y - s.h / 2, s.w, s.h);
                gc.restore();
            }
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private double getScale() {
        if (elapsed <= T_IMPACT) return lerp(3.8,  0.82, elapsed / SLAM_DUR);
        if (elapsed <= T_BOUNCE) return lerp(0.82, 1.10, (elapsed - T_IMPACT) / OVER_DUR);
        if (elapsed <= T_SETTLE) return lerp(1.10, 1.00, (elapsed - T_BOUNCE) / SETTL_DUR);
        return 1.0;
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * Math.min(1, Math.max(0, t));
    }

    private void initCracks(double cx, double cy) {
        if (!cracks.isEmpty()) return;
        int num = 6;
        for (int i = 0; i < num; i++) {
            double angle = (Math.PI * 2.0 * i / num) + (rng.nextDouble() - 0.5) * 0.7;
            double len   = 26 + rng.nextDouble() * 22;
            cracks.add(buildCrackPath(cx, cy, angle, len));
        }
    }

    private List<double[]> buildCrackPath(double sx, double sy, double angle, double len) {
        List<double[]> pts = new ArrayList<>();
        double x = sx, y = sy, traveled = 0;
        double grid = 4;
        pts.add(new double[]{x, y});
        while (traveled < len) {
            double step = 5 + rng.nextDouble() * 5;
            double jag  = (rng.nextDouble() - 0.5) * 0.6;
            x += Math.cos(angle + jag) * step;
            y += Math.sin(angle + jag) * step;
            x  = Math.round(x / grid) * grid;
            y  = Math.round(y / grid) * grid;
            pts.add(new double[]{x, y});
            traveled += step;
        }
        return pts;
    }

    private void initShards(double cx, double cy) {
        int cols = 22, rows = 9;
        double pw = TEXT_W / cols;
        double ph = TEXT_H / rows;
        Color  base = Color.web("#00ffff");

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Shard s = new Shard();
                s.x = cx - TEXT_W / 2 + (c + 0.5) * pw;
                s.y = cy - TEXT_H / 2 + (r + 0.5) * ph;
                double dx   = s.x - cx, dy = s.y - cy;
                double dist = Math.sqrt(dx * dx + dy * dy) + 1;
                double spd  = 80 + rng.nextDouble() * 220;
                s.vx    = (dx / dist) * spd + (rng.nextDouble() - 0.5) * 80;
                s.vy    = (dy / dist) * spd - rng.nextDouble() * 100;
                s.w     = pw - 1;
                s.h     = ph - 1;
                s.alpha = 1.0;
                s.color = base.deriveColor(rng.nextDouble() * 40 - 20,
                                           0.7 + rng.nextDouble() * 0.3, 1, 1);
                shards.add(s);
            }
        }
    }
}
