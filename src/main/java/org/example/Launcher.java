package org.example;

/**
 * Launcher class để tránh vấn đề với JavaFX module system
 * Sử dụng class này nếu gặp lỗi "JavaFX runtime components are missing"
 */
public class Launcher {
    public static void main(String[] args) {
        Main.main(args);
    }
}

