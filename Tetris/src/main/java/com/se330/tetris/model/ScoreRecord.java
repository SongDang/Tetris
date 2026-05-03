package com.se330.tetris.model;

import java.io.Serializable;

public class ScoreRecord implements Serializable {
    // serialVersionUID giúp đảm bảo tính tương thích khi đọc/ghi file
    private static final long serialVersionUID = 1L;

    private String playerName;
    private int score;
    private String date; // Định dạng dd/MM/yyyy
    private String gameMode; // "NORMAL", "HARD", hoặc "TIME_ATTACK"

    public ScoreRecord() {}
    public ScoreRecord(String playerName, int score, String date, String gameMode) {
        this.playerName = playerName;
        this.score = score;
        this.date = date;
        this.gameMode = gameMode;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getGameMode() {
        return gameMode;
    }

    public void setGameMode(String gameMode) {
        this.gameMode = gameMode;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s: %d (%s)", gameMode, playerName, score, date);
    }
}