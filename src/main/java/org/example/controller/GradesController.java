package org.example.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.example.model.GradeItem;
import org.example.service.ApiService;

import java.io.IOException;

public class GradesController {
    @FXML
    private TableView<GradeItem> gradesTable;
    
    @FXML
    private TableColumn<GradeItem, String> subjectColumn;
    
    @FXML
    private TableColumn<GradeItem, String> creditColumn;
    
    @FXML
    private TableColumn<GradeItem, String> midtermColumn;
    
    @FXML
    private TableColumn<GradeItem, String> finalColumn;
    
    @FXML
    private TableColumn<GradeItem, String> averageColumn;
    
    @FXML
    private TableColumn<GradeItem, String> letterGradeColumn;
    
    @FXML
    private Button backButton;
    
    @FXML
    private Label statusLabel;
    
    @FXML
    private Label gpaLabel;
    
    private String authToken;
    private ApiService apiService;
    
    public void setAuthToken(String token) {
        this.authToken = token;
    }
    
    @FXML
    public void initialize() {
        apiService = ApiService.getInstance();
        
        // Setup table columns
        subjectColumn.setCellValueFactory(new PropertyValueFactory<>("subject"));
        creditColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(String.valueOf(cellData.getValue().getCredit())));
        midtermColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(String.format("%.2f", cellData.getValue().getMidterm())));
        finalColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(String.format("%.2f", cellData.getValue().getFinal())));
        averageColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(String.format("%.2f", cellData.getValue().getAverage())));
        letterGradeColumn.setCellValueFactory(new PropertyValueFactory<>("letterGrade"));
    }
    
    public void loadGrades() {
        statusLabel.setText("Đang tải bảng điểm...");
        gradesTable.getItems().clear();
        
        new Thread(() -> {
            try {
                String response = apiService.getGrades(authToken);
                JsonElement element = JsonParser.parseString(response);
                
                Platform.runLater(() -> {
                    try {
                        double totalPoints = 0;
                        int totalCredits = 0;
                        
                        if (element.isJsonArray()) {
                            JsonArray gradesArray = element.getAsJsonArray();
                            for (JsonElement item : gradesArray) {
                                JsonObject gradeObj = item.getAsJsonObject();
                                GradeItem gradeItem = parseGradeItem(gradeObj);
                                gradesTable.getItems().add(gradeItem);
                                
                                // Tính GPA
                                if (gradeItem.getCredit() > 0 && gradeItem.getAverage() > 0) {
                                    totalPoints += gradeItem.getAverage() * gradeItem.getCredit();
                                    totalCredits += gradeItem.getCredit();
                                }
                            }
                        } else if (element.isJsonObject()) {
                            JsonObject obj = element.getAsJsonObject();
                            if (obj.has("grades") && obj.get("grades").isJsonArray()) {
                                JsonArray gradesArray = obj.getAsJsonArray("grades");
                                for (JsonElement item : gradesArray) {
                                    JsonObject gradeObj = item.getAsJsonObject();
                                    GradeItem gradeItem = parseGradeItem(gradeObj);
                                    gradesTable.getItems().add(gradeItem);
                                    
                                    if (gradeItem.getCredit() > 0 && gradeItem.getAverage() > 0) {
                                        totalPoints += gradeItem.getAverage() * gradeItem.getCredit();
                                        totalCredits += gradeItem.getCredit();
                                    }
                                }
                            }
                            
                            // Lấy GPA từ response nếu có
                            if (obj.has("gpa")) {
                                gpaLabel.setText("GPA: " + obj.get("gpa").getAsString());
                            }
                        }
                        
                        // Tính GPA nếu chưa có
                        if (totalCredits > 0 && gpaLabel.getText().isEmpty()) {
                            double gpa = totalPoints / totalCredits;
                            gpaLabel.setText(String.format("GPA: %.2f", gpa));
                        }
                        
                        statusLabel.setText("Đã tải " + gradesTable.getItems().size() + " môn học");
                    } catch (Exception e) {
                        statusLabel.setText("Lỗi khi parse dữ liệu");
                        e.printStackTrace();
                    }
                });
                
            } catch (IOException e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Lỗi khi tải bảng điểm");
                    showAlert(Alert.AlertType.ERROR, "Lỗi", 
                            "Không thể tải bảng điểm: " + e.getMessage());
                });
            }
        }).start();
    }
    
    private GradeItem parseGradeItem(JsonObject obj) {
        GradeItem item = new GradeItem();
        item.setSubject(getStringValue(obj, "subject", "subjectName", "courseName"));
        item.setCredit(getIntValue(obj, "credit", "credits"));
        item.setMidterm(getDoubleValue(obj, "midterm", "midtermScore", "midTerm"));
        item.setFinal(getDoubleValue(obj, "final", "finalScore", "finalExam"));
        item.setAverage(getDoubleValue(obj, "average", "avg", "total"));
        item.setLetterGrade(getStringValue(obj, "letterGrade", "grade", "letter"));
        return item;
    }
    
    private String getStringValue(JsonObject obj, String... keys) {
        for (String key : keys) {
            if (obj.has(key) && !obj.get(key).isJsonNull()) {
                return obj.get(key).getAsString();
            }
        }
        return "";
    }
    
    private int getIntValue(JsonObject obj, String... keys) {
        for (String key : keys) {
            if (obj.has(key) && !obj.get(key).isJsonNull()) {
                return obj.get(key).getAsInt();
            }
        }
        return 0;
    }
    
    private double getDoubleValue(JsonObject obj, String... keys) {
        for (String key : keys) {
            if (obj.has(key) && !obj.get(key).isJsonNull()) {
                return obj.get(key).getAsDouble();
            }
        }
        return 0.0;
    }
    
    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainScreen.fxml"));
            Parent root = loader.load();
            
            MainScreenController controller = loader.getController();
            controller.setAuthToken(authToken);
            
            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(new Scene(root, 1200, 800));
            stage.setTitle("KMA Legend Desktop - Trang chủ");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

