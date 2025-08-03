package com.panda.flashlocaldownloadserver.controllers;

import com.panda.flashlocaldownloadserver.utils.MultiThreadedDownloader;
import com.panda.flashlocaldownloadserver.utils.StageSwitcher;
import com.panda.flashlocaldownloadserver.utils.yt_dlp.Youtube.YoutubeUtility;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.Map;

public class DownloadController {

    @FXML
    private ImageView minimizeButton;
    @FXML
    private Label filenameLabel, percentLabel, connectionSpeedLabel, etaLabel, fileSize;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Button finishButton, cancelButton;

    private String fileUrl;
    private String savePath;
    private String formatId;
    private Map<String, String> headers;

    private final MultiThreadedDownloader multiThreadedDownloader;
    private Thread fileDownloadThread;

    public DownloadController() {
        multiThreadedDownloader = new MultiThreadedDownloader();
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getSavePath() {
        return savePath;
    }

    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }

    public String getFormatId() {
        return formatId;
    }

    public void setFormatId(String formatId) {
        this.formatId = formatId;
    }

    public void startDownload(boolean isYoutube) {
        fileDownloadThread = new Thread(() -> {
            try {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        if(isYoutube) {
                            YoutubeUtility.progressPercentObserver.setListener((oldValue, newValue) -> Platform.runLater(()-> {
                                percentLabel.setText((int)Math.round(newValue)+"%");
                                updateProgress(newValue, 100);
                            }));
                        } else {
                            MultiThreadedDownloader.progressPercentObserver.setListener((oldValue, newValue) -> Platform.runLater(()-> {
                                percentLabel.setText((int)Math.round(newValue)+"%");
                                updateProgress(newValue, 100);
                            }));
                        }
                        return null;
                    }
                };
                progressBar.progressProperty().bind(task.progressProperty());
                new Thread(task).start();
                if(isYoutube) {
                    YoutubeUtility.fileNameObserver.setListener((oldValue, newValue) -> Platform.runLater(()->filenameLabel.setText(newValue)));
                    YoutubeUtility.fileSizeObserver.setListener((oldValue, newValue) -> Platform.runLater(()->fileSize.setText(newValue.trim())));
                    YoutubeUtility.connectionSpeedObserver.setListener(((oldValue, newValue) -> Platform.runLater(()->connectionSpeedLabel.setText("("+newValue+")"))));
                    YoutubeUtility.etaObserver.setListener((oldValue, newValue) -> Platform.runLater(()->etaLabel.setText("| ETA: "+newValue)));
                    YoutubeUtility.downloadFinishStatusObserver.setListener(((oldValue, newValue) -> changeCancelButtonStatus(newValue)));
                    YoutubeUtility.downloadAndMerge(formatId, fileUrl, savePath);
                } else {
                    MultiThreadedDownloader.fileNameObserver.setListener((oldValue, newValue) -> Platform.runLater(()->filenameLabel.setText(newValue)));
                    MultiThreadedDownloader.fileSizeObserver.setListener((oldValue, newValue) -> Platform.runLater(()->fileSize.setText(newValue.trim())));
                    MultiThreadedDownloader.connectionSpeedObserver.setListener(((oldValue, newValue) -> Platform.runLater(()->connectionSpeedLabel.setText("("+newValue+")"))));
                    MultiThreadedDownloader.etaObserver.setListener((oldValue, newValue) -> Platform.runLater(()->etaLabel.setText("| ETA: "+newValue)));
                    MultiThreadedDownloader.downloadFinishStatusObserver.setListener(((oldValue, newValue) -> changeCancelButtonStatus(newValue)));
                    multiThreadedDownloader.setHeaders(headers);
                    multiThreadedDownloader.downloadFile(fileUrl, savePath);
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        fileDownloadThread.start();
    }

    private void changeCancelButtonStatus(boolean isFinish) {
        if(isFinish) {
            Platform.runLater(()->cancelButton.setText("Finished"));
            cancelButton.getStyleClass().add("pause-button");
            cancelButton.getStyleClass().remove("cancel-button");
        } else {
            Platform.runLater(()->cancelButton.setText("Cancel"));
            cancelButton.getStyleClass().add("cancel-button");
            cancelButton.getStyleClass().remove("pause-button");
        }
    }

    @FXML
    public void clickOnCancelBtn() {
        StageSwitcher.getCurrentStage().close();
        multiThreadedDownloader.stopDownload();
    }

    @FXML
    public void hoverOnMinimizeBtn() {
        minimizeButton.setOpacity(0.7);
    }
    @FXML
    public void hoverOffMinimizeBtn() {
        minimizeButton.setOpacity(0.4);
    }
    @FXML
    public void onClickMinimizeBtn() {
        Stage stage = StageSwitcher.getCurrentStage();

        // Root node of the scene
        var root = stage.getScene().getRoot();

        // Longer duration for smoother effect
        Duration duration = Duration.millis(400);

        // Fade out
        FadeTransition fadeOut = new FadeTransition(duration, root);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setInterpolator(Interpolator.EASE_IN);

        // Slide down (more distance for dramatic effect)
        TranslateTransition slideDown = new TranslateTransition(duration, root);
        slideDown.setFromY(0);
        slideDown.setToY(100); // was 30 before
        slideDown.setInterpolator(Interpolator.EASE_IN);

        // Combine both transitions
        ParallelTransition minimizeEffect = new ParallelTransition(fadeOut, slideDown);

        // When finished, minimize the stage and reset UI
        minimizeEffect.setOnFinished(event -> {
            stage.setIconified(true);
            // Reset visuals for when restored
            root.setOpacity(1.0);
            root.setTranslateY(0);
        });

        minimizeEffect.play();
    }

}
