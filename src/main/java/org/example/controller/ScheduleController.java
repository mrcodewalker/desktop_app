package org.example.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.model.ScheduleItem;
import org.example.model.StudentInfo;
import org.example.service.ApiService;
import org.example.service.IcsExportService;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScheduleController {
    @FXML
    private TableView<ScheduleItem> scheduleTable;
    
    @FXML
    private TableColumn<ScheduleItem, String> dayColumn;
    
    @FXML
    private TableColumn<ScheduleItem, String> timeColumn;
    
    @FXML
    private TableColumn<ScheduleItem, String> subjectColumn;
    
    @FXML
    private TableColumn<ScheduleItem, String> roomColumn;
    
    @FXML
    private TableColumn<ScheduleItem, String> teacherColumn;
    
    @FXML
    private Button backButton;
    
    @FXML
    private Label statusLabel;
    
    @FXML
    private VBox calendarContainer;
    
    @FXML
    private ScrollPane calendarScrollPane;
    
    @FXML
    private Button exportIcsButton;
    
    @FXML
    private ComboBox<String> monthFilterComboBox;
    
    @FXML
    private DatePicker dateFilterPicker;
    
    @FXML
    private Button clearFilterButton;
    
    @FXML
    private ScrollPane subjectInfoScrollPane;
    
    @FXML
    private VBox subjectInfoContainer;
    
    private String authToken;
    private ApiService apiService;
    private StudentInfo studentInfo;
    private List<ScheduleItem> allScheduleItems = new ArrayList<>();
    private List<ScheduleItem> filteredScheduleItems = new ArrayList<>();
    private Map<String, VBox> monthBoxMap = new HashMap<>();
    private Map<LocalDate, VBox> dayBoxMap = new HashMap<>();
    private boolean isInitialLoad = true;
    
    public void setAuthToken(String token) {
        this.authToken = token;
    }
    
    public void setStudentInfo(StudentInfo studentInfo) {
        this.studentInfo = studentInfo;
    }
    
    @FXML
    public void initialize() {
        apiService = ApiService.getInstance();
        
        // Setup table columns
        dayColumn.setCellValueFactory(new PropertyValueFactory<>("day"));
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("time"));
        subjectColumn.setCellValueFactory(new PropertyValueFactory<>("subject"));
        roomColumn.setCellValueFactory(new PropertyValueFactory<>("room"));
        teacherColumn.setCellValueFactory(new PropertyValueFactory<>("teacher"));
        
        // Setup month filter
        monthFilterComboBox.getItems().add("Tất cả tháng");
        
        // Format DatePicker
        dateFilterPicker.setPromptText("dd/MM/yyyy");
        dateFilterPicker.setConverter(new javafx.util.StringConverter<LocalDate>() {
            private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            
            @Override
            public String toString(LocalDate date) {
                if (date != null) {
                    return dateFormatter.format(date);
                } else {
                    return "";
                }
            }
            
            @Override
        public LocalDate fromString(String string) {
                if (string != null && !string.isEmpty()) {
                    return LocalDate.parse(string, dateFormatter);
                } else {
                    return null;
                }
            }
        });
        
        // Load từ local storage nếu có (loadFilterState sẽ được gọi trong loadFromLocalStorage)
        loadFromLocalStorage();
    }
    
    private void loadFromLocalStorage() {
        try {
            org.example.service.LocalStorageService storage = org.example.service.LocalStorageService.getInstance();
            String scheduleJson = storage.loadSchedule();
            if (scheduleJson != null && !scheduleJson.isEmpty()) {
                // Load filter state trước để biết có filter hay không
                loadFilterState();
                boolean hasFilter = monthFilterComboBox.getSelectionModel().getSelectedIndex() > 0 || 
                    dateFilterPicker.getValue() != null;
                
                // Nếu có filter, không scroll đến ngày hôm nay
                if (hasFilter) {
                    isInitialLoad = false;
                }
                
                loadScheduleFromJson(scheduleJson);
                
                // Nếu có filter, apply lại filter sau khi load
                if (hasFilter) {
                    applyFilter();
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading schedule from local storage: " + e.getMessage());
        }
    }
    
    public void loadScheduleFromJson(String scheduleJson) {
        statusLabel.setText("Đang xử lý lịch học...");
        scheduleTable.getItems().clear();
        allScheduleItems.clear();
        
        Platform.runLater(() -> {
            try {
                JsonArray scheduleArray = JsonParser.parseString(scheduleJson).getAsJsonArray();
                parseAndDisplaySchedule(scheduleArray);
            } catch (Exception e) {
                statusLabel.setText("Lỗi khi parse dữ liệu");
                e.printStackTrace();
            }
        });
    }
    
    public void loadSchedule() {
        statusLabel.setText("Đang tải lịch học...");
        scheduleTable.getItems().clear();
        allScheduleItems.clear();
        
        new Thread(() -> {
            try {
                String response = apiService.getSchedule(authToken);
                JsonElement element = JsonParser.parseString(response);
                
                Platform.runLater(() -> {
                    try {
                        JsonArray scheduleArray = null;
                        if (element.isJsonArray()) {
                            scheduleArray = element.getAsJsonArray();
                        } else if (element.isJsonObject()) {
                            JsonObject obj = element.getAsJsonObject();
                            if (obj.has("data") && obj.getAsJsonObject("data").has("student_schedule")) {
                                scheduleArray = obj.getAsJsonObject("data").getAsJsonArray("student_schedule");
                            } else if (obj.has("schedule") && obj.get("schedule").isJsonArray()) {
                                scheduleArray = obj.getAsJsonArray("schedule");
                            }
                        }
                        
                        if (scheduleArray != null) {
                            parseAndDisplaySchedule(scheduleArray);
                        } else {
                            statusLabel.setText("Không tìm thấy dữ liệu lịch học");
                        }
                    } catch (Exception e) {
                        statusLabel.setText("Lỗi khi parse dữ liệu");
                        e.printStackTrace();
                    }
                });
                
            } catch (IOException e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Lỗi khi tải lịch học");
                    showAlert(Alert.AlertType.ERROR, "Lỗi", 
                            "Không thể tải lịch học: " + e.getMessage());
                });
            }
        }).start();
    }
    
    private void parseAndDisplaySchedule(JsonArray scheduleArray) {
        allScheduleItems.clear();
        
        for (JsonElement item : scheduleArray) {
            JsonObject scheduleObj = item.getAsJsonObject();
            
            String studyDays = getStringValue(scheduleObj, "study_days");
            String lessons = getStringValue(scheduleObj, "lessons");
            String courseName = getStringValue(scheduleObj, "course_name");
            String courseCode = getStringValue(scheduleObj, "course_code");
            String teacher = getStringValue(scheduleObj, "teacher");
            String studyLocation = getStringValue(scheduleObj, "study_location");
            
            // Parse study_days và lessons
            String[] days = studyDays.split("\\s+");
            String[] lessonArray = lessons.split("\\s+");
            
            // Tạo ScheduleItem cho mỗi ngày
            for (int i = 0; i < days.length && i < lessonArray.length; i++) {
                try {
                    LocalDate date = parseDate(days[i]);
                    String lessonStr = lessonArray[i];
                    
                    // Map lessons thành thời gian
                    String[] timeRange = mapLessonsToTime(lessonStr);
                    
                    ScheduleItem scheduleItem = new ScheduleItem();
                    scheduleItem.setDate(date);
                    scheduleItem.setStartTime(timeRange[0]);
                    scheduleItem.setEndTime(timeRange[1]);
                    scheduleItem.setTime(timeRange[0] + " - " + timeRange[1]);
                    scheduleItem.setSubject(courseName);
                    scheduleItem.setCourseCode(courseCode);
                    scheduleItem.setRoom(studyLocation);
                    scheduleItem.setTeacher(teacher);
                    scheduleItem.setDay(formatDate(date));
                    
                    allScheduleItems.add(scheduleItem);
                    scheduleTable.getItems().add(scheduleItem);
                } catch (Exception e) {
                    System.err.println("Error parsing date: " + days[i] + " - " + e.getMessage());
                }
            }
        }
        
        // Sắp xếp theo thời gian tăng dần (ngày và giờ)
        allScheduleItems.sort((a, b) -> {
            // So sánh theo ngày trước
            int dateCompare = a.getDate().compareTo(b.getDate());
            if (dateCompare != 0) {
                return dateCompare;
            }
            // Nếu cùng ngày, so sánh theo giờ bắt đầu
            if (a.getStartTime() != null && b.getStartTime() != null) {
                return a.getStartTime().compareTo(b.getStartTime());
            }
            return 0;
        });
        
        // Cập nhật table với dữ liệu đã sắp xếp
        scheduleTable.getItems().clear();
        scheduleTable.getItems().addAll(allScheduleItems);
        
        statusLabel.setText("Đã tải " + allScheduleItems.size() + " buổi học");
        
        // Cập nhật filter options
        updateFilterOptions();
        
        // Apply filter (mặc định hiển thị tất cả)
        // applyFilter() sẽ tự động scroll đến ngày hôm nay hoặc ngày gần nhất nếu là lần đầu load
        applyFilter();
    }
    
    private void updateSubjectInfo() {
        if (subjectInfoContainer == null) return;
        
        subjectInfoContainer.getChildren().clear();
        
        // Sử dụng filteredScheduleItems nếu có, nếu không thì dùng allScheduleItems
        List<ScheduleItem> itemsToAnalyze = filteredScheduleItems.isEmpty() ? 
            allScheduleItems : filteredScheduleItems;
        
        if (itemsToAnalyze.isEmpty()) {
            Label emptyLabel = new Label("Chưa có thông tin môn học");
            emptyLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
            subjectInfoContainer.getChildren().add(emptyLabel);
            return;
        }
        
        // Nhóm theo môn học (courseCode + courseName)
        Map<String, SubjectStats> subjectStatsMap = new HashMap<>();
        
        for (ScheduleItem item : itemsToAnalyze) {
            String key = item.getCourseCode() + "|" + item.getSubject();
            SubjectStats stats = subjectStatsMap.computeIfAbsent(key, k -> new SubjectStats());
            stats.courseCode = item.getCourseCode();
            stats.courseName = item.getSubject();
            stats.sessionCount++;
            if (stats.teacher == null || stats.teacher.isEmpty()) {
                stats.teacher = item.getTeacher();
            }
            if (stats.room == null || stats.room.isEmpty()) {
                stats.room = item.getRoom();
            }
        }
        
        // Hiển thị thông tin từng môn học
        subjectStatsMap.values().stream()
            .sorted((a, b) -> a.courseName.compareToIgnoreCase(b.courseName))
            .forEach(stats -> {
                VBox subjectBox = createSubjectInfoBox(stats);
                subjectInfoContainer.getChildren().add(subjectBox);
            });
    }
    
    private VBox createSubjectInfoBox(SubjectStats stats) {
        VBox subjectBox = new VBox(10);
        subjectBox.setPadding(new Insets(15));
        subjectBox.setStyle("-fx-background-color: linear-gradient(to bottom, #ffffff, #f8f9fa); -fx-border-color: #d0d0d0; -fx-border-radius: 8; -fx-border-width: 1; -fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.05), 3, 0, 0, 1);");
        
        // Tên môn học
        HBox titleBox = new HBox(8);
        Label subjectLabel = new Label("📚 " + stats.courseName);
        subjectLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #2c3e50;");
        titleBox.getChildren().add(subjectLabel);
        subjectBox.getChildren().add(titleBox);
        
        // Mã môn học
        if (stats.courseCode != null && !stats.courseCode.isEmpty()) {
            Label codeLabel = new Label("🔢 Mã môn: " + stats.courseCode);
            codeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #555555;");
            subjectBox.getChildren().add(codeLabel);
        }
        
        // Số buổi học
        Label sessionLabel = new Label("📊 Số buổi học: " + stats.sessionCount);
        sessionLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #2980b9; -fx-font-weight: bold; -fx-background-color: #e8f4f8; -fx-padding: 5 10 5 10; -fx-background-radius: 5;");
        subjectBox.getChildren().add(sessionLabel);
        
        // Giảng viên
        if (stats.teacher != null && !stats.teacher.isEmpty()) {
            Label teacherLabel = new Label("👤 Giảng viên: " + stats.teacher);
            teacherLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #555555;");
            subjectBox.getChildren().add(teacherLabel);
        }
        
        // Phòng học
        if (stats.room != null && !stats.room.isEmpty()) {
            Label roomLabel = new Label("📍 Phòng học: " + stats.room);
            roomLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #555555;");
            subjectBox.getChildren().add(roomLabel);
        }
        
        return subjectBox;
    }
    
    private static class SubjectStats {
        String courseCode = "";
        String courseName = "";
        int sessionCount = 0;
        String teacher = "";
        String room = "";
    }
    
    private void updateFilterOptions() {
        // Lấy danh sách các tháng có lịch học
        monthFilterComboBox.getItems().clear();
        monthFilterComboBox.getItems().add("Tất cả tháng");
        
        allScheduleItems.stream()
            .filter(item -> item.getDate() != null)
            .map(item -> item.getDate().format(DateTimeFormatter.ofPattern("MM/yyyy")))
            .distinct()
            .sorted()
            .forEach(month -> monthFilterComboBox.getItems().add(month));
    }
    
    @FXML
    private void handleMonthFilter() {
        applyFilter();
        saveFilterState();
    }
    
    @FXML
    private void handleDateFilter() {
        applyFilter();
        saveFilterState();
    }
    
    @FXML
    private void handleClearFilter() {
        monthFilterComboBox.getSelectionModel().select(0); // "Tất cả tháng"
        dateFilterPicker.setValue(null);
        applyFilter();
        saveFilterState();
    }
    
    private void saveFilterState() {
        try {
            org.example.service.LocalStorageService storage = org.example.service.LocalStorageService.getInstance();
            String monthFilter = monthFilterComboBox.getSelectionModel().getSelectedItem();
            String dateFilter = dateFilterPicker.getValue() != null ? 
                dateFilterPicker.getValue().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : null;
            storage.saveFilterState(monthFilter, dateFilter);
        } catch (IOException e) {
            System.err.println("Error saving filter state: " + e.getMessage());
        }
    }
    
    private void loadFilterState() {
        try {
            org.example.service.LocalStorageService storage = org.example.service.LocalStorageService.getInstance();
            com.google.gson.JsonObject filterState = storage.loadFilterState();
            if (filterState != null) {
                // Restore month filter
                if (filterState.has("monthFilter")) {
                    String monthFilter = filterState.get("monthFilter").getAsString();
                    int index = monthFilterComboBox.getItems().indexOf(monthFilter);
                    if (index >= 0) {
                        monthFilterComboBox.getSelectionModel().select(index);
                    }
                }
                
                // Restore date filter
                if (filterState.has("dateFilter")) {
                    String dateStr = filterState.get("dateFilter").getAsString();
                    try {
                        LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                        dateFilterPicker.setValue(date);
                    } catch (Exception e) {
                        System.err.println("Error parsing saved date: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading filter state: " + e.getMessage());
        }
    }
    
    private void applyFilter() {
        filteredScheduleItems.clear();
        
        String selectedMonth = monthFilterComboBox.getSelectionModel().getSelectedItem();
        LocalDate selectedDate = dateFilterPicker.getValue();
        
        for (ScheduleItem item : allScheduleItems) {
            if (item.getDate() == null) continue;
            
            // Filter by month
            boolean monthMatch = selectedMonth == null || 
                                "Tất cả tháng".equals(selectedMonth) ||
                                item.getDate().format(DateTimeFormatter.ofPattern("MM/yyyy")).equals(selectedMonth);
            
            // Filter by date
            boolean dateMatch = selectedDate == null || item.getDate().equals(selectedDate);
            
            if (monthMatch && dateMatch) {
                filteredScheduleItems.add(item);
            }
        }
        
        // Cập nhật table
        scheduleTable.getItems().clear();
        scheduleTable.getItems().addAll(filteredScheduleItems);
        
        // Hiển thị message nếu không có lịch khi filter theo ngày
        if (selectedDate != null && filteredScheduleItems.isEmpty()) {
            statusLabel.setText("Không có ca học nào cho ngày " + formatDate(selectedDate));
        } else {
            statusLabel.setText("Đã tải " + filteredScheduleItems.size() + " buổi học");
        }
        
        // Cập nhật calendar view
        displayCalendarView();
        
        // Cập nhật thông tin chi tiết môn học
        updateSubjectInfo();
        
        // Xử lý scroll
        if (isInitialLoad) {
            // Lần đầu load: scroll đến ngày hôm nay hoặc ngày gần nhất
            Platform.runLater(() -> {
                scrollToTodayOrNearest();
            });
            isInitialLoad = false;
        } else if (selectedDate != null) {
            // Filter theo ngày: scroll đến đầu tháng chứa ngày đó
            Platform.runLater(() -> {
                scrollToMonth(selectedDate);
            });
        } else if (selectedMonth != null && !"Tất cả tháng".equals(selectedMonth)) {
            // Filter theo tháng: scroll đến đầu tháng đó
            try {
                String[] parts = selectedMonth.split("/");
                if (parts.length == 2) {
                    int month = Integer.parseInt(parts[0]);
                    int year = Integer.parseInt(parts[1]);
                    LocalDate monthStart = LocalDate.of(year, month, 1);
                    Platform.runLater(() -> {
                        scrollToMonth(monthStart);
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    scrollToTop();
                });
            }
        }
    }
    
    private void scrollToTodayOrNearest() {
        LocalDate today = LocalDate.now();
        
        // Tìm ngày hôm nay trước
        boolean hasToday = allScheduleItems.stream()
            .anyMatch(item -> item.getDate() != null && item.getDate().equals(today));
        
        LocalDate targetDate;
        if (hasToday) {
            // Nếu có lịch hôm nay, scroll đến đầu tháng chứa hôm nay
            targetDate = today;
        } else {
            // Nếu không có lịch hôm nay, tìm ngày gần nhất từ hôm nay trở đi
            targetDate = allScheduleItems.stream()
                .filter(item -> item.getDate() != null && !item.getDate().isBefore(today))
                .map(ScheduleItem::getDate)
                .min(LocalDate::compareTo)
                .orElse(null);
        }
        
        if (targetDate != null) {
            // Scroll đến đầu tháng chứa ngày này
            scrollToMonth(targetDate);
        }
    }
    
    private void scrollToTop() {
        if (calendarScrollPane == null) return;
        
        Platform.runLater(() -> {
            Platform.runLater(() -> {
                calendarScrollPane.setVvalue(0.0);
            });
        });
    }
    
    private void scrollToMonth(LocalDate targetDate) {
        if (calendarScrollPane == null || calendarContainer == null) return;
        
        // Lấy tháng từ targetDate
        String monthKey = targetDate.format(DateTimeFormatter.ofPattern("MM/yyyy"));
        VBox targetMonthBox = monthBoxMap.get(monthKey);
        
        if (targetMonthBox == null) return;
        
        // Đợi layout được tính toán xong
        Platform.runLater(() -> {
            Platform.runLater(() -> {
                double targetY = 0;
                boolean found = false;
                
                // Tìm vị trí của month box trong container
                for (javafx.scene.Node node : calendarContainer.getChildren()) {
                    if (node == targetMonthBox) {
                        javafx.geometry.Bounds bounds = node.getBoundsInParent();
                        targetY = bounds.getMinY();
                        found = true;
                        break;
                    }
                }
                
                if (found) {
                    double containerHeight = calendarContainer.getBoundsInLocal().getHeight();
                    double viewportHeight = calendarScrollPane.getViewportBounds().getHeight();
                    double maxScroll = Math.max(0, containerHeight - viewportHeight);
                    
                    if (maxScroll > 0) {
                        // Scroll để month box nằm ở đầu viewport (có thể thấy ngay)
                        double scrollValue = Math.min(1.0, Math.max(0.0, targetY / containerHeight));
                        calendarScrollPane.setVvalue(scrollValue);
                    }
                }
            });
        });
    }
    
    private String[] mapLessonsToTime(String lessonStr) {
        String startTime = "00:00";
        String endTime = "00:00";
        
        switch (lessonStr) {
            case "1,2,3":
                startTime = "07:00";
                endTime = "09:25";
                break;
            case "4,5,6":
                startTime = "09:35";
                endTime = "12:00";
                break;
            case "7,8,9":
                startTime = "12:30";
                endTime = "14:55";
                break;
            case "10,11,12":
                startTime = "15:05";
                endTime = "17:30";
                break;
            case "13,14,15,16":
                startTime = "18:00";
                endTime = "20:30";
                break;
        }
        
        return new String[]{startTime, endTime};
    }
    
    private LocalDate parseDate(String dateStr) throws DateTimeParseException {
        // Format: dd/MM/yyyy
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return LocalDate.parse(dateStr, formatter);
    }
    
    private String formatDate(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return date.format(formatter);
    }
    
    private void displayCalendarView() {
        if (calendarContainer == null) return;
        
        calendarContainer.getChildren().clear();
        monthBoxMap.clear();
        dayBoxMap.clear();
        
        // Sử dụng filteredScheduleItems thay vì allScheduleItems
        List<ScheduleItem> itemsToDisplay = filteredScheduleItems.isEmpty() ? 
            allScheduleItems : filteredScheduleItems;
        
        // Nếu filter theo ngày và không có lịch, hiển thị message
        LocalDate selectedDate = dateFilterPicker.getValue();
        if (selectedDate != null && itemsToDisplay.isEmpty()) {
            VBox messageBox = new VBox();
            messageBox.setAlignment(javafx.geometry.Pos.CENTER);
            messageBox.setPadding(new Insets(50));
            Label messageLabel = new Label("Không có ca học nào cho ngày " + formatDate(selectedDate));
            messageLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #7f8c8d;");
            messageBox.getChildren().add(messageLabel);
            calendarContainer.getChildren().add(messageBox);
            return;
        }
        
        // Group by month
        Map<String, List<ScheduleItem>> monthlySchedule = new HashMap<>();
        for (ScheduleItem item : itemsToDisplay) {
            if (item.getDate() != null) {
                String monthKey = item.getDate().format(DateTimeFormatter.ofPattern("MM/yyyy"));
                monthlySchedule.computeIfAbsent(monthKey, k -> new ArrayList<>()).add(item);
            }
        }
        
        // Display each month
        for (Map.Entry<String, List<ScheduleItem>> entry : monthlySchedule.entrySet()) {
            VBox monthBox = createMonthView(entry.getKey(), entry.getValue());
            calendarContainer.getChildren().add(monthBox);
            monthBoxMap.put(entry.getKey(), monthBox);
        }
    }
    
    private VBox createMonthView(String monthKey, List<ScheduleItem> items) {
        VBox monthBox = new VBox(15);
        monthBox.setPadding(new Insets(25));
        monthBox.setStyle("-fx-background-color: linear-gradient(to bottom, #ffffff, #f8f9fa); -fx-border-color: #3498db; -fx-border-radius: 12; -fx-border-width: 2; -fx-effect: dropshadow(three-pass-box, rgba(52,152,219,0.2), 10, 0, 0, 3);");
        
        // Header với tháng và năm
        HBox headerBox = new HBox();
        headerBox.setAlignment(javafx.geometry.Pos.CENTER);
        headerBox.setPadding(new Insets(0, 0, 10, 0));
        Label monthLabel = new Label("📅 Tháng " + monthKey);
        monthLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2980b9; -fx-padding: 12px; -fx-background-color: linear-gradient(to bottom, #e8f4f8, #d6eaf8); -fx-background-radius: 8;");
        headerBox.getChildren().add(monthLabel);
        monthBox.getChildren().add(headerBox);
        
        // Separator
        Separator separator = new Separator();
        separator.setStyle("-fx-background-color: linear-gradient(to right, transparent, #3498db, transparent); -fx-pref-height: 2;");
        monthBox.getChildren().add(separator);
        
        // Group by date
        Map<LocalDate, List<ScheduleItem>> dailySchedule = new HashMap<>();
        for (ScheduleItem item : items) {
            dailySchedule.computeIfAbsent(item.getDate(), k -> new ArrayList<>()).add(item);
        }
        
        // Display each day
        dailySchedule.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                VBox dayBox = createDayView(entry.getKey(), entry.getValue());
                monthBox.getChildren().add(dayBox);
                // Lưu dayBox vào map để scroll đến đúng ngày
                dayBoxMap.put(entry.getKey(), dayBox);
            });
        
        return monthBox;
    }
    
    private VBox createDayView(LocalDate date, List<ScheduleItem> items) {
        VBox dayBox = new VBox(10);
        dayBox.setPadding(new Insets(15));
        dayBox.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e0e0e0; -fx-border-radius: 8; -fx-border-width: 1; -fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.05), 3, 0, 0, 1);");
        
        // Header với ngày và số lượng buổi học
        HBox headerBox = new HBox(12);
        headerBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label dateLabel = new Label("📆 " + formatDate(date));
        dateLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 17px; -fx-text-fill: #2c3e50;");
        Label countLabel = new Label("(" + items.size() + " buổi)");
        countLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #7f8c8d; -fx-background-color: #f0f0f0; -fx-padding: 4 10 4 10; -fx-background-radius: 12;");
        headerBox.getChildren().addAll(dateLabel, countLabel);
        dayBox.getChildren().add(headerBox);
        
        // Sắp xếp items trong ngày theo thời gian
        items.sort((a, b) -> {
            if (a.getStartTime() != null && b.getStartTime() != null) {
                return a.getStartTime().compareTo(b.getStartTime());
            }
            return 0;
        });
        
        // Hiển thị từng buổi học
        for (ScheduleItem item : items) {
            VBox sessionBox = new VBox(8);
            sessionBox.setPadding(new Insets(12));
            sessionBox.setStyle("-fx-background-color: linear-gradient(to bottom, #f8f9fa, #ffffff); -fx-border-color: #d0d0d0; -fx-border-radius: 6; -fx-border-width: 1; -fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.03), 2, 0, 0, 1);");
            
            // Thời gian
            HBox timeBox = new HBox(8);
            timeBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            Label timeIcon = new Label("🕐");
            Label timeLabel = new Label(item.getTimeRange());
            timeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2980b9; -fx-background-color: #e8f4f8; -fx-padding: 5 12 5 12; -fx-background-radius: 6;");
            timeBox.getChildren().addAll(timeIcon, timeLabel);
            
            // Môn học
            Label subjectLabel = new Label("📚 " + item.getSubject());
            subjectLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #2c3e50; -fx-padding: 4 0 4 0;");
            
            // Thông tin bổ sung
            HBox infoBox = new HBox(20);
            infoBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            Label roomLabel = new Label("📍 " + item.getRoom());
            roomLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #555555;");
            Label teacherLabel = new Label("👤 " + item.getTeacher());
            teacherLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #555555;");
            infoBox.getChildren().addAll(roomLabel, teacherLabel);
            
            sessionBox.getChildren().addAll(timeBox, subjectLabel, infoBox);
            dayBox.getChildren().add(sessionBox);
        }
        
        return dayBox;
    }
    
    private String getStringValue(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return "";
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
    
    @FXML
    private void handleExportICS() {
        if (allScheduleItems.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", 
                    "Không có dữ liệu lịch học để xuất.");
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Lưu file lịch học");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("iCalendar Files", "*.ics")
        );
        fileChooser.setInitialFileName("lich_hoc.ics");
        
        Stage stage = (Stage) exportIcsButton.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);
        
        if (file != null) {
            try {
                String studentName = studentInfo != null ? studentInfo.getDisplayName() : "Sinh viên";
                IcsExportService.exportToIcs(allScheduleItems, studentName, file);
                showAlert(Alert.AlertType.INFORMATION, "Thành công", 
                        "Đã xuất lịch học thành công!\nFile: " + file.getAbsolutePath());
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", 
                        "Không thể xuất file: " + e.getMessage());
                e.printStackTrace();
            }
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

