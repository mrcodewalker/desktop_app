package org.example.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class StudentInfo {
    private final StringProperty studentCode;
    private final StringProperty displayName;
    private final StringProperty birthday;
    private final StringProperty gender;
    
    public StudentInfo() {
        this.studentCode = new SimpleStringProperty();
        this.displayName = new SimpleStringProperty();
        this.birthday = new SimpleStringProperty();
        this.gender = new SimpleStringProperty();
    }
    
    public String getStudentCode() {
        return studentCode.get();
    }
    
    public void setStudentCode(String studentCode) {
        this.studentCode.set(studentCode);
    }
    
    public StringProperty studentCodeProperty() {
        return studentCode;
    }
    
    public String getDisplayName() {
        return displayName.get();
    }
    
    public void setDisplayName(String displayName) {
        this.displayName.set(displayName);
    }
    
    public StringProperty displayNameProperty() {
        return displayName;
    }
    
    public String getBirthday() {
        return birthday.get();
    }
    
    public void setBirthday(String birthday) {
        this.birthday.set(birthday);
    }
    
    public StringProperty birthdayProperty() {
        return birthday;
    }
    
    public String getGender() {
        return gender.get();
    }
    
    public void setGender(String gender) {
        this.gender.set(gender);
    }
    
    public StringProperty genderProperty() {
        return gender;
    }
}

