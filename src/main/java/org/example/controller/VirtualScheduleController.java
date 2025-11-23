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
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.time.DayOfWeek;
import java.time.YearMonth;
import org.example.model.VirtualCourse;
import org.example.service.ApiService;
import org.example.service.EncryptionService;
import org.example.service.LocalStorageService;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class VirtualScheduleController {
    @FXML
    private ComboBox<String> courseComboBox;
    
    @FXML
    private ComboBox<String> subjectComboBox;
    
    @FXML
    private ScrollPane coursesScrollPane;
    
    @FXML
    private VBox coursesContainer;
    
    @FXML
    private ScrollPane selectedScheduleScrollPane;
    
    @FXML
    private VBox selectedScheduleContainer;
    
    @FXML
    private Label statusLabel;
    
    @FXML
    private Button backButton;
    
    @FXML
    private Button clearAllButton;
    
    @FXML
    private Button exportTxtButton;
    
    @FXML
    private Button importTxtButton;
    
    @FXML
    private Button viewCalendarButton;
    
    @FXML
    private TableView<CourseStats> registrationTable;
    
    @FXML
    private TableColumn<CourseStats, String> courseColumn;
    
    @FXML
    private TableColumn<CourseStats, Integer> subjectCountColumn;
    
    @FXML
    private TableColumn<CourseStats, Integer> totalSubjectsColumn;
    
    @FXML
    private TableColumn<CourseStats, String> percentageColumn;
    
    @FXML
    private TableView<SelectedCourseInfo> selectedCoursesTable;
    
    @FXML
    private TableColumn<SelectedCourseInfo, String> selectedCourseColumn;
    
    @FXML
    private TableColumn<SelectedCourseInfo, String> selectedSubjectColumn;
    
    @FXML
    private TableColumn<SelectedCourseInfo, String> selectedClassColumn;
    
    private ApiService apiService;
    private EncryptionService encryptionService;
    private LocalStorageService localStorageService;
    
    private List<VirtualCourse> allCourses = new ArrayList<>();
    private List<VirtualCourse> selectedCourses = new ArrayList<>();
    private Map<String, List<VirtualCourse>> coursesByCourse = new HashMap<>(); // Group by course (AT22, AT21...)
    private Map<String, List<VirtualCourse>> coursesByDisplayName = new HashMap<>(); // Group by displayCourseName (để filter theo môn)
    private Map<VirtualCourse, CheckBox> courseCheckBoxMap = new HashMap<>();
    
    @FXML
    public void initialize() {
        apiService = ApiService.getInstance();
        encryptionService = EncryptionService.getInstance();
        localStorageService = LocalStorageService.getInstance();
        
        courseComboBox.setOnAction(e -> applyFilters());
        subjectComboBox.setOnAction(e -> applyFilters());
        
        // Setup registration statistics table
        courseColumn.setCellValueFactory(new PropertyValueFactory<>("course"));
        subjectCountColumn.setCellValueFactory(new PropertyValueFactory<>("registeredCount"));
        totalSubjectsColumn.setCellValueFactory(new PropertyValueFactory<>("totalCount"));
        percentageColumn.setCellValueFactory(new PropertyValueFactory<>("percentage"));
        
        // Setup selected courses table
        selectedCourseColumn.setCellValueFactory(new PropertyValueFactory<>("course"));
        selectedSubjectColumn.setCellValueFactory(new PropertyValueFactory<>("subjectName"));
        selectedClassColumn.setCellValueFactory(new PropertyValueFactory<>("classNumber"));
    }
    
    public void loadVirtualCalendar() {
        statusLabel.setText("Đang tải danh sách môn học ảo...");
        
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
                
                // Lấy public key
                String publicKey = apiService.getPublicKey();
                encryptionService.setPublicKey(publicKey);
                
                // Gọi API virtual calendar
                String response = apiService.getVirtualCalendar(
                    credentials.get("encryptedKey").getAsString(),
                    credentials.get("encryptedData").getAsString(),
                    credentials.get("iv").getAsString()
                );
                
                // Parse response
                JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
                
                if (!"200".equals(jsonResponse.get("code").getAsString())) {
                    String message = jsonResponse.has("message") ? 
                        jsonResponse.get("message").getAsString() : 
                        "Không thể tải danh sách môn học ảo";
                    throw new IOException(message);
                }
                
                JsonArray virtualCalendar = jsonResponse.getAsJsonArray("virtual_calendar");
                
                Platform.runLater(() -> {
                    parseAndDisplayCourses(virtualCalendar);
                });
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Lỗi khi tải danh sách môn học ảo");
                    showAlert(Alert.AlertType.ERROR, "Lỗi", 
                            "Không thể tải danh sách môn học ảo: " + e.getMessage());
                    e.printStackTrace();
                });
            }
        }).start();
    }
    
    private void parseAndDisplayCourses(JsonArray virtualCalendar) {
        allCourses.clear();
        coursesByCourse.clear();
        coursesByDisplayName.clear();
        
        for (JsonElement element : virtualCalendar) {
            JsonObject courseObj = element.getAsJsonObject();
            
            VirtualCourse course = new VirtualCourse();
            course.setCourse(getStringValue(courseObj, "course"));
            course.setBaseTime(getStringValue(courseObj, "base_time"));
            
            // Lưu course_name từ ngoài (tên thường, không có mã lớp) - dùng để filter
            String displayCourseName = getStringValue(courseObj, "course_name");
            course.setDisplayCourseName(displayCourseName);
            
            if (courseObj.has("details")) {
                JsonObject details = courseObj.getAsJsonObject("details");
                // Sử dụng course_name từ details (có mã lớp trong ngoặc như A22C10D901)
                String detailsCourseName = getStringValue(details, "course_name");
                if (detailsCourseName != null && !detailsCourseName.isEmpty()) {
                    // Giữ nguyên course_name từ details, không thêm đuôi (badge sẽ hiển thị riêng)
                    course.setCourseName(detailsCourseName);
                } else {
                    course.setCourseName(displayCourseName);
                }
                
                course.setCourseCode(getStringValue(details, "course_code"));
                course.setTeacher(getStringValue(details, "teacher"));
                course.setStudyLocation(getStringValue(details, "study_location"));
                course.setStudyDays(getStringValue(details, "study_days"));
                course.setLessons(getStringValue(details, "lessons"));
            } else {
                course.setCourseName(displayCourseName);
            }
            
            // Không parse schedule slots ngay - sẽ parse lazy khi cần
            // course.parseScheduleSlots(); // Comment out để lazy load
            
            allCourses.add(course);
            
            // Group by course (AT22, AT21, etc.)
            String courseKey = course.getCourse();
            coursesByCourse.computeIfAbsent(courseKey, k -> new ArrayList<>()).add(course);
            
            // Group by displayCourseName (để filter theo môn học)
            if (displayCourseName != null && !displayCourseName.isEmpty()) {
                coursesByDisplayName.computeIfAbsent(displayCourseName, k -> new ArrayList<>()).add(course);
            }
        }
        
        // Update course combo box
        courseComboBox.getItems().clear();
        courseComboBox.getItems().add("Tất cả khóa");
        courseComboBox.getItems().addAll(coursesByCourse.keySet().stream().sorted().collect(Collectors.toList()));
        courseComboBox.getSelectionModel().select(0);
        
        // Update subject combo box
        subjectComboBox.getItems().clear();
        subjectComboBox.getItems().add("Tất cả môn");
        subjectComboBox.getItems().addAll(coursesByDisplayName.keySet().stream().sorted().collect(Collectors.toList()));
        subjectComboBox.getSelectionModel().select(0);
        
        applyFilters();
        updateRegistrationTable();
        
        // Đếm số môn học distinct theo displayCourseName
        long distinctSubjectCount = allCourses.stream()
            .map(VirtualCourse::getDisplayCourseName)
            .filter(name -> name != null && !name.isEmpty())
            .distinct()
            .count();
        
        statusLabel.setText("Đã tải " + distinctSubjectCount + " môn học (" + allCourses.size() + " lớp)");
        
        // Restore selected courses từ local storage
        restoreSelectedCourses();
    }
    
    @FXML
    private void applyFilters() {
        String selectedCourse = courseComboBox.getSelectionModel().getSelectedItem();
        String selectedSubject = subjectComboBox.getSelectionModel().getSelectedItem();
        
        List<VirtualCourse> coursesToShow = new ArrayList<>(allCourses);
        
        // Filter by course (AT22, AT21...)
        if (selectedCourse != null && !"Tất cả khóa".equals(selectedCourse)) {
            coursesToShow = coursesToShow.stream()
                .filter(c -> selectedCourse.equals(c.getCourse()))
                .collect(Collectors.toList());
        }
        
        // Filter by subject (displayCourseName)
        if (selectedSubject != null && !"Tất cả môn".equals(selectedSubject)) {
            coursesToShow = coursesToShow.stream()
                .filter(c -> selectedSubject.equals(c.getDisplayCourseName()))
                .collect(Collectors.toList());
        }
        
        displayCourses(coursesToShow);
    }
    
    private void displayCourses(List<VirtualCourse> courses) {
        coursesContainer.getChildren().clear();
        courseCheckBoxMap.clear();
        
        for (VirtualCourse course : courses) {
            VBox courseBox = createCourseBox(course);
            coursesContainer.getChildren().add(courseBox);
        }
    }
    
    private VBox createCourseBox(VirtualCourse course) {
        VBox courseBox = new VBox(10);
        courseBox.setPadding(new Insets(15));
        courseBox.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e0e0e0; -fx-border-radius: 8; -fx-border-width: 1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 5, 0, 0, 2);");
        
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        CheckBox checkBox = new CheckBox();
        checkBox.setUserData(course);
        checkBox.setOnAction(e -> handleCourseSelection(course, checkBox.isSelected()));
        courseCheckBoxMap.put(course, checkBox);
        
        // Update checkbox state
        checkBox.setSelected(selectedCourses.contains(course));
        
        // Hiển thị lớp số nếu có
        String classNumber = course.getClassNumber();
        HBox titleBox = new HBox(8);
        titleBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label courseNameLabel = new Label(course.getCourseName());
        courseNameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #1a1a1a;");
        courseNameLabel.setWrapText(true);
        
        // Hiển thị: checkbox, tên môn học, lớp số (nếu có)
        titleBox.getChildren().add(checkBox);
        titleBox.getChildren().add(courseNameLabel);
        
        if (!classNumber.isEmpty()) {
            Label classLabel = new Label(classNumber);
            classLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #ffffff; -fx-background-color: linear-gradient(to bottom, #3498db, #2980b9); -fx-padding: 5 10 5 10; -fx-background-radius: 12; -fx-effect: dropshadow(one-pass-box, rgba(52,152,219,0.3), 3, 0, 0, 1);");
            titleBox.getChildren().add(classLabel);
        }
        
        headerBox.getChildren().add(titleBox);
        
        // Course info
        VBox infoBox = new VBox(6);
        infoBox.setPadding(new Insets(8, 0, 0, 0));
        
        // Hiển thị lớp số nếu có (sử dụng lại biến classNumber đã khai báo ở trên)
        if (!classNumber.isEmpty()) {
            Label classLabel = new Label("📚 Lớp: " + classNumber);
            classLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #2980b9; -fx-font-weight: bold;");
            infoBox.getChildren().add(classLabel);
        }
        
        if (course.getCourseCode() != null && !course.getCourseCode().isEmpty()) {
            Label codeLabel = new Label("🔢 Mã môn: " + course.getCourseCode());
            codeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #555555;");
            infoBox.getChildren().add(codeLabel);
        }
        
        // Hiển thị thời gian từ base_time
        if (course.getBaseTime() != null && !course.getBaseTime().isEmpty()) {
            Label timeLabel = new Label("⏰ " + course.getBaseTime());
            timeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #27ae60; -fx-font-weight: 500;");
            timeLabel.setWrapText(true);
            infoBox.getChildren().add(timeLabel);
        }
        
        // Hiển thị thông tin chi tiết từ schedule slots (nếu có)
        if (course.getLessons() != null && !course.getLessons().isEmpty()) {
            // Parse một vài lessons đầu để hiển thị thời gian
            String[] lessonArray = course.getLessons().split("\\s+");
            if (lessonArray.length > 0) {
                String firstLesson = lessonArray[0];
                String timeRange = course.mapLessonsToTimeRange(firstLesson);
                if (!timeRange.equals(firstLesson)) {
                    Label detailTimeLabel = new Label("🕐 " + timeRange);
                    detailTimeLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #2980b9; -fx-font-weight: bold; -fx-background-color: #e8f4f8; -fx-padding: 4 8 4 8; -fx-background-radius: 4;");
                    infoBox.getChildren().add(detailTimeLabel);
                }
            }
        }
        
        if (course.getTeacher() != null && !course.getTeacher().isEmpty()) {
            Label teacherLabel = new Label("👤 " + course.getTeacher());
            teacherLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");
            infoBox.getChildren().add(teacherLabel);
        }
        
        if (course.getStudyLocation() != null && !course.getStudyLocation().isEmpty()) {
            Label locationLabel = new Label("📍 " + course.getStudyLocation());
            locationLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");
            infoBox.getChildren().add(locationLabel);
        }
        
        courseBox.getChildren().addAll(headerBox, infoBox);
        
        return courseBox;
    }
    
    private void handleCourseSelection(VirtualCourse course, boolean selected) {
        if (selected) {
            // Kiểm tra xem đã có môn học cùng tên (displayCourseName) chưa
            // Nếu có thì tự động xóa lớp cũ và thay thế bằng lớp mới
            String displayName = course.getDisplayCourseName();
            if (displayName != null && !displayName.isEmpty()) {
                VirtualCourse existingCourse = selectedCourses.stream()
                    .filter(c -> displayName.equals(c.getDisplayCourseName()))
                    .findFirst()
                    .orElse(null);
                
                if (existingCourse != null && !existingCourse.equals(course)) {
                    // Xóa lớp cũ
                    selectedCourses.remove(existingCourse);
                    // Uncheck checkbox của lớp cũ
                    if (courseCheckBoxMap.containsKey(existingCourse)) {
                        courseCheckBoxMap.get(existingCourse).setSelected(false);
                    }
                }
            }
            
            // Kiểm tra trùng lịch
            List<VirtualCourse> conflictingCourses = findConflictingCourses(course);
            
            if (!conflictingCourses.isEmpty()) {
                // Có trùng lịch, hủy selection và thông báo
                courseCheckBoxMap.get(course).setSelected(false);
                
                StringBuilder message = new StringBuilder("Môn học này trùng lịch với:\n");
                for (VirtualCourse conflict : conflictingCourses) {
                    message.append("- ").append(conflict.getCourseName()).append("\n");
                }
                message.append("\nVui lòng hủy chọn các môn trùng hoặc chọn lại.");
                
                showAlert(Alert.AlertType.WARNING, "Trùng lịch học", message.toString());
                return;
            }
            
            selectedCourses.add(course);
        } else {
            selectedCourses.remove(course);
        }
        
        updateSelectedScheduleDisplay();
        updateRegistrationTable();
        
        // Lưu selected courses vào local storage
        saveSelectedCourses();
    }
    
    private List<VirtualCourse> findConflictingCourses(VirtualCourse newCourse) {
        List<VirtualCourse> conflicts = new ArrayList<>();
        
        for (VirtualCourse selectedCourse : selectedCourses) {
            if (hasScheduleConflict(newCourse, selectedCourse)) {
                conflicts.add(selectedCourse);
            }
        }
        
        return conflicts;
    }
    
    private boolean hasScheduleConflict(VirtualCourse course1, VirtualCourse course2) {
        // Parse slots khi cần kiểm tra conflict
        List<VirtualCourse.ScheduleSlot> slots1 = course1.getScheduleSlots();
        List<VirtualCourse.ScheduleSlot> slots2 = course2.getScheduleSlots();
        
        // Giới hạn số lượng so sánh để tránh quá tải
        int maxCompare = Math.min(slots1.size(), 100);
        int maxCompare2 = Math.min(slots2.size(), 100);
        
        for (int i = 0; i < maxCompare; i++) {
            VirtualCourse.ScheduleSlot slot1 = slots1.get(i);
            for (int j = 0; j < maxCompare2; j++) {
                VirtualCourse.ScheduleSlot slot2 = slots2.get(j);
                if (slot1.conflictsWith(slot2)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private void updateSelectedScheduleDisplay() {
        selectedScheduleContainer.getChildren().clear();
        
        if (selectedCourses.isEmpty()) {
            Label emptyLabel = new Label("Chưa có môn học nào được chọn");
            emptyLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14px;");
            selectedScheduleContainer.getChildren().add(emptyLabel);
            return;
        }
        
        // Group by date
        Map<LocalDate, List<VirtualCourse.ScheduleSlot>> slotsByDate = new HashMap<>();
        
        for (VirtualCourse course : selectedCourses) {
            for (VirtualCourse.ScheduleSlot slot : course.getScheduleSlots()) {
                slotsByDate.computeIfAbsent(slot.getDate(), k -> new ArrayList<>()).add(slot);
            }
        }
        
        // Display by date
        slotsByDate.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                VBox dayBox = createDayScheduleBox(entry.getKey(), entry.getValue());
                selectedScheduleContainer.getChildren().add(dayBox);
            });
    }
    
    private VBox createDayScheduleBox(LocalDate date, List<VirtualCourse.ScheduleSlot> slots) {
        VBox dayBox = new VBox(10);
        dayBox.setPadding(new Insets(15));
        dayBox.setStyle("-fx-background-color: linear-gradient(to bottom, #f8f9fa, #ffffff); -fx-border-color: #d0d0d0; -fx-border-radius: 8; -fx-border-width: 1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.06), 4, 0, 0, 2);");
        
        Label dateLabel = new Label("📅 " + formatDate(date));
        dateLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 17px; -fx-text-fill: #2c3e50; -fx-padding: 0 0 8 0;");
        dayBox.getChildren().add(dateLabel);
        
        // Group slots by course
        Map<VirtualCourse, List<VirtualCourse.ScheduleSlot>> slotsByCourse = new HashMap<>();
        for (VirtualCourse.ScheduleSlot slot : slots) {
            slotsByCourse.computeIfAbsent(slot.getVirtualCourse(), k -> new ArrayList<>()).add(slot);
        }
        
        for (Map.Entry<VirtualCourse, List<VirtualCourse.ScheduleSlot>> entry : slotsByCourse.entrySet()) {
            VirtualCourse course = entry.getKey();
            List<VirtualCourse.ScheduleSlot> courseSlots = entry.getValue();
            
            VBox courseSlotBox = new VBox(6);
            courseSlotBox.setPadding(new Insets(12));
            courseSlotBox.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e0e0e0; -fx-border-radius: 6; -fx-border-width: 1; -fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.05), 2, 0, 0, 1);");
            
            HBox titleBox = new HBox(8);
            titleBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            
            Label courseLabel = new Label(course.getCourseName());
            courseLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2980b9;");
            
            // Hiển thị: tên môn học, lớp số (nếu có)
            titleBox.getChildren().add(courseLabel);
            
            // Hiển thị lớp số nếu có
            String classNumber = course.getClassNumber();
            if (!classNumber.isEmpty()) {
                Label classLabel = new Label(classNumber);
                classLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #ffffff; -fx-background-color: linear-gradient(to bottom, #3498db, #2980b9); -fx-padding: 4 8 4 8; -fx-background-radius: 10; -fx-effect: dropshadow(one-pass-box, rgba(52,152,219,0.3), 2, 0, 0, 1);");
                titleBox.getChildren().add(classLabel);
            }
            
            VBox infoBox = new VBox(4);
            
            // Hiển thị thời gian cụ thể từ lessons
            String firstLesson = courseSlots.get(0).getLessons();
            String timeRange = course.mapLessonsToTimeRange(firstLesson);
            if (!timeRange.equals(firstLesson)) {
                Label timeLabel = new Label("🕐 " + timeRange);
                timeLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #2980b9; -fx-font-weight: bold; -fx-background-color: #e8f4f8; -fx-padding: 5 10 5 10; -fx-background-radius: 5;");
                infoBox.getChildren().add(timeLabel);
            } else {
                Label lessonsLabel = new Label("📖 Tiết: " + firstLesson);
                lessonsLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #495057;");
                infoBox.getChildren().add(lessonsLabel);
            }
            
            if (course.getStudyLocation() != null && !course.getStudyLocation().isEmpty()) {
                Label locationLabel = new Label("📍 " + course.getStudyLocation());
                locationLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");
                infoBox.getChildren().add(locationLabel);
            }
            
            if (course.getTeacher() != null && !course.getTeacher().isEmpty()) {
                Label teacherLabel = new Label("👤 " + course.getTeacher());
                teacherLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");
                infoBox.getChildren().add(teacherLabel);
            }
            
            if (course.getCourseCode() != null && !course.getCourseCode().isEmpty()) {
                Label codeLabel = new Label("Mã: " + course.getCourseCode());
                codeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6c757d;");
                infoBox.getChildren().add(codeLabel);
            }
            
            courseSlotBox.getChildren().addAll(titleBox, infoBox);
            dayBox.getChildren().add(courseSlotBox);
        }
        
        return dayBox;
    }
    
    @FXML
    private void handleExportTxt() {
        if (selectedCourses.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", 
                    "Không có môn học nào được chọn để xuất.");
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Lưu danh sách môn học đã chọn");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Text Files", "*.txt")
        );
        fileChooser.setInitialFileName("danh_sach_mon_hoc.txt");
        
        Stage stage = (Stage) exportTxtButton.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);
        
        if (file != null) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8))) {
                // Ghi header
                writer.write("# Danh sách môn học đã chọn");
                writer.newLine();
                writer.write("# Format: Mỗi dòng là tên môn học đầy đủ");
                writer.newLine();
                writer.write("# Tổng số: " + selectedCourses.size() + " môn học");
                writer.newLine();
                writer.newLine();
                
                // Ghi danh sách môn học
                for (VirtualCourse course : selectedCourses) {
                    writer.write(course.getCourseName());
                    writer.newLine();
                }
                
                showAlert(Alert.AlertType.INFORMATION, "Thành công", 
                        "Đã xuất danh sách môn học thành công!\nFile: " + file.getAbsolutePath());
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", 
                        "Không thể xuất file: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    @FXML
    private void handleImportTxt() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn file danh sách môn học");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Text Files", "*.txt")
        );
        
        Stage stage = (Stage) importTxtButton.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        
        if (file != null) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file, java.nio.charset.StandardCharsets.UTF_8))) {
                Set<String> courseNamesToImport = new HashSet<>();
                String line;
                
                // Đọc file, bỏ qua các dòng comment (bắt đầu bằng #)
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("#")) {
                        courseNamesToImport.add(line);
                    }
                }
                
                if (courseNamesToImport.isEmpty()) {
                    showAlert(Alert.AlertType.WARNING, "Cảnh báo", 
                            "File không chứa môn học nào.");
                    return;
                }
                
                // Tìm và chọn các môn học matching
                int foundCount = 0;
                int notFoundCount = 0;
                List<String> notFoundCourses = new ArrayList<>();
                
                // Xóa tất cả selection hiện tại
                handleClearAll();
                
                // Tìm và chọn các môn học
                for (VirtualCourse course : allCourses) {
                    if (courseNamesToImport.contains(course.getCourseName())) {
                        // Kiểm tra trùng lịch trước khi chọn
                        List<VirtualCourse> conflictingCourses = findConflictingCourses(course);
                        
                        if (conflictingCourses.isEmpty()) {
                            selectedCourses.add(course);
                            if (courseCheckBoxMap.containsKey(course)) {
                                courseCheckBoxMap.get(course).setSelected(true);
                            }
                            foundCount++;
                        } else {
                            // Có trùng lịch, bỏ qua
                            notFoundCount++;
                            notFoundCourses.add(course.getCourseName() + " (trùng lịch)");
                        }
                        
                        courseNamesToImport.remove(course.getCourseName());
                    }
                }
                
                // Các môn không tìm thấy
                notFoundCourses.addAll(courseNamesToImport);
                notFoundCount += courseNamesToImport.size();
                
                // Cập nhật UI
                updateSelectedScheduleDisplay();
                updateRegistrationTable();
                
                // Lưu vào local storage
                saveSelectedCourses();
                
                // Thông báo kết quả
                StringBuilder message = new StringBuilder();
                message.append("Đã nhập ").append(foundCount).append(" môn học.\n");
                
                if (notFoundCount > 0) {
                    message.append("\nKhông tìm thấy hoặc không thể chọn: ").append(notFoundCount).append(" môn:\n");
                    int showCount = Math.min(5, notFoundCourses.size());
                    for (int i = 0; i < showCount; i++) {
                        message.append("- ").append(notFoundCourses.get(i)).append("\n");
                    }
                    if (notFoundCourses.size() > 5) {
                        message.append("... và ").append(notFoundCourses.size() - 5).append(" môn khác");
                    }
                }
                
                showAlert(Alert.AlertType.INFORMATION, "Kết quả nhập file", message.toString());
                
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", 
                        "Không thể đọc file: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    @FXML
    private void handleClearAll() {
        selectedCourses.clear();
        updateSelectedScheduleDisplay();
        updateRegistrationTable();
        
        // Uncheck all checkboxes
        for (Map.Entry<VirtualCourse, CheckBox> entry : courseCheckBoxMap.entrySet()) {
            entry.getValue().setSelected(false);
        }
        
        // Lưu vào local storage (xóa tất cả)
        saveSelectedCourses();
    }
    
    @FXML
    private void handleViewCalendar() {
        if (selectedCourses.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", 
                    "Chưa có môn học nào được chọn. Vui lòng chọn môn học trước khi xem lịch.");
            return;
        }
        
        // Tạo Stage fullscreen cho calendar view
        Stage calendarStage = new Stage();
        calendarStage.setTitle("Lịch học ảo - Calendar View");
        calendarStage.initModality(Modality.WINDOW_MODAL);
        calendarStage.initOwner(viewCalendarButton.getScene().getWindow());
        calendarStage.initStyle(StageStyle.DECORATED);
        
        // Tạo root container
        VBox root = new VBox(15);
        root.setStyle("-fx-background-color: #f5f5f5;");
        
        // Header với navigation
        VBox headerBox = new VBox(10);
        headerBox.setPadding(new Insets(20));
        headerBox.setStyle("-fx-background-color: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 0);");
        
        HBox titleBox = new HBox(15);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        
        Label titleLabel = new Label("📅 Lịch học ảo - Calendar View");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button closeButton = new Button("✕ Đóng");
        closeButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-pref-width: 100px; -fx-pref-height: 35px; -fx-background-radius: 5px; -fx-cursor: hand; -fx-font-weight: 500;");
        closeButton.setOnAction(e -> calendarStage.close());
        
        titleBox.getChildren().addAll(titleLabel, spacer, closeButton);
        
        // Navigation box
        HBox navBox = new HBox(20);
        navBox.setAlignment(Pos.CENTER);
        navBox.setPadding(new Insets(10, 0, 10, 0));
        
        Button prevMonthButton = new Button("◀");
        prevMonthButton.setStyle("-fx-background-color: linear-gradient(to bottom, #3498db, #2980b9); -fx-text-fill: white; -fx-pref-width: 50px; -fx-pref-height: 50px; -fx-background-radius: 25px; -fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: 18px; -fx-effect: dropshadow(three-pass-box, rgba(52,152,219,0.3), 4, 0, 0, 2);");
        
        Label currentMonthLabel = new Label();
        currentMonthLabel.setStyle("-fx-text-fill: #2980b9; -fx-padding: 10px 30px; -fx-background-color: linear-gradient(to bottom, #e8f4f8, #d6eaf8); -fx-background-radius: 8; -fx-min-width: 250px; -fx-font-weight: bold; -fx-font-size: 20px;");
        
        Button nextMonthButton = new Button("▶");
        nextMonthButton.setStyle("-fx-background-color: linear-gradient(to bottom, #3498db, #2980b9); -fx-text-fill: white; -fx-pref-width: 50px; -fx-pref-height: 50px; -fx-background-radius: 25px; -fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: 18px; -fx-effect: dropshadow(three-pass-box, rgba(52,152,219,0.3), 4, 0, 0, 2);");
        
        navBox.getChildren().addAll(prevMonthButton, currentMonthLabel, nextMonthButton);
        
        headerBox.getChildren().addAll(titleBox, navBox);
        
        // Calendar container
        VBox calendarContainer = new VBox();
        calendarContainer.setAlignment(Pos.CENTER);
        calendarContainer.setPadding(new Insets(20));
        
        ScrollPane scrollPane = new ScrollPane(calendarContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        
        root.getChildren().addAll(headerBox, scrollPane);
        
        // Tạo scene và set fullscreen
        Scene scene = new Scene(root, 1400, 900);
        calendarStage.setScene(scene);
        calendarStage.setMaximized(true);
        
        // Tìm tháng đầu tiên có lịch hoặc tháng hiện tại
        YearMonth currentMonth = YearMonth.now();
        if (!selectedCourses.isEmpty()) {
            LocalDate firstDate = selectedCourses.stream()
                .flatMap(course -> course.getScheduleSlots().stream())
                .map(VirtualCourse.ScheduleSlot::getDate)
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now());
            currentMonth = YearMonth.from(firstDate);
        }
        
        final YearMonth[] displayMonth = {currentMonth};
        
        // Update month label
        updateMonthLabel(currentMonthLabel, displayMonth[0]);
        
        // Navigation handlers
        prevMonthButton.setOnAction(e -> {
            displayMonth[0] = displayMonth[0].minusMonths(1);
            updateMonthLabel(currentMonthLabel, displayMonth[0]);
            displayCalendarGrid(calendarContainer, displayMonth[0]);
        });
        
        nextMonthButton.setOnAction(e -> {
            displayMonth[0] = displayMonth[0].plusMonths(1);
            updateMonthLabel(currentMonthLabel, displayMonth[0]);
            displayCalendarGrid(calendarContainer, displayMonth[0]);
        });
        
        // Hiển thị calendar ban đầu
        displayCalendarGrid(calendarContainer, displayMonth[0]);
        
        calendarStage.show();
    }
    
    private void updateMonthLabel(Label label, YearMonth yearMonth) {
        String monthName = yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.forLanguageTag("vi")));
        label.setText("📅 " + monthName);
    }
    
    private void displayCalendarGrid(VBox container, YearMonth yearMonth) {
        container.getChildren().clear();
        
        // Thu thập tất cả schedule slots từ selectedCourses
        Map<LocalDate, List<VirtualCourse.ScheduleSlot>> slotsByDate = new HashMap<>();
        for (VirtualCourse course : selectedCourses) {
            for (VirtualCourse.ScheduleSlot slot : course.getScheduleSlots()) {
                slotsByDate.computeIfAbsent(slot.getDate(), k -> new ArrayList<>()).add(slot);
            }
        }
        
        // Lọc slots trong tháng hiện tại
        Map<LocalDate, List<VirtualCourse.ScheduleSlot>> monthSlots = new HashMap<>();
        for (Map.Entry<LocalDate, List<VirtualCourse.ScheduleSlot>> entry : slotsByDate.entrySet()) {
            if (YearMonth.from(entry.getKey()).equals(yearMonth)) {
                monthSlots.put(entry.getKey(), entry.getValue());
            }
        }
        
        // Tạo calendar grid
        VBox monthBox = createMonthCalendarGrid(yearMonth, monthSlots);
        container.getChildren().add(monthBox);
    }
    
    private VBox createMonthCalendarGrid(YearMonth yearMonth, Map<LocalDate, List<VirtualCourse.ScheduleSlot>> slotsByDate) {
        VBox monthBox = new VBox(15);
        monthBox.setPadding(new Insets(20));
        monthBox.setAlignment(Pos.CENTER);
        monthBox.setStyle("-fx-background-color: linear-gradient(to bottom, #ffffff, #f8f9fa); -fx-border-color: #3498db; -fx-border-radius: 12; -fx-border-width: 2; -fx-effect: dropshadow(three-pass-box, rgba(52,152,219,0.2), 10, 0, 0, 3);");
        
        // Tạo calendar grid
        GridPane calendarGrid = new GridPane();
        calendarGrid.setHgap(3);
        calendarGrid.setVgap(3);
        calendarGrid.setPadding(new Insets(10));
        calendarGrid.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8;");
        
        // Header cho các ngày trong tuần
        String[] dayNames = {"Chủ Nhật", "Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7"};
        for (int i = 0; i < 7; i++) {
            Label dayHeader = new Label(dayNames[i]);
            dayHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: white; -fx-padding: 12px; -fx-alignment: center; -fx-background-color: linear-gradient(to bottom, #34495e, #2c3e50); -fx-background-radius: 6 6 0 0;");
            dayHeader.setMaxWidth(Double.MAX_VALUE);
            dayHeader.setPrefHeight(45);
            GridPane.setHgrow(dayHeader, Priority.ALWAYS);
            calendarGrid.add(dayHeader, i, 0);
        }
        
        // Lấy ngày đầu tiên và cuối cùng của tháng
        LocalDate firstDay = yearMonth.atDay(1);
        LocalDate lastDay = yearMonth.atEndOfMonth();
        
        // Tìm ngày đầu tiên trong tuần (Chủ Nhật)
        LocalDate calendarStart = firstDay.with(DayOfWeek.SUNDAY);
        if (calendarStart.isAfter(firstDay)) {
            calendarStart = calendarStart.minusWeeks(1);
        }
        
        // Tìm ngày cuối cùng trong tuần (Thứ 7)
        LocalDate calendarEnd = lastDay.with(DayOfWeek.SATURDAY);
        if (calendarEnd.isBefore(lastDay)) {
            calendarEnd = calendarEnd.plusWeeks(1);
        }
        
        LocalDate currentDate = calendarStart;
        int row = 1;
        LocalDate today = LocalDate.now();
        
        while (!currentDate.isAfter(calendarEnd)) {
            for (int col = 0; col < 7; col++) {
                final LocalDate dateForCell = currentDate;
                List<VirtualCourse.ScheduleSlot> daySlots = slotsByDate.getOrDefault(dateForCell, new ArrayList<>());
                VBox dayCell = createDayCell(dateForCell, yearMonth, daySlots, dateForCell.equals(today));
                dayCell.setMaxWidth(Double.MAX_VALUE);
                dayCell.setPrefHeight(130);
                GridPane.setHgrow(dayCell, Priority.ALWAYS);
                
                // Thêm click handler để hiển thị popup
                if (!daySlots.isEmpty()) {
                    final List<VirtualCourse.ScheduleSlot> slotsForDialog = new ArrayList<>(daySlots);
                    dayCell.setOnMouseClicked(e -> showDayDetailsDialog(dateForCell, slotsForDialog));
                }
                
                calendarGrid.add(dayCell, col, row);
                
                currentDate = currentDate.plusDays(1);
            }
            row++;
        }
        
        monthBox.getChildren().add(calendarGrid);
        return monthBox;
    }
    
    private VBox createDayCell(LocalDate date, YearMonth yearMonth, List<VirtualCourse.ScheduleSlot> slots, boolean isToday) {
        VBox dayCell = new VBox(4);
        dayCell.setPadding(new Insets(5));
        dayCell.setAlignment(Pos.TOP_CENTER);
        
        // Xác định style dựa trên ngày
        boolean isCurrentMonth = YearMonth.from(date).equals(yearMonth);
        String backgroundColor = isToday ? "#e8f4f8" : (isCurrentMonth ? "#ffffff" : "#f5f5f5");
        String borderColor = isToday ? "#3498db" : "#e0e0e0";
        String textColor = isToday ? "#2980b9" : (isCurrentMonth ? "#2c3e50" : "#bdc3c7");
        int borderWidth = isToday ? 2 : 1;
        
        dayCell.setStyle(String.format(
            "-fx-background-color: %s; -fx-border-color: %s; -fx-border-radius: 6; -fx-border-width: %d;",
            backgroundColor, borderColor, borderWidth
        ));
        
        // Số ngày
        Label dayNumber = new Label(String.valueOf(date.getDayOfMonth()));
        dayNumber.setStyle(String.format(
            "-fx-font-weight: %s; -fx-font-size: %dpx; -fx-text-fill: %s;",
            isToday ? "bold" : "normal",
            isToday ? 16 : 14,
            textColor
        ));
        dayCell.getChildren().add(dayNumber);
        
        // Hiển thị số lượng buổi học
        if (!slots.isEmpty()) {
            // Thêm background highlight cho ngày có lịch
            if (!isToday) {
                dayCell.setStyle(String.format(
                    "-fx-background-color: %s; -fx-border-color: %s; -fx-border-radius: 6; -fx-border-width: %d;",
                    isCurrentMonth ? "#fff9e6" : "#f5f5f5", borderColor, borderWidth
                ));
            }
            
            Label countLabel = new Label(slots.size() + " buổi");
            countLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-background-color: #ffebee; -fx-padding: 2 6 2 6; -fx-background-radius: 10;");
            dayCell.getChildren().add(countLabel);
            
            // Hiển thị tối đa 2 môn học đầu tiên
            int maxDisplay = Math.min(slots.size(), 2);
            Set<String> displayedSubjects = new HashSet<>();
            int displayedCount = 0;
            
            for (VirtualCourse.ScheduleSlot slot : slots) {
                if (displayedCount >= maxDisplay) break;
                
                VirtualCourse course = slot.getVirtualCourse();
                String subjectText = course.getDisplayCourseName() != null ? 
                    course.getDisplayCourseName() : course.getCourseName();
                
                if (displayedSubjects.add(subjectText)) {
                    if (subjectText.length() > 15) {
                        subjectText = subjectText.substring(0, 13) + "...";
                    }
                    Label subjectLabel = new Label(subjectText);
                    subjectLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #555; -fx-padding: 2 4 2 4; -fx-background-color: #f0f0f0; -fx-background-radius: 4; -fx-max-width: 100;");
                    subjectLabel.setWrapText(true);
                    dayCell.getChildren().add(subjectLabel);
                    displayedCount++;
                }
            }
            
            if (slots.size() > maxDisplay) {
                Label moreLabel = new Label("+" + (slots.size() - maxDisplay) + " nữa");
                moreLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #7f8c8d; -fx-font-style: italic;");
                dayCell.getChildren().add(moreLabel);
            }
            
            // Thêm cursor và hover effect
            dayCell.setCursor(javafx.scene.Cursor.HAND);
            dayCell.setOnMouseEntered(e -> {
                dayCell.setStyle(String.format(
                    "-fx-background-color: %s; -fx-border-color: #3498db; -fx-border-radius: 6; -fx-border-width: 2; -fx-cursor: hand;",
                    isToday ? "#d6eaf8" : (isCurrentMonth ? "#f0f8ff" : "#f5f5f5")
                ));
            });
            
            dayCell.setOnMouseExited(e -> {
                String bgColor = isToday ? "#e8f4f8" : (isCurrentMonth ? (!slots.isEmpty() ? "#fff9e6" : "#ffffff") : "#f5f5f5");
                dayCell.setStyle(String.format(
                    "-fx-background-color: %s; -fx-border-color: %s; -fx-border-radius: 6; -fx-border-width: %d;",
                    bgColor, borderColor, borderWidth
                ));
            });
        }
        
        return dayCell;
    }
    
    private void showDayDetailsDialog(LocalDate date, List<VirtualCourse.ScheduleSlot> slots) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Chi tiết lịch học");
        dialog.setHeaderText("📅 " + formatDate(date) + " - " + slots.size() + " buổi học");
        
        // Group slots by course
        Map<VirtualCourse, List<VirtualCourse.ScheduleSlot>> slotsByCourse = new HashMap<>();
        for (VirtualCourse.ScheduleSlot slot : slots) {
            slotsByCourse.computeIfAbsent(slot.getVirtualCourse(), k -> new ArrayList<>()).add(slot);
        }
        
        // Tạo nội dung dialog
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(600);
        
        for (Map.Entry<VirtualCourse, List<VirtualCourse.ScheduleSlot>> entry : slotsByCourse.entrySet()) {
            VirtualCourse course = entry.getKey();
            List<VirtualCourse.ScheduleSlot> courseSlots = entry.getValue();
            
            VBox sessionBox = new VBox(10);
            sessionBox.setPadding(new Insets(15));
            sessionBox.setStyle("-fx-background-color: linear-gradient(to bottom, #f8f9fa, #ffffff); -fx-border-color: #d0d0d0; -fx-border-radius: 8; -fx-border-width: 1; -fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.05), 3, 0, 0, 1);");
            
            // Môn học
            HBox titleBox = new HBox(8);
            titleBox.setAlignment(Pos.CENTER_LEFT);
            
            Label subjectLabel = new Label("📚 " + (course.getDisplayCourseName() != null ? course.getDisplayCourseName() : course.getCourseName()));
            subjectLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #2c3e50; -fx-padding: 5 0 5 0;");
            
            String classNumber = course.getClassNumber();
            if (!classNumber.isEmpty()) {
                Label classLabel = new Label(classNumber);
                classLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #ffffff; -fx-background-color: linear-gradient(to bottom, #3498db, #2980b9); -fx-padding: 4 8 4 8; -fx-background-radius: 10;");
                titleBox.getChildren().add(classLabel);
            }
            
            titleBox.getChildren().add(0, subjectLabel);
            
            // Thời gian
            String firstLesson = courseSlots.get(0).getLessons();
            String timeRange = course.mapLessonsToTimeRange(firstLesson);
            HBox timeBox = new HBox(8);
            timeBox.setAlignment(Pos.CENTER_LEFT);
            Label timeIcon = new Label("🕐");
            Label timeLabel = new Label(timeRange);
            timeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #2980b9; -fx-background-color: #e8f4f8; -fx-padding: 6 14 6 14; -fx-background-radius: 6;");
            timeBox.getChildren().addAll(timeIcon, timeLabel);
            
            VBox infoBox = new VBox(8);
            if (course.getCourseCode() != null && !course.getCourseCode().isEmpty()) {
                Label codeLabel = new Label("🔢 Mã môn: " + course.getCourseCode());
                codeLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #555555;");
                infoBox.getChildren().add(codeLabel);
            }
            if (course.getStudyLocation() != null && !course.getStudyLocation().isEmpty()) {
                Label locationLabel = new Label("📍 Phòng học: " + course.getStudyLocation());
                locationLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #555555;");
                infoBox.getChildren().add(locationLabel);
            }
            if (course.getTeacher() != null && !course.getTeacher().isEmpty()) {
                Label teacherLabel = new Label("👤 Giảng viên: " + course.getTeacher());
                teacherLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #555555;");
                infoBox.getChildren().add(teacherLabel);
            }
            
            sessionBox.getChildren().addAll(titleBox, timeBox);
            if (!infoBox.getChildren().isEmpty()) {
                sessionBox.getChildren().add(infoBox);
            }
            
            content.getChildren().add(sessionBox);
        }
        
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(400);
        scrollPane.setStyle("-fx-background-color: transparent;");
        
        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setStyle("-fx-background-color: #f5f5f5;");
        
        dialog.showAndWait();
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
    
    private String formatDate(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return date.format(formatter);
    }
    
    private String getStringValue(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return "";
    }
    
    /**
     * Trích xuất số lớp từ course_name (helper method)
     */
    private String extractClassNumber(String courseName) {
        if (courseName == null || courseName.isEmpty()) {
            return "";
        }
        
        int lastOpenParen = courseName.lastIndexOf('(');
        int lastCloseParen = courseName.lastIndexOf(')');
        
        if (lastOpenParen == -1 || lastCloseParen == -1 || lastCloseParen <= lastOpenParen) {
            return "";
        }
        
        String classCode = courseName.substring(lastOpenParen + 1, lastCloseParen);
        
        if (classCode.length() >= 2) {
            String lastTwoDigits = classCode.substring(classCode.length() - 2);
            try {
                Integer.parseInt(lastTwoDigits);
                return "L" + lastTwoDigits;
            } catch (NumberFormatException e) {
                String digits = classCode.replaceAll("[^0-9]", "");
                if (digits.length() >= 2) {
                    return "L" + digits.substring(digits.length() - 2);
                }
            }
        }
        
        return "";
    }
    
    private void updateRegistrationTable() {
        registrationTable.getItems().clear();
        selectedCoursesTable.getItems().clear();
        
        if (allCourses.isEmpty()) {
            return;
        }
        
        // Tính toán thống kê theo từng course (AT22, AT21, etc.)
        Map<String, CourseStats> statsMap = new HashMap<>();
        
        // Khởi tạo stats cho tất cả các course
        // Tính tổng số môn distinct (theo displayCourseName) cho mỗi khóa
        for (String courseKey : coursesByCourse.keySet()) {
            List<VirtualCourse> coursesInKey = coursesByCourse.get(courseKey);
            // Đếm số môn học distinct theo displayCourseName
            long totalDistinctSubjects = coursesInKey.stream()
                .map(VirtualCourse::getDisplayCourseName)
                .filter(name -> name != null && !name.isEmpty())
                .distinct()
                .count();
            statsMap.put(courseKey, new CourseStats(courseKey, 0, (int)totalDistinctSubjects));
        }
        
        // Đếm số môn distinct đã đăng ký cho mỗi course
        Map<String, Set<String>> registeredSubjectsByCourse = new HashMap<>();
        for (VirtualCourse selectedCourse : selectedCourses) {
            String courseKey = selectedCourse.getCourse();
            String displayName = selectedCourse.getDisplayCourseName();
            
            if (displayName != null && !displayName.isEmpty()) {
                registeredSubjectsByCourse.computeIfAbsent(courseKey, k -> new HashSet<>()).add(displayName);
            }
            
            // Thêm vào bảng danh sách lớp đã chọn
            SelectedCourseInfo info = new SelectedCourseInfo(
                selectedCourse.getCourse(),
                selectedCourse.getDisplayCourseName() != null ? selectedCourse.getDisplayCourseName() : selectedCourse.getCourseName(),
                selectedCourse.getClassNumber()
            );
            selectedCoursesTable.getItems().add(info);
        }
        
        // Cập nhật số môn đã đăng ký cho mỗi course
        for (Map.Entry<String, Set<String>> entry : registeredSubjectsByCourse.entrySet()) {
            String courseKey = entry.getKey();
            int registeredCount = entry.getValue().size();
            CourseStats stats = statsMap.get(courseKey);
            if (stats != null) {
                stats.setRegisteredCount(registeredCount);
            }
        }
        
        // Thêm vào bảng thống kê
        List<CourseStats> statsList = new ArrayList<>(statsMap.values());
        statsList.sort((a, b) -> a.getCourse().compareTo(b.getCourse()));
        registrationTable.getItems().addAll(statsList);
    }
    
    /**
     * Lưu danh sách môn học đã chọn vào local storage
     */
    private void saveSelectedCourses() {
        try {
            com.google.gson.JsonArray selections = new com.google.gson.JsonArray();
            for (VirtualCourse course : selectedCourses) {
                selections.add(course.getCourseName());
            }
            localStorageService.saveVirtualScheduleSelections(selections);
        } catch (IOException e) {
            System.err.println("Error saving selected courses: " + e.getMessage());
        }
    }
    
    /**
     * Khôi phục danh sách môn học đã chọn từ local storage
     */
    private void restoreSelectedCourses() {
        try {
            com.google.gson.JsonArray savedSelections = localStorageService.loadVirtualScheduleSelections();
            if (savedSelections == null || savedSelections.size() == 0) {
                return;
            }
            
            // Tạo set để tìm nhanh
            Set<String> courseNamesToRestore = new HashSet<>();
            for (com.google.gson.JsonElement element : savedSelections) {
                courseNamesToRestore.add(element.getAsString());
            }
            
            // Tìm và chọn lại các môn học
            int restoredCount = 0;
            List<VirtualCourse> coursesToSelect = new ArrayList<>();
            
            for (VirtualCourse course : allCourses) {
                if (courseNamesToRestore.contains(course.getCourseName())) {
                    coursesToSelect.add(course);
                }
            }
            
            // Chọn từng môn học (giống như user click)
            for (VirtualCourse course : coursesToSelect) {
                // Kiểm tra trùng lịch trước khi restore
                List<VirtualCourse> conflictingCourses = findConflictingCourses(course);
                
                if (conflictingCourses.isEmpty()) {
                    selectedCourses.add(course);
                    // Update checkbox nếu đã có trong map
                    if (courseCheckBoxMap.containsKey(course)) {
                        courseCheckBoxMap.get(course).setSelected(true);
                    }
                    restoredCount++;
                }
            }
            
            // Cập nhật UI
            if (restoredCount > 0) {
                updateSelectedScheduleDisplay();
                updateRegistrationTable();
                
                // Update lại tất cả checkboxes để đảm bảo đồng bộ
                Platform.runLater(() -> {
                    for (Map.Entry<VirtualCourse, CheckBox> entry : courseCheckBoxMap.entrySet()) {
                        entry.getValue().setSelected(selectedCourses.contains(entry.getKey()));
                    }
                });
                
                statusLabel.setText(statusLabel.getText() + " | Đã khôi phục " + restoredCount + " môn học đã chọn");
            }
        } catch (IOException e) {
            System.err.println("Error loading selected courses: " + e.getMessage());
        }
    }
    
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Data class cho bảng thống kê đăng ký
     */
    public static class CourseStats {
        private String course;
        private int registeredCount;
        private int totalCount;
        
        public CourseStats(String course, int registeredCount, int totalCount) {
            this.course = course;
            this.registeredCount = registeredCount;
            this.totalCount = totalCount;
        }
        
        public String getCourse() {
            return course;
        }
        
        public int getRegisteredCount() {
            return registeredCount;
        }
        
        public void setRegisteredCount(int registeredCount) {
            this.registeredCount = registeredCount;
        }
        
        public void incrementRegistered() {
            this.registeredCount++;
        }
        
        public int getTotalCount() {
            return totalCount;
        }
        
        public String getPercentage() {
            if (totalCount == 0) {
                return "0%";
            }
            double percentage = (registeredCount * 100.0) / totalCount;
            return String.format("%.1f%%", percentage);
        }
    }
    
    /**
     * Data class cho bảng danh sách lớp đã chọn
     */
    public static class SelectedCourseInfo {
        private String course;
        private String subjectName;
        private String classNumber;
        
        public SelectedCourseInfo(String course, String subjectName, String classNumber) {
            this.course = course;
            this.subjectName = subjectName;
            this.classNumber = classNumber;
        }
        
        public String getCourse() {
            return course;
        }
        
        public String getSubjectName() {
            return subjectName;
        }
        
        public String getClassNumber() {
            return classNumber;
        }
    }
}

