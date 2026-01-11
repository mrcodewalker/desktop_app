package org.example.controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.model.StudentInfo;
import org.example.service.ApiService;
import org.example.service.EncryptionService;
import org.example.service.EncryptionService.EncryptionResult;
import org.example.service.LocalStorageService;

import java.io.IOException;

public class LoginController {
    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    private ApiService apiService;
    private EncryptionService encryptionService;
    private String authToken;
    private StudentInfo studentInfo;
    private String studentScheduleJson; // Lưu student_schedule từ login response

    @FXML
    public void initialize() {
        apiService = ApiService.getInstance();
        encryptionService = EncryptionService.getInstance();

        // Lấy public key khi khởi tạo
        usernameField.setText("ct070216");
        passwordField.setText("maimia2505");
        loadPublicKey();
    }

    private void loadPublicKey() {
        new Thread(() -> {
            try {
                String publicKeyResponse = apiService.getPublicKey();
                System.out.println("Public key response: "
                        + publicKeyResponse.substring(0, Math.min(100, publicKeyResponse.length())) + "...");
                encryptionService.setPublicKey(publicKeyResponse);
                Platform.runLater(() -> {
                    loginButton.setDisable(false);
                    System.out.println("Public key loaded successfully");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "Lỗi",
                            "Không thể kết nối đến server. Vui lòng kiểm tra lại.");
                    e.printStackTrace();
                });
            }
        }).start();
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo",
                    "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu.");
            return;
        }

        if (!encryptionService.isPublicKeyLoaded()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi",
                    "Public key chưa được tải. Vui lòng đợi...");
            return;
        }

        loginButton.setDisable(true);

        new Thread(() -> {
            try {
                // Đảm bảo public key đã được load (lấy lại để đảm bảo key mới nhất)
                String publicKey = apiService.getPublicKey();
                encryptionService.setPublicKey(publicKey);
                System.out.println("Public key reloaded before login");

                // Tạo JSON chứa username và password
                JsonObject loginData = new JsonObject();
                loginData.addProperty("username", username);
                loginData.addProperty("password", password);

                String dataToEncrypt = loginData.toString();
                System.out.println("Data to encrypt: " + dataToEncrypt);

                // Mã hóa bằng hybrid encryption (RSA + AES)
                EncryptionResult encryptionResult = encryptionService.encryptHybrid(dataToEncrypt);

                System.out.println("Encryption completed. Key length: " + encryptionResult.getEncryptedKey().length());

                // Gọi API đăng nhập với format mới
                String response = apiService.login(
                        encryptionResult.getEncryptedKey(),
                        encryptionResult.getEncryptedData(),
                        encryptionResult.getIv());

                // Parse response mới với format: { "code": "200", "data": {...} }
                JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();

                // Kiểm tra code
                if (jsonResponse.has("code")) {
                    String code = jsonResponse.get("code").getAsString();
                    if (!"200".equals(code)) {
                        String message = jsonResponse.has("message") ? jsonResponse.get("message").getAsString()
                                : "Đăng nhập thất bại";
                        throw new IOException(message);
                    }
                }

                // Lấy token và student_info từ data
                if (jsonResponse.has("data")) {
                    JsonObject data = jsonResponse.getAsJsonObject("data");

                    // Parse student_info
                    if (data.has("student_info")) {
                        JsonObject studentInfoObj = data.getAsJsonObject("student_info");
                        studentInfo = new StudentInfo();
                        studentInfo.setStudentCode(getStringValue(studentInfoObj, "student_code"));
                        studentInfo.setDisplayName(getStringValue(studentInfoObj, "display_name"));
                        studentInfo.setBirthday(getStringValue(studentInfoObj, "birthday"));
                        studentInfo.setGender(getStringValue(studentInfoObj, "gender"));

                        // Dùng student_code làm token identifier
                        authToken = studentInfo.getStudentCode();
                    }

                    // Lưu student_schedule để dùng sau
                    if (data.has("student_schedule") && data.get("student_schedule").isJsonArray()) {
                        studentScheduleJson = data.getAsJsonArray("student_schedule").toString();
                    }

                    // Lưu vào local storage
                    try {
                        LocalStorageService storage = LocalStorageService.getInstance();
                        if (data.has("student_info")) {
                            storage.saveStudentInfo(data.getAsJsonObject("student_info"));
                        }
                        if (studentScheduleJson != null) {
                            storage.saveSchedule(studentScheduleJson);
                        }
                        // Lưu credentials đã mã hóa để dùng cho virtual calendar
                        storage.saveCredentials(
                                encryptionResult.getEncryptedKey(),
                                encryptionResult.getEncryptedData(),
                                encryptionResult.getIv());
                    } catch (IOException e) {
                        System.err.println("Error saving to local storage: " + e.getMessage());
                    }
                } else if (jsonResponse.has("token")) {
                    authToken = jsonResponse.get("token").getAsString();
                } else if (jsonResponse.has("accessToken")) {
                    authToken = jsonResponse.get("accessToken").getAsString();
                } else {
                    authToken = "authenticated";
                }

                Platform.runLater(() -> {
                    navigateToMainScreen();
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "Đăng nhập thất bại",
                            "Tên đăng nhập hoặc mật khẩu không đúng.");
                    loginButton.setDisable(false);
                    e.printStackTrace();
                });
            }
        }).start();
    }

    private void navigateToMainScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainScreen.fxml"));
            Parent root = loader.load();

            MainScreenController controller = loader.getController();
            controller.setAuthToken(authToken);
            if (studentInfo != null) {
                controller.setStudentInfo(studentInfo);
            }
            if (studentScheduleJson != null) {
                controller.setStudentScheduleJson(studentScheduleJson);
            }

            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(root, 1300, 800));
            // stage.setFullScreen(true);
            stage.setResizable(true);
            stage.setTitle("KMA Legend Desktop - Trang chủ");

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi",
                    "Không thể tải màn hình chính.");
            e.printStackTrace();
        }
    }

    private String getStringValue(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return "";
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public String getAuthToken() {
        return authToken;
    }
}
