package org.example.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.example.model.ScholarshipItem;
import org.example.service.ApiService;
import org.example.service.EncryptionService;

import java.util.ArrayList;
import java.util.List;

public class ScholarshipController {
    @FXML
    private TableView<ScholarshipItem> scholarshipTable;
    
    @FXML
    private TableColumn<ScholarshipItem, Integer> rankColumn;
    
    @FXML
    private TableColumn<ScholarshipItem, String> studentCodeColumn;
    
    @FXML
    private TableColumn<ScholarshipItem, String> studentNameColumn;
    
    @FXML
    private TableColumn<ScholarshipItem, String> studentClassColumn;
    
    @FXML
    private TableColumn<ScholarshipItem, Double> gpaColumn;
    
    @FXML
    private TableColumn<ScholarshipItem, String> classificationColumn;
    
    @FXML
    private TableColumn<ScholarshipItem, String> scholarshipTypeColumn;
    
    @FXML
    private ComboBox<String> courseFilterComboBox;
    
    @FXML
    private Button filterButton;
    
    @FXML
    private Button backButton;
    
    @FXML
    private Label statusLabel;
    
    private ApiService apiService;
    private EncryptionService encryptionService;
    
    @FXML
    public void initialize() {
        apiService = ApiService.getInstance();
        encryptionService = EncryptionService.getInstance();
        
        // Setup table columns - Thứ hạng tự động đánh số
        rankColumn.setCellValueFactory(param -> {
            int index = scholarshipTable.getItems().indexOf(param.getValue());
            return new javafx.beans.property.SimpleIntegerProperty(index + 1).asObject();
        });
        rankColumn.setCellFactory(column -> new TableCell<ScholarshipItem, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    // Hiển thị số thứ hạng với icon nhẹ cho top 3
                    if (item == 1) {
                        setText("🥇 " + item);
                        setStyle("-fx-text-fill: #B8860B; -fx-font-weight: bold;");
                    } else if (item == 2) {
                        setText("🥈 " + item);
                        setStyle("-fx-text-fill: #708090; -fx-font-weight: bold;");
                    } else if (item == 3) {
                        setText("🥉 " + item);
                        setStyle("-fx-text-fill: #8B4513; -fx-font-weight: bold;");
                    } else {
                        setText(String.valueOf(item));
                        setStyle("");
                    }
                    setAlignment(Pos.CENTER);
                }
            }
        });
        
        studentCodeColumn.setCellValueFactory(new PropertyValueFactory<>("studentCode"));
        studentCodeColumn.setCellFactory(column -> new TableCell<ScholarshipItem, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setAlignment(Pos.CENTER);
                }
            }
        });
        
        studentNameColumn.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        studentNameColumn.setCellFactory(column -> new TableCell<ScholarshipItem, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setAlignment(Pos.CENTER);
                }
            }
        });
        
        studentClassColumn.setCellValueFactory(new PropertyValueFactory<>("studentClass"));
        studentClassColumn.setCellFactory(column -> new TableCell<ScholarshipItem, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setAlignment(Pos.CENTER);
                }
            }
        });
        
        gpaColumn.setCellValueFactory(new PropertyValueFactory<>("gpa"));
        gpaColumn.setCellFactory(column -> new TableCell<ScholarshipItem, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f", item));
                    setAlignment(Pos.CENTER);
                }
            }
        });
        
        classificationColumn.setCellValueFactory(new PropertyValueFactory<>("classification"));
        classificationColumn.setCellFactory(column -> new TableCell<ScholarshipItem, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setAlignment(Pos.CENTER);
                    // Màu sắc cho xếp loại
                    if ("Xuất sắc".equals(item)) {
                        setStyle("-fx-text-fill: #FF6B6B; -fx-font-weight: bold;");
                    } else if ("Giỏi".equals(item)) {
                        setStyle("-fx-text-fill: #4ECDC4; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #95A5A6; -fx-font-weight: bold;");
                    }
                }
            }
        });
        
        scholarshipTypeColumn.setCellValueFactory(new PropertyValueFactory<>("scholarshipType"));
        scholarshipTypeColumn.setCellFactory(column -> new TableCell<ScholarshipItem, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setAlignment(Pos.CENTER);
                }
            }
        });
        
        // Setup dropdown với danh sách khóa
        List<String> courses = new ArrayList<>();
        courses.add("AT18");
        courses.add("AT19");
        courses.add("AT20");
        courses.add("AT21");
        courses.add("AT22");
        courses.add("CT06");
        courses.add("CT07");
        courses.add("CT08");
        courses.add("CT09");
        courses.add("CT10");
        courses.add("DT05");
        courses.add("DT06");
        courses.add("DT07");
        courses.add("DT08");
        courses.add("DT09");
        
        ObservableList<String> courseList = FXCollections.observableArrayList(courses);
        courseFilterComboBox.setItems(courseList);
        courseFilterComboBox.setPromptText("Chọn khóa");
        
        // Setup filter button
        if (filterButton != null) {
            filterButton.setOnAction(e -> handleFilter());
        }
        
        // Setup row factory để highlight top 3 nhẹ nhàng
        scholarshipTable.setRowFactory(tv -> new TableRow<ScholarshipItem>() {
            @Override
            protected void updateItem(ScholarshipItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else {
                    int index = scholarshipTable.getItems().indexOf(item);
                    if (index == 0) {
                        // Top 1 - Vàng nhẹ
                        setStyle("-fx-background-color: #FFF9E6; " +
                                "-fx-background-insets: 0; -fx-background-radius: 0;");
                    } else if (index == 1) {
                        // Top 2 - Bạc nhẹ
                        setStyle("-fx-background-color: #F5F5F5; " +
                                "-fx-background-insets: 0; -fx-background-radius: 0;");
                    } else if (index == 2) {
                        // Top 3 - Đồng nhẹ
                        setStyle("-fx-background-color: #FFF4E6; " +
                                "-fx-background-insets: 0; -fx-background-radius: 0;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
    }
    
    @FXML
    private void handleFilter() {
        String selectedCourse = courseFilterComboBox.getSelectionModel().getSelectedItem();
        
        if (selectedCourse == null || selectedCourse.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng chọn khóa để lọc.");
            return;
        }
        
        statusLabel.setText("Đang tải danh sách học bổng...");
        scholarshipTable.getItems().clear();
        
        new Thread(() -> {
            try {
                // Lấy public key
                String publicKey = apiService.getPublicKey();
                encryptionService.setPublicKey(publicKey);
                
                // Tạo JSON chứa code để mã hóa
                JsonObject dataToEncrypt = new JsonObject();
                dataToEncrypt.addProperty("code", selectedCourse);
                
                String dataString = dataToEncrypt.toString();
                
                // Mã hóa bằng hybrid encryption
                EncryptionService.EncryptionResult encryptionResult = 
                    encryptionService.encryptHybrid(dataString);
                
                // Gọi API filter scholarship
                String response = apiService.filterScholarship(
                    encryptionResult.getEncryptedKey(),
                    encryptionResult.getEncryptedData(),
                    encryptionResult.getIv()
                );
                
                // Parse response
                JsonElement element = JsonParser.parseString(response);
                
                Platform.runLater(() -> {
                    try {
                        List<ScholarshipItem> items = new ArrayList<>();
                        
                        if (element.isJsonArray()) {
                            JsonArray array = element.getAsJsonArray();
                            for (JsonElement jsonElement : array) {
                                JsonObject obj = jsonElement.getAsJsonObject();
                                ScholarshipItem item = parseScholarshipItem(obj);
                                items.add(item);
                            }
                        } else if (element.isJsonObject()) {
                            JsonObject obj = element.getAsJsonObject();
                            // Nếu response là object chứa array
                            if (obj.has("data") && obj.get("data").isJsonArray()) {
                                JsonArray array = obj.getAsJsonArray("data");
                                for (JsonElement jsonElement : array) {
                                    JsonObject itemObj = jsonElement.getAsJsonObject();
                                    ScholarshipItem item = parseScholarshipItem(itemObj);
                                    items.add(item);
                                }
                            } else {
                                // Thử parse trực tiếp
                                ScholarshipItem item = parseScholarshipItem(obj);
                                items.add(item);
                            }
                        }
                        
                        // Tự động đánh số thứ hạng và tính xếp loại
                        for (int i = 0; i < items.size(); i++) {
                            ScholarshipItem item = items.get(i);
                            item.setRank(i + 1); // Đánh số từ 1
                            item.calculateClassification(); // Tính xếp loại
                        }
                        
                        scholarshipTable.getItems().addAll(items);
                        statusLabel.setText("Đã tải " + items.size() + " sinh viên");
                        
                    } catch (Exception e) {
                        statusLabel.setText("Lỗi khi parse dữ liệu");
                        showAlert(Alert.AlertType.ERROR, "Lỗi", 
                                "Không thể parse dữ liệu từ server: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Lỗi khi tải danh sách học bổng");
                    showAlert(Alert.AlertType.ERROR, "Lỗi", 
                            "Không thể tải danh sách học bổng: " + e.getMessage());
                    e.printStackTrace();
                });
            }
        }).start();
    }
    
    private ScholarshipItem parseScholarshipItem(JsonObject obj) {
        ScholarshipItem item = new ScholarshipItem();
        
        // Parse từ JSON - có thể có ranking từ server hoặc không
        int ranking = getIntValue(obj, "ranking");
        if (ranking > 0) {
            item.setRank(ranking);
        }
        
        item.setStudentCode(getStringValue(obj, "studentCode"));
        item.setStudentName(getStringValue(obj, "studentName"));
        item.setStudentClass(getStringValue(obj, "studentClass"));
        item.setGpa(getDoubleValue(obj, "gpa"));
        item.setScholarshipType(getStringValue(obj, "scholarshipType"));
        
        // Tính xếp loại ngay khi parse
        item.calculateClassification();
        
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
    private void handleBack() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/MainScreen.fxml"));
            javafx.scene.Parent root = loader.load();
            
            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(new javafx.scene.Scene(root, 1200, 800));
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

