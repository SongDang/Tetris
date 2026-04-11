package com.se330.tetris.game;

import javafx.scene.paint.Color;

public class Particle {
    public double x, y;
    public double vx, vy;
    public double life;   // 1.0 = full, 0.0 = dead
    public double decay;  // life lost per second
    public Color color;
    public double size;

    public Particle(double x, double y, double vx, double vy,
                    double decay, Color color, double size) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.life = 1.0;
        this.decay = decay;
        this.color = color;
        this.size = size;
    }

    public void update(double dt) {
        x += vx * dt;
        y += vy * dt;
        life -= decay * dt;
    }

    public boolean isDead() {
        return life <= 0;
    }
}
