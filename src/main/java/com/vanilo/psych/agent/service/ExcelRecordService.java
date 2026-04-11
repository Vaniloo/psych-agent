package com.vanilo.psych.agent.service;

import com.vanilo.psych.agent.dto.AnalyzeResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.time.LocalDateTime;

@Service
public class ExcelRecordService {
    private static final String FILE_PATH = "risk_records.xlsx";
    public void appendHighRiskRecord(String username, String message, AnalyzeResponse response){
        if (message == null || message.isBlank() || username == null || username.isBlank() || response == null) {
            throw new RuntimeException("名称，内容不能为空");
        }
        Workbook workbook = null;
        File file = new File(FILE_PATH);
        try {
            if (file.exists()) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    workbook = WorkbookFactory.create(fis);
                }
            } else {
                workbook = new XSSFWorkbook();
            }

            Sheet sheet = workbook.getSheet("Sheet1");
            if (sheet == null) {
                sheet = workbook.createSheet("Sheet1");
                String[] headerNames = {"reportID", "username", "message", "risk", "emotion", "confidence", "time"};
                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < headerNames.length; i++) {
                    headerRow.createCell(i).setCellValue(headerNames[i]);
                }
            }

            String[] inp = {
                    response.getReportId() == null ? "" : response.getReportId().toString(),
                    username,
                    message,
                    response.getRisk(),
                    response.getEmotion(),
                    response.getConfidence() == null ? "" : response.getConfidence().toString(),
                    LocalDateTime.now().toString()
            };

            Row newRow = sheet.createRow(sheet.getLastRowNum() + 1);
            for (int i = 0; i < inp.length; i++) {
                newRow.createCell(i).setCellValue(inp[i]);
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }

        } catch (Exception e) {
            throw new RuntimeException("Excel记录失败", e);
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
