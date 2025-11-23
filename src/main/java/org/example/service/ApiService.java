package org.example.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import org.example.config.AppConfig;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Service để gọi các API từ backend
 */
public class ApiService {
    private static ApiService instance;
    private final OkHttpClient client;
    private String baseUrl = AppConfig.BACKEND_BASE_URL;
    
    private ApiService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }
    
    public static ApiService getInstance() {
        if (instance == null) {
            instance = new ApiService();
        }
        return instance;
    }
    
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    public String getBaseUrl() {
        return baseUrl;
    }
    
    /**
     * Lấy public key từ backend
     * Trả về public key dạng Base64 string
     */
    public String getPublicKey() throws IOException {
        Request request = new Request.Builder()
                .url(baseUrl + AppConfig.PUBLIC_KEY_ENDPOINT)
                .get()
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            String responseBody = response.body().string();
            
            // Kiểm tra xem response có phải là JSON không
            if (responseBody.trim().startsWith("{")) {
                // Parse JSON response
                JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
                if (jsonResponse.has("publicKey")) {
                    return jsonResponse.get("publicKey").getAsString();
                } else if (jsonResponse.has("data")) {
                    // Có thể public key nằm trong data
                    JsonObject data = jsonResponse.getAsJsonObject("data");
                    if (data.has("publicKey")) {
                        return data.get("publicKey").getAsString();
                    }
                }
                // Nếu không tìm thấy publicKey, thử lấy toàn bộ response
                throw new IOException("Public key không tìm thấy trong response JSON");
            }
            
            // Nếu không phải JSON, trả về trực tiếp
            return responseBody.trim();
        }
    }
    
    /**
     * Đăng nhập với format mới (encryptedKey, encryptedData, iv)
     */
    public String login(String encryptedKey, String encryptedData, String iv) throws IOException {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("encryptedKey", encryptedKey);
        jsonObject.addProperty("encryptedData", encryptedData);
        jsonObject.addProperty("iv", iv);
        
        RequestBody body = RequestBody.create(
                jsonObject.toString(),
                MediaType.get("application/json; charset=utf-8")
        );
        
        Request request = new Request.Builder()
                .url(baseUrl + AppConfig.LOGIN_ENDPOINT)
                .post(body)
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Login failed: " + response.code());
            }
            return response.body().string();
        }
    }
    
    /**
     * Đăng nhập (giữ lại cho tương thích - deprecated)
     */
    @Deprecated
    public String login(String username, String encryptedPassword) throws IOException {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("username", username);
        jsonObject.addProperty("password", encryptedPassword);
        
        RequestBody body = RequestBody.create(
                jsonObject.toString(),
                MediaType.get("application/json; charset=utf-8")
        );
        
        Request request = new Request.Builder()
                .url(baseUrl + AppConfig.LOGIN_ENDPOINT)
                .post(body)
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Login failed: " + response.code());
            }
            return response.body().string();
        }
    }
    
    /**
     * Lấy lịch học
     */
    public String getSchedule(String token) throws IOException {
        Request request = new Request.Builder()
                .url(baseUrl + AppConfig.SCHEDULE_ENDPOINT)
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to get schedule: " + response.code());
            }
            return response.body().string();
        }
    }
    
    /**
     * Lấy bảng điểm
     */
    public String getGrades(String token) throws IOException {
        Request request = new Request.Builder()
                .url(baseUrl + AppConfig.GRADES_ENDPOINT)
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to get grades: " + response.code());
            }
            return response.body().string();
        }
    }
    
    /**
     * Lấy danh sách môn học ảo (virtual calendar)
     * Sử dụng cùng format như login: encryptedKey, encryptedData, iv
     */
    public String getVirtualCalendar(String encryptedKey, String encryptedData, String iv) throws IOException {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("encryptedKey", encryptedKey);
        jsonObject.addProperty("encryptedData", encryptedData);
        jsonObject.addProperty("iv", iv);
        
        RequestBody body = RequestBody.create(
                jsonObject.toString(),
                MediaType.get("application/json; charset=utf-8")
        );
        
        Request request = new Request.Builder()
                .url(baseUrl + AppConfig.VIRTUAL_CALENDAR_ENDPOINT)
                .post(body)
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to get virtual calendar: " + response.code());
            }
            return response.body().string();
        }
    }
    
    /**
     * Lấy điểm thi (scores)
     * Sử dụng format encryptedKey, encryptedData, iv với studentCode trong encryptedData
     */
    public String getScores(String encryptedKey, String encryptedData, String iv) throws IOException {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("encryptedKey", encryptedKey);
        jsonObject.addProperty("encryptedData", encryptedData);
        jsonObject.addProperty("iv", iv);
        
        RequestBody body = RequestBody.create(
                jsonObject.toString(),
                MediaType.get("application/json; charset=utf-8")
        );
        
        Request request = new Request.Builder()
                .url(baseUrl + AppConfig.SCORES_ENDPOINT)
                .post(body)
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to get scores: " + response.code());
            }
            return response.body().string();
        }
    }
    
    /**
     * Lấy bảng điểm ảo (score batch)
     * GET request với studentCode trong URL
     */
    public String getScoreBatch(String studentCode) throws IOException {
        Request request = new Request.Builder()
                .url(baseUrl + AppConfig.SCORE_BATCH_ENDPOINT + "/" + studentCode)
                .get()
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to get score batch: " + response.code());
            }
            return response.body().string();
        }
    }
    
    /**
     * Lọc danh sách học bổng theo khóa
     * POST /api/v1/semester/filter/scholarship
     * Body: {"code": "AT19"} (đã được mã hóa)
     */
    public String filterScholarship(String encryptedKey, String encryptedData, String iv) throws IOException {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("encryptedKey", encryptedKey);
        jsonObject.addProperty("encryptedData", encryptedData);
        jsonObject.addProperty("iv", iv);
        
        RequestBody body = RequestBody.create(
                jsonObject.toString(),
                MediaType.get("application/json; charset=utf-8")
        );
        
        Request request = new Request.Builder()
                .url(baseUrl + "/api/v1/semester/filter/scholarship")
                .post(body)
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to filter scholarship: " + response.code());
            }
            return response.body().string();
        }
    }
}

