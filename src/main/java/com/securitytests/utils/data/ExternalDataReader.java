package com.securitytests.utils.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Utility class to read test data from external CSV, Excel, and JSON files
 */
public class ExternalDataReader {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalDataReader.class);
    private static final String DATA_DIRECTORY = "src/test/resources/testdata/";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Read test data from a CSV file
     *
     * @param fileName The name of the CSV file in the test data directory
     * @return List of maps with column names as keys and cell values as values
     */
    public static List<Map<String, String>> readCsvData(String fileName) {
        String filePath = DATA_DIRECTORY + fileName;
        List<Map<String, String>> data = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            List<String[]> allRows = reader.readAll();
            
            if (allRows.isEmpty()) {
                LOGGER.warn("CSV file is empty: {}", fileName);
                return data;
            }
            
            String[] headers = allRows.get(0);
            
            for (int i = 1; i < allRows.size(); i++) {
                Map<String, String> row = new HashMap<>();
                String[] cells = allRows.get(i);
                
                for (int j = 0; j < headers.length && j < cells.length; j++) {
                    row.put(headers[j], cells[j]);
                }
                
                data.add(row);
            }
            
            LOGGER.info("Successfully read {} records from CSV file: {}", data.size(), fileName);
        } catch (IOException | CsvException e) {
            LOGGER.error("Error reading CSV file: {}", fileName, e);
        }
        
        return data;
    }
    
    /**
     * Read test data from an Excel file
     *
     * @param fileName The name of the Excel file in the test data directory
     * @param sheetName The name of the sheet to read (if null, reads the first sheet)
     * @return List of maps with column names as keys and cell values as values
     */
    public static List<Map<String, String>> readExcelData(String fileName, String sheetName) {
        String filePath = DATA_DIRECTORY + fileName;
        List<Map<String, String>> data = new ArrayList<>();
        
        try (Workbook workbook = new XSSFWorkbook(new File(filePath))) {
            Sheet sheet = (sheetName != null) ? workbook.getSheet(sheetName) : workbook.getSheetAt(0);
            
            if (sheet == null) {
                LOGGER.warn("Sheet '{}' not found in Excel file: {}", sheetName, fileName);
                return data;
            }
            
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                LOGGER.warn("Excel sheet has no header row: {}", fileName);
                return data;
            }
            
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(getCellValueAsString(cell));
            }
            
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                Map<String, String> rowData = new HashMap<>();
                for (int j = 0; j < headers.size(); j++) {
                    Cell cell = row.getCell(j, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    String value = (cell != null) ? getCellValueAsString(cell) : "";
                    rowData.put(headers.get(j), value);
                }
                
                data.add(rowData);
            }
            
            LOGGER.info("Successfully read {} records from Excel file: {}", data.size(), fileName);
        } catch (IOException e) {
            LOGGER.error("Error reading Excel file: {}", fileName, e);
        }
        
        return data;
    }
    
    /**
     * Read test data from a JSON file
     *
     * @param fileName The name of the JSON file in the test data directory
     * @return List of maps with property names as keys and values as values
     */
    public static List<Map<String, Object>> readJsonData(String fileName) {
        String filePath = DATA_DIRECTORY + fileName;
        List<Map<String, Object>> data = new ArrayList<>();
        
        try {
            JsonNode rootNode = objectMapper.readTree(new File(filePath));
            
            if (rootNode.isArray()) {
                for (JsonNode node : rootNode) {
                    Map<String, Object> item = objectMapper.convertValue(node, Map.class);
                    data.add(item);
                }
            } else {
                // If it's a single object, wrap it in a list
                Map<String, Object> item = objectMapper.convertValue(rootNode, Map.class);
                data.add(item);
            }
            
            LOGGER.info("Successfully read {} records from JSON file: {}", data.size(), fileName);
        } catch (IOException e) {
            LOGGER.error("Error reading JSON file: {}", fileName, e);
        }
        
        return data;
    }
    
    /**
     * Convert a cell value to a string regardless of its type
     *
     * @param cell The cell to convert
     * @return The cell value as a string
     */
    private static String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
}
