package org.example.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Model cho môn học ảo (virtual course)
 */
public class VirtualCourse {
    private String course; // AT22, AT21, AT20...
    private String courseCode;
    private String courseName; // Tên đầy đủ có mã lớp
    private String displayCourseName; // Tên môn học từ ngoài (không có mã lớp, dùng để filter)
    private String baseTime;
    private String teacher;
    private String studyLocation;
    private String studyDays; // Chuỗi ngày học
    private String lessons; // Chuỗi tiết học
    private List<ScheduleSlot> scheduleSlots; // Danh sách các slot lịch học đã parse
    private boolean slotsParsed = false; // Flag để lazy loading
    
    public VirtualCourse() {
        this.scheduleSlots = new ArrayList<>();
    }
    
    public String getCourse() {
        return course;
    }
    
    public void setCourse(String course) {
        this.course = course;
    }
    
    public String getCourseCode() {
        return courseCode;
    }
    
    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }
    
    public String getCourseName() {
        return courseName;
    }
    
    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }
    
    public String getDisplayCourseName() {
        return displayCourseName;
    }
    
    public void setDisplayCourseName(String displayCourseName) {
        this.displayCourseName = displayCourseName;
    }
    
    /**
     * Trích xuất số lớp từ course_name
     * Ví dụ: "Triết học Mác - Lênin-1-25 (A22C10D904)" -> "L04"
     * Lấy 2 số cuối cùng trong phần (A22C10D904)
     */
    public String getClassNumber() {
        if (courseName == null || courseName.isEmpty()) {
            return "";
        }
        
        // Tìm phần trong ngoặc đơn cuối cùng
        int lastOpenParen = courseName.lastIndexOf('(');
        int lastCloseParen = courseName.lastIndexOf(')');
        
        if (lastOpenParen == -1 || lastCloseParen == -1 || lastCloseParen <= lastOpenParen) {
            return "";
        }
        
        String classCode = courseName.substring(lastOpenParen + 1, lastCloseParen);
        
        // Lấy 2 số cuối cùng
        if (classCode.length() >= 2) {
            String lastTwoDigits = classCode.substring(classCode.length() - 2);
            // Kiểm tra xem có phải là số không
            try {
                Integer.parseInt(lastTwoDigits);
                return "L" + lastTwoDigits;
            } catch (NumberFormatException e) {
                // Nếu không phải số, thử tìm 2 số cuối trong chuỗi
                String digits = classCode.replaceAll("[^0-9]", "");
                if (digits.length() >= 2) {
                    return "L" + digits.substring(digits.length() - 2);
                }
            }
        }
        
        return "";
    }
    
    /**
     * Map lessons string thành thời gian cụ thể
     * Ví dụ: "1,2,3" -> "07:00 - 09:25"
     */
    public String mapLessonsToTimeRange(String lessonStr) {
        if (lessonStr == null || lessonStr.isEmpty()) {
            return "";
        }
        
        String[] timeRange = mapLessonsToTime(lessonStr);
        if (timeRange[0].equals("00:00") && timeRange[1].equals("00:00")) {
            return lessonStr; // Trả về nguyên lessons nếu không map được
        }
        return timeRange[0] + " - " + timeRange[1];
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
            case "1,2,3,4":
                startTime = "07:00";
                endTime = "09:35";
                break;
        }
        
        return new String[]{startTime, endTime};
    }
    
    public String getBaseTime() {
        return baseTime;
    }
    
    public void setBaseTime(String baseTime) {
        this.baseTime = baseTime;
    }
    
    public String getTeacher() {
        return teacher;
    }
    
    public void setTeacher(String teacher) {
        this.teacher = teacher;
    }
    
    public String getStudyLocation() {
        return studyLocation;
    }
    
    public void setStudyLocation(String studyLocation) {
        this.studyLocation = studyLocation;
    }
    
    public String getStudyDays() {
        return studyDays;
    }
    
    public void setStudyDays(String studyDays) {
        this.studyDays = studyDays;
    }
    
    public String getLessons() {
        return lessons;
    }
    
    public void setLessons(String lessons) {
        this.lessons = lessons;
    }
    
    public void setScheduleSlots(List<ScheduleSlot> scheduleSlots) {
        this.scheduleSlots = scheduleSlots;
        this.slotsParsed = true;
    }
    
    /**
     * Parse studyDays và lessons thành danh sách ScheduleSlot (lazy loading)
     */
    public void parseScheduleSlots() {
        if (slotsParsed) {
            return; // Đã parse rồi, không parse lại
        }
        
        scheduleSlots.clear();
        if (studyDays == null || lessons == null || studyDays.isEmpty() || lessons.isEmpty()) {
            slotsParsed = true;
            return;
        }
        
        String[] days = studyDays.split("\\s+");
        String[] lessonArray = lessons.split("\\s+");
        
        // Giới hạn số lượng slots để tránh memory overflow
        int maxSlots = Math.min(days.length, lessonArray.length);
        maxSlots = Math.min(maxSlots, 1000); // Giới hạn tối đa 1000 slots
        
        for (int i = 0; i < maxSlots; i++) {
            try {
                LocalDate date = parseDate(days[i]);
                String lessonStr = lessonArray[i];
                
                ScheduleSlot slot = new ScheduleSlot();
                slot.setDate(date);
                slot.setLessons(lessonStr);
                slot.setVirtualCourse(this);
                
                scheduleSlots.add(slot);
            } catch (Exception e) {
                System.err.println("Error parsing date: " + days[i] + " - " + e.getMessage());
            }
        }
        
        slotsParsed = true;
    }
    
    /**
     * Lấy schedule slots (parse nếu chưa parse)
     */
    public List<ScheduleSlot> getScheduleSlots() {
        if (!slotsParsed) {
            parseScheduleSlots();
        }
        return scheduleSlots;
    }
    
    private LocalDate parseDate(String dateStr) {
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return LocalDate.parse(dateStr, formatter);
    }
    
    /**
     * Inner class để lưu thông tin slot lịch học
     */
    public static class ScheduleSlot {
        private LocalDate date;
        private String lessons; // Ví dụ: "1,2,3" hoặc "9,10,11,12"
        private VirtualCourse virtualCourse;
        
        public LocalDate getDate() {
            return date;
        }
        
        public void setDate(LocalDate date) {
            this.date = date;
        }
        
        public String getLessons() {
            return lessons;
        }
        
        public void setLessons(String lessons) {
            this.lessons = lessons;
        }
        
        public VirtualCourse getVirtualCourse() {
            return virtualCourse;
        }
        
        public void setVirtualCourse(VirtualCourse virtualCourse) {
            this.virtualCourse = virtualCourse;
        }
        
        /**
         * Kiểm tra xem slot này có trùng với slot khác không
         * Trùng khi: cùng ngày và có tiết học chung
         */
        public boolean conflictsWith(ScheduleSlot other) {
            if (!this.date.equals(other.date)) {
                return false;
            }
            
            // Parse lessons thành danh sách số tiết
            List<Integer> thisLessons = parseLessons(this.lessons);
            List<Integer> otherLessons = parseLessons(other.lessons);
            
            // Kiểm tra có tiết nào trùng không
            for (Integer lesson : thisLessons) {
                if (otherLessons.contains(lesson)) {
                    return true;
                }
            }
            
            return false;
        }
        
        private List<Integer> parseLessons(String lessonStr) {
            List<Integer> result = new ArrayList<>();
            if (lessonStr == null || lessonStr.isEmpty()) {
                return result;
            }
            
            String[] parts = lessonStr.split(",");
            for (String part : parts) {
                try {
                    result.add(Integer.parseInt(part.trim()));
                } catch (NumberFormatException e) {
                    // Ignore invalid numbers
                }
            }
            return result;
        }
    }
}

