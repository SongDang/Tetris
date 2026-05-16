package com.se330.tetris.game;

import javafx.animation.AnimationTimer;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;

public class BulbSwingEffect {

    private static final double FREQUENCY  = 3.0;
    private static final double DAMPING    = 1.8;
    private static final double DURATION   = 2.0;
    private static final double BASE_GLOW  = 28.0;
    private static final double GLOW_SCALE = 0.75;

    // The string ImageView is managed=false, translateX=170, translateY=-580, fitWidth=10.
    // Its top-centre in the parent VBox's local coordinate space: (170 + 10/2, -580) = (175, -580).
    // Rotating the VBox around this point makes the full string+bulb unit swing as one.
    private static final double PIVOT_X = 175.0;
    private static final double PIVOT_Y = -580.0;

    private final Rotate         pivot;
    private final DropShadow     glow;
    private final AnimationTimer timer;

    private double maxAngle  = 0;
    private long   triggerNs = 0;

    public BulbSwingEffect(ImageView bulbView) {
        // Attach the rotation to the parent VBox so the string swings with the bulb.
        // Fall back to rotating just the bulb if no parent is available.
        javafx.scene.Parent parent = bulbView.getParent();
        if (parent != null) {
            pivot = new Rotate(0, PIVOT_X, PIVOT_Y);
            parent.getTransforms().add(pivot);
        } else {
            pivot = new Rotate(0, bulbView.getFitWidth() / 2.0, 0);
            bulbView.getTransforms().add(pivot);
        }

        // Warm amber glow beneath the bulb, radius pulses with swing angle
        glow = new DropShadow(BASE_GLOW, 0, 10, Color.web("#ffcc44"));
        glow.setSpread(0.30);
        bulbView.setEffect(glow);

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (maxAngle == 0) return;
                double t = (now - triggerNs) / 1_000_000_000.0;
                if (t >= DURATION) {
                    pivot.setAngle(0);
                    glow.setRadius(BASE_GLOW);
                    maxAngle = 0;
                    return;
                }
                double angle = maxAngle * Math.sin(t * FREQUENCY) * Math.exp(-DAMPING * t);
                pivot.setAngle(angle);
                glow.setRadius(BASE_GLOW + Math.abs(angle) * GLOW_SCALE);
            }
        };
        timer.start();
    }

    /** Trigger a swing scaled by lines cleared (1 = subtle, 4 = dramatic). */
    public void trigger(int lines) {
        maxAngle  = switch (lines) {
            case 1  -> 15.0;
            case 2  -> 22.0;
            case 3  -> 30.0;
            default -> 40.0;
        };
        triggerNs = System.nanoTime();
    }

    public void stop() { timer.stop(); }
}
