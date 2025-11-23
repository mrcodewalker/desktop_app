package org.example.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Model class cho điểm thi ảo (có checkbox để select)
 */
public class VirtualScoreItem {
    private final StringProperty subjectName;
    private final SimpleIntegerProperty subjectCredit;
    private final SimpleDoubleProperty scoreFirst;
    private final SimpleDoubleProperty scoreSecond;
    private final SimpleDoubleProperty scoreFinal;
    private final SimpleDoubleProperty scoreOverall;
    private final StringProperty scoreText;
    private final BooleanProperty isSelected;
    private Long itemId; // ID từ server (null nếu là môn mới thêm)
    private boolean isNewItem; // Đánh dấu môn học mới thêm
    
    public VirtualScoreItem() {
        this.subjectName = new SimpleStringProperty();
        this.subjectCredit = new SimpleIntegerProperty();
        this.scoreFirst = new SimpleDoubleProperty();
        this.scoreSecond = new SimpleDoubleProperty();
        this.scoreFinal = new SimpleDoubleProperty();
        this.scoreOverall = new SimpleDoubleProperty();
        this.scoreText = new SimpleStringProperty();
        this.isSelected = new SimpleBooleanProperty(false);
        this.itemId = null;
        this.isNewItem = false;
    }
    
    /**
     * Tính điểm tổng kết từ điểm thành phần 1, 2 và điểm cuối kỳ
     * Công thức: (TP1 x 0.7 + TP2 x 0.3) x 0.3 + Cuối kỳ x 0.7
     * Làm tròn 2 số sau dấu phẩy
     */
    public void calculateOverallScore() {
        double tp1 = getScoreFirst();
        double tp2 = getScoreSecond();
        double finalScore = getScoreFinal();
        
        // Tính điểm thành phần: (TP1 x 0.7 + TP2 x 0.3)
        double componentScore = (tp1 * 0.7) + (tp2 * 0.3);
        
        // Tính điểm tổng kết: (Điểm thành phần x 0.3) + (Cuối kỳ x 0.7)
        double overall = (componentScore * 0.3) + (finalScore * 0.7);
        
        // Làm tròn 2 số sau dấu phẩy
        overall = Math.round(overall * 100.0) / 100.0;
        
        setScoreOverall(overall);
        calculateScoreText(overall);
    }
    
    /**
     * Tính điểm chữ từ điểm tổng kết
     */
    private void calculateScoreText(double overall) {
        if (overall >= 9.0) {
            setScoreText("A+");
        } else if (overall >= 8.5) {
            setScoreText("A");
        } else if (overall >= 7.8) {
            setScoreText("B+");
        } else if (overall >= 7.0) {
            setScoreText("B");
        } else if (overall >= 6.3) {
            setScoreText("C+");
        } else if (overall >= 5.5) {
            setScoreText("C");
        } else if (overall >= 4.8) {
            setScoreText("D+");
        } else if (overall >= 4.0) {
            setScoreText("D");
        } else {
            setScoreText("F");
        }
    }
    
    /**
     * Kiểm tra có phải môn Giáo dục thể chất không
     */
    public boolean isPhysicalEducation() {
        String name = getSubjectName();
        if (name == null) return false;
        return name.toLowerCase().contains("giáo dục thể chất") || 
               name.toLowerCase().contains("gdtt");
    }
    
    /**
     * Kiểm tra môn học có trượt không
     * Trượt nếu: điểm cuối kỳ < 2 HOẶC (điểm cuối kỳ >= 2 nhưng tổng điểm < 4)
     */
    public boolean checkFailed() {
        double finalScore = getScoreFinal();
        double overallScore = getScoreOverall();
        return finalScore < 2.0 || (finalScore >= 2.0 && overallScore < 4.0);
    }
    
    /**
     * Tính điểm chữ nếu chưa có
     */
    public void ensureScoreText() {
        if (getScoreText() == null || getScoreText().isEmpty()) {
            calculateScoreText(getScoreOverall());
        }
    }
    
    public String getSubjectName() {
        return subjectName.get();
    }
    
    public void setSubjectName(String subjectName) {
        this.subjectName.set(subjectName);
    }
    
    public StringProperty subjectNameProperty() {
        return subjectName;
    }
    
    public int getSubjectCredit() {
        return subjectCredit.get();
    }
    
    public void setSubjectCredit(int subjectCredit) {
        this.subjectCredit.set(subjectCredit);
    }
    
    public SimpleIntegerProperty subjectCreditProperty() {
        return subjectCredit;
    }
    
    public double getScoreFirst() {
        return scoreFirst.get();
    }
    
    public void setScoreFirst(double scoreFirst) {
        this.scoreFirst.set(scoreFirst);
        if (scoreFirst > 0 || scoreSecond.get() > 0 || scoreFinal.get() > 0) {
            calculateOverallScore();
        }
    }
    
    public SimpleDoubleProperty scoreFirstProperty() {
        return scoreFirst;
    }
    
    public double getScoreSecond() {
        return scoreSecond.get();
    }
    
    public void setScoreSecond(double scoreSecond) {
        this.scoreSecond.set(scoreSecond);
        if (scoreFirst.get() > 0 || scoreSecond > 0 || scoreFinal.get() > 0) {
            calculateOverallScore();
        }
    }
    
    public SimpleDoubleProperty scoreSecondProperty() {
        return scoreSecond;
    }
    
    public double getScoreFinal() {
        return scoreFinal.get();
    }
    
    public void setScoreFinal(double scoreFinal) {
        this.scoreFinal.set(scoreFinal);
        if (scoreFirst.get() > 0 || scoreSecond.get() > 0 || scoreFinal > 0) {
            calculateOverallScore();
        }
    }
    
    public SimpleDoubleProperty scoreFinalProperty() {
        return scoreFinal;
    }
    
    public double getScoreOverall() {
        return scoreOverall.get();
    }
    
    public void setScoreOverall(double scoreOverall) {
        this.scoreOverall.set(scoreOverall);
    }
    
    public SimpleDoubleProperty scoreOverallProperty() {
        return scoreOverall;
    }
    
    public String getScoreText() {
        return scoreText.get();
    }
    
    public void setScoreText(String scoreText) {
        this.scoreText.set(scoreText);
    }
    
    public StringProperty scoreTextProperty() {
        return scoreText;
    }
    
    public boolean isSelected() {
        return isSelected.get();
    }
    
    public void setSelected(boolean selected) {
        this.isSelected.set(selected);
    }
    
    public BooleanProperty selectedProperty() {
        return isSelected;
    }
    
    public Long getItemId() {
        return itemId;
    }
    
    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }
    
    public boolean isNewItem() {
        return isNewItem;
    }
    
    public void setNewItem(boolean isNewItem) {
        this.isNewItem = isNewItem;
    }
}

