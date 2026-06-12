package com.se330.tetris.controller;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import com.se330.tetris.service.SceneManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class StandardModeIntroController {

    @FXML private AnchorPane introPane;
    @FXML private Canvas mainCanvas;

    private GraphicsContext gc;
    private AnimationTimer timer;
    private double elapsed = 0;
    private long lastNano = 0;
    private final Random rng = new Random();

    private static final double SPIN_START  = 0.5;
    private static final double SPIN_DUR    = 1.3;
    private static final double SPIN_END    = SPIN_START + SPIN_DUR;
    private static final double FLASH_AT    = SPIN_END;
    private static final double DONE_AT     = FLASH_AT + 0.45;
    private static final double TOTAL_ROT   = Math.PI * 4;

    private static final Color BLOCK_FILL = Color.web("#30285B");

    private static final int    CELL      = 90;
    private static final double CY_OFFSET = 0.25 * CELL;

    private static final double[][] T_CELLS = {
        {0, -1}, {-1, 0}, {0, 0}, {1, 0}
    };

    private static final Color[] SPARK_COLORS = {
        Color.web("#7060CC"), Color.web("#9080EE"),
        Color.web("#B0A0FF"), Color.WHITE,
        Color.web("#5040AA"), Color.web("#C0B0FF")
    };

    private static class Spark {
        double x, y, vx, vy, size, alpha, life, maxLife;
        Color color;
    }
    private final List<Spark> sparks = new ArrayList<>();

    private double rotAngle      = 0;
    private double lastRotAngle  = 0;
    private double scale         = 0;
    private double emitAccum     = 0;

    @FXML
    private void initialize() {
        gc = mainCanvas.getGraphicsContext2D();
        mainCanvas.widthProperty().bind(introPane.widthProperty());
        mainCanvas.heightProperty().bind(introPane.heightProperty());

        lastNano = System.nanoTime();
        timer = new AnimationTimer() {
            @Override public void handle(long now) {
                double dt = Math.min((now - lastNano) / 1_000_000_000.0, 0.05);
                lastNano = now;
                elapsed += dt;
                tick(dt);
            }
        };
        timer.start();
    }

    private void tick(double dt) {
        double w = mainCanvas.getWidth();
        double h = mainCanvas.getHeight();
        double cx = w / 2, cy = h / 2;

        // Scale in with cubic ease-out
        double t0 = Math.min(elapsed / SPIN_START, 1.0);
        scale = 1.0 - Math.pow(1.0 - t0, 3);

        // Ease-in-out rotation after hold
        lastRotAngle = rotAngle;
        if (elapsed >= SPIN_START) {
            double st = Math.min((elapsed - SPIN_START) / SPIN_DUR, 1.0);
            double eased = 0.5 - 0.5 * Math.cos(Math.PI * st);
            rotAngle = eased * TOTAL_ROT;
        }

        // Sparks only during spin
        if (elapsed >= SPIN_START && elapsed < FLASH_AT) {
            // Burst on every 90° crossing
            double prevDeg = Math.toDegrees(lastRotAngle) % 360;
            double currDeg = Math.toDegrees(rotAngle) % 360;
            for (int mark = 0; mark < 360; mark += 90) {
                if (crossed(prevDeg, currDeg, mark)) burstSparks(cx, cy, 18);
            }

            emitAccum += dt;
            while (emitAccum > 0.03) {
                emitAccum -= 0.03;
                emitSparks(cx, cy, 3);
            }
        }

        gc.clearRect(0, 0, w, h);
        drawAmbientGlow(cx, cy);
        renderSparks(dt);

        gc.save();
        gc.translate(cx, cy);
        gc.scale(scale, scale);
        gc.rotate(Math.toDegrees(rotAngle));
        drawBlock();
        gc.restore();

        if (elapsed >= FLASH_AT) {
            double fa = Math.min((elapsed - FLASH_AT) / (DONE_AT - FLASH_AT), 1.0);
            gc.setFill(Color.WHITE.deriveColor(0, 1, 1, fa));
            gc.fillRect(0, 0, w, h);
        }

        if (elapsed >= DONE_AT) {
            timer.stop();
            SceneManager.getInstance().switchToScene(SceneManager.GAME_SCENE);
        }
    }

    private boolean crossed(double prev, double curr, double mark) {
        if (curr >= prev) return prev < mark && curr >= mark;
        return prev < mark + 360 && curr + 360 >= mark + 360;
    }

    private void drawAmbientGlow(double cx, double cy) {
        double pulse = 0.22 + 0.08 * Math.sin(elapsed * 3.5);
        RadialGradient glow = new RadialGradient(0, 0, cx, cy, 400 * scale, false,
            CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.web("#30285B", pulse)),
            new Stop(1.0, Color.web("#30285B", 0))
        );
        gc.setFill(glow);
        gc.fillOval(cx - 400, cy - 400, 800, 800);
    }

    private void drawBlock() {
        gc.setFill(BLOCK_FILL);
        for (double[] cell : T_CELLS) {
            double x = cell[0] * CELL - CELL / 2.0;
            double y = cell[1] * CELL + CY_OFFSET - CELL / 2.0;
            gc.fillRect(x, y, CELL, CELL);
        }
    }

    private void emitSparks(double cx, double cy, int count) {
        double[] cell = T_CELLS[rng.nextInt(T_CELLS.length)];
        double half = CELL / 2.0;
        double bx = cell[0] * CELL, by = cell[1] * CELL + CY_OFFSET;
        double t  = rng.nextDouble();
        double lx, ly;
        switch (rng.nextInt(4)) {
            case 0 -> { lx = bx - half + t * CELL; ly = by - half; }
            case 1 -> { lx = bx + half;             ly = by - half + t * CELL; }
            case 2 -> { lx = bx - half + t * CELL; ly = by + half; }
            default -> { lx = bx - half;            ly = by - half + t * CELL; }
        }
        spawnSparks(cx, cy, lx, ly, count, 60, 200);
    }

    private void burstSparks(double cx, double cy, int total) {
        for (double[] cell : T_CELLS) {
            double bx = cell[0] * CELL, by = cell[1] * CELL + CY_OFFSET;
            for (int corner = 0; corner < 4; corner++) {
                double lx = bx + (corner % 2 == 0 ? -CELL / 2.0 : CELL / 2.0);
                double ly = by + (corner < 2      ? -CELL / 2.0 : CELL / 2.0);
                spawnSparks(cx, cy, lx, ly, total / 4, 120, 380);
            }
        }
    }

    private void spawnSparks(double cx, double cy,
                              double lx, double ly,
                              int count, double minSpeed, double maxSpeed) {
        double cos = Math.cos(rotAngle), sin = Math.sin(rotAngle);
        double wx  = cx + scale * (cos * lx - sin * ly);
        double wy  = cy + scale * (sin * lx + cos * ly);
        double out = Math.atan2(ly, lx) + rotAngle;

        for (int i = 0; i < count; i++) {
            Spark s   = new Spark();
            s.x       = wx + (rng.nextDouble() - 0.5) * 8;
            s.y       = wy + (rng.nextDouble() - 0.5) * 8;
            double a  = out + (rng.nextDouble() - 0.5) * 1.4;
            double sp = minSpeed + rng.nextDouble() * (maxSpeed - minSpeed);
            s.vx      = Math.cos(a) * sp;
            s.vy      = Math.sin(a) * sp - rng.nextDouble() * 60;
            s.size    = 3 + rng.nextDouble() * 7;
            s.alpha   = 0.55 + rng.nextDouble() * 0.45;
            s.maxLife = 0.25 + rng.nextDouble() * 0.55;
            s.life    = 0;
            s.color   = SPARK_COLORS[rng.nextInt(SPARK_COLORS.length)];
            sparks.add(s);
        }
    }

    private void renderSparks(double dt) {
        sparks.removeIf(s -> s.life >= s.maxLife);
        for (Spark s : sparks) {
            s.life += dt;
            s.x    += s.vx * dt;
            s.y    += s.vy * dt;
            s.vy   += 180 * dt;
            double t  = s.life / s.maxLife;
            double a  = (1.0 - t) * s.alpha;
            double sz = s.size * (1.0 - t * 0.6);
            gc.save();
            gc.setGlobalAlpha(a);
            gc.setFill(s.color);
            gc.fillOval(s.x - sz / 2, s.y - sz / 2, sz, sz);
            gc.restore();
        }
        gc.setGlobalAlpha(1.0);
    }
}
