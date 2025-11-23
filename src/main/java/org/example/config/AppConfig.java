package org.example.config;

/**
 * Cấu hình ứng dụng
 * Có thể thay đổi baseUrl tại đây hoặc thông qua biến môi trường
 */
public class AppConfig {
    // URL của backend Spring Boot
    // Có thể thay đổi thành URL thực tế của bạn, ví dụ: "http://localhost:8080"
    // Hoặc lấy từ biến môi trường: System.getenv("BACKEND_URL")
    public static final String BACKEND_BASE_URL = 
        System.getenv("BACKEND_URL") != null ? 
        System.getenv("BACKEND_URL") : 
//        "http://localhost:8765";
        "https://kma-legend.click";
    
    // Các endpoint API
    public static final String PUBLIC_KEY_ENDPOINT = "/api/v1/encryption/public-key";
    public static final String LOGIN_ENDPOINT = "/api/v1/auth/login";
    public static final String SCHEDULE_ENDPOINT = "/api/v1/schedule";
    public static final String GRADES_ENDPOINT = "/api/v1/grades";
    public static final String VIRTUAL_CALENDAR_ENDPOINT = "/api/v1/auth/virtual-calendar";
    public static final String SCORES_ENDPOINT = "/api/v1/ranking/scores";
    public static final String SCORE_BATCH_ENDPOINT = "/api/v1/score-batch/student";
}

