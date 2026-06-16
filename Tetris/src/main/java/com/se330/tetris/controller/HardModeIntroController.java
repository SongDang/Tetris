package com.se330.tetris.controller;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import com.se330.tetris.service.SceneManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class HardModeIntroController {

    @FXML private AnchorPane introPane;
    @FXML private Canvas     glowCanvas;
    @FXML private VBox       bulbContainer;
    @FXML private ImageView  bulbView;
    @FXML private Region     blackOverlay;

    private Image imgOn, imgOff;
    private AnimationTimer timer;
    private double elapsed  = 0;
    private long   lastNano = 0;
    private boolean bulbOn  = true;
    private final Random rng = new Random();

    private GraphicsContext glowGc;
    private RadialGradient  cachedGlow = null;
    private double cachedGx = -1, cachedGy = -1;

    private static final DropShadow BULB_GLOW = new DropShadow(
        BlurType.GAUSSIAN, Color.web("#FFE8A0"), 100, 0.0, 0, 20
    );
    private static final double GLOW_RADIUS = 700;

    private static final double DESCEND_END = 0.50;
    private static final double BOUNCE_END  = 0.80;
    private static final double FLICKER_END = 1.68;
    private static final double DONE_AT     = 1.98;

    private static final double START_Y    = -640.0;
    private static final double TARGET_Y   =    0.0;
    private static final double BOUNCE_AMP =   25.0;

    private static class BulbMote {
        double baseX, y, vy, sineAmp, sineFreq, sinePhase, life, maxLife, size;
        boolean white;
        double x() { return baseX + Math.sin(sinePhase + life * sineFreq) * sineAmp; }
    }
    private final List<BulbMote> bulbMotes = new ArrayList<>();
    private double moteTimer = 0;

    @FXML
    private void initialize() {
        imgOn  = loadImage("/assets/lightson.png");
        imgOff = loadImage("/assets/lightsoff.png");

        bulbView.setImage(imgOn);
        bulbView.setEffect(BULB_GLOW);
        bulbContainer.setTranslateY(START_Y);
        bulbContainer.layoutXProperty().bind(
            introPane.widthProperty().subtract(320.0).divide(2.0)
        );
        blackOverlay.setOpacity(0);
        glowGc = glowCanvas.getGraphicsContext2D();
        glowCanvas.widthProperty().bind(introPane.widthProperty());
        glowCanvas.heightProperty().bind(introPane.heightProperty());

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
        if (elapsed < DESCEND_END) {
            double t     = elapsed / DESCEND_END;
            double eased = 1.0 - Math.pow(1.0 - t, 3);
            bulbContainer.setTranslateY(START_Y + (TARGET_Y - START_Y) * eased);

        } else if (elapsed < BOUNCE_END) {
            double bt     = elapsed - DESCEND_END;
            double bd     = BOUNCE_END - DESCEND_END;
            double bounce = -BOUNCE_AMP * Math.sin(Math.PI * bt / bd) * Math.exp(-4.0 * bt);
            bulbContainer.setTranslateY(TARGET_Y + bounce);

        } else if (elapsed < FLICKER_END) {
            bulbContainer.setTranslateY(TARGET_Y);
            double ft    = elapsed - BOUNCE_END;
            boolean want = ft < 0.08 ? false
                         : ft < 0.50 ? true
                         : ft < 0.58 ? false
                         : ft < 0.68 ? true
                         : ft < 0.76 ? false
                         :             true;
            if (want != bulbOn) applyBulbState(want);

        } else if (elapsed < DONE_AT) {
            if (bulbOn) applyBulbState(false);
            blackOverlay.setOpacity(1.0);

        } else {
            timer.stop();
            SceneManager.getInstance().switchToScene(SceneManager.HARD_GAME_SCENE);
            return;
        }

        renderGlow(dt);
    }

    private void renderGlow(double dt) {
        glowGc.clearRect(0, 0, glowCanvas.getWidth(), glowCanvas.getHeight());
        if (!bulbOn) return;

        javafx.geometry.Bounds bScene = bulbView.localToScene(bulbView.getBoundsInLocal());
        javafx.geometry.Bounds bLocal = glowCanvas.sceneToLocal(bScene);
        double gx = bLocal.getMinX() + bLocal.getWidth()  * 0.5;
        double gy = bLocal.getMinY() + bLocal.getHeight() * 0.62;
        double bulbBottom = bLocal.getMinY() + bLocal.getHeight() * 0.78;

        if (cachedGlow == null || Math.abs(gx - cachedGx) > 1 || Math.abs(gy - cachedGy) > 1) {
            cachedGx = gx;
            cachedGy = gy;
            cachedGlow = new RadialGradient(
                0, 0, gx, gy, GLOW_RADIUS, false,
                CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#FFE8A0", 0.50)),
                new Stop(0.3, Color.web("#FFD060", 0.18)),
                new Stop(1.0, Color.TRANSPARENT)
            );
        }
        glowGc.setFill(cachedGlow);
        glowGc.fillRect(0, 0, glowCanvas.getWidth(), glowCanvas.getHeight());

        renderBulbMotes(dt, gx, bulbBottom);
    }

    private void renderBulbMotes(double dt, double bulbCx, double bulbBottom) {
        moteTimer -= dt;
        if (moteTimer <= 0) {
            moteTimer = 0.15 + rng.nextDouble() * 0.20;
            BulbMote m = new BulbMote();
            m.baseX     = bulbCx + (rng.nextDouble() - 0.5) * 30;
            m.y         = bulbBottom + rng.nextDouble() * 10;
            m.vy        = 18 + rng.nextDouble() * 28;
            m.sineAmp   = 8 + rng.nextDouble() * 22;
            m.sineFreq  = 1.2 + rng.nextDouble() * 2.0;
            m.sinePhase = rng.nextDouble() * Math.PI * 2;
            m.maxLife   = 2.0 + rng.nextDouble() * 2.5;
            m.life      = 0;
            m.size      = 1.5 + rng.nextDouble() * 1.5;
            m.white     = rng.nextDouble() < 0.35;
            bulbMotes.add(m);
        }

        bulbMotes.removeIf(m -> m.life >= m.maxLife);
        for (BulbMote m : bulbMotes) {
            m.life += dt;
            m.y    += m.vy * dt;
            double t = m.life / m.maxLife;
            double alpha = t < 0.12 ? t / 0.12 : (t > 0.65 ? (1.0 - t) / 0.35 : 1.0);
            glowGc.setGlobalAlpha(alpha * 0.85);
            glowGc.setFill(m.white ? Color.web("#FFFDE0") : Color.web("#FFD966"));
            glowGc.fillRect(m.x(), m.y, m.size, m.size);
        }
        glowGc.setGlobalAlpha(1.0);
    }

    private void applyBulbState(boolean on) {
        bulbOn = on;
        bulbView.setImage(on ? imgOn : imgOff);
        bulbView.setEffect(on ? BULB_GLOW : null);
        introPane.setStyle("-fx-background-color: " + (on ? "#280C00" : "#000000") + ";");
    }

    private Image loadImage(String path) {
        try { return new Image(getClass().getResourceAsStream(path)); }
        catch (Exception e) { return null; }
    }
}
