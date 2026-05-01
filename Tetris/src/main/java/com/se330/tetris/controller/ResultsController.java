package com.se330.tetris.controller;

import com.se330.tetris.model.ScoreRecord;
import com.se330.tetris.service.HighScoreManager;
import com.se330.tetris.service.SoundManager;
import com.se330.tetris.util.SoundType;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import com.se330.tetris.service.GameContext;
import com.se330.tetris.service.SceneManager;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ResultsController {

    @FXML
    private AnchorPane resultsPane;

    @FXML
    private Label finalScoreLabel;

    @FXML
    private Label gameModeLabel;

    @FXML
    private Label linesLabel;

    @FXML
    private Label levelLabel;

    @FXML
    private Button saveBtn;

    @FXML
    private Button retryBtn;

    @FXML
    private Button menuBtn;

    @FXML
    private VBox newRecordBox;
    @FXML
    private TextField playerNameInput;
    @FXML
    private TableView<ScoreRecord> highScoreTable;
    @FXML
    private TableColumn<ScoreRecord, String> colName;
    @FXML
    private TableColumn<ScoreRecord, Integer> colScore;
    @FXML
    private TableColumn<ScoreRecord, String> colDate;

    private SceneManager sceneManager;
    private GameContext gameContext;

    int score, level, lines;
    String mode;
    String date;

    @FXML
    private void initialize() {
        sceneManager = SceneManager.getInstance();
        gameContext = GameContext.getInstance();

        colName.setCellValueFactory(new PropertyValueFactory<>("playerName"));
        colScore.setCellValueFactory(new PropertyValueFactory<>("score"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));

        populateResults();
    }

    private void populateResults() {
        score = gameContext.getScore();
        level = gameContext.getLevel();
        lines = gameContext.getLines();
        mode = gameContext.getGameMode().getDisplayName();
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        date = now.format(formatter);

        finalScoreLabel.setText(String.valueOf(score));
        gameModeLabel.setText(mode);
        levelLabel.setText(String.valueOf(level));
        linesLabel.setText(String.valueOf(lines));


        System.out.println("Results displayed:");
        System.out.println("  Final Score: " + score);
        System.out.println("  Mode: " + gameContext.getGameMode().getDisplayName());
        System.out.println("  Level: " + level);
        System.out.println("  Lines: " + lines);

        //high score
        Boolean isNewHighScore = HighScoreManager.getInstance().checkIfHighscore(score, mode);
        if(isNewHighScore)
        {
            newRecordBox.setVisible(true);
        }
        else {
            newRecordBox.setVisible(false);
        }

        refreshHighscoreTable();
    }

    @FXML
    private void onSaveClicked() {
        String playerName = playerNameInput.getText();
        ScoreRecord scoreRecord = new ScoreRecord(playerName, score, date, mode);
        HighScoreManager.getInstance().addScore(scoreRecord);
        System.out.println("Add score record: " + scoreRecord);

        newRecordBox.setVisible(false);
        newRecordBox.setManaged(false);

        refreshHighscoreTable();
    }

    private void refreshHighscoreTable() {
        List<ScoreRecord> topScores = HighScoreManager.getInstance().getTopScores(mode);

        ObservableList<ScoreRecord> observableData = FXCollections.observableArrayList(topScores);

        highScoreTable.setItems(observableData);
    }

    @FXML
    private void onRetryClicked() {
        System.out.println("Retrying with mode: " + gameContext.getGameMode().getDisplayName());
        gameContext.reset();
        // Clear cache to force GameController.initialize() to run again
        sceneManager.clearSceneCache();
        sceneManager.switchToScene(SceneManager.GAME_SCENE);
        SoundManager.getInstance().playMusic(SoundType.GAMEPLAY_THEME);
    }

    @FXML
    private void onMenuClicked() {
        System.out.println("Returning to main menu");
        gameContext.reset();
        sceneManager.switchToScene(SceneManager.MAIN_MENU_SCENE);
    }
}
