package org.example.controller;

import com.google.gson.JsonObject;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.example.model.StudentInfo;
import org.example.service.LocalStorageService;

import java.io.IOException;
import java.util.List;

public class MainScreenController {
    @FXML
    private BorderPane mainBorderPane;

    @FXML
    private Button scheduleButton;

    @FXML
    private Button virtualScheduleButton;

    @FXML
    private Button homeButton;

    @FXML
    private Button scoresButton;

    @FXML
    private Button scholarshipButton;

    @FXML
    private Button logoutButton;

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label studentNameLabel;

    @FXML
    private Label studentCodeLabel;

    @FXML
    private Label studentBirthdayLabel;

    @FXML
    private Label studentGenderLabel;

    private List<Button> menuButtons;

    private String authToken;
    private StudentInfo studentInfo;
    private String studentScheduleJson;

    @FXML
    public void initialize() {
        // Load từ local storage khi khởi tạo
        loadFromLocalStorage();
        menuButtons = List.of(homeButton, scheduleButton, virtualScheduleButton, scoresButton, scholarshipButton);
    }

    private void loadFromLocalStorage() {
        try {
            LocalStorageService storage = LocalStorageService.getInstance();

            // Load student info
            JsonObject studentInfoObj = storage.loadStudentInfo();
            if (studentInfoObj != null) {
                studentInfo = new StudentInfo();
                studentInfo.setStudentCode(getStringValue(studentInfoObj, "student_code"));
                studentInfo.setDisplayName(getStringValue(studentInfoObj, "display_name"));
                studentInfo.setBirthday(getStringValue(studentInfoObj, "birthday"));
                studentInfo.setGender(getStringValue(studentInfoObj, "gender"));

                if (authToken == null || authToken.isEmpty()) {
                    authToken = studentInfo.getStudentCode();
                }

                updateStudentInfoDisplay();
            }

            // Load schedule
            studentScheduleJson = storage.loadSchedule();
        } catch (IOException e) {
            System.err.println("Error loading from local storage: " + e.getMessage());
        }
    }

    private void updateStudentInfoDisplay() {
        if (studentInfo != null) {
            studentNameLabel.setText(studentInfo.getDisplayName());
            studentCodeLabel.setText("Mã SV: " + studentInfo.getStudentCode());
            // studentBirthdayLabel.setText("Ngày sinh: " + studentInfo.getBirthday());
            // studentGenderLabel.setText("Giới tính: " + studentInfo.getGender());
        }
    }

    private String getStringValue(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return "";
    }

    public void setAuthToken(String token) {
        this.authToken = token;
    }

    public void setStudentInfo(StudentInfo studentInfo) {
        this.studentInfo = studentInfo;
        updateStudentInfoDisplay();
    }

    public void setStudentScheduleJson(String studentScheduleJson) {
        this.studentScheduleJson = studentScheduleJson;
    }

    private void setActiveButton(Button buttonToActivate) {
        // Loop through all buttons and remove the "active" style class
        for (Button btn : menuButtons) {
            btn.getStyleClass().remove("active");
        }

        // Add the "active" style class to the clicked button
        if (!buttonToActivate.getStyleClass().contains("active")) {
            buttonToActivate.getStyleClass().add("active");
        }
    }

    @FXML
    private void handleViewSchedule() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ScheduleScreen.fxml"));
            Parent root = loader.load();

            ScheduleController controller = loader.getController();
            controller.setAuthToken(authToken);
            if (studentInfo != null) {
                controller.setStudentInfo(studentInfo);
            }
            // Load schedule từ JSON nếu có, nếu không thì gọi API
            if (studentScheduleJson != null && !studentScheduleJson.isEmpty()) {
                controller.loadScheduleFromJson(studentScheduleJson);
            } else {
                controller.loadSchedule();
            }

            Stage stage = (Stage) scheduleButton.getScene().getWindow();
            stage.setScene(new Scene(root, 1400, 1000));
            stage.setTitle("Lịch học");
            stage.setMinWidth(1200);
            stage.setMinHeight(800);

            setActiveButton(scheduleButton);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleVirtualSchedule() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/VirtualScheduleScreen.fxml"));
            Parent centerContent = loader.load();
            VirtualScheduleController controller = loader.getController();
            controller.loadVirtualCalendar();
            mainBorderPane.setCenter(centerContent);
            setActiveButton(virtualScheduleButton);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleViewScores() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ScoresScreen.fxml"));
            Parent centerContent = loader.load();
            ScoresController controller = loader.getController();
            controller.loadScores();
            mainBorderPane.setCenter(centerContent);
            setActiveButton(scoresButton);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleViewScholarship() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ScholarshipScreen.fxml"));
            Parent centerContent = loader.load();
            ScholarshipController controller = loader.getController();
            controller.initialize();
            mainBorderPane.setCenter(centerContent);
            setActiveButton(scholarshipButton);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginScreen.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) logoutButton.getScene().getWindow();
            Scene scene = new Scene(root, 1000, 600);
            stage.setScene(scene);
            stage.setTitle("Đăng nhập - KMA Legend Desktop");
            stage.setResizable(false);
            // Center cửa sổ trên màn hình
            stage.centerOnScreen();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
