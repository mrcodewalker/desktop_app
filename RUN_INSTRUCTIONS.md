# Hướng dẫn chạy ứng dụng KMA Legend Desktop

## Cách 1: Chạy bằng Maven (Khuyến nghị)

Mở terminal trong thư mục dự án và chạy:

```bash
mvn clean javafx:run
```

## Cách 2: Chạy từ IntelliJ IDEA

### Bước 1: Tạo Run Configuration

1. Vào **Run** → **Edit Configurations...**
2. Click **+** → **Application**
3. Đặt tên: `KMA Legend Desktop`
4. **Main class**: `org.example.Main`
5. **VM options**: Thêm các dòng sau:

```
--module-path "${env.JAVAFX_HOME}/lib" --add-modules javafx.controls,javafx.fxml
```

**Lưu ý**: Bạn cần tải JavaFX SDK và set biến môi trường `JAVAFX_HOME` trỏ đến thư mục JavaFX SDK.

### Bước 2: Hoặc sử dụng Maven Run Configuration

1. Vào **Run** → **Edit Configurations...**
2. Click **+** → **Maven**
3. **Name**: `Run JavaFX App`
4. **Command line**: `clean javafx:run`
5. **Working directory**: `$PROJECT_DIR$`

## Cách 3: Chạy trực tiếp với Java (Nếu đã cài JavaFX SDK)

Nếu bạn đã tải JavaFX SDK và đặt ở thư mục `C:\javafx-sdk-17.0.2`:

```bash
java --module-path "C:\javafx-sdk-17.0.2\lib" --add-modules javafx.controls,javafx.fxml -cp "target/classes;%USERPROFILE%\.m2\repository\org\openjfx\javafx-controls\17.0.2\javafx-controls-17.0.2.jar;..." org.example.Main
```

## Tải JavaFX SDK (Nếu cần)

1. Truy cập: https://openjfx.io/
2. Tải JavaFX SDK 17.0.2 cho Windows
3. Giải nén và set biến môi trường `JAVAFX_HOME` trỏ đến thư mục SDK

## Lưu ý

- Backend phải đang chạy tại `http://localhost:8765` (hoặc URL bạn đã cấu hình trong `AppConfig.java`)
- Đảm bảo đã chạy `mvn clean compile` trước khi chạy ứng dụng

