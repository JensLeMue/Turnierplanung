package com.fencingplanner;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.fencingplanner.model.AgeCategory;
import com.fencingplanner.model.Event;
import com.fencingplanner.model.Schedule;
import com.fencingplanner.model.Weekend;

public class ExcelScheduleExporter {

    // Farbcodierung für Event-Typen
    private static final Map<String, short[]> TYPE_COLORS = new HashMap<>();
    static {
        TYPE_COLORS.put("FIE", new short[]{192, 0, 0});        // Rot
        TYPE_COLORS.put("EFC", new short[]{0, 112, 192});      // Blau
        TYPE_COLORS.put("QB", new short[]{0, 176, 80});        // Grün
        TYPE_COLORS.put("CHALLENGE", new short[]{0, 176, 80}); // Grün
        TYPE_COLORS.put("DM", new short[]{255, 192, 0});       // Orange
    }

    private final Schedule schedule;
    private final String outputPath;

    /**
     * Constructs an ExcelScheduleExporter with the given schedule and output path.
     * @param schedule the schedule to export
     * @param outputPath the path where the Excel file will be saved
     */
    public ExcelScheduleExporter(Schedule schedule, String outputPath) {
        this.schedule = schedule;
        this.outputPath = outputPath;
    }

    /**
     * Exports the schedule to an Excel file.
     * @throws IOException if an error occurs during file writing
     */
    public void export() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Turnierplanung");
            
            // Header erstellen
            createHeader(sheet);
            
            // Daten einfügen
            populateData(sheet);
            
            // Spaltenbreite anpassen
            adjustColumnWidths(sheet);
            
            // Datei speichern
            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                workbook.write(fos);
            }
            
            System.out.println("\nExcel-Planung erstellt: " + outputPath);
        }
    }

    /**
     * Creates the header row in the Excel sheet with weekend and age category columns.
     * @param sheet the Excel sheet to create the header in
     */
    private void createHeader(Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(25);
        
        CellStyle headerStyle = sheet.getWorkbook().createCellStyle();
        Font headerFont = sheet.getWorkbook().createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        
        // Spalte 1: Wochenende/Datum
        Cell cell0 = headerRow.createCell(0);
        cell0.setCellValue("Wochenende (KW)");
        cell0.setCellStyle(headerStyle);
        
        // Spalten 2-8: Altersklassen
        AgeCategory[] categories = AgeCategory.values();
        for (int i = 0; i < categories.length; i++) {
            Cell cell = headerRow.createCell(i + 1);
            cell.setCellValue(categories[i].toString());
            cell.setCellStyle(headerStyle);
        }
    }

    /**
     * Populates the Excel sheet with schedule data, organizing events by weekend and age category.
     * @param sheet the Excel sheet to populate
     */
    private void populateData(Sheet sheet) {
        // Events nach Wochenende sortieren
        List<Weekend> weekends = schedule.getWeekends().stream()
                .filter(w -> schedule.getEvents().stream()
                        .anyMatch(e -> e.getWeekend() != null && e.getWeekend().equals(w)))
                .sorted(Comparator.comparing(Weekend::getDate))
                .collect(Collectors.toList());
        
        int rowIndex = 1;
        
        for (Weekend weekend : weekends) {
            Row row = sheet.createRow(rowIndex++);
            row.setHeightInPoints(40);
            
            // Wochenende und Kalenderwoche
            Cell dateCell = row.createCell(0);
            LocalDate date = weekend.getDate();
            int weekNumber = date.get(WeekFields.ISO.weekOfWeekBasedYear());
            String dateStr = String.format("KW %d\n%s", weekNumber, date);
            dateCell.setCellValue(dateStr);
            dateCell.setCellStyle(createDateCellStyle(sheet.getWorkbook()));
            
            // Nach Altersklasse gruppierte Events für dieses Wochenende
            Map<AgeCategory, List<Event>> eventsByCategory = schedule.getEvents().stream()
                    .filter(e -> e.getWeekend() != null && e.getWeekend().equals(weekend))
                    .collect(Collectors.groupingBy(Event::getAgeCategory));
            
            // Für jede Altersklasse
            AgeCategory[] categories = AgeCategory.values();
            for (int i = 0; i < categories.length; i++) {
                Cell cell = row.createCell(i + 1);
                AgeCategory category = categories[i];
                
                List<Event> eventsForCategory = eventsByCategory.getOrDefault(category, new ArrayList<>());
                
                if (!eventsForCategory.isEmpty()) {
                    String cellValue = eventsForCategory.stream()
                            .map(e -> e.getClub().getName() + " - " + e.getName() + " (" + e.getType() + ")")
                            .collect(Collectors.joining("\n"));
                    cell.setCellValue(cellValue);
                    
                    // Hintergrundfarbe basierend auf Event-Typ
                    if (!eventsForCategory.isEmpty()) {
                        String type = eventsForCategory.get(0).getType();
                        cell.setCellStyle(createEventCellStyle(sheet.getWorkbook(), type));
                    }
                }
                
                cell.setCellStyle(createEventCellStyle(sheet.getWorkbook(), 
                        eventsForCategory.isEmpty() ? "Default" : eventsForCategory.get(0).getType()));
            }
        }
    }

    /**
     * Creates the cell style for date cells in the Excel sheet.
     * @param workbook the Excel workbook
     * @return the cell style for date cells
     */
    private CellStyle createDateCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setWrapText(true);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * Creates the cell style for event cells in the Excel sheet, with color coding based on event type.
     * @param workbook the Excel workbook
     * @param eventType the type of the event to determine the color
     * @return the cell style for event cells
     */
    private CellStyle createEventCellStyle(Workbook workbook, String eventType) {
        if (workbook instanceof XSSFWorkbook) {
            XSSFCellStyle style = (XSSFCellStyle) workbook.createCellStyle();
            style.setAlignment(HorizontalAlignment.LEFT);
            style.setVerticalAlignment(VerticalAlignment.TOP);
            style.setWrapText(true);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            
            // Farbe setzen basierend auf Event-Typ
            short[] color = TYPE_COLORS.getOrDefault(eventType, new short[]{255, 255, 255});
            byte[] rgbBytes = new byte[]{(byte)color[0], (byte)color[1], (byte)color[2]};
            
            XSSFColor xssfColor = new XSSFColor(rgbBytes);
            style.setFillForegroundColor(xssfColor);
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            return style;
        } else {
            CellStyle style = workbook.createCellStyle();
            style.setAlignment(HorizontalAlignment.LEFT);
            style.setVerticalAlignment(VerticalAlignment.TOP);
            style.setWrapText(true);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            return style;
        }
    }

    /**
     * Adjusts the column widths in the Excel sheet for better readability.
     * @param sheet the Excel sheet to adjust
     */
    private void adjustColumnWidths(Sheet sheet) {
        sheet.setColumnWidth(0, 25 * 256);
        for (int i = 1; i <= 7; i++) {
            sheet.setColumnWidth(i, 30 * 256);
        }
    }
}
