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
                    processInitResponse(response);
                }
            } else {
                System.out.println("프라이머리 스토리지 연결 실패. 응답 코드: " + responseCode);
            }
        } catch (IOException e) {
            System.out.println("프라이머리 스토리지와의 연결 중 오류 발생: " + e.getMessage());
        }
    }

    private void processInitResponse(String responseJson) throws IOException {
        Map<String, Object> responseMap = objectMapper.readValue(responseJson, Map.class);
        if ("READY".equals(responseMap.get("status"))) {
            List<Map<String, Object>> notesList = (List<Map<String, Object>>) responseMap.get("notes");
            for (Map<String, Object> noteMap : notesList) {
                NoteDto note = objectMapper.convertValue(noteMap, NoteDto.class);
                localRepository.addNote(note);
            }
            System.out.println("[DEBUG] 프라이머리 스토리지에서 가져온 데이터: " + responseJson);
            System.out.println("[DEBUG] 로컬 저장소 동기화 결과: " + localRepository.getAllNotes());
            System.out.println("프라이머리 스토리지의 데이터를 로컬 저장소에 동기화했습니다.");
        } else {
            System.out.println("프라이머리 스토리지가 준비되지 않았습니다.");
        }
    }

    // 클라이언트로부터 메시지 처리
    public void processMessageFromClient(String message) {
        try {
            // 메시지를 구분자로 나눔
            String[] parts = message.split("\\|", 2);
            String methodAndPathJson = parts[0].trim();
            String dataJson = parts.length > 1 ? parts[1].trim() : null;

            Map<String, String> methodAndPath = objectMapper.readValue(methodAndPathJson, HashMap.class);
            String method = methodAndPath.get("method");
            String path = methodAndPath.get("path");
            System.out.println("[DEBUG] 프라이머리 스토리지로 보낼 데이터: " + dataJson);

            // GET 요청은 로컬 저장소에서 처리
            if ("GET".equalsIgnoreCase(method)) {
                handleGetRequest(path);
            } else {
                sendRequestToPrimary(method, path, dataJson);
            }
        } catch (IOException e) {
            System.out.println("클라이언트 메시지를 처리하는 중 오류 발생: " + e.getMessage());
        }
    }

    // GET 요청 처리
    private void handleGetRequest(String path) {
        System.out.println("[DEBUG] GET 요청 처리 시작: " + path);

        try {
            if ("/notes".equalsIgnoreCase(path)) {
                Map<Integer, NoteDto> allNotes = localRepository.getAllNotes();
                if (allNotes.isEmpty()) {
                    System.out.println("현재 저장된 노트는 없습니다.");
                } else {
                    // 모든 노트를 JSON으로 변환하여 출력
                    String jsonResponse = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(allNotes);
                    System.out.println("모든 노트 (JSON): " + jsonResponse);
                }
            } else {
                Integer id = extractIdFromPath(path);
                NoteDto note = localRepository.getNoteById(id);
                if (note != null) {
                    // 단일 노트를 JSON으로 변환하여 출력
                    String jsonResponse = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(note);
                    System.out.println("ID " + id + "에 대한 노트 (JSON): " + jsonResponse);
                } else {
                    System.out.println("ID " + id + "에 대한 노트를 찾을 수 없습니다.");
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR] JSON 변환 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }


    // 프라이머리 스토리지로 요청 전송
    private void sendRequestToPrimary(String method, String path, String jsonData) {
        try {
            // 디버깅: 요청 URL 확인
            if (path.startsWith("/notes")) {
                path = path.replaceFirst("/notes", "/primary");
            }
            System.out.println("[DEBUG] 프라이머리 스토리지 요청 시작");
            String requestUrl = PRIMARY_STORAGE_URL + path;

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

                System.out.println("응답 메시지: " + response);
                // JSON 파싱 (필요한 경우)
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> responseMap = objectMapper.readValue(response.toString(), Map.class);

                // 응답 처리 로직
                if (responseCode >= 200 && responseCode < 300) {
                    System.out.println("노트 추가 성공: " + responseMap);
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
