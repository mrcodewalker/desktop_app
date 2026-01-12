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
    private Button cpaCalculatorButton;

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
        scoresTable.getItems()
                .addListener((javafx.collections.ListChangeListener.Change<? extends VirtualScoreItem> c) -> {
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

        // Căn giữa cột tên môn học - thêm DefaultStringConverter để Enter có thể lưu
        // được
        subjectNameColumn.setCellFactory(column -> {
            TextFieldTableCell<VirtualScoreItem, String> cell = new TextFieldTableCell<>(
                    new javafx.util.converter.DefaultStringConverter());
            cell.setAlignment(Pos.CENTER);
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

        // Setup CPA calculator button
        if (cpaCalculatorButton != null) {
            cpaCalculatorButton.setOnAction(e -> showCPACalculator());
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
                        if (studentInfoText.length() > 0)
                            studentInfoText.append(" | ");
                        studentInfoText.append("Mã SV: ").append(studentCodeDisplay);
                    }
                    if (!finalStudentClass.isEmpty()) {
                        if (studentInfoText.length() > 0)
                            studentInfoText.append(" | ");
                        studentInfoText.append("Lớp: ").append(finalStudentClass);
                    }
                    if (!finalLastUpdated.isEmpty()) {
                        if (studentInfoText.length() > 0)
                            studentInfoText.append(" | ");
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
                ? obj.get("itemId").getAsLong()
                : null);
        item.setSubjectName(getStringValue(obj, "subjectName"));
        item.setSubjectCredit(getIntValue(obj, "subjectCredit"));
        item.setScoreFirst(getDoubleValue(obj, "scoreFirst"));
        item.setScoreSecond(getDoubleValue(obj, "scoreSecond"));
        item.setScoreFinal(getDoubleValue(obj, "scoreFinal"));

        double scoreOverall = getDoubleValue(obj, "scoreOverall");
        // Nếu điểm tổng kết chưa có hoặc bằng 0 nhưng có điểm thành phần, tính lại
        if (scoreOverall == 0.0
                && (item.getScoreFirst() > 0 || item.getScoreSecond() > 0 || item.getScoreFinal() > 0)) {
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
                EncryptionService.EncryptionResult encryptionResult = encryptionService.encryptHybrid(dataToEncrypt);

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
                        MediaType.get("application/json; charset=utf-8"));

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
        // Dùng WINDOW_MODAL thay vì APPLICATION_MODAL để tránh trigger events với các
        // window khác
        popupStage.initModality(Modality.WINDOW_MODAL);
        popupStage.initOwner(conversionTableButton.getScene().getWindow());
        popupStage.initStyle(StageStyle.DECORATED);
        popupStage.setTitle("Bảng quy đổi điểm");
        popupStage.setResizable(false);

        VBox root = new VBox(15);
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: #1a1a1a;");

        Label titleLabel = new Label("📊 Bảng quy đổi điểm");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        // Tạo bảng
        VBox tableContainer = new VBox(0);
        tableContainer.setStyle("-fx-border-color: #2a2a2a; -fx-border-radius: 8px; -fx-background-radius: 8px;");

        // Header
        HBox headerRow = new HBox();
        headerRow.setStyle("-fx-background-color: #2254c9; -fx-background-radius: 8px 8px 0 0;");
        headerRow.setPadding(new Insets(12));
        headerRow.setSpacing(10);

        String[] headers = { "Thang 10", "Thang 4", "Điểm chữ", "Xếp loại" };
        for (String header : headers) {
            Label headerLabel = new Label(header);
            headerLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");
            headerLabel.setPrefWidth(120);
            headerRow.getChildren().add(headerLabel);
        }

        tableContainer.getChildren().add(headerRow);

        // Data rows
        String[][] data = {
                { "9.0 - 10.0", "4", "A+", "Xuất sắc" },
                { "8.5 - 8.9", "3.8", "A", "Giỏi" },
                { "7.8 - 8.4", "3.5", "B+", "Khá" },
                { "7.0 - 7.7", "3", "B", "Khá" },
                { "6.3 - 6.9", "2.4", "C+", "Trung bình" },
                { "5.5 - 6.2", "2", "C", "Trung bình" },
                { "4.8 - 5.4", "1.5", "D+", "Trung bình yếu" },
                { "4.0 - 4.7", "1", "D", "Trung bình yếu" },
                { "0.0 - 3.9", "0", "F", "Kém" }
        };

        for (int i = 0; i < data.length; i++) {
            HBox dataRow = new HBox();
            dataRow.setPadding(new Insets(10, 12, 10, 12));
            dataRow.setSpacing(10);
            if (i % 2 == 0) {
                dataRow.setStyle("-fx-background-color: #1d1d1d;");
            } else {
                dataRow.setStyle("-fx-background-color: #1a1a1a;");
            }

            for (String cell : data[i]) {
                Label cellLabel = new Label(cell);
                cellLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
                cellLabel.setPrefWidth(120);
                dataRow.getChildren().add(cellLabel);
            }

            tableContainer.getChildren().add(dataRow);
        }

        // Note
        Label noteLabel = new Label("💡 Lưu ý: GPA tính theo các môn đã chọn (không tính Giáo dục thể chất)");
        noteLabel.setStyle("-fx-text-fill: #99a8b8; -fx-font-size: 11px; -fx-wrap-text: true;");
        noteLabel.setMaxWidth(500);

        Button closeButton = new Button("Đóng");
        closeButton.setStyle(
                "-fx-background-color: linear-gradient(to right, #3F5EFB, #FC466B); -fx-text-fill: white; -fx-pref-width: 100px; -fx-pref-height: 35px; -fx-background-radius: 5px; -fx-cursor: hand; -fx-font-weight: 500;");
        closeButton.setOnAction(e -> popupStage.close());

        closeButton.setOnMouseEntered(e -> {
            closeButton.setStyle(
                    "-fx-background-color: linear-gradient(to right, #833AB4, #FD1D1D, #FCB045); -fx-text-fill: white; -fx-pref-width: 100px; -fx-pref-height: 35px; -fx-background-radius: 5px; -fx-cursor: hand; -fx-font-weight: 500;");
        });

        closeButton.setOnMouseExited(e -> {
            closeButton.setStyle(
                    "-fx-background-color: linear-gradient(to right, #3F5EFB, #FC466B); -fx-text-fill: white; -fx-pref-width: 100px; -fx-pref-height: 35px; -fx-background-radius: 5px; -fx-cursor: hand; -fx-font-weight: 500;");
        });

        root.getChildren().addAll(titleLabel, tableContainer, noteLabel, closeButton);
        root.setAlignment(Pos.CENTER);

        Scene scene = new Scene(root, 550, 600);
        popupStage.setScene(scene);
        popupStage.showAndWait();
    }

    @FXML
    private void showCPACalculator() {
        // Tính toán thống kê hiện tại
        int totalSubjects = scoresTable.getItems().size();
        int failedSubjects = 0;
        int totalCompletedCredits = 0;
        double currentTotalPoints = 0.0;
        int currentTotalCredits = 0;

        for (VirtualScoreItem item : scoresTable.getItems()) {
            if (!item.isPhysicalEducation() && item.getScoreOverall() > 0) {
                if (item.checkFailed()) {
                    failedSubjects++;
                } else {
                    totalCompletedCredits += item.getSubjectCredit();
                    double score4 = ScoreItem.convertToScale4(item.getScoreOverall());
                    currentTotalPoints += score4 * item.getSubjectCredit();
                    currentTotalCredits += item.getSubjectCredit();
                }
            }
        }

        int completedSubjects = totalSubjects - failedSubjects;
        double currentGPA = currentTotalCredits > 0 ? currentTotalPoints / currentTotalCredits : 0.0;

        // Tạo final variables để sử dụng trong lambda
        final int finalTotalCompletedCredits = totalCompletedCredits;
        final double finalCurrentTotalPoints = currentTotalPoints;
        final int finalCurrentTotalCredits = currentTotalCredits;

        Stage popupStage = new Stage();
        popupStage.initModality(Modality.WINDOW_MODAL);
        popupStage.initOwner(cpaCalculatorButton.getScene().getWindow());
        popupStage.initStyle(StageStyle.DECORATED);
        popupStage.setTitle("CPA Dự Kiến");
        popupStage.setResizable(false);

        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: #1a1a1a;");

        Label titleLabel = new Label("🎯 CPA Dự Kiến");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");

        // Thông tin hiện tại
        VBox currentInfoBox = new VBox(10);
        currentInfoBox.setStyle("-fx-background-color: #2a2a2a; -fx-background-radius: 8px; -fx-padding: 15px;");
        Label currentInfoTitle = new Label("📊 Thông tin hiện tại:");
        currentInfoTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label currentInfoText = new Label(String.format(
                "Số môn hoàn thành: %d môn\nSố môn trượt: %d môn\nTổng số tín chỉ hoàn thành: %d tín chỉ\nGPA hiện tại: %.2f",
                completedSubjects, failedSubjects, finalTotalCompletedCredits, currentGPA));
        currentInfoText.setStyle("-fx-font-size: 13px; -fx-text-fill: #e0e0e0; -fx-line-spacing: 5px;");
        currentInfoBox.getChildren().addAll(currentInfoTitle, currentInfoText);

        // Input fields
        VBox inputBox = new VBox(15);
        inputBox.setStyle("-fx-background-color: #2a2a2a; -fx-background-radius: 8px; -fx-padding: 20px;");

        Label inputTitle = new Label("📝 Nhập thông tin:");
        inputTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");

        HBox totalCreditsBox = new HBox(10);
        totalCreditsBox.setAlignment(Pos.CENTER_LEFT);
        Label totalCreditsLabel = new Label("Tổng số tín chỉ cần đạt:");
        totalCreditsLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: white; -fx-pref-width: 200px;");
        TextField totalCreditsField = new TextField();
        totalCreditsField.setStyle(
                "-fx-background-color: #1a1a1a; -fx-text-fill: white; -fx-border-color: #3a3a3a; -fx-border-radius: 5px; -fx-padding: 8px; -fx-pref-width: 150px;");
        totalCreditsBox.getChildren().addAll(totalCreditsLabel, totalCreditsField);

        HBox targetCPABox = new HBox(10);
        targetCPABox.setAlignment(Pos.CENTER_LEFT);
        Label targetCPALabel = new Label("CPA mong muốn:");
        targetCPALabel.setStyle("-fx-font-size: 13px; -fx-text-fill: white; -fx-pref-width: 200px;");
        TextField targetCPAField = new TextField();
        targetCPAField.setStyle(
                "-fx-background-color: #1a1a1a; -fx-text-fill: white; -fx-border-color: #3a3a3a; -fx-border-radius: 5px; -fx-padding: 8px; -fx-pref-width: 150px;");
        targetCPABox.getChildren().addAll(targetCPALabel, targetCPAField);

        // Chọn loại môn
        HBox creditTypeBox = new HBox(10);
        creditTypeBox.setAlignment(Pos.CENTER_LEFT);
        Label creditTypeLabel = new Label("Loại môn cần đạt:");
        creditTypeLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: white; -fx-pref-width: 200px;");
        ToggleGroup creditTypeGroup = new ToggleGroup();
        RadioButton credit2Button = new RadioButton("2 tín chỉ");
        credit2Button.setToggleGroup(creditTypeGroup);
        credit2Button.setSelected(true);
        credit2Button.setStyle("-fx-text-fill: white;");
        RadioButton credit3Button = new RadioButton("3 tín chỉ");
        credit3Button.setToggleGroup(creditTypeGroup);
        credit3Button.setStyle("-fx-text-fill: white;");
        HBox radioBox = new HBox(15);
        radioBox.getChildren().addAll(credit2Button, credit3Button);
        creditTypeBox.getChildren().addAll(creditTypeLabel, radioBox);

        // Slider khả năng
        VBox sliderBox = new VBox(10);
        Label sliderLabel = new Label("Chỉ số khả năng (mức tối thiểu bạn có thể đạt, có thể đạt cao hơn):");
        sliderLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: white;");

        Slider abilitySlider = new Slider(0, 7, 5);
        abilitySlider.setShowTickLabels(true);
        abilitySlider.setShowTickMarks(true);
        abilitySlider.setMajorTickUnit(1);
        abilitySlider.setMinorTickCount(0);
        abilitySlider.setSnapToTicks(true);
        abilitySlider.setPrefWidth(560);
        abilitySlider.setMaxWidth(560);

        // Labels cho slider - căn đều với các mốc 0-7
        String[] gradeLabels = { "D", "D+", "C", "C+", "B", "B+", "A", "A+" };
        HBox sliderLabelsBox = new HBox();
        sliderLabelsBox.setPrefWidth(560);
        sliderLabelsBox.setMaxWidth(560);
        sliderLabelsBox.setAlignment(Pos.CENTER);
        // Tính spacing để căn đều: với 8 labels và width 560px, spacing khoảng 57px
        // Sử dụng cách đơn giản: đặt mỗi label vào vị trí tương ứng
        for (int i = 0; i < gradeLabels.length; i++) {
            Label gradeLabel = new Label(gradeLabels[i]);
            gradeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #99a8b8;");
            gradeLabel.setPrefWidth(70); // 560 / 8 = 70px mỗi label
            gradeLabel.setAlignment(Pos.CENTER);
            sliderLabelsBox.getChildren().add(gradeLabel);
        }

        Label sliderValueLabel = new Label("B+");
        sliderValueLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #3F5EFB;");

        abilitySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int index = (int) Math.round(newVal.doubleValue());
            if (index >= 0 && index < gradeLabels.length) {
                sliderValueLabel.setText(gradeLabels[index]);
            }
        });

        sliderBox.getChildren().addAll(sliderLabel, abilitySlider, sliderLabelsBox, sliderValueLabel);

        inputBox.getChildren().addAll(inputTitle, totalCreditsBox, targetCPABox, creditTypeBox, sliderBox);

        // Kết quả
        VBox resultBox = new VBox(10);
        resultBox.setStyle("-fx-background-color: #2a2a2a; -fx-background-radius: 8px; -fx-padding: 20px;");
        Label resultTitle = new Label("📈 Kết quả:");
        resultTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");

        // ScrollPane để chứa kết quả
        ScrollPane resultScrollPane = new ScrollPane();
        resultScrollPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        resultScrollPane.setFitToWidth(true);
        resultScrollPane.setPrefHeight(250);
        resultScrollPane.setMaxHeight(350);

        VBox resultContent = new VBox(10);
        resultContent.setStyle("-fx-background-color: transparent;");
        Label resultText = new Label("Nhập thông tin và bấm 'Tính toán' để xem kết quả");
        resultText.setStyle("-fx-font-size: 13px; -fx-text-fill: #99a8b8; -fx-wrap-text: true;");
        resultText.setMaxWidth(500);
        resultContent.getChildren().add(resultText);
        resultScrollPane.setContent(resultContent);

        resultBox.getChildren().addAll(resultTitle, resultScrollPane);

        // Biến final để sử dụng trong lambda
        final VBox finalResultContent = resultContent;

        // Buttons
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        Button calculateButton = new Button("Tính toán");
        calculateButton.setStyle(
                "-fx-background-color: linear-gradient(to right, #3F5EFB, #FC466B); -fx-text-fill: white; -fx-pref-width: 120px; -fx-pref-height: 40px; -fx-background-radius: 5px; -fx-cursor: hand; -fx-font-weight: 500;");
        calculateButton.setOnMouseEntered(e -> {
            calculateButton.setStyle(
                    "-fx-background-color: linear-gradient(to right, #833AB4, #FD1D1D, #FCB045); -fx-text-fill: white; -fx-pref-width: 120px; -fx-pref-height: 40px; -fx-background-radius: 5px; -fx-cursor: hand; -fx-font-weight: 500;");
        });
        calculateButton.setOnMouseExited(e -> {
            calculateButton.setStyle(
                    "-fx-background-color: linear-gradient(to right, #3F5EFB, #FC466B); -fx-text-fill: white; -fx-pref-width: 120px; -fx-pref-height: 40px; -fx-background-radius: 5px; -fx-cursor: hand; -fx-font-weight: 500;");
        });

        Button closeButton = new Button("Đóng");
        closeButton.setStyle(
                "-fx-background-color: #3a3a3a; -fx-text-fill: white; -fx-pref-width: 120px; -fx-pref-height: 40px; -fx-background-radius: 5px; -fx-cursor: hand;");
        closeButton.setOnAction(e -> popupStage.close());

        buttonBox.getChildren().addAll(calculateButton, closeButton);

        // Tính toán khi bấm nút
        calculateButton.setOnAction(e -> {
            try {
                int totalCreditsNeeded = Integer.parseInt(totalCreditsField.getText().trim());
                double targetCPA = Double.parseDouble(targetCPAField.getText().trim());
                int selectedCredit = credit2Button.isSelected() ? 2 : 3;
                int abilityIndex = (int) Math.round(abilitySlider.getValue());

                if (totalCreditsNeeded <= 0 || targetCPA < 0 || targetCPA > 4.0) {
                    resultText.setText("❌ Vui lòng nhập giá trị hợp lệ!");
                    resultText.setStyle("-fx-font-size: 13px; -fx-text-fill: #ff5252; -fx-wrap-text: true;");
                    return;
                }

                // Tính toán
                int remainingCredits = totalCreditsNeeded - finalTotalCompletedCredits;
                if (remainingCredits <= 0) {
                    resultText.setText("✅ Bạn đã đạt đủ số tín chỉ cần thiết!");
                    resultText.setStyle("-fx-font-size: 13px; -fx-text-fill: #4caf50; -fx-wrap-text: true;");
                    return;
                }

                // Tính số điểm cần đạt
                double totalPointsNeeded = targetCPA * totalCreditsNeeded;
                double remainingPointsNeeded = totalPointsNeeded - finalCurrentTotalPoints;

                if (remainingPointsNeeded < 0) {
                    resultText.setText("✅ Bạn đã đạt được CPA mong muốn!");
                    resultText.setStyle("-fx-font-size: 13px; -fx-text-fill: #4caf50; -fx-wrap-text: true;");
                    return;
                }

                // Tính số môn cần đạt
                int numSubjectsNeeded = (int) Math.ceil((double) remainingCredits / selectedCredit);

                // Điểm thang 4 tương ứng với khả năng (mức tối thiểu)
                double[] gradePoints = { 1.0, 1.5, 2.0, 2.4, 3.0, 3.5, 3.8, 4.0 };
                double abilityPoint = gradePoints[abilityIndex];

                // Kiểm tra xem có thể đạt được CPA không (nếu tất cả đạt A+)
                double maxPossiblePoints = remainingCredits * 4.0;
                if (remainingPointsNeeded > maxPossiblePoints) {
                    resultText.setText(
                            "❌ Không thể đạt được CPA này! Ngay cả khi đạt A+ (4.0) cho tất cả các môn còn lại, bạn vẫn không thể đạt được CPA mong muốn.");
                    resultText.setStyle("-fx-font-size: 13px; -fx-text-fill: #ff5252; -fx-wrap-text: true;");
                    return;
                }

                // Kiểm tra xem với khả năng tối thiểu (nếu tất cả đạt mức khả năng) có đạt được
                // không
                double minPossiblePoints = remainingCredits * abilityPoint;

                // Tính phân bổ điểm
                int numSubjectsAtAbility = 0;
                int numSubjectsAtA = 0;
                int numSubjectsAtAPlus = 0;

                if (remainingPointsNeeded <= minPossiblePoints) {
                    // Nếu tất cả môn ở mức khả năng đã đủ, phân bổ tất cả ở mức khả năng
                    numSubjectsAtAbility = numSubjectsNeeded;
                } else {
                    // Cần một số môn đạt cao hơn mức khả năng
                    // Tính xem cần bao nhiêu điểm từ A/A+ để bù đắp
                    double pointsNeededFromHighGrades = remainingPointsNeeded - minPossiblePoints;

                    // Thử tìm phân bổ tối ưu: nhiều môn ở mức khả năng, còn lại ở A/A+
                    boolean foundSolution = false;

                    // Thử từ nhiều môn ở mức khả năng nhất có thể
                    for (int nAbility = numSubjectsNeeded; nAbility >= 0; nAbility--) {
                        int remainingSubjects = numSubjectsNeeded - nAbility;
                        int remainingCreditsForHigh = remainingCredits - (nAbility * selectedCredit);

                        if (remainingCreditsForHigh < 0)
                            continue;
                        if (remainingCreditsForHigh == 0 && nAbility > 0) {
                            // Tất cả tín chỉ đã được phân bổ cho mức khả năng
                            double totalPoints = nAbility * abilityPoint * selectedCredit;
                            if (Math.abs(totalPoints - remainingPointsNeeded) < 0.1) {
                                numSubjectsAtAbility = nAbility;
                                foundSolution = true;
                                break;
                            }
                            continue;
                        }

                        // Thử phân bổ giữa A và A+ cho số môn còn lại
                        int maxSubjectsForHigh = (int) Math.ceil((double) remainingCreditsForHigh / selectedCredit);
                        for (int nA = 0; nA <= maxSubjectsForHigh; nA++) {
                            int nAPlus = maxSubjectsForHigh - nA;
                            int creditsA = nA * selectedCredit;
                            int creditsAPlus = nAPlus * selectedCredit;

                            if (creditsA + creditsAPlus != remainingCreditsForHigh)
                                continue;

                            double totalPoints = nAbility * abilityPoint * selectedCredit +
                                    nA * 3.8 * selectedCredit +
                                    nAPlus * 4.0 * selectedCredit;

                            // Kiểm tra xem có đạt được điểm cần thiết không (cho phép sai số nhỏ)
                            if (Math.abs(totalPoints - remainingPointsNeeded) < 0.1) {
                                numSubjectsAtAbility = nAbility;
                                numSubjectsAtA = nA;
                                numSubjectsAtAPlus = nAPlus;
                                foundSolution = true;
                                break;
                            }
                        }
                        if (foundSolution)
                            break;
                    }

                    if (!foundSolution) {
                        // Nếu không tìm được giải pháp chính xác, tính gần đúng
                        // Ưu tiên nhiều môn ở mức khả năng
                        numSubjectsAtAbility = (int) Math.floor((double) remainingCredits / selectedCredit);
                        int remainingCreditsForHigh = remainingCredits - (numSubjectsAtAbility * selectedCredit);
                        int remainingSubjectsForHigh = (int) Math
                                .ceil((double) remainingCreditsForHigh / selectedCredit);

                        double pointsFromAbility = numSubjectsAtAbility * abilityPoint * selectedCredit;
                        double remainingPointsForHigh = remainingPointsNeeded - pointsFromAbility;

                        if (remainingPointsForHigh > 0 && remainingSubjectsForHigh > 0) {
                            // Phân bổ giữa A và A+
                            double avgNeededForHigh = remainingPointsForHigh / remainingCreditsForHigh;
                            if (avgNeededForHigh >= 3.9) {
                                // Cần nhiều A+
                                numSubjectsAtAPlus = remainingSubjectsForHigh;
                                numSubjectsAtA = 0;
                            } else {
                                // Phân bổ giữa A và A+
                                numSubjectsAtAPlus = (int) Math
                                        .ceil((remainingPointsForHigh - remainingCreditsForHigh * 3.8)
                                                / (selectedCredit * 0.2));
                                if (numSubjectsAtAPlus > remainingSubjectsForHigh) {
                                    numSubjectsAtAPlus = remainingSubjectsForHigh;
                                }
                                numSubjectsAtA = remainingSubjectsForHigh - numSubjectsAtAPlus;
                            }
                        }
                    }
                }

                // Kiểm tra lại
                int totalCreditsCheck = numSubjectsAtAPlus * selectedCredit + numSubjectsAtA * selectedCredit
                        + numSubjectsAtAbility * selectedCredit;
                double totalPointsCheck = numSubjectsAtAPlus * 4.0 * selectedCredit +
                        numSubjectsAtA * 3.8 * selectedCredit +
                        numSubjectsAtAbility * abilityPoint * selectedCredit;
                double finalCPA = (finalCurrentTotalPoints + totalPointsCheck) / totalCreditsNeeded;

                // Kiểm tra xem có thể đạt được CPA với khả năng hiện tại không
                if (remainingPointsNeeded > minPossiblePoints) {
                    // Cần một số môn đạt cao hơn mức khả năng
                    // Kiểm tra xem phân bổ có hợp lý không
                    double actualPoints = totalPointsCheck;
                    if (actualPoints < remainingPointsNeeded - 0.5) {
                        // Không thể đạt được với khả năng này
                        finalResultContent.getChildren().clear();
                        Label errorLabel = new Label(String.format(
                                "❌ Với khả năng học ở mức %s (%.1f), bạn không thể đạt được CPA mục tiêu %.2f!\n\n" +
                                        "Để đạt được CPA này, bạn cần:\n" +
                                        "• Nâng cao khả năng học lên mức cao hơn, hoặc\n" +
                                        "• Giảm CPA mục tiêu xuống mức thấp hơn.\n\n" +
                                        "Với khả năng hiện tại, CPA tối đa có thể đạt được là: %.2f",
                                gradeLabels[abilityIndex], abilityPoint, targetCPA,
                                (finalCurrentTotalPoints + minPossiblePoints) / totalCreditsNeeded));
                        errorLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #ff5252; -fx-wrap-text: true;");
                        errorLabel.setMaxWidth(500);
                        finalResultContent.getChildren().add(errorLabel);
                        return;
                    }
                }

                // Tạo bảng kết quả rõ ràng
                finalResultContent.getChildren().clear();

                // Thông tin tổng quan
                VBox summaryBox = new VBox(8);
                summaryBox.setStyle("-fx-background-color: #1a1a1a; -fx-background-radius: 5px; -fx-padding: 12px;");
                Label summaryTitle = new Label("📊 Thông tin tổng quan:");
                summaryTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");
                Label summaryText = new Label(String.format(
                        "Số tín chỉ còn lại cần đạt: %d tín chỉ\n" +
                                "Số môn cần đạt (môn %d tín chỉ/môn): %d môn",
                        remainingCredits, selectedCredit, numSubjectsNeeded));
                summaryText.setStyle("-fx-font-size: 12px; -fx-text-fill: #e0e0e0; -fx-line-spacing: 5px;");
                summaryBox.getChildren().addAll(summaryTitle, summaryText);

                // Bảng phân bổ điểm
                VBox tableBox = new VBox(0);
                tableBox.setStyle(
                        "-fx-background-color: #1a1a1a; -fx-background-radius: 5px; -fx-border-color: #3a3a3a; -fx-border-radius: 5px;");

                // Header
                HBox headerRow = new HBox();
                headerRow.setStyle(
                        "-fx-background-color: #2254c9; -fx-background-radius: 5px 5px 0 0; -fx-padding: 12px;");
                headerRow.setSpacing(10);

                String[] headers = { "Mức điểm", "Số môn", "Tín chỉ/môn", "Tổng tín chỉ", "Điểm thang 4" };
                double[] headerWidths = { 120.0, 80.0, 100.0, 100.0, 100.0 };
                for (int i = 0; i < headers.length; i++) {
                    Label headerLabel = new Label(headers[i]);
                    headerLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;");
                    headerLabel.setPrefWidth(headerWidths[i]);
                    headerLabel.setAlignment(Pos.CENTER);
                    headerRow.getChildren().add(headerLabel);
                }
                tableBox.getChildren().add(headerRow);

                // Data rows
                int rowIndex = 0;
                if (numSubjectsAtAPlus > 0) {
                    HBox dataRow = createCPADataRow("A+", numSubjectsAtAPlus, selectedCredit, 4.0, rowIndex % 2 == 0);
                    tableBox.getChildren().add(dataRow);
                    rowIndex++;
                }
                if (numSubjectsAtA > 0) {
                    HBox dataRow = createCPADataRow("A", numSubjectsAtA, selectedCredit, 3.8, rowIndex % 2 == 0);
                    tableBox.getChildren().add(dataRow);
                    rowIndex++;
                }
                if (numSubjectsAtAbility > 0) {
                    HBox dataRow = createCPADataRow(gradeLabels[abilityIndex], numSubjectsAtAbility, selectedCredit,
                            abilityPoint, rowIndex % 2 == 0);
                    tableBox.getChildren().add(dataRow);
                    rowIndex++;
                }

                // Tổng cộng
                HBox totalRow = new HBox();
                totalRow.setStyle(
                        "-fx-background-color: #2a4a7a; -fx-padding: 12px; -fx-background-radius: 0 0 5px 5px;");
                totalRow.setSpacing(10);

                Label totalLabel1 = new Label("TỔNG CỘNG");
                totalLabel1.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;");
                totalLabel1.setPrefWidth(120.0);
                totalLabel1.setAlignment(Pos.CENTER);

                Label totalLabel2 = new Label(String.valueOf(numSubjectsNeeded));
                totalLabel2.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;");
                totalLabel2.setPrefWidth(80.0);
                totalLabel2.setAlignment(Pos.CENTER);

                Label totalLabel3 = new Label(String.valueOf(selectedCredit));
                totalLabel3.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;");
                totalLabel3.setPrefWidth(100.0);
                totalLabel3.setAlignment(Pos.CENTER);

                Label totalLabel4 = new Label(String.valueOf(totalCreditsCheck));
                totalLabel4.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;");
                totalLabel4.setPrefWidth(100.0);
                totalLabel4.setAlignment(Pos.CENTER);

                Label totalLabel5 = new Label(String.format("%.1f", totalPointsCheck / totalCreditsCheck));
                totalLabel5.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;");
                totalLabel5.setPrefWidth(100.0);
                totalLabel5.setAlignment(Pos.CENTER);

                totalRow.getChildren().addAll(totalLabel1, totalLabel2, totalLabel3, totalLabel4, totalLabel5);
                tableBox.getChildren().add(totalRow);

                // CPA kết quả
                VBox cpaBox = new VBox(8);
                cpaBox.setStyle(
                        "-fx-background-color: #1a3a1a; -fx-background-radius: 5px; -fx-padding: 15px; -fx-border-color: #4caf50; -fx-border-width: 2px; -fx-border-radius: 5px;");
                Label cpaTitle = new Label("✅ CPA dự kiến khi đạt được:");
                cpaTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #4caf50;");
                Label cpaValue = new Label(String.format("%.2f / %.2f", finalCPA, targetCPA));
                cpaValue.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #4caf50;");
                Label cpaNote = new Label(String.format("(CPA mục tiêu: %.2f)", targetCPA));
                cpaNote.setStyle("-fx-font-size: 11px; -fx-text-fill: #99a8b8;");
                cpaBox.getChildren().addAll(cpaTitle, cpaValue, cpaNote);

                finalResultContent.getChildren().addAll(summaryBox, tableBox, cpaBox);
                finalResultContent.setSpacing(15);

            } catch (NumberFormatException ex) {
                finalResultContent.getChildren().clear();
                Label errorLabel = new Label("❌ Vui lòng nhập số hợp lệ!");
                errorLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #ff5252; -fx-wrap-text: true;");
                errorLabel.setMaxWidth(500);
                finalResultContent.getChildren().add(errorLabel);
            } catch (Exception ex) {
                finalResultContent.getChildren().clear();
                Label errorLabel = new Label("❌ Lỗi: " + ex.getMessage());
                errorLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #ff5252; -fx-wrap-text: true;");
                errorLabel.setMaxWidth(500);
                finalResultContent.getChildren().add(errorLabel);
                ex.printStackTrace();
            }
        });

        root.getChildren().addAll(titleLabel, currentInfoBox, inputBox, resultBox, buttonBox);
        root.setAlignment(Pos.CENTER);

        Scene scene = new Scene(root, 600, 800);
        popupStage.setScene(scene);
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

    private HBox createCPADataRow(String gradeLabel, int numSubjects, int creditPerSubject, double gradePoint,
            boolean isEven) {
        HBox dataRow = new HBox();
        dataRow.setStyle(isEven ? "-fx-background-color: #1d1d1d; -fx-padding: 12px;"
                : "-fx-background-color: #1a1a1a; -fx-padding: 12px;");
        dataRow.setSpacing(10);

        double[] columnWidths = { 120.0, 80.0, 100.0, 100.0, 100.0 };

        // Mức điểm
        Label gradeLabelCell = new Label(gradeLabel);
        gradeLabelCell.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
        gradeLabelCell.setPrefWidth(columnWidths[0]);
        gradeLabelCell.setAlignment(Pos.CENTER);

        // Số môn
        Label numSubjectsCell = new Label(String.valueOf(numSubjects));
        numSubjectsCell.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
        numSubjectsCell.setPrefWidth(columnWidths[1]);
        numSubjectsCell.setAlignment(Pos.CENTER);

        // Tín chỉ/môn
        Label creditPerSubjectCell = new Label(String.valueOf(creditPerSubject));
        creditPerSubjectCell.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
        creditPerSubjectCell.setPrefWidth(columnWidths[2]);
        creditPerSubjectCell.setAlignment(Pos.CENTER);

        // Tổng tín chỉ
        Label totalCreditsCell = new Label(String.valueOf(numSubjects * creditPerSubject));
        totalCreditsCell.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
        totalCreditsCell.setPrefWidth(columnWidths[3]);
        totalCreditsCell.setAlignment(Pos.CENTER);

        // Điểm thang 4
        Label gradePointCell = new Label(String.format("%.1f", gradePoint));
        gradePointCell.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
        gradePointCell.setPrefWidth(columnWidths[4]);
        gradePointCell.setAlignment(Pos.CENTER);

        dataRow.getChildren().addAll(gradeLabelCell, numSubjectsCell, creditPerSubjectCell, totalCreditsCell,
                gradePointCell);
        return dataRow;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
