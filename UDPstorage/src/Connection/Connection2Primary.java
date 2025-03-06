package Connection;

import Dto.NoteDto;
import Repository.LocalRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Connection2Primary {
    private static final String PRIMARY_STORAGE_URL = "http://localhost:5000";
    private final LocalRepository localRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Connection2Primary(LocalRepository localRepository) {
        this.localRepository = localRepository;
    }

    // 초기 데이터 가져오기
    public void initConnectionAndFetchNotes() {
        try {
            String dataUrl = PRIMARY_STORAGE_URL + "/primary/allnotes";
            HttpURLConnection connection = (HttpURLConnection) new URL(dataUrl).openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String response = reader.lines().collect(Collectors.joining());
                    processInitResponse(response); // 응답 처리
                }
            } else {
                System.out.println("[ERROR] 프라이머리 스토리지 연결 실패. 응답 코드: " + responseCode);
            }
        } catch (IOException e) {
            System.out.println("[ERROR] 프라이머리 스토리지와의 연결 중 오류 발생: " + e.getMessage());
        }
    }

    // 초기 데이터 응답 처리
    private void processInitResponse(String responseJson) throws IOException {
        Map<String, Object> responseMap = objectMapper.readValue(responseJson, Map.class);

        // 프라이머리 스토리지 상태 확인
        if ("READY".equals(responseMap.get("status"))) {
            List<Map<String, Object>> notesList = (List<Map<String, Object>>) responseMap.get("notes");

            for (Map<String, Object> noteMap : notesList) {
                NoteDto note = objectMapper.convertValue(noteMap, NoteDto.class);
                localRepository.addNote(note); // 로컬 저장소에 추가
            }

            System.out.println("[DEBUG] 프라이머리 스토리지에서 가져온 데이터: " + responseJson);
            System.out.println("[DEBUG] 로컬 저장소 동기화 결과: " + localRepository.getAllNotes());
            System.out.println("프라이머리 스토리지의 데이터를 로컬 저장소에 동기화했습니다.");
        } else {
            System.out.println("[ERROR] 프라이머리 스토리지가 준비되지 않았습니다.");
        }
    }


    // 클라이언트로부터 메시지 처리
    public void processMessageFromClient(String message) {
        try {
            // 메시지를 구분자로 나눔
            System.out.println("[DEBUG] processMessageFromClient 호출됨: " + message);

            if (message.contains("\"cmd\":\"check_status\"")) {
                // check_status 메시지 처리
                System.out.println("[DEBUG] check_status 메시지 처리");
                return; // 프라이머리 스토리지로 전달하지 않음
            }

            String[] parts = message.split("\\|", 2);
            String methodAndPathJson = parts[0].trim();
            String dataJson = parts.length > 1 ? parts[1].trim() : null;

            // JSON 파싱: 메서드와 경로 정보 추출
            Map<String, String> methodAndPath = objectMapper.readValue(methodAndPathJson, HashMap.class);
            String method = methodAndPath.get("method");
            String path = methodAndPath.get("path");
            System.out.println("[DEBUG] 요청 처리 시작: method=" + method + ", path=" + path + ", body=" + dataJson);

            // GET 요청은 로컬 저장소에서 처리
            if ("GET".equalsIgnoreCase(method)) {
                String response = handleGetRequest(path);
                System.out.println("[DEBUG] GET 요청 처리 결과: " + response);
            } else {
                // GET 이외의 요청은 프라이머리 스토리지로 전달
                System.out.println("[DEBUG] sendRequestToPrimary 호출 준비");
                sendRequestToPrimary(method, path, dataJson);
            }
        } catch (IOException e) {
            System.err.println("[ERROR] 클라이언트 메시지 처리 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // GET 요청 처리
    private String handleGetRequest(String path) {
        System.out.println("[DEBUG] GET 요청 처리 시작: " + path);

        try {
            if ("/notes".equalsIgnoreCase(path)) {
                // 모든 노트 조회
                Map<Integer, NoteDto> allNotes = localRepository.getAllNotes();
                if (allNotes.isEmpty()) {
                    System.out.println("현재 저장된 노트는 없습니다.");
                    return "{\"message\": \"현재 저장된 노트는 없습니다.\"}";
                } else {
                    // 모든 노트를 JSON으로 변환하여 반환
                    String jsonResponse = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(allNotes);
                    System.out.println("모든 노트 (JSON): " + jsonResponse);
                    return jsonResponse;
                }
            } else {
                // 특정 ID의 노트 조회
                Integer id = extractIdFromPath(path);
                NoteDto note = localRepository.getNoteById(id);
                if (note != null) {
                    // 단일 노트를 JSON으로 변환하여 반환
                    String jsonResponse = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(note);
                    System.out.println("ID " + id + "에 대한 노트 (JSON): " + jsonResponse);
                    return jsonResponse;
                } else {
                    System.out.println("ID " + id + "에 대한 노트를 찾을 수 없습니다.");
                    return "{\"error\": \"ID " + id + "에 대한 노트를 찾을 수 없습니다.\"}";
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR] JSON 변환 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return "{\"error\": \"JSON 변환 중 오류 발생\"}";
        }
    }



    // 프라이머리 스토리지로 요청 전송
    private void sendRequestToPrimary(String method, String path, String jsonData) {
        if (path == null) {
            System.err.println("[ERROR] path가 null입니다. 요청을 처리할 수 없습니다.");
            return;
        }
        try {
            // 디버깅: 요청 URL 확인
            if (path.startsWith("/notes")) {
                path = path.replaceFirst("/notes", "/primary");
            }
            String requestUrl = PRIMARY_STORAGE_URL + path;
            System.out.println("프라이머리 스토리지로 보낼 URL: " + requestUrl);

            HttpURLConnection connection = (HttpURLConnection) new URL(requestUrl).openConnection();
            connection.setRequestMethod(method);

            System.out.println("HTTP 요청 보냄: URL=" + PRIMARY_STORAGE_URL + path + ", Method=" + method);

            // POST, PATCH, PUT 요청에 대한 바디 설정
            if ("POST".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
                if (jsonData == null || jsonData.isEmpty()) {
                    System.out.println("WARN: JSON 데이터가 비어 있습니다. 요청이 제대로 처리되지 않을 수 있습니다.");
                } else {
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/json");
                    try (OutputStream os = connection.getOutputStream()) {
                        os.write(jsonData.getBytes(StandardCharsets.UTF_8));
                        System.out.println("보낸 JSON 데이터: " + jsonData);
                    }
                }
            }

            // 응답 코드 확인
            int responseCode = connection.getResponseCode();
            System.out.println("프라이머리 스토리지에 요청을 보냈습니다. 응답 코드: " + responseCode);

            // 응답 메시지 출력
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    responseCode >= 200 && responseCode < 300
                            ? connection.getInputStream()
                            : connection.getErrorStream(), StandardCharsets.UTF_8))) {

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                // JSON 파싱 (필요한 경우)
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> responseMap = objectMapper.readValue(response.toString(), Map.class);

                // 응답 처리 로직
                if (responseCode >= 200 && responseCode < 300) {
                    System.out.println("프라이머리 스토리지에 노트 추가 성공: " + responseMap);
                } else {
                    System.err.println("프라이머리 스토리지 응답 에러: " + responseMap);
                }
            }

        } catch (IOException e) {
            // 디버깅: 예외 메시지와 스택 트레이스 출력
            System.err.println("프라이머리 스토리지 요청 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            // 예상치 못한 예외 처리
            System.err.println("알 수 없는 예외 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private Integer extractIdFromPath(String path) {
        String[] parts = path.split("/");
        try {
            return parts.length > 2 ? Integer.parseInt(parts[2]) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
