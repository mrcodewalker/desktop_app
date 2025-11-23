package org.example.model;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * Model class cho danh sách học bổng
 */
public class ScholarshipItem {
    private final SimpleIntegerProperty rank;
    private final SimpleStringProperty studentCode;
    private final SimpleStringProperty studentName;
    private final SimpleStringProperty studentClass;
    private final SimpleDoubleProperty gpa;
    private final SimpleStringProperty scholarshipType;
    private final SimpleStringProperty classification;
    
    public ScholarshipItem() {
        this.rank = new SimpleIntegerProperty();
        this.studentCode = new SimpleStringProperty();
        this.studentName = new SimpleStringProperty();
        this.studentClass = new SimpleStringProperty();
        this.gpa = new SimpleDoubleProperty();
        this.scholarshipType = new SimpleStringProperty();
        this.classification = new SimpleStringProperty();
    }
    
    /**
     * Tính xếp loại dựa trên GPA
     * >= 3.6: Xuất sắc
     * >= 3.2: Giỏi
     * < 3.2: Khá
     */
    public void calculateClassification() {
        double gpaValue = getGpa();
        if (gpaValue >= 3.6) {
            setClassification("Xuất sắc");
        } else if (gpaValue >= 3.2) {
            setClassification("Giỏi");
        } else {
            setClassification("Khá");
        }
    }
    
    public int getRank() {
        return rank.get();
    }
    
    public void setRank(int rank) {
        this.rank.set(rank);
    }
    
    public SimpleIntegerProperty rankProperty() {
        return rank;
    }
    
    public String getStudentCode() {
        return studentCode.get();
    }
    
    public void setStudentCode(String studentCode) {
        this.studentCode.set(studentCode);
    }
    
    public SimpleStringProperty studentCodeProperty() {
        return studentCode;
    }
    
    public String getStudentName() {
        return studentName.get();
    }
    
    public void setStudentName(String studentName) {
        this.studentName.set(studentName);
    }
    
    public SimpleStringProperty studentNameProperty() {
        return studentName;
    }
    
    public String getStudentClass() {
        return studentClass.get();
    }
    
    public void setStudentClass(String studentClass) {
        this.studentClass.set(studentClass);
    }
    
    public SimpleStringProperty studentClassProperty() {
        return studentClass;
    }
    
    public double getGpa() {
        return gpa.get();
    }
    
    public void setGpa(double gpa) {
        this.gpa.set(gpa);
    }
    
    public SimpleDoubleProperty gpaProperty() {
        return gpa;
    }
    
    public String getScholarshipType() {
        return scholarshipType.get();
    }
    
    public void setScholarshipType(String scholarshipType) {
        this.scholarshipType.set(scholarshipType);
    }
    
    public SimpleStringProperty scholarshipTypeProperty() {
        return scholarshipType;
    }
    
    public String getClassification() {
        return classification.get();
    }
    
    public void setClassification(String classification) {
        this.classification.set(classification);
    }
    
    public SimpleStringProperty classificationProperty() {
        return classification;
    }
}

