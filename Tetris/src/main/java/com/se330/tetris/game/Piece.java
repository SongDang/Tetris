package com.se330.tetris.game;

import com.se330.tetris.service.SoundManager;
import com.se330.tetris.util.SoundType;

public class Piece {
    private final TetrominoType type;
    private int rotation;
    private int x;
    private int y;

    public Piece(TetrominoType type, int x, int y) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.rotation = 0;
    }

    public TetrominoType getType() {
        return type;
    }

    public int getRotation() {
        return rotation;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setRotation(int rotation)
    {
        SoundManager.getInstance().playSE(SoundType.BUTTON_CLICK);
        this.rotation = rotation;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }
}