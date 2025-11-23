package org.example.service;

import org.example.model.ScheduleItem;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service để xuất lịch học ra file ICS (iCalendar format)
 */
public class IcsExportService {
    
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    
    /**
     * Xuất lịch học ra file ICS
     */
    public static void exportToIcs(List<ScheduleItem> scheduleItems, String studentName, File outputFile) throws IOException {
        try (FileWriter writer = new FileWriter(outputFile)) {
            // Write ICS header
            writer.write("BEGIN:VCALENDAR\r\n");
            writer.write("VERSION:2.0\r\n");
            writer.write("PRODID:-//KMA Legend Desktop//Schedule Export//EN\r\n");
            writer.write("CALSCALE:GREGORIAN\r\n");
            writer.write("METHOD:PUBLISH\r\n");
            
            // Write each event
            for (ScheduleItem item : scheduleItems) {
                writeEvent(writer, item, studentName);
            }
            
            // Write ICS footer
            writer.write("END:VCALENDAR\r\n");
        }
    }
    
    private static void writeEvent(FileWriter writer, ScheduleItem item, String studentName) throws IOException {
        LocalDate date = item.getDate();
        if (date == null) return;
        
        // Parse start and end time
        LocalTime startTime = parseTime(item.getStartTime());
        LocalTime endTime = parseTime(item.getEndTime());
        
        if (startTime == null || endTime == null) return;
        
        LocalDateTime startDateTime = LocalDateTime.of(date, startTime);
        LocalDateTime endDateTime = LocalDateTime.of(date, endTime);
        
        // Generate unique ID
        String uid = generateUid(item, startDateTime);
        
        // Write event
        writer.write("BEGIN:VEVENT\r\n");
        writer.write("UID:" + uid + "\r\n");
        writer.write("DTSTART:" + startDateTime.format(DATETIME_FORMATTER) + "\r\n");
        writer.write("DTEND:" + endDateTime.format(DATETIME_FORMATTER) + "\r\n");
        writer.write("SUMMARY:" + escapeText(item.getSubject()) + "\r\n");
        writer.write("DESCRIPTION:" + escapeText(
            "Môn học: " + item.getSubject() + "\\n" +
            "Mã môn: " + (item.getCourseCode() != null ? item.getCourseCode() : "") + "\\n" +
            "Phòng học: " + item.getRoom() + "\\n" +
            "Giảng viên: " + item.getTeacher()
        ) + "\r\n");
        writer.write("LOCATION:" + escapeText(item.getRoom()) + "\r\n");
        writer.write("STATUS:CONFIRMED\r\n");
        writer.write("SEQUENCE:0\r\n");
        writer.write("END:VEVENT\r\n");
    }
    
    private static LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) return null;
        try {
            String[] parts = timeStr.split(":");
            if (parts.length >= 2) {
                return LocalTime.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
            }
        } catch (Exception e) {
            System.err.println("Error parsing time: " + timeStr);
        }
        return null;
    }
    
    private static String generateUid(ScheduleItem item, LocalDateTime dateTime) {
        String base = (item.getCourseCode() != null ? item.getCourseCode() : "") + 
                     dateTime.format(DATETIME_FORMATTER);
        return base.replaceAll("[^a-zA-Z0-9]", "") + "@kma-legend.local";
    }
    
    private static String escapeText(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace(",", "\\,")
                   .replace(";", "\\;")
                   .replace("\n", "\\n")
                   .replace("\r", "");
    }
}

