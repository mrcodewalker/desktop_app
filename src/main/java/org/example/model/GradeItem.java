package org.example.model;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class GradeItem {
    private final StringProperty subject;
    private final SimpleIntegerProperty credit;
    private final SimpleDoubleProperty midterm;
    private final SimpleDoubleProperty finalScore;
    private final SimpleDoubleProperty average;
    private final StringProperty letterGrade;
    
    public GradeItem() {
        this.subject = new SimpleStringProperty();
        this.credit = new SimpleIntegerProperty();
        this.midterm = new SimpleDoubleProperty();
        this.finalScore = new SimpleDoubleProperty();
        this.average = new SimpleDoubleProperty();
        this.letterGrade = new SimpleStringProperty();
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
    
    public int getCredit() {
        return credit.get();
    }
    
    public void setCredit(int credit) {
        this.credit.set(credit);
    }
    
    public SimpleIntegerProperty creditProperty() {
        return credit;
    }
    
    public double getMidterm() {
        return midterm.get();
    }
    
    public void setMidterm(double midterm) {
        this.midterm.set(midterm);
    }
    
    public SimpleDoubleProperty midtermProperty() {
        return midterm;
    }
    
    public double getFinal() {
        return finalScore.get();
    }
    
    public void setFinal(double finalScore) {
        this.finalScore.set(finalScore);
    }
    
    public SimpleDoubleProperty finalProperty() {
        return finalScore;
    }
    
    public double getAverage() {
        return average.get();
    }
    
    public void setAverage(double average) {
        this.average.set(average);
    }
    
    public SimpleDoubleProperty averageProperty() {
        return average;
    }
    
    public String getLetterGrade() {
        return letterGrade.get();
    }
    
    public void setLetterGrade(String letterGrade) {
        this.letterGrade.set(letterGrade);
    }
    
    public StringProperty letterGradeProperty() {
        return letterGrade;
    }
}

