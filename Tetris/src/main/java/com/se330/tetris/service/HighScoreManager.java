package com.se330.tetris.service;

import com.se330.tetris.model.ScoreRecord;

import java.io.*;
import java.util.*;

public class HighScoreManager {
    private static final String DATA_FILE = "highscores.dat";
    private static final int MAX_RECORDS = 10;

    private Map<String, List<ScoreRecord>> allHighScores;

    private static HighScoreManager instance;

    private HighScoreManager() {
        allHighScores = new HashMap<>();
        loadScores();
    }

    public static HighScoreManager getInstance() {
        if (instance == null) {
            instance = new HighScoreManager();
        }
        return instance;
    }

    public boolean checkIfHighscore(int score, String mode) {
        if (score <= 0) return false;

        List<ScoreRecord> modeScores = allHighScores.getOrDefault(mode, new ArrayList<>());

        if (modeScores.size() < MAX_RECORDS) return true;
        return score > modeScores.get(modeScores.size() - 1).getScore();
    }

    public void addScore(ScoreRecord record) {
        if (record == null || record.getScore() <= 0) {
            System.out.println("Bỏ qua việc ghi nhận kỷ lục: Điểm số bằng 0 hoặc bản ghi không hợp lệ.");
            return;
        }

        String mode = record.getGameMode();
        List<ScoreRecord> scores = allHighScores.computeIfAbsent(mode, k -> new ArrayList<>());

        scores.add(record);

        scores.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));

        if (scores.size() > MAX_RECORDS) {
            scores.subList(MAX_RECORDS, scores.size()).clear();
        }

        saveScores();
    }

    public List<ScoreRecord> getTopScores(String mode) {
        return allHighScores.getOrDefault(mode, new ArrayList<>());
    }

    public void resetAllScores() {
        allHighScores.clear();

        saveScores();

        System.out.println("Tất cả thành tựu đã được reset về mặc định.");
    }

    @SuppressWarnings("unchecked")
    private void saveScores() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(allHighScores);
        } catch (IOException e) {
            System.err.println("Lỗi khi lưu bảng điểm: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void loadScores() {
        File file = new File(DATA_FILE);
        if (!file.exists()) return;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            allHighScores = (Map<String, List<ScoreRecord>>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Không thể đọc file bảng điểm cũ, khởi tạo mới.");
            allHighScores = new HashMap<>();
        }
    }
}