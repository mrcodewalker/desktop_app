package org.example.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.example.model.ScoreItem;
import org.example.service.ApiService;
import org.example.service.EncryptionService;
import org.example.service.LocalStorageService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ScoresController {
    @FXML
    private TableView<ScoreItem> scoresTable;
    
    @FXML
    private TableColumn<ScoreItem, String> subjectNameColumn;
    
    @FXML
    private TableColumn<ScoreItem, Integer> creditColumn;
    
    @FXML
    private TableColumn<ScoreItem, Double> scoreFirstColumn;
    
    @FXML
    private TableColumn<ScoreItem, Double> scoreSecondColumn;
    
    @FXML
    private TableColumn<ScoreItem, Double> scoreFinalColumn;
    
    @FXML
    private TableColumn<ScoreItem, Double> scoreOverallColumn;
    
    @FXML
    private TableColumn<ScoreItem, String> scoreTextColumn;
    
    @FXML
    private Button backButton;
    
    @FXML
    private Button infoButton;
    
    @FXML
    private Label statusLabel;
    
    @FXML
    private Label studentInfoLabel;
    
    @FXML
    private Label gpaLabel;
    
    @FXML
    private Label cpaLabel;
    
    @FXML
    private Label formulaLabel;
    
    @FXML
    private Button virtualScoresButton;
    
    private ApiService apiService;
    private EncryptionService encryptionService;
    private LocalStorageService localStorageService;
    // Màu sắc cho điểm số dựa trên giá trị
    private String getScoreColor(double score) {
        if (score >= 9.0) {
            return "linear-gradient(to right, #11998e, #38ef7d)"; // Xanh lá - Xuất sắc
        } else if (score >= 8.5) {
            return "linear-gradient(to right, #667eea, #764ba2)"; // Tím - Giỏi
        } else if (score >= 7.8) {
            return "linear-gradient(to right, #4facfe, #00f2fe)"; // Xanh dương - Khá
        } else if (score >= 7.0) {
            return "linear-gradient(to right, #43e97b, #38f9d7)"; // Xanh ngọc - Khá
        } else if (score >= 6.3) {
            return "linear-gradient(to right, #fa709a, #fee140)"; // Vàng hồng - Trung bình
        } else if (score >= 5.5) {
            return "linear-gradient(to right, #f093fb, #f5576c)"; // Hồng - Trung bình
        } else if (score >= 4.8) {
            return "linear-gradient(to right, #ffa726, #fb8c00)"; // Cam - Trung bình yếu
        } else if (score >= 4.0) {
            return "linear-gradient(to right, #ff7043, #f4511e)"; // Cam đỏ - Trung bình yếu
        } else {
            return "linear-gradient(to right, #ff6b6b, #ee5a6f)"; // Đỏ - Kém
        }
    }
    
    @FXML
    public void initialize() {
        apiService = ApiService.getInstance();
        encryptionService = EncryptionService.getInstance();
        localStorageService = LocalStorageService.getInstance();
        
        // Setup công thức tính GPA
        if (formulaLabel != null) {
            formulaLabel.setText("📐 Công thức tính điểm: GPA = Σ(Điểm thang 4 × Số tín chỉ) / Σ(Số tín chỉ) | " +
                    "GPA tính theo các môn kì gần nhất, CPA tính theo tất cả các môn học");
            formulaLabel.setWrapText(true);
        }
        
        // Setup table columns
        subjectNameColumn.setCellValueFactory(new PropertyValueFactory<>("subjectName"));
        creditColumn.setCellValueFactory(new PropertyValueFactory<>("subjectCredit"));
        scoreFirstColumn.setCellValueFactory(new PropertyValueFactory<>("scoreFirst"));
        scoreSecondColumn.setCellValueFactory(new PropertyValueFactory<>("scoreSecond"));
        scoreFinalColumn.setCellValueFactory(new PropertyValueFactory<>("scoreFinal"));
        scoreOverallColumn.setCellValueFactory(new PropertyValueFactory<>("scoreOverall"));
        scoreTextColumn.setCellValueFactory(new PropertyValueFactory<>("scoreText"));
        
        // Custom cell factory cho tên môn học với icon
        subjectNameColumn.setCellFactory(column -> new TableCell<ScoreItem, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label label = new Label("📚 " + item);
                    label.setStyle("-fx-font-weight: 600; -fx-text-fill: #2c3e50;");
                    setGraphic(label);
                    setText(null);
                }
            }
        });
        
        // Custom cell factory cho tín chỉ
        creditColumn.setCellFactory(column -> new TableCell<ScoreItem, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label label = new Label("💎 " + item);
                    label.setStyle("-fx-font-weight: 500; -fx-text-fill: #555;");
                    setGraphic(label);
                    setText(null);
                    setAlignment(javafx.geometry.Pos.CENTER);
                }
            }
        });
        
        // Custom cell factory cho điểm số với màu random
        setupScoreColumn(scoreFirstColumn, "📝"); // Điểm thành phần 1 (Điểm GK)
        setupScoreColumn(scoreSecondColumn, "📋"); // Điểm thành phần 2 (QT)
        setupScoreColumn(scoreFinalColumn, "🎯");
        setupScoreColumn(scoreOverallColumn, "⭐");
        
        // Custom cell factory cho điểm chữ
        scoreTextColumn.setCellFactory(column -> new TableCell<ScoreItem, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String color = getScoreTextColor(item);
                    Label label = new Label(item);
                    label.setStyle(String.format(
                        "-fx-background-color: %s; -fx-background-radius: 15px; " +
                        "-fx-padding: 6px 12px; -fx-text-fill: white; -fx-font-weight: bold;",
                        color
                    ));
                    setGraphic(label);
                    setText(null);
                    setAlignment(javafx.geometry.Pos.CENTER);
                }
            }
        });
        
        // Highlight các môn học kì gần nhất và môn trượt
        scoresTable.setRowFactory(tv -> new TableRow<ScoreItem>() {
            @Override
            protected void updateItem(ScoreItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                    getStyleClass().removeAll("recent-semester", "failed");
                } else {
                    List<String> styleClasses = new ArrayList<>();
                    
                    if (item.isRecentSemester()) {
                        styleClasses.add("recent-semester");
                    }
                    
                    if (item.isFailed()) {
                        styleClasses.add("failed");
                    }
                    
                    getStyleClass().setAll(styleClasses);
                    
                    if (item.isFailed()) {
                        setStyle("-fx-background-color: #ffebee; -fx-background-insets: 0;");
                    } else if (item.isRecentSemester()) {
                        setStyle("-fx-background-color: #fff9c4; -fx-background-insets: 0;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
        
        // Setup info button
        if (infoButton != null) {
            infoButton.setText("ℹ");
            infoButton.setOnAction(e -> showGradeConversionTable());
        }
    }
    
    private void setupScoreColumn(TableColumn<ScoreItem, Double> column, String emoji) {
        column.setCellFactory(col -> new TableCell<ScoreItem, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String color = getScoreColor(item);
                    Label label = new Label(emoji + " " + String.format("%.1f", item));
                    label.setStyle(String.format(
                        "-fx-background-color: %s; -fx-background-radius: 15px; " +
                        "-fx-padding: 6px 12px; -fx-text-fill: white; -fx-font-weight: bold; " +
                        "-fx-font-size: 13px;",
                        color
                    ));
                    setGraphic(label);
                    setText(null);
                    setAlignment(javafx.geometry.Pos.CENTER);
                }
            }
        });
    }
    
    private String getScoreTextColor(String scoreText) {
        if (scoreText == null) return "#95a5a6";
        switch (scoreText.toUpperCase()) {
            case "A+": return "linear-gradient(to right, #11998e, #38ef7d)";
            case "A": return "linear-gradient(to right, #667eea, #764ba2)";
            case "B+": return "linear-gradient(to right, #4facfe, #00f2fe)";
            case "B": return "linear-gradient(to right, #43e97b, #38f9d7)";
            case "C+": return "linear-gradient(to right, #fa709a, #fee140)";
            case "C": return "linear-gradient(to right, #f093fb, #f5576c)";
            case "D+": return "linear-gradient(to right, #ffa726, #fb8c00)";
            case "D": return "linear-gradient(to right, #ff7043, #f4511e)";
            case "F": return "linear-gradient(to right, #ff6b6b, #ee5a6f)";
            default: return "#95a5a6";
        }
    }
    
    public void loadScores() {
        statusLabel.setText("Đang tải điểm thi...");
        scoresTable.getItems().clear();
        studentInfoLabel.setText("");
        gpaLabel.setText("GPA: -");
        cpaLabel.setText("CPA: -");
        if (formulaLabel != null) {
            formulaLabel.setText("📐 Công thức tính điểm: GPA = Σ(Điểm thang 4 × Số tín chỉ) / Σ(Số tín chỉ) | " +
                    "GPA tính theo các môn kì gần nhất, CPA tính theo tất cả các môn học");
        }
        
        new Thread(() -> {
            try {
                // Load credentials từ local storage
                JsonObject credentials = localStorageService.loadCredentials();
                if (credentials == null) {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.ERROR, "Lỗi", 
                                "Không tìm thấy thông tin đăng nhập. Vui lòng đăng nhập lại.");
                        handleBack();
                    });
                    return;
                }
                
                // Lấy studentCode từ student info
                JsonObject studentInfo = localStorageService.loadStudentInfo();
                if (studentInfo == null || !studentInfo.has("student_code")) {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.ERROR, "Lỗi", 
                                "Không tìm thấy mã sinh viên. Vui lòng đăng nhập lại.");
                        handleBack();
                    });
                    return;
                }
                
                String studentCode = studentInfo.get("student_code").getAsString();
                
                // Lấy public key
                String publicKey = apiService.getPublicKey();
                encryptionService.setPublicKey(publicKey);
                
                // Tạo JSON chứa studentCode để mã hóa
                JsonObject dataToEncrypt = new JsonObject();
                dataToEncrypt.addProperty("studentCode", studentCode);
                
                String dataString = dataToEncrypt.toString();
                
                // Mã hóa bằng hybrid encryption
                EncryptionService.EncryptionResult encryptionResult = 
                    encryptionService.encryptHybrid(dataString);
                
                // Gọi API điểm thi
                String response = apiService.getScores(
                    encryptionResult.getEncryptedKey(),
                    encryptionResult.getEncryptedData(),
                    encryptionResult.getIv()
                );
                
                // Parse response
                JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
                
                // Kiểm tra code nếu có
                if (jsonResponse.has("code")) {
                    String code = jsonResponse.get("code").getAsString();
                    if (!"200".equals(code)) {
                        String message = jsonResponse.has("message") ? 
                            jsonResponse.get("message").getAsString() : 
                            "Không thể tải điểm thi";
                        throw new IOException(message);
                    }
                }
                
                // Lấy dữ liệu từ response
                JsonObject listScoreDTO = jsonResponse.has("listScoreDTO") 
                    ? jsonResponse.getAsJsonObject("listScoreDTO") 
                    : null;
                JsonArray subjectDTOS = jsonResponse.has("subjectDTOS") 
                    ? jsonResponse.getAsJsonArray("subjectDTOS") 
                    : null;
                
                // Tạo set các môn học kì gần nhất để highlight
                Set<String> recentSemesterSubjects = new HashSet<>();
                if (subjectDTOS != null) {
                    for (JsonElement element : subjectDTOS) {
                        JsonObject subjectObj = element.getAsJsonObject();
                        if (subjectObj.has("subjectName") && !subjectObj.get("subjectName").isJsonNull()) {
                            recentSemesterSubjects.add(subjectObj.get("subjectName").getAsString());
                        }
                    }
                }
                
                // Parse student info
                String studentName = "";
                String studentCodeDisplay = "";
                String studentClass = "";
                if (listScoreDTO != null && listScoreDTO.has("studentDTO")) {
                    JsonObject studentDTO = listScoreDTO.getAsJsonObject("studentDTO");
                    studentName = getStringValue(studentDTO, "studentName");
                    studentCodeDisplay = getStringValue(studentDTO, "studentCode");
                    studentClass = getStringValue(studentDTO, "studentClass");
                }
                
                // Parse scores
                final JsonArray scoreDTOS = (listScoreDTO != null && listScoreDTO.has("scoreDTOS")) 
                    ? listScoreDTO.getAsJsonArray("scoreDTOS") 
                    : null;
                
                final String finalStudentName = studentName;
                final String finalStudentCode = studentCodeDisplay;
                final String finalStudentClass = studentClass;
                final Set<String> finalRecentSemesterSubjects = recentSemesterSubjects;
                
                Platform.runLater(() -> {
                    // Hiển thị thông tin sinh viên
                    StringBuilder studentInfoText = new StringBuilder();
                    if (!finalStudentName.isEmpty()) {
                        studentInfoText.append("Họ tên: ").append(finalStudentName);
                    }
                    if (!finalStudentCode.isEmpty()) {
                        if (studentInfoText.length() > 0) studentInfoText.append(" | ");
                        studentInfoText.append("Mã SV: ").append(finalStudentCode);
                    }
                    if (!finalStudentClass.isEmpty()) {
                        if (studentInfoText.length() > 0) studentInfoText.append(" | ");
                        studentInfoText.append("Lớp: ").append(finalStudentClass);
                    }
                    studentInfoLabel.setText(studentInfoText.toString());
                    
                    List<ScoreItem> allScores = new ArrayList<>();
                    List<ScoreItem> recentSemesterScores = new ArrayList<>();
                    
                    // Parse và hiển thị điểm
                    if (scoreDTOS != null) {
                        for (JsonElement element : scoreDTOS) {
                            JsonObject scoreObj = element.getAsJsonObject();
                            ScoreItem scoreItem = parseScoreItem(scoreObj);
                            
                            // Đảm bảo tính điểm chữ nếu chưa có
                            scoreItem.ensureScoreText();
                            
                            // Đánh dấu môn học kì gần nhất
                            if (finalRecentSemesterSubjects.contains(scoreItem.getSubjectName())) {
                                scoreItem.setRecentSemester(true);
                                recentSemesterScores.add(scoreItem);
                            }
                            
                            // Kiểm tra môn trượt
                            if (scoreItem.checkFailed()) {
                                scoreItem.setFailed(true);
                            }
                            
                            allScores.add(scoreItem);
                            scoresTable.getItems().add(scoreItem);
                        }
                    }
                    
                    // Tính GPA (theo môn kì gần nhất)
                    double gpa = calculateGPA(recentSemesterScores);
                    gpaLabel.setText(String.format("GPA: %.2f", gpa));
                    
                    // Tính CPA (tổng tất cả)
                    double cpa = calculateCPA(allScores);
                    cpaLabel.setText(String.format("CPA: %.2f", cpa));
                    
                    // Lưu backup scores để có thể restore trong VirtualScoresController
                    try {
                        JsonObject backupData = new JsonObject();
                        backupData.add("scoreDTOS", scoreDTOS);
                        localStorageService.saveBackupScores(backupData.toString());
                    } catch (Exception e) {
                        System.err.println("Không thể lưu backup scores: " + e.getMessage());
                    }
                    
                    statusLabel.setText("Đã tải " + scoresTable.getItems().size() + " môn học");
                });
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Lỗi khi tải điểm thi");
                    showAlert(Alert.AlertType.ERROR, "Lỗi", 
                            "Không thể tải điểm thi: " + e.getMessage());
                    e.printStackTrace();
                });
            }
        }).start();
    }
    
    /**
     * Tính GPA theo các môn học kì gần nhất
     */
    private double calculateGPA(List<ScoreItem> recentSemesterScores) {
        if (recentSemesterScores == null || recentSemesterScores.isEmpty()) {
            return 0.0;
        }
        
        double totalPoints = 0.0;
        int totalCredits = 0;
        
        for (ScoreItem item : recentSemesterScores) {
            double score4 = ScoreItem.convertToScale4(item.getScoreOverall());
            int credit = item.getSubjectCredit();
            
            if (credit > 0) {
                totalPoints += score4 * credit;
                totalCredits += credit;
            }
        }
        
        return totalCredits > 0 ? totalPoints / totalCredits : 0.0;
    }
    
    /**
     * Tính CPA theo tất cả các môn học
     */
    private double calculateCPA(List<ScoreItem> allScores) {
        if (allScores == null || allScores.isEmpty()) {
            return 0.0;
        }
        
        double totalPoints = 0.0;
        int totalCredits = 0;
        
        for (ScoreItem item : allScores) {
            double score4 = ScoreItem.convertToScale4(item.getScoreOverall());
            int credit = item.getSubjectCredit();
            
            if (credit > 0) {
                totalPoints += score4 * credit;
                totalCredits += credit;
            }
        }
        
        return totalCredits > 0 ? totalPoints / totalCredits : 0.0;
    }
    
    private ScoreItem parseScoreItem(JsonObject obj) {
        ScoreItem item = new ScoreItem();
        item.setSubjectName(getStringValue(obj, "subjectName"));
        item.setSubjectCredit(getIntValue(obj, "subjectCredit"));
        item.setScoreFirst(getDoubleValue(obj, "scoreFirst"));
        item.setScoreSecond(getDoubleValue(obj, "scoreSecond"));
        item.setScoreFinal(getDoubleValue(obj, "scoreFinal"));
        item.setScoreOverall(getDoubleValue(obj, "scoreOverall"));
        item.setScoreText(getStringValue(obj, "scoreText"));
        return item;
    }
    
    private String getStringValue(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return "";
    }
    
    private int getIntValue(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsInt();
        }
        return 0;
    }
    
    private double getDoubleValue(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsDouble();
        }
        return 0.0;
    }
    
    @FXML
    private void showGradeConversionTable() {
        Stage popupStage = new Stage();
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.initStyle(StageStyle.UTILITY);
        popupStage.setTitle("Bảng quy đổi điểm");
        
        VBox root = new VBox(15);
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: white;");
        
        Label titleLabel = new Label("📊 Bảng quy đổi điểm");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        // Tạo bảng
        VBox tableContainer = new VBox(0);
        tableContainer.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 8px; -fx-background-radius: 8px;");
        
        // Header
        HBox headerRow = new HBox();
        headerRow.setStyle("-fx-background-color: linear-gradient(to right, #667eea, #764ba2); -fx-background-radius: 8px 8px 0 0;");
        headerRow.setPadding(new Insets(12));
        headerRow.setSpacing(10);
        
        String[] headers = {"Thang 10", "Thang 4", "Điểm chữ", "Xếp loại"};
        for (String header : headers) {
            Label headerLabel = new Label(header);
            headerLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");
            headerLabel.setPrefWidth(120);
            headerRow.getChildren().add(headerLabel);
        }
        
        tableContainer.getChildren().add(headerRow);
        
        // Data rows
        String[][] data = {
            {"9.0 - 10.0", "4", "A+", "Xuất sắc"},
            {"8.5 - 8.9", "3.8", "A", "Giỏi"},
            {"7.8 - 8.4", "3.5", "B+", "Khá"},
            {"7.0 - 7.7", "3", "B", "Khá"},
            {"6.3 - 6.9", "2.4", "C+", "Trung bình"},
            {"5.5 - 6.2", "2", "C", "Trung bình"},
            {"4.8 - 5.4", "1.5", "D+", "Trung bình yếu"},
            {"4.0 - 4.7", "1", "D", "Trung bình yếu"},
            {"0.0 - 3.9", "0", "F", "Kém"}
        };
        
        for (int i = 0; i < data.length; i++) {
            HBox dataRow = new HBox();
            dataRow.setPadding(new Insets(10, 12, 10, 12));
            dataRow.setSpacing(10);
            if (i % 2 == 0) {
                dataRow.setStyle("-fx-background-color: #f8f9fa;");
            } else {
                dataRow.setStyle("-fx-background-color: white;");
            }
            
            for (String cell : data[i]) {
                Label cellLabel = new Label(cell);
                cellLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 12px;");
                cellLabel.setPrefWidth(120);
                dataRow.getChildren().add(cellLabel);
            }
            
            tableContainer.getChildren().add(dataRow);
        }
        
        // Note
        Label noteLabel = new Label("💡 Lưu ý: GPA tính theo các môn kì gần nhất, CPA tính theo tất cả các môn học");
        noteLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px; -fx-wrap-text: true;");
        noteLabel.setMaxWidth(500);
        
        Button closeButton = new Button("Đóng");
        closeButton.setStyle("-fx-background-color: #667eea; -fx-text-fill: white; -fx-pref-width: 100px; -fx-pref-height: 35px; -fx-background-radius: 5px; -fx-cursor: hand;");
        closeButton.setOnAction(e -> popupStage.close());
        
        root.getChildren().addAll(titleLabel, tableContainer, noteLabel, closeButton);
        root.setAlignment(Pos.CENTER);
        
        Scene scene = new Scene(root, 550, 600);
        popupStage.setScene(scene);
        popupStage.setResizable(false);
        popupStage.showAndWait();
    }
    
    @FXML
    private void handleViewVirtualScores() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/VirtualScoresScreen.fxml"));
            Parent root = loader.load();
            
            VirtualScoresController controller = loader.getController();
            controller.loadVirtualScores();
            
            Stage virtualScoresStage = new Stage();
            virtualScoresStage.setScene(new Scene(root, 1800, 1000));
            virtualScoresStage.setTitle("Bảng điểm ảo");
            virtualScoresStage.setMinWidth(1400);
            virtualScoresStage.setMinHeight(800);
            
            // Setup close handler để hiển thị cảnh báo khi đóng
            controller.setupCloseHandler(virtualScoresStage);
            
            // Không đóng màn hình xem điểm thi, để có thể quay lại sau khi đóng bảng điểm ảo
            virtualScoresStage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể mở bảng điểm ảo: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainScreen.fxml"));
            Parent root = loader.load();
            
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
