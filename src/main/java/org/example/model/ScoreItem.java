package org.example.model;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Model class cho điểm thi
 */
public class ScoreItem {
    private final StringProperty subjectName;
    private final SimpleIntegerProperty subjectCredit;
    private final SimpleDoubleProperty scoreFirst;
    private final SimpleDoubleProperty scoreSecond;
    private final SimpleDoubleProperty scoreFinal;
    private final SimpleDoubleProperty scoreOverall;
    private final StringProperty scoreText;
    private boolean isRecentSemester; // Đánh dấu môn học kì gần nhất
    private boolean isFailed; // Đánh dấu môn trượt
    
    public ScoreItem() {
        this.subjectName = new SimpleStringProperty();
        this.subjectCredit = new SimpleIntegerProperty();
        this.scoreFirst = new SimpleDoubleProperty();
        this.scoreSecond = new SimpleDoubleProperty();
        this.scoreFinal = new SimpleDoubleProperty();
        this.scoreOverall = new SimpleDoubleProperty();
        this.scoreText = new SimpleStringProperty();
        this.isRecentSemester = false;
        this.isFailed = false;
    }
    
    /**
     * Chuyển đổi điểm từ thang 10 sang thang 4
     */
    public static double convertToScale4(double score10) {
        if (score10 >= 9.0) return 4.0;
        if (score10 >= 8.5) return 3.8;
        if (score10 >= 7.8) return 3.5;
        if (score10 >= 7.0) return 3.0;
        if (score10 >= 6.3) return 2.4;
        if (score10 >= 5.5) return 2.0;
        if (score10 >= 4.8) return 1.5;
        if (score10 >= 4.0) return 1.0;
        return 0.0;
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
    public void calculateScoreText(double overall) {
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
    }
    
    public SimpleDoubleProperty scoreFirstProperty() {
        return scoreFirst;
    }
    
    public double getScoreSecond() {
        return scoreSecond.get();
    }
    
    public void setScoreSecond(double scoreSecond) {
        this.scoreSecond.set(scoreSecond);
    }
    
    public SimpleDoubleProperty scoreSecondProperty() {
        return scoreSecond;
    }
    
    public double getScoreFinal() {
        return scoreFinal.get();
    }
    
    public void setScoreFinal(double scoreFinal) {
        this.scoreFinal.set(scoreFinal);
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
    
    public boolean isRecentSemester() {
        return isRecentSemester;
    }
    
    public void setRecentSemester(boolean isRecentSemester) {
        this.isRecentSemester = isRecentSemester;
    }
    
    public boolean isFailed() {
        return isFailed;
    }
    
    public void setFailed(boolean isFailed) {
        this.isFailed = isFailed;
    }
}

