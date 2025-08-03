package com.panda.flashlocaldownloadserver.utils.network;

import com.panda.flashlocaldownloadserver.controllers.DownloadController;
import com.panda.flashlocaldownloadserver.utils.StageSwitcher;
import com.panda.flashlocaldownloadserver.utils.UIUtility;
import com.panda.flashlocaldownloadserver.utils.yt_dlp.YtDlpFormatFetcherJson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class LocalDownloadServer {

    public static void startServer(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/download", LocalDownloadServer::handleDownloadRequest);
        server.createContext("/formats", LocalDownloadServer::handleFormatRequest);
        server.createContext("/intercepted-download", LocalDownloadServer::handleInterceptedDownload); // NEW
        server.setExecutor(null); // creates a default executor
        server.start();
        System.out.println("Download server running at http://localhost:" + port);
    }

    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
    }

    private static void handleDownloadRequest(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            addCorsHeaders(exchange);
            exchange.sendResponseHeaders(204, -1); // No content for OPTIONS
            exchange.close();
            return;
        }

        addCorsHeaders(exchange);

        String response = DownloadRequestHandler.processRequest(exchange);
        byte[] responseBytes = response.getBytes();
        int statusCode = response.startsWith("Invalid") ? 400 : 200;

        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    private static void handleFormatRequest(HttpExchange exchange) throws IOException {
        System.out.println("Youtube formats requested...");
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            System.out.println("Options");
            addCorsHeaders(exchange);
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            System.out.println("Not get method");
            addCorsHeaders(exchange);
            exchange.sendResponseHeaders(405, -1); // Method Not Allowed
            return;
        }

        addCorsHeaders(exchange);
        // Get the `url` parameter from query
        URI requestURI = exchange.getRequestURI();
        String query = requestURI.getQuery(); // url=https://youtube.com/watch?v=xxx
        Map<String, String> params = ObjectUtil.queryToMap(query);
        String videoUrl = params.get("url");

        if (videoUrl == null || videoUrl.isEmpty()) {
            String response = "Missing 'url' query parameter";
            byte[] responseBytes = response.getBytes();
            exchange.sendResponseHeaders(400, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
            return;
        }

        List<YtDlpFormatFetcherJson.Format> formats = YtDlpFormatFetcherJson.getFormats(videoUrl).values().stream()
                .filter(format -> !format.getFilesize().equalsIgnoreCase("unknown")).toList();

        // Convert to JSON
        String json = ObjectUtil.toJson(formats);

        exchange.getResponseHeaders().add("Content-Type", "application/json");
        byte[] responseBytes = json.getBytes();
        exchange.sendResponseHeaders(200, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    // ✅ NEW METHOD: For Chrome extension download interception
    private static void handleInterceptedDownload(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            addCorsHeaders(exchange);
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            addCorsHeaders(exchange);
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        addCorsHeaders(exchange);
        System.out.println("Entered to intercepted download...");

        // Read JSON body
        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
        BufferedReader reader = new BufferedReader(isr);
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }

        String body = sb.toString();
        Map<String, Object> data = ObjectUtil.jsonToMap(body);
        String url = (String) data.get("url");
        String filename = (String) data.getOrDefault("filename", "downloaded_file");
        Map<String, String> headers = (Map<String, String>) data.get("headers");
        if (headers == null) {
            headers = Map.of();
        }

        if (url == null || url.isEmpty()) {
            byte[] responseBytes = "Missing 'url' field".getBytes();
            exchange.sendResponseHeaders(400, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
            return;
        }

        Map<String, String> finalHeaders = headers;
        new Thread(() -> {
            Platform.runLater(() -> {
                try {
                    System.out.println("Intercepted download from extension: " + url);
                    DownloadController downloadController = UIUtility.getDownloadController();
                    StageSwitcher.switchStage(StageSwitcher.Stages.DOWNLOAD_STAGE);
                    Stage downloadStage = StageSwitcher.getCurrentStage();
                    downloadStage.show();
                    downloadController.setHeaders(finalHeaders);
                    downloadController.setFileUrl(url);
                    downloadController.setSavePath("C:/Users/" + System.getProperty("user.name") + "/Downloads");
                    downloadController.startDownload(false);
                } catch (Exception e) {
                    System.err.println("Download failed: " + e.getMessage());
                }
            });
        }).start();

        byte[] responseBytes = "Download started".getBytes();
        exchange.sendResponseHeaders(200, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

}
