package org.example.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import java.time.LocalDate;

public class ScheduleItem {
    private final StringProperty day;
    private final StringProperty time;
    private final StringProperty subject;
    private final StringProperty room;
    private final StringProperty teacher;
    private LocalDate date;
    private String startTime;
    private String endTime;
    private String courseCode;
    
    public ScheduleItem() {
        this.day = new SimpleStringProperty();
        this.time = new SimpleStringProperty();
        this.subject = new SimpleStringProperty();
        this.room = new SimpleStringProperty();
        this.teacher = new SimpleStringProperty();
    }
    
    public String getDay() {
        return day.get();
    }
    
    public void setDay(String day) {
        this.day.set(day);
    }
    
    public StringProperty dayProperty() {
        return day;
    }
    
    public String getTime() {
        return time.get();
    }
    
    public void setTime(String time) {
        this.time.set(time);
    }
    
    public StringProperty timeProperty() {
        return time;
    }
    
    public String getSubject() {
        return subject.get();
    }
    
    public void setSubject(String subject) {
        this.subject.set(subject);
    }
    
    public StringProperty subjectProperty() {
        return subject;
    }
    
    public String getRoom() {
        return room.get();
    }
    
    public void setRoom(String room) {
        this.room.set(room);
    }
    
    public StringProperty roomProperty() {
        return room;
    }
    
    public String getTeacher() {
        return teacher.get();
    }
    
    public void setTeacher(String teacher) {
        this.teacher.set(teacher);
    }
    
    public StringProperty teacherProperty() {
        return teacher;
    }
    
    public LocalDate getDate() {
        return date;
    }
    
    public void setDate(LocalDate date) {
        this.date = date;
    }
    
    public String getStartTime() {
        return startTime;
    }
    
    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }
    
    public String getEndTime() {
        return endTime;
    }
    
    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }
    
    public String getCourseCode() {
        return courseCode;
    }
    
    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }
    
    public String getTimeRange() {
        if (startTime != null && endTime != null) {
            return startTime + " - " + endTime;
        }
        return time.get();
    }
}

