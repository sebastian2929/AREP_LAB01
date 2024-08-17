package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class WebServer {

    private static final int PORT = 8080;
    private static final String WEB_ROOT = "webroot";
    private static final List<String> todos = new ArrayList<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor iniciado en el puerto " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new RequestHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class RequestHandler implements Runnable {
        private Socket clientSocket;

        public RequestHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 OutputStream out = clientSocket.getOutputStream()) {

                String requestLine = in.readLine();
                if (requestLine == null || requestLine.isEmpty()) return;
                System.out.println("Request: " + requestLine);

                String[] tokens = requestLine.split(" ");
                if (tokens.length < 2) return;
                String method = tokens[0];
                String requestedFile = tokens[1];

                if (method.equals("GET")) {
                    if (requestedFile.equals("/api/todos")) {
                        handleGetTodos(out);
                    } else {
                        handleGetRequest(out, requestedFile);
                    }
                } else if (method.equals("POST")) {
                    int contentLength = 0;
                    String line;
                    while (!(line = in.readLine()).isEmpty()) {
                        if (line.startsWith("Content-Length:")) {
                            contentLength = Integer.parseInt(line.substring("Content-Length:".length()).trim());
                        }
                    }

                    char[] bodyChars = new char[contentLength];
                    in.read(bodyChars);
                    String body = new String(bodyChars);

                    if (requestedFile.equals("/api/todos")) {
                        handlePostTodos(out, body);
                    } else if (requestedFile.equals("/api/todos/clear")) {
                        handleClearTodos(out);
                    } else {
                        sendError(out, 404, "Not Found");
                    }
                } else {
                    sendError(out, 405, "Method Not Allowed");
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void handleGetRequest(OutputStream out, String requestedFile) throws IOException {
            if (requestedFile.equals("/api/paisaje")) {
                Path imagePath = Paths.get(WEB_ROOT, "paisaje.png");
                if (Files.exists(imagePath)) {
                    sendResponse(out, 200, "OK", "paisaje/png", Files.readAllBytes(imagePath));
                } else {
                    sendError(out, 404, "Image Not Found");
                }
            } else {
                Path filePath = Paths.get(WEB_ROOT, requestedFile.equals("/") ? "index.html" : requestedFile.substring(1));
                if (Files.exists(filePath)) {
                    byte[] content = Files.readAllBytes(filePath);
                    String contentType = Files.probeContentType(filePath);
                    sendResponse(out, 200, "OK", contentType, content);
                } else {
                    sendError(out, 404, "Not Found");
                }
            }
        }

        private void handleGetTodos(OutputStream out) throws IOException {
            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("{\"todos\":");
            jsonBuilder.append("[");
            for (int i = 0; i < todos.size(); i++) {
                jsonBuilder.append("\"").append(escapeJson(todos.get(i))).append("\"");
                if (i < todos.size() - 1) {
                    jsonBuilder.append(",");
                }
            }
            jsonBuilder.append("]}");

            byte[] jsonBytes = jsonBuilder.toString().getBytes(StandardCharsets.UTF_8);
            sendResponse(out, 200, "OK", "application/json", jsonBytes);
        }

        private void handlePostTodos(OutputStream out, String body) throws IOException {
            String task = parseTaskFromJson(body);
            if (task != null && !task.isEmpty()) {
                todos.add(task);
                String responseJson = "{\"status\":\"success\",\"message\":\"Task added successfully\"}";
                sendResponse(out, 200, "OK", "application/json", responseJson.getBytes(StandardCharsets.UTF_8));
            } else {
                sendError(out, 400, "Bad Request");
            }
        }

        private void handleClearTodos(OutputStream out) throws IOException {
            todos.clear();
            String responseJson = "{\"status\":\"success\",\"message\":\"All tasks cleared\"}";
            sendResponse(out, 200, "OK", "application/json", responseJson.getBytes(StandardCharsets.UTF_8));
        }

        private void sendResponse(OutputStream out, int statusCode, String statusText, String contentType, byte[] content) throws IOException {
            PrintWriter pw = new PrintWriter(out, false);
            pw.printf("HTTP/1.1 %d %s\r\n", statusCode, statusText);
            pw.printf("Content-Type: %s\r\n", contentType);
            pw.printf("Content-Length: %d\r\n", content.length);
            pw.print("\r\n");
            pw.flush();
            out.write(content);
            out.flush();
        }

        private void sendError(OutputStream out, int statusCode, String statusText) throws IOException {
            String errorMessage = String.format("{\"error\":\"%s\"}", statusText);
            sendResponse(out, statusCode, statusText, "application/json", errorMessage.getBytes(StandardCharsets.UTF_8));
        }

        private String parseTaskFromJson(String json) {
            if (json == null || json.isEmpty()) return null;
            json = json.trim();
            if (!json.startsWith("{") || !json.endsWith("}")) return null;

            int taskIndex = json.indexOf("\"task\":");
            if (taskIndex == -1) return null;

            int start = json.indexOf("\"", taskIndex + 7) + 1;
            int end = json.indexOf("\"", start);
            if (start == -1 || end == -1) return null;

            return json.substring(start, end);
        }

        private String escapeJson(String s) {
            return s.replace("\\", "\\\\").replace("\"", "\\\"");
        }
    }
}
