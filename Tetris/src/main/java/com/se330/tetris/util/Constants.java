package com.se330.tetris.util;

/**
 * Constants class containing all constant values used throughout the Tetris application.
 * Includes board dimensions, colors, file paths, and game settings.
 */
public class Constants {

    // ========== Board Configuration ==========
    public static final int BOARD_WIDTH = 10;
    public static final int BOARD_HEIGHT = 20;
    public static final int BLOCK_SIZE = 30;

    // ========== Colors ==========
    public static final String PRIMARY_COLOR = "#1e1e1e";
    public static final String SECONDARY_COLOR = "#2d2d2d";
    public static final String SUCCESS_COLOR = "#00ff00";
    public static final String ACCENT_COLOR = "#ffff00";

    // ========== FXML File Paths ==========
    public static final String FXML_MAIN = "/fxml/main.fxml";
    public static final String FXML_GAMEMODES = "/fxml/gamemodes.fxml";
    public static final String FXML_GAME = "/fxml/game.fxml";
    public static final String FXML_RESULTS = "/fxml/results.fxml";
    public static final String FXML_SETTINGS = "/fxml/settings.fxml";

    // ========== CSS File Paths ==========
    public static final String CSS_MAIN = "/styles/main.css";
    public static final String CSS_DARK_THEME = "/styles/dark-theme.css";

    // ========== Game Settings ==========
    public static final int DEFAULT_LEVEL = 1;
    public static final int DEFAULT_SPEED = 1000; // milliseconds

    // ========== Game Constants ==========
    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 10;
    public static final int MIN_SPEED = 100; // milliseconds
    public static final int SPEED_INCREMENT = 50; // milliseconds per level

    // ========== Score Settings ==========
    public static final int POINTS_PER_LINE = 100;
    public static final int POINTS_PER_TETRIS = 400; // 4 lines
    public static final int LEVEL_UP_THRESHOLD = 1000; // Points needed to level up

    // ========== Sound Settings ==========
    public static final boolean SOUND_ENABLED_DEFAULT = true;
    public static final double SOUND_VOLUME_DEFAULT = 0.7;

    // ========== Window Settings ==========
    public static final int WINDOW_WIDTH = 1024;
    public static final int WINDOW_HEIGHT = 768;
    public static final String WINDOW_TITLE = "Tetris";

    // Prevent instantiation
    private Constants() {
        throw new AssertionError("Cannot instantiate Constants class");
    }
}
