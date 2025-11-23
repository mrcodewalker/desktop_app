package org.example.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.example.model.ScoreItem;
import org.example.model.VirtualScoreItem;
import org.example.service.ApiService;
import org.example.service.EncryptionService;
import org.example.service.LocalStorageService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class VirtualScoresController {
    @FXML
    private TableView<VirtualScoreItem> scoresTable;
    
    @FXML
    private TableColumn<VirtualScoreItem, Boolean> selectedColumn;
    
    @FXML
    private TableColumn<VirtualScoreItem, String> subjectNameColumn;
    
    @FXML
    private TableColumn<VirtualScoreItem, Integer> creditColumn;
    
    @FXML
    private TableColumn<VirtualScoreItem, Double> scoreFirstColumn;
    
    @FXML
    private TableColumn<VirtualScoreItem, Double> scoreSecondColumn;
    
    @FXML
    private TableColumn<VirtualScoreItem, Double> scoreFinalColumn;
    
    @FXML
    private TableColumn<VirtualScoreItem, Double> scoreOverallColumn;
    
    @FXML
    private TableColumn<VirtualScoreItem, String> scoreTextColumn;
    
    @FXML
    private Button addSubjectButton;
    
    @FXML
    private Button restoreButton;
    
    @FXML
    private Button selectAllButton;
    
    @FXML
    private Button conversionTableButton;
    
    @FXML
    private Button saveButton;
    
    @FXML
    private Button backButton;
    
    @FXML
    private Label statusLabel;
    
    @FXML
    private Label studentInfoLabel;
    
    @FXML
    private Label statisticsLabel;
    
    @FXML
    private Label gpaLabel;
    
    @FXML
    private Label noteLabel;
    
    @FXML
    private TableColumn<VirtualScoreItem, Void> actionColumn;
    
    private ApiService apiService;
    private EncryptionService encryptionService;
    private LocalStorageService localStorageService;
    
    // Lưu thông tin batch để gửi lại khi save
    private Long batchId;
    private Long studentId;
    private String studentName;
    private String studentClass;
    private String lastUpdated;
    
    // Track changes để hiển thị cảnh báo khi đóng
    private boolean hasUnsavedChanges = false;
    
    @FXML
    public void initialize() {
        apiService = ApiService.getInstance();
        encryptionService = EncryptionService.getInstance();
        localStorageService = LocalStorageService.getInstance();
        
        // Setup table columns
        selectedColumn.setCellValueFactory(new PropertyValueFactory<>("selected"));
        selectedColumn.setEditable(true);
        
        subjectNameColumn.setCellValueFactory(new PropertyValueFactory<>("subjectName"));
        subjectNameColumn.setEditable(true);
        
        creditColumn.setCellValueFactory(new PropertyValueFactory<>("subjectCredit"));
        creditColumn.setCellFactory(column -> {
            TextFieldTableCell<VirtualScoreItem, Integer> cell = new TextFieldTableCell<>(new IntegerStringConverter());
            cell.setAlignment(Pos.CENTER);
            return cell;
        });
        creditColumn.setEditable(true);
        
        scoreFirstColumn.setCellValueFactory(new PropertyValueFactory<>("scoreFirst"));
        scoreFirstColumn.setCellFactory(column -> {
            TextFieldTableCell<VirtualScoreItem, Double> cell = new TextFieldTableCell<>(new DoubleStringConverter());
            cell.setAlignment(Pos.CENTER);
            return cell;
        });
        scoreFirstColumn.setEditable(true);
        
        scoreSecondColumn.setCellValueFactory(new PropertyValueFactory<>("scoreSecond"));
        scoreSecondColumn.setCellFactory(column -> {
            TextFieldTableCell<VirtualScoreItem, Double> cell = new TextFieldTableCell<>(new DoubleStringConverter());
            cell.setAlignment(Pos.CENTER);
            return cell;
        });
        scoreSecondColumn.setEditable(true);
        
        scoreFinalColumn.setCellValueFactory(new PropertyValueFactory<>("scoreFinal"));
        scoreFinalColumn.setCellFactory(column -> {
            TextFieldTableCell<VirtualScoreItem, Double> cell = new TextFieldTableCell<>(new DoubleStringConverter());
            cell.setAlignment(Pos.CENTER);
            return cell;
        });
        scoreFinalColumn.setEditable(true);
        
        scoreOverallColumn.setCellValueFactory(new PropertyValueFactory<>("scoreOverall"));
        scoreOverallColumn.setEditable(false);
        
        scoreTextColumn.setCellValueFactory(new PropertyValueFactory<>("scoreText"));
        scoreTextColumn.setEditable(false);
        
        scoresTable.setEditable(true);
        
        // Setup listeners for score changes
        setupScoreChangeListeners();
        
        // Setup row factory for highlighting failed subjects
        setupRowFactory();
        
        // Listen to selection changes to recalculate GPA and update select all button
        scoresTable.getItems().addListener((javafx.collections.ListChangeListener.Change<? extends VirtualScoreItem> c) -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (VirtualScoreItem item : c.getAddedSubList()) {
                        // Listen to property changes for new items
                        item.selectedProperty().addListener((obs, oldVal, newVal) -> {
                            calculateGPA();
                            updateSelectAllButtonText();
                        });
                        item.scoreOverallProperty().addListener((obs, oldVal, newVal) -> calculateGPA());
                        item.subjectCreditProperty().addListener((obs, oldVal, newVal) -> calculateGPA());
                    }
                    calculateGPA();
                    updateSelectAllButtonText();
                } else if (c.wasRemoved()) {
                    calculateGPA();
                    updateSelectAllButtonText();
                }
            }
        });
        
        // Format điểm số columns
        scoreOverallColumn.setCellFactory(column -> new TableCell<VirtualScoreItem, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f", item));
                    setAlignment(javafx.geometry.Pos.CENTER);
                }
            }
        });
        
        scoreTextColumn.setCellFactory(column -> new TableCell<VirtualScoreItem, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isEmpty()) {
                    setText(null);
                } else {
                    setText(item);
                    setAlignment(Pos.CENTER);
                }
            }
        });
        
        // Căn giữa cột checkbox - sử dụng custom cell factory
        selectedColumn.setCellFactory(column -> {
            CheckBoxTableCell<VirtualScoreItem, Boolean> cell = new CheckBoxTableCell<VirtualScoreItem, Boolean>() {
                @Override
                public void updateItem(Boolean item, boolean empty) {
                    super.updateItem(item, empty);
                    if (!empty) {
                        setAlignment(Pos.CENTER);
                    }
                }
            };
            return cell;
        });
        
        // Căn giữa cột tên môn học - đã được set ở trên, chỉ cần thêm alignment
        subjectNameColumn.setCellFactory(column -> {
            TextFieldTableCell<VirtualScoreItem, String> cell = new TextFieldTableCell<VirtualScoreItem, String>() {
                @Override
                public void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (!empty) {
                        setAlignment(Pos.CENTER);
                    }
                }
            };
            return cell;
        });
        
        // Căn giữa cột action
        actionColumn.setCellFactory(param -> new TableCell<VirtualScoreItem, Void>() {
            private final Button deleteButton = new Button("Xóa");
            
            {
                deleteButton.setStyle("-fx-background-color: #ff5252; -fx-text-fill: white; " +
                        "-fx-background-radius: 5px; -fx-padding: 5px 10px; -fx-cursor: hand;");
                deleteButton.setOnAction(event -> {
                    VirtualScoreItem item = getTableView().getItems().get(getIndex());
                    if (item != null) {
                        scoresTable.getItems().remove(item);
                        hasUnsavedChanges = true;
                        calculateGPA();
                    }
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(deleteButton);
                    box.setAlignment(Pos.CENTER);
                    setGraphic(box);
                }
            }
        });
        
        // Setup note label
        if (noteLabel != null) {
            noteLabel.setText("💡 Lưu ý: GPA tính theo các môn đã chọn (không tính Giáo dục thể chất). " +
                    "Điểm tổng kết = (TP1 × 0.7 + TP2 × 0.3) × 0.3 + Cuối kỳ × 0.7");
            noteLabel.setWrapText(true);
        }
        
        // Setup add button
        if (addSubjectButton != null) {
            addSubjectButton.setOnAction(e -> addNewSubject());
        }
        
        // Setup restore button
        if (restoreButton != null) {
            restoreButton.setOnAction(e -> restoreFromBackupScores());
        }
        
        // Setup select all button
        if (selectAllButton != null) {
            selectAllButton.setOnAction(e -> selectAllSubjects());
        }
        
        // Setup conversion table button
        if (conversionTableButton != null) {
            conversionTableButton.setOnAction(e -> showGradeConversionTable());
        }
        
        // Setup save button
        if (saveButton != null) {
            saveButton.setOnAction(e -> saveScoresToSystem());
        }
    }
    
    
    private void setupScoreChangeListeners() {
        // Listen to cell edit events
        scoreFirstColumn.setOnEditCommit(event -> {
            VirtualScoreItem item = event.getRowValue();
            item.setScoreFirst(event.getNewValue());
            item.calculateOverallScore();
            hasUnsavedChanges = true;
            calculateGPA();
            scoresTable.refresh();
        });
        
        scoreSecondColumn.setOnEditCommit(event -> {
            VirtualScoreItem item = event.getRowValue();
            item.setScoreSecond(event.getNewValue());
            item.calculateOverallScore();
            hasUnsavedChanges = true;
            calculateGPA();
            scoresTable.refresh();
        });
        
        scoreFinalColumn.setOnEditCommit(event -> {
            VirtualScoreItem item = event.getRowValue();
            item.setScoreFinal(event.getNewValue());
            item.calculateOverallScore();
            hasUnsavedChanges = true;
            calculateGPA();
            scoresTable.refresh();
        });
        
        subjectNameColumn.setOnEditCommit(event -> {
            VirtualScoreItem item = event.getRowValue();
            item.setSubjectName(event.getNewValue());
            hasUnsavedChanges = true;
        });
        
        creditColumn.setOnEditCommit(event -> {
            VirtualScoreItem item = event.getRowValue();
            item.setSubjectCredit(event.getNewValue());
            hasUnsavedChanges = true;
            calculateGPA(); // This will also update statistics
        });
    }
    
    private void setupRowFactory() {
        scoresTable.setRowFactory(tv -> new TableRow<VirtualScoreItem>() {
            @Override
            protected void updateItem(VirtualScoreItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else {
                    // Kiểm tra lại trạng thái trượt
                    if (item.checkFailed()) {
                        // Highlight màu đỏ cho môn trượt
                        setStyle("-fx-background-color: #ffcdd2; -fx-background-insets: 0;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
    }
    
    public void loadVirtualScores() {
        statusLabel.setText("Đang tải bảng điểm ảo...");
        scoresTable.getItems().clear();
        studentInfoLabel.setText("");
        gpaLabel.setText("GPA: -");
        hasUnsavedChanges = false; // Reset khi tải lại
        if (statisticsLabel != null) {
            statisticsLabel.setText("");
        }
        
        new Thread(() -> {
            try {
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
                
                // Gọi API bảng điểm ảo
                String response = apiService.getScoreBatch(studentCode);
                
                // Parse response
                JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
                
                String studentNameValue = getStringValue(jsonResponse, "studentName");
                String studentCodeDisplay = getStringValue(jsonResponse, "studentCode");
                String studentClassValue = getStringValue(jsonResponse, "studentClass");
                String lastUpdatedValue = getStringValue(jsonResponse, "lastUpdated");
                
                // Lưu batchId và studentId nếu có
                final Long batchIdValue;
                if (jsonResponse.has("batchId") && !jsonResponse.get("batchId").isJsonNull()) {
                    batchIdValue = jsonResponse.get("batchId").getAsLong();
                } else {
                    batchIdValue = null;
                }
                
                final Long studentIdValue;
                if (jsonResponse.has("studentId") && !jsonResponse.get("studentId").isJsonNull()) {
                    studentIdValue = jsonResponse.get("studentId").getAsLong();
                } else {
                    studentIdValue = null;
                }
                
                JsonArray scoreItems = jsonResponse.has("scoreItems") 
                    ? jsonResponse.getAsJsonArray("scoreItems") 
                    : null;
                
                final String finalStudentName = studentNameValue;
                final String finalStudentClass = studentClassValue;
                final String finalLastUpdated = lastUpdatedValue;
                
                Platform.runLater(() -> {
                    // Lưu thông tin để dùng khi save
                    batchId = batchIdValue;
                    studentId = studentIdValue;
                    studentName = finalStudentName;
                    studentClass = finalStudentClass;
                    lastUpdated = finalLastUpdated;
                    
                    // Hiển thị thông tin sinh viên
                    StringBuilder studentInfoText = new StringBuilder();
                    if (!finalStudentName.isEmpty()) {
                        studentInfoText.append("Họ tên: ").append(finalStudentName);
                    }
                    if (!studentCodeDisplay.isEmpty()) {
                        if (studentInfoText.length() > 0) studentInfoText.append(" | ");
                        studentInfoText.append("Mã SV: ").append(studentCodeDisplay);
                    }
                    if (!finalStudentClass.isEmpty()) {
                        if (studentInfoText.length() > 0) studentInfoText.append(" | ");
                        studentInfoText.append("Lớp: ").append(finalStudentClass);
                    }
                    if (!finalLastUpdated.isEmpty()) {
                        if (studentInfoText.length() > 0) studentInfoText.append(" | ");
                        studentInfoText.append("Cập nhật: ").append(finalLastUpdated);
                    }
                    studentInfoLabel.setText(studentInfoText.toString());
                    
                    // Parse và hiển thị điểm
                    if (scoreItems != null) {
                        for (JsonElement element : scoreItems) {
                            JsonObject scoreObj = element.getAsJsonObject();
                            VirtualScoreItem scoreItem = parseScoreItem(scoreObj);
                            
                            // Đảm bảo tính điểm chữ nếu chưa có
                            scoreItem.ensureScoreText();
                            
                            // Add listeners for GPA calculation and statistics
                            scoreItem.selectedProperty().addListener((obs, oldVal, newVal) -> calculateGPA());
                            scoreItem.scoreOverallProperty().addListener((obs, oldVal, newVal) -> calculateGPA());
                            scoreItem.subjectCreditProperty().addListener((obs, oldVal, newVal) -> calculateGPA());
                            
                            scoresTable.getItems().add(scoreItem);
                        }
                    }
                    
                    calculateGPA(); // This will also update statistics
                    updateSelectAllButtonText();
                    statusLabel.setText("Đã tải " + scoresTable.getItems().size() + " môn học");
                });
                
            } catch (IOException e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Lỗi khi tải bảng điểm ảo");
                    if (e.getMessage().contains("404") || e.getMessage().contains("Failed")) {
                        showAlert(Alert.AlertType.INFORMATION, "Thông báo", 
                                "Bạn chưa có điểm ở bảng điểm ảo.");
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Lỗi", 
                                "Không thể tải bảng điểm ảo: " + e.getMessage());
                    }
                    e.printStackTrace();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Lỗi khi tải bảng điểm ảo");
                    showAlert(Alert.AlertType.ERROR, "Lỗi", 
                            "Không thể tải bảng điểm ảo: " + e.getMessage());
                    e.printStackTrace();
                });
            }
        }).start();
    }
    
    private VirtualScoreItem parseScoreItem(JsonObject obj) {
        VirtualScoreItem item = new VirtualScoreItem();
        item.setItemId(obj.has("itemId") && !obj.get("itemId").isJsonNull() 
            ? obj.get("itemId").getAsLong() : null);
        item.setSubjectName(getStringValue(obj, "subjectName"));
        item.setSubjectCredit(getIntValue(obj, "subjectCredit"));
        item.setScoreFirst(getDoubleValue(obj, "scoreFirst"));
        item.setScoreSecond(getDoubleValue(obj, "scoreSecond"));
        item.setScoreFinal(getDoubleValue(obj, "scoreFinal"));
        
        double scoreOverall = getDoubleValue(obj, "scoreOverall");
        // Nếu điểm tổng kết chưa có hoặc bằng 0 nhưng có điểm thành phần, tính lại
        if (scoreOverall == 0.0 && (item.getScoreFirst() > 0 || item.getScoreSecond() > 0 || item.getScoreFinal() > 0)) {
            item.calculateOverallScore();
        } else {
            item.setScoreOverall(scoreOverall);
        }
        
        String scoreText = getStringValue(obj, "scoreText");
        if (scoreText == null || scoreText.isEmpty()) {
            // Tính điểm chữ nếu chưa có
            item.ensureScoreText();
        } else {
            item.setScoreText(scoreText);
        }
        
        item.setSelected(obj.has("isSelected") && obj.get("isSelected").getAsBoolean());
        return item;
    }
    
    private void addNewSubject() {
        VirtualScoreItem newItem = new VirtualScoreItem();
        newItem.setNewItem(true);
        newItem.setSubjectName("Môn học mới");
        newItem.setSubjectCredit(2);
        newItem.setScoreFirst(0.0);
        newItem.setScoreSecond(0.0);
        newItem.setScoreFinal(0.0);
        newItem.calculateOverallScore(); // Tính điểm tổng kết và điểm chữ
        newItem.setSelected(true);
        
        // Add listeners for GPA calculation and statistics
        newItem.selectedProperty().addListener((obs, oldVal, newVal) -> calculateGPA());
        newItem.scoreOverallProperty().addListener((obs, oldVal, newVal) -> calculateGPA());
        newItem.subjectCreditProperty().addListener((obs, oldVal, newVal) -> calculateGPA());
        
        scoresTable.getItems().add(newItem);
        hasUnsavedChanges = true;
        scoresTable.getSelectionModel().select(newItem);
        scoresTable.scrollTo(newItem);
        
        // Edit the subject name cell
        Platform.runLater(() -> {
            scoresTable.edit(scoresTable.getItems().size() - 1, subjectNameColumn);
        });
    }
    
    private void saveScoresToSystem() {
        statusLabel.setText("Đang lưu điểm lên hệ thống...");
        
        new Thread(() -> {
            try {
                // Lấy studentCode từ student info
                JsonObject studentInfo = localStorageService.loadStudentInfo();
                if (studentInfo == null || !studentInfo.has("student_code")) {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.ERROR, "Lỗi", 
                                "Không tìm thấy mã sinh viên. Vui lòng đăng nhập lại.");
                    });
                    return;
                }
                
                String studentCode = studentInfo.get("student_code").getAsString();
                
                // Lấy public key và thiết lập encryption service
                String publicKey = apiService.getPublicKey();
                encryptionService.setPublicKey(publicKey);
                
                // Tạo payload theo đúng cấu trúc DTO ScoreBatchRequestDTO
                JsonObject payload = new JsonObject();
                
                // Tạo studentInfo object
                JsonObject studentInfoObj = new JsonObject();
                if (studentId != null) {
                    studentInfoObj.addProperty("studentId", studentId);
                }
                studentInfoObj.addProperty("studentCode", studentCode);
                if (studentName != null && !studentName.isEmpty()) {
                    studentInfoObj.addProperty("studentName", studentName);
                }
                if (studentClass != null && !studentClass.isEmpty()) {
                    studentInfoObj.addProperty("studentClass", studentClass);
                }
                payload.add("studentInfo", studentInfoObj);
                
                // Tạo scores array (không phải scoreItems)
                JsonArray scores = new JsonArray();
                for (VirtualScoreItem item : scoresTable.getItems()) {
                    JsonObject scoreItem = new JsonObject();
                    scoreItem.addProperty("scoreText", item.getScoreText() != null ? item.getScoreText() : "");
                    scoreItem.addProperty("scoreFirst", item.getScoreFirst());
                    scoreItem.addProperty("scoreSecond", item.getScoreSecond());
                    scoreItem.addProperty("scoreFinal", item.getScoreFinal());
                    scoreItem.addProperty("scoreOverall", item.getScoreOverall());
                    scoreItem.addProperty("subjectName", item.getSubjectName());
                    scoreItem.addProperty("subjectCredit", item.getSubjectCredit());
                    scoreItem.addProperty("isSelected", item.isSelected());
                    scores.add(scoreItem);
                }
                payload.add("scores", scores);
                
                // Thêm lastUpdated nếu có
                if (lastUpdated != null && !lastUpdated.isEmpty()) {
                    payload.addProperty("lastUpdated", lastUpdated);
                }
                
                // Mã hóa payload bằng hybrid encryption
                String dataToEncrypt = payload.toString();
                EncryptionService.EncryptionResult encryptionResult = 
                    encryptionService.encryptHybrid(dataToEncrypt);
                
                // Tạo request body với encrypted data
                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("encryptedKey", encryptionResult.getEncryptedKey());
                requestBody.addProperty("encryptedData", encryptionResult.getEncryptedData());
                requestBody.addProperty("iv", encryptionResult.getIv());
                
                // Gọi API POST /api/v1/score-batch/create-or-update
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .writeTimeout(30, TimeUnit.SECONDS)
                        .build();
                
                RequestBody body = RequestBody.create(
                        requestBody.toString(),
                        MediaType.get("application/json; charset=utf-8")
                );
                
                Request request = new Request.Builder()
                        .url(apiService.getBaseUrl() + "/api/v1/score-batch/create-or-update")
                        .post(body)
                        .build();
                
                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body().string();
                    
                    Platform.runLater(() -> {
                        // Nếu response thành công (status code 200-299), coi như thành công
                        if (response.isSuccessful()) {
                            hasUnsavedChanges = false; // Đánh dấu đã lưu
                            statusLabel.setText("Đã lưu thành công " + scoresTable.getItems().size() + " môn học");
                            showAlert(Alert.AlertType.INFORMATION, "Thành công", 
                                    "Đã lưu điểm lên hệ thống thành công!");
                            
                            // Cập nhật batchId nếu có trong response
                            try {
                                JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
                                if (jsonResponse.has("batchId") && !jsonResponse.get("batchId").isJsonNull()) {
                                    batchId = jsonResponse.get("batchId").getAsLong();
                                }
                            } catch (Exception e) {
                                // Ignore parsing errors if response is not JSON
                            }
                        } else {
                            // Response không thành công
                            String message = "Không thể lưu điểm";
                            try {
                                JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
                                if (jsonResponse.has("message")) {
                                    message = jsonResponse.get("message").getAsString();
                                }
                            } catch (Exception e) {
                                message = "Lỗi HTTP " + response.code();
                            }
                            statusLabel.setText("Lỗi khi lưu điểm");
                            showAlert(Alert.AlertType.ERROR, "Lỗi", message);
                        }
                    });
                }
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Lỗi khi lưu điểm");
                    showAlert(Alert.AlertType.ERROR, "Lỗi", 
                            "Không thể lưu điểm lên hệ thống: " + e.getMessage());
                    e.printStackTrace();
                });
            }
        }).start();
    }
    
    private void calculateGPA() {
        List<VirtualScoreItem> selectedItems = new ArrayList<>();
        List<VirtualScoreItem> completedItems = new ArrayList<>();
        List<VirtualScoreItem> failedItems = new ArrayList<>();
        int totalCompletedCredits = 0;
        
        for (VirtualScoreItem item : scoresTable.getItems()) {
            if (item.isSelected() && !item.isPhysicalEducation()) {
                selectedItems.add(item);
            }
            
            // Tính toán thống kê cho tất cả các môn (không chỉ môn đã chọn)
            if (!item.isPhysicalEducation() && item.getScoreOverall() > 0) {
                if (!item.checkFailed()) {
                    completedItems.add(item);
                    totalCompletedCredits += item.getSubjectCredit();
                } else {
                    failedItems.add(item);
                }
            }
        }
        
        // Tính GPA
        if (selectedItems.isEmpty()) {
            gpaLabel.setText("GPA: -");
        } else {
            double totalPoints = 0.0;
            int totalCredits = 0;
            
            for (VirtualScoreItem item : selectedItems) {
                double score4 = ScoreItem.convertToScale4(item.getScoreOverall());
                int credit = item.getSubjectCredit();
                
                if (credit > 0) {
                    totalPoints += score4 * credit;
                    totalCredits += credit;
                }
            }
            
            double gpa = totalCredits > 0 ? totalPoints / totalCredits : 0.0;
            gpa = Math.round(gpa * 100.0) / 100.0;
            
            gpaLabel.setText(String.format("GPA: %.2f", gpa));
        }
        
        // Cập nhật thống kê
        updateStatistics(completedItems.size(), failedItems.size(), totalCompletedCredits);
    }
    
    private void updateStatistics(int completedCount, int failedCount, int totalCredits) {
        if (statisticsLabel != null) {
            String stats = String.format("✅ Hoàn thành: %d môn | ❌ Chưa đạt: %d môn | 💎 Tín chỉ: %d", 
                    completedCount, failedCount, totalCredits);
            statisticsLabel.setText(stats);
        }
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
    private void handleBack() {
        if (hasUnsavedChanges) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Xác nhận");
            alert.setHeaderText(null);
            alert.setContentText("Bạn có chắc chắn muốn đóng? Các thay đổi chưa được lưu sẽ bị mất.");
            
            ButtonType buttonTypeYes = new ButtonType("Đóng", ButtonBar.ButtonData.YES);
            ButtonType buttonTypeNo = new ButtonType("Hủy", ButtonBar.ButtonData.NO);
            alert.getButtonTypes().setAll(buttonTypeYes, buttonTypeNo);
            
            alert.showAndWait().ifPresent(type -> {
                if (type == buttonTypeYes) {
                    Stage stage = (Stage) backButton.getScene().getWindow();
                    stage.close();
                }
            });
        } else {
            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.close();
        }
    }
    
    // Override để bắt sự kiện đóng window
    public void setupCloseHandler(Stage stage) {
        stage.setOnCloseRequest(event -> {
            if (hasUnsavedChanges) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Xác nhận");
                alert.setHeaderText(null);
                alert.setContentText("Bạn có chắc chắn muốn đóng? Các thay đổi chưa được lưu sẽ bị mất.");
                
                ButtonType buttonTypeYes = new ButtonType("Đóng", ButtonBar.ButtonData.YES);
                ButtonType buttonTypeNo = new ButtonType("Hủy", ButtonBar.ButtonData.NO);
                alert.getButtonTypes().setAll(buttonTypeYes, buttonTypeNo);
                
                alert.showAndWait().ifPresent(type -> {
                    if (type == buttonTypeYes) {
                        // Cho phép đóng
                    } else {
                        event.consume(); // Ngăn đóng window
                    }
                });
            }
        });
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
        Label noteLabel = new Label("💡 Lưu ý: GPA tính theo các môn đã chọn (không tính Giáo dục thể chất)");
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
    
    private void restoreFromBackupScores() {
        statusLabel.setText("Đang khôi phục điểm từ bảng điểm thi...");
        
        new Thread(() -> {
            try {
                // Lấy backup scores từ local storage
                String backupJson = localStorageService.loadBackupScores();
                if (backupJson == null || backupJson.isEmpty()) {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.INFORMATION, "Thông báo", 
                                "Không tìm thấy điểm backup. Vui lòng vào màn hình 'Xem điểm thi' trước để lưu backup.");
                        statusLabel.setText("Không có điểm backup");
                    });
                    return;
                }
                
                // Parse backup JSON
                JsonObject backupData = JsonParser.parseString(backupJson).getAsJsonObject();
                JsonArray scoreDTOS = backupData.has("scoreDTOS") 
                    ? backupData.getAsJsonArray("scoreDTOS") 
                    : null;
                
                if (scoreDTOS == null || scoreDTOS.size() == 0) {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.INFORMATION, "Thông báo", 
                                "Không có điểm nào trong backup.");
                        statusLabel.setText("Không có điểm trong backup");
                    });
                    return;
                }
                
                // Convert ScoreItem từ backup sang VirtualScoreItem
                List<VirtualScoreItem> restoredItems = new ArrayList<>();
                for (JsonElement element : scoreDTOS) {
                    JsonObject scoreObj = element.getAsJsonObject();
                    VirtualScoreItem virtualItem = new VirtualScoreItem();
                    
                    virtualItem.setSubjectName(getStringValue(scoreObj, "subjectName"));
                    virtualItem.setSubjectCredit(getIntValue(scoreObj, "subjectCredit"));
                    virtualItem.setScoreFirst(getDoubleValue(scoreObj, "scoreFirst"));
                    virtualItem.setScoreSecond(getDoubleValue(scoreObj, "scoreSecond"));
                    virtualItem.setScoreFinal(getDoubleValue(scoreObj, "scoreFinal"));
                    virtualItem.setScoreOverall(getDoubleValue(scoreObj, "scoreOverall"));
                    
                    String scoreText = getStringValue(scoreObj, "scoreText");
                    if (scoreText == null || scoreText.isEmpty()) {
                        virtualItem.ensureScoreText();
                    } else {
                        virtualItem.setScoreText(scoreText);
                    }
                    
                    virtualItem.setSelected(true); // Mặc định chọn tất cả khi restore
                    
                    // Add listeners
                    virtualItem.selectedProperty().addListener((obs, oldVal, newVal) -> calculateGPA());
                    virtualItem.scoreOverallProperty().addListener((obs, oldVal, newVal) -> calculateGPA());
                    
                    restoredItems.add(virtualItem);
                }
                
                Platform.runLater(() -> {
                    // Xóa các môn hiện tại và thay thế bằng môn từ backup
                    scoresTable.getItems().clear();
                    scoresTable.getItems().addAll(restoredItems);
                    
                    hasUnsavedChanges = true; // Đánh dấu có thay đổi sau khi restore
                    calculateGPA();
                    updateSelectAllButtonText();
                    statusLabel.setText("Đã khôi phục " + restoredItems.size() + " môn học từ backup");
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", 
                            "Đã khôi phục " + restoredItems.size() + " môn học từ bảng điểm thi.");
                });
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Lỗi khi khôi phục điểm");
                    showAlert(Alert.AlertType.ERROR, "Lỗi", 
                            "Không thể khôi phục điểm từ backup: " + e.getMessage());
                    e.printStackTrace();
                });
            }
        }).start();
    }
    
    private void selectAllSubjects() {
        if (scoresTable.getItems().isEmpty()) {
            return;
        }
        
        boolean allSelected = scoresTable.getItems().stream()
                .allMatch(VirtualScoreItem::isSelected);
        
        // Nếu tất cả đã được chọn, bỏ chọn tất cả. Ngược lại, chọn tất cả.
        boolean newValue = !allSelected;
        
        for (VirtualScoreItem item : scoresTable.getItems()) {
            item.setSelected(newValue);
        }
        
        scoresTable.refresh();
        calculateGPA();
        updateSelectAllButtonText();
    }
    
    private void updateSelectAllButtonText() {
        if (selectAllButton != null && !scoresTable.getItems().isEmpty()) {
            boolean allSelected = scoresTable.getItems().stream()
                    .allMatch(VirtualScoreItem::isSelected);
            selectAllButton.setText(allSelected ? "☐ Bỏ chọn tất cả" : "✓ Chọn tất cả");
        } else if (selectAllButton != null) {
            selectAllButton.setText("✓ Chọn tất cả");
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

