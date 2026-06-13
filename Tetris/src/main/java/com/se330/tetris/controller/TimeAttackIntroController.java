package com.se330.tetris.controller;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import com.se330.tetris.service.SceneManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TimeAttackIntroController {

    @FXML private AnchorPane introPane;
    @FXML private Canvas     mainCanvas;

    private Image sheet;
    private double frameW, frameH, displayW, displayH;

    private GraphicsContext gc;
    private AnimationTimer  timer;
    private double elapsed  = 0;
    private long   lastNano = 0;
    private boolean bgFlipped    = false;
    private boolean shardsSpawned = false;
    private final Random rng = new Random();

    private static final int    FRAMES          = 8;
    private static final double FRAME_DUR       = 0.50;
    private static final double SECOND_LAST_EXTRA = 0.75;
    private static final double LAST_FRAME_START = (FRAMES - 1) * FRAME_DUR + SECOND_LAST_EXTRA;
    private static final double DONE_AT         = LAST_FRAME_START + FRAME_DUR + 0.80;

    private static final double GRAVITY = 420;

    private static final Color[] SHARD_COLORS = {
        Color.web("#C8C8C8"), Color.web("#C8C8C8"),
        Color.web("#C8C8C8"), Color.web("#C8C8C8"),
        Color.web("#C8C8C8"), Color.web("#C8C8C8")
    };

    private static class Shard {
        double x, y, vx, vy, angle, spin, w, h, alpha, life, maxLife;
        Color color;
    }
    private final List<Shard> shards = new ArrayList<>();

    @FXML
    private void initialize() {
        sheet = loadImage("/assets/timeintro.png");
        if (sheet != null) {
            frameW   = sheet.getWidth();
            frameH   = sheet.getHeight() / FRAMES;
            displayW = 320;
            displayH = displayW * (frameH / frameW);
        }

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

        gc.clearRect(0, 0, w, h);

        if (elapsed >= LAST_FRAME_START && !bgFlipped) {
            bgFlipped = true;
            introPane.setStyle("-fx-background-color: #228BB1;");
        }

        if (elapsed >= LAST_FRAME_START && !shardsSpawned) {
            shardsSpawned = true;
            spawnShards(cx, cy);
        }

        drawGlow(cx, cy);
        drawClock(cx, cy);
        renderShards(dt);

        if (elapsed >= DONE_AT) {
            timer.stop();
            SceneManager.getInstance().switchToScene(SceneManager.GAME_SCENE);
        }
    }

    private void drawGlow(double cx, double cy) {
        double pulse = 0.18 + 0.06 * Math.sin(elapsed * 2.5);
        RadialGradient glow = new RadialGradient(0, 0, cx, cy, 320, false,
            CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.web("#00CCCC", pulse)),
            new Stop(1.0, Color.web("#00CCCC", 0))
        );
        gc.setFill(glow);
        gc.fillOval(cx - 320, cy - 320, 640, 640);
    }

    private void drawClock(double cx, double cy) {
        if (sheet == null) return;
        int frame;
        if (elapsed < (FRAMES - 2) * FRAME_DUR) {
            frame = (int)(elapsed / FRAME_DUR);
        } else if (elapsed < LAST_FRAME_START) {
            frame = FRAMES - 2;
        } else {
            frame = FRAMES - 1;
        }
        double sx = cx - displayW / 2;
        double sy = cy - displayH / 2;
        gc.drawImage(sheet, 0, frame * frameH, frameW, frameH, sx, sy, displayW, displayH);
    }

    private void spawnShards(double cx, double cy) {
        double originX = cx + displayW / 2;
        double originY = cy - displayH / 2;
        for (int i = 0; i < 60; i++) {
            Shard s   = new Shard();
            double ang = rng.nextDouble() * Math.PI - Math.PI * 0.75;
            double jitter = rng.nextDouble() * 20;
            s.x       = originX + Math.cos(ang) * jitter;
            s.y       = originY + Math.sin(ang) * jitter;
            double speed = 180 + rng.nextDouble() * 520;
            s.vx      = Math.cos(ang) * speed;
            s.vy      = Math.sin(ang) * speed - rng.nextDouble() * 150;
            s.angle   = rng.nextDouble() * 360;
            s.spin    = (rng.nextDouble() - 0.5) * 800;
            s.w       = 2 + rng.nextDouble() * 5;
            s.h       = 8 + rng.nextDouble() * 28;
            s.alpha   = 0.65 + rng.nextDouble() * 0.35;
            s.maxLife = 0.5 + rng.nextDouble() * 1.0;
            s.life    = 0;
            s.color   = SHARD_COLORS[rng.nextInt(SHARD_COLORS.length)];
            shards.add(s);
        }
    }

    private void renderShards(double dt) {
        shards.removeIf(s -> s.life >= s.maxLife);
        for (Shard s : shards) {
            s.life  += dt;
            s.x     += s.vx * dt;
            s.y     += s.vy * dt;
            s.vy    += GRAVITY * dt;
            s.angle += s.spin * dt;
            double t = s.life / s.maxLife;
            double alpha = t < 0.1 ? 1.0 : (1.0 - t) / 0.9;
            gc.save();
            gc.setGlobalAlpha(alpha * s.alpha);
            gc.setFill(s.color);
            gc.translate(s.x, s.y);
            gc.rotate(s.angle);
            gc.fillRect(-s.w / 2, -s.h / 2, s.w, s.h);
            gc.restore();
        }
        gc.setGlobalAlpha(1.0);
    }

    private Image loadImage(String path) {
        try { return new Image(getClass().getResourceAsStream(path)); }
        catch (Exception e) { return null; }
    }
}
