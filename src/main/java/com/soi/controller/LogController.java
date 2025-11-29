package com.soi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

/**
 * 로그 조회 컨트롤러
 */
@Controller
@RequestMapping("/logs")
public class LogController {

    private static final String LOG_DIR = "logs";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 로그 조회 페이지
     */
    @GetMapping("/view")
    public String viewLogs(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        
        // 로그 파일 목록 가져오기
        List<Map<String, Object>> logFiles = getLogFiles();
        model.addAttribute("logFiles", logFiles);
        
        return "log-viewer";
    }

    /**
     * 로그 파일 목록 조회 API
     */
    @GetMapping("/api/files")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getLogFilesApi() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Map<String, Object>> logFiles = getLogFiles();
            response.put("success", true);
            response.put("files", logFiles);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    /**
     * 로그 내용 조회 API
     */
    @GetMapping("/api/content")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getLogContent(
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) String date,
            @RequestParam(required = false, defaultValue = "1000") Integer lines,
            @RequestParam(required = false) String filter) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String logFilePath = determineLogFilePath(fileName, date);
            
            if (logFilePath == null || !Files.exists(Paths.get(logFilePath))) {
                response.put("success", false);
                response.put("message", "로그 파일을 찾을 수 없습니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            List<String> logLines = readLogFile(logFilePath, lines, filter);
            
            response.put("success", true);
            response.put("lines", logLines);
            response.put("filePath", logFilePath);
            response.put("totalLines", logLines.size());
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "로그를 읽는 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 로그 파일 다운로드 API
     */
    @GetMapping("/api/download")
    @ResponseBody
    public ResponseEntity<byte[]> downloadLogFile(
            @RequestParam String fileName,
            @RequestParam(required = false) String date) {
        
        try {
            String logFilePath = determineLogFilePath(fileName, date);
            
            if (logFilePath == null || !Files.exists(Paths.get(logFilePath))) {
                return ResponseEntity.notFound().build();
            }
            
            byte[] fileContent = Files.readAllBytes(Paths.get(logFilePath));
            
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .header("Content-Type", "text/plain; charset=UTF-8")
                    .body(fileContent);
                    
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 로그 파일 삭제 API
     */
    @DeleteMapping("/api/delete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteLogFile(
            @RequestParam String fileName,
            @RequestParam(required = false) String date) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String logFilePath = determineLogFilePath(fileName, date);
            
            if (logFilePath == null || !Files.exists(Paths.get(logFilePath))) {
                response.put("success", false);
                response.put("message", "로그 파일을 찾을 수 없습니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            Files.delete(Paths.get(logFilePath));
            
            response.put("success", true);
            response.put("message", "로그 파일이 삭제되었습니다.");
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "로그 파일 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 로그 파일 목록 가져오기
     */
    private List<Map<String, Object>> getLogFiles() {
        List<Map<String, Object>> files = new ArrayList<>();
        
        try {
            Path logDir = Paths.get(LOG_DIR);
            if (!Files.exists(logDir)) {
                return files;
            }
            
            try (Stream<Path> paths = Files.walk(logDir, 1)) {
                paths.filter(Files::isRegularFile)
                     .filter(p -> p.toString().endsWith(".log"))
                     .forEach(path -> {
                         try {
                             Map<String, Object> fileInfo = new HashMap<>();
                             fileInfo.put("name", path.getFileName().toString());
                             fileInfo.put("path", path.toString());
                             fileInfo.put("size", Files.size(path));
                             fileInfo.put("lastModified", Files.getLastModifiedTime(path).toMillis());
                             files.add(fileInfo);
                         } catch (IOException e) {
                             // 무시
                         }
                     });
            }
            
            // 최신 파일 순으로 정렬
            files.sort((a, b) -> Long.compare(
                (Long) b.get("lastModified"),
                (Long) a.get("lastModified")
            ));
            
        } catch (Exception e) {
            System.err.println("Error getting log files: " + e.getMessage());
        }
        
        return files;
    }

    /**
     * 로그 파일 경로 결정
     */
    private String determineLogFilePath(String fileName, String date) {
        if (fileName == null || fileName.isEmpty()) {
            // 기본값: 오늘 날짜의 application.log
            fileName = "application.log";
            date = LocalDate.now().format(DATE_FORMATTER);
        }
        
        if (date != null && !date.isEmpty()) {
            // 날짜별 로그 파일
            String baseName = fileName.replace(".log", "");
            return LOG_DIR + "/" + baseName + "." + date + ".log";
        } else {
            // 현재 로그 파일
            return LOG_DIR + "/" + fileName;
        }
    }

    /**
     * 로그 파일 읽기
     */
    private List<String> readLogFile(String filePath, int maxLines, String filter) throws IOException {
        List<String> lines = new ArrayList<>();
        
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (filter == null || filter.isEmpty() || line.contains(filter)) {
                    lines.add(line);
                }
            }
        }
        
        // 최신 로그부터 (뒤에서부터)
        Collections.reverse(lines);
        
        // 최대 라인 수 제한
        if (lines.size() > maxLines) {
            lines = lines.subList(0, maxLines);
        }
        
        return lines;
    }

}

