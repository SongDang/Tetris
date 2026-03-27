package com.se330.tetris.service;

/**
 * GameContext is a singleton class that stores the current game state and configuration.
 * It manages information about the selected game mode, score, level, and other game settings.
 */
public class GameContext {

    // ========== Game Mode Enum ==========
    public enum GameMode {
        STANDARD("Standard Mode"),
        TIME_ATTACK("Time Attack"),
        HARD_MODE("Hard Mode");

        private final String displayName;

        GameMode(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // ========== Singleton Instance ==========
    private static GameContext instance;

    // ========== Instance Variables ==========
    private GameMode gameMode;
    private int score;
    private int level;
    private int lines;
    private int blocksPlaced;
    private long gameTime;

    /**
     * Private constructor to enforce singleton pattern
     */
    private GameContext() {
        this.gameMode = GameMode.STANDARD;
        this.score = 0;
        this.level = 1;
        this.lines = 0;
        this.blocksPlaced = 0;
        this.gameTime = 0;
    }

    /**
     * Gets the singleton instance of GameContext
     *
     * @return the singleton instance
     */
    public static synchronized GameContext getInstance() {
        if (instance == null) {
            instance = new GameContext();
        }
        return instance;
    }

    /**
     * Resets the game context to default values
     */
    public void reset() {
        this.score = 0;
        this.level = 1;
        this.lines = 0;
        this.blocksPlaced = 0;
        this.gameTime = 0;
    }

    // ========== Getters and Setters ==========

    public GameMode getGameMode() {
        return gameMode;
    }

    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public void addScore(int points) {
        this.score += points;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void increaseLevel() {
        this.level++;
    }

    public int getLines() {
        return lines;
    }

    public void setLines(int lines) {
        this.lines = lines;
    }

    public void addLines(int count) {
        this.lines += count;
    }

    public int getBlocksPlaced() {
        return blocksPlaced;
    }

    public void setBlocksPlaced(int blocksPlaced) {
        this.blocksPlaced = blocksPlaced;
    }

    public void increaseBlocksPlaced() {
        this.blocksPlaced++;
    }

    public long getGameTime() {
        return gameTime;
    }

    public void setGameTime(long gameTime) {
        this.gameTime = gameTime;
    }
}
