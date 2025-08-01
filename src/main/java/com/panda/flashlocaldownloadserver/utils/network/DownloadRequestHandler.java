package com.panda.flashlocaldownloadserver.utils.network;

import com.panda.flashlocaldownloadserver.controllers.DownloadController;
import com.panda.flashlocaldownloadserver.utils.StageSwitcher;
import com.panda.flashlocaldownloadserver.utils.UIUtility;
import com.sun.net.httpserver.HttpExchange;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.util.Map;

public class DownloadRequestHandler {

    public static String processRequest(HttpExchange exchange) {
        if (!"GET".equals(exchange.getRequestMethod())) {
            return "Invalid request method";
        }

        String query = exchange.getRequestURI().getQuery();
        if (query == null) {
            return "Missing query parameters";
        }

        Map<String, String> params = ObjectUtil.queryToMap(query);
        String videoUrl = params.get("url");
        String formatId = params.get("formatId");

        if (videoUrl == null || videoUrl.isEmpty()) {
            return "Missing URL parameter";
        }

        if (!videoUrl.startsWith("https://www.youtube.com")) {
            return "Invalid YouTube URL";
        }

        System.out.println("Download request received for: " + videoUrl + " with formatId: " + formatId);

        new Thread(() -> {
            Platform.runLater(() -> {
                try {
                    DownloadController downloadController = UIUtility.getDownloadController();
                    StageSwitcher.switchStage(StageSwitcher.Stages.DOWNLOAD_STAGE);
                    Stage downloadStage = StageSwitcher.getCurrentStage();
                    downloadStage.show();
                    downloadController.setFileUrl(videoUrl);
                    downloadController.setFormatId(formatId);  // use dynamic formatId here
                    downloadController.setSavePath("C:/Users/" + System.getProperty("user.name") + "/Downloads");
                    downloadController.startDownload(true);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }).start();

        return "Started download for " + videoUrl + " with formatId " + formatId;
    }

}
