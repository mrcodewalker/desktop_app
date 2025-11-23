package org.example.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Service để lưu trữ dữ liệu local (như localStorage trên web)
 */
public class LocalStorageService {
    private static LocalStorageService instance;
    private static final String STORAGE_DIR = System.getProperty("user.home") + File.separator + ".kma-legend";
    private static final String STUDENT_INFO_FILE = "student_info.json";
    private static final String SCHEDULE_FILE = "schedule.json";
    private static final String FILTER_STATE_FILE = "filter_state.json";
    private static final String CREDENTIALS_FILE = "credentials.json";
    private static final String BACKUP_SCORES_FILE = "backup_scores.json";
    
    private LocalStorageService() {
        // Tạo thư mục nếu chưa có
        try {
            Files.createDirectories(Paths.get(STORAGE_DIR));
        } catch (IOException e) {
            System.err.println("Cannot create storage directory: " + e.getMessage());
        }
    }
    
    public static LocalStorageService getInstance() {
        if (instance == null) {
            instance = new LocalStorageService();
        }
        return instance;
    }
    
    /**
     * Lưu student info
     */
    public void saveStudentInfo(JsonObject studentInfo) throws IOException {
        saveToFile(STUDENT_INFO_FILE, studentInfo.toString());
    }
    
    /**
     * Đọc student info
     */
    public JsonObject loadStudentInfo() throws IOException {
        String content = loadFromFile(STUDENT_INFO_FILE);
        if (content != null && !content.isEmpty()) {
            return JsonParser.parseString(content).getAsJsonObject();
        }
        return null;
    }
    
    /**
     * Lưu schedule
     */
    public void saveSchedule(String scheduleJson) throws IOException {
        saveToFile(SCHEDULE_FILE, scheduleJson);
    }
    
    /**
     * Đọc schedule
     */
    public String loadSchedule() throws IOException {
        return loadFromFile(SCHEDULE_FILE);
    }
    
    /**
     * Lưu filter state
     */
    public void saveFilterState(String monthFilter, String dateFilter) throws IOException {
        JsonObject filterState = new JsonObject();
        if (monthFilter != null) {
            filterState.addProperty("monthFilter", monthFilter);
        }
        if (dateFilter != null) {
            filterState.addProperty("dateFilter", dateFilter);
        }
        saveToFile(FILTER_STATE_FILE, filterState.toString());
    }
    
    /**
     * Đọc filter state
     */
    public JsonObject loadFilterState() throws IOException {
        String content = loadFromFile(FILTER_STATE_FILE);
        if (content != null && !content.isEmpty()) {
            return JsonParser.parseString(content).getAsJsonObject();
        }
        return null;
    }
    
    /**
     * Lưu credentials đã mã hóa (encryptedKey, encryptedData, iv)
     */
    public void saveCredentials(String encryptedKey, String encryptedData, String iv) throws IOException {
        JsonObject credentials = new JsonObject();
        credentials.addProperty("encryptedKey", encryptedKey);
        credentials.addProperty("encryptedData", encryptedData);
        credentials.addProperty("iv", iv);
        saveToFile(CREDENTIALS_FILE, credentials.toString());
    }
    
    /**
     * Đọc credentials đã mã hóa
     */
    public JsonObject loadCredentials() throws IOException {
        String content = loadFromFile(CREDENTIALS_FILE);
        if (content != null && !content.isEmpty()) {
            return JsonParser.parseString(content).getAsJsonObject();
        }
        return null;
    }
    
    /**
     * Lưu backup scores từ bảng điểm thi
     */
    public void saveBackupScores(String scoresJson) throws IOException {
        saveToFile(BACKUP_SCORES_FILE, scoresJson);
    }
    
    /**
     * Đọc backup scores
     */
    public String loadBackupScores() throws IOException {
        return loadFromFile(BACKUP_SCORES_FILE);
    }
    
    /**
     * Xóa tất cả dữ liệu
     */
    public void clearAll() {
        try {
            deleteFile(STUDENT_INFO_FILE);
            deleteFile(SCHEDULE_FILE);
            deleteFile(FILTER_STATE_FILE);
            deleteFile(CREDENTIALS_FILE);
            deleteFile(BACKUP_SCORES_FILE);
        } catch (IOException e) {
            System.err.println("Error clearing storage: " + e.getMessage());
        }
    }
    
    private void saveToFile(String filename, String content) throws IOException {
        Path filePath = Paths.get(STORAGE_DIR, filename);
        Files.write(filePath, content.getBytes(StandardCharsets.UTF_8));
    }
    
    private String loadFromFile(String filename) throws IOException {
        Path filePath = Paths.get(STORAGE_DIR, filename);
        if (!Files.exists(filePath)) {
            return null;
        }
        return Files.readString(filePath, StandardCharsets.UTF_8);
    }
    
    private void deleteFile(String filename) throws IOException {
        Path filePath = Paths.get(STORAGE_DIR, filename);
        Files.deleteIfExists(filePath);
    }
}

