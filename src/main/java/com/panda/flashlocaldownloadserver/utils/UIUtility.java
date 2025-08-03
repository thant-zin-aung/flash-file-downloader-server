package com.panda.flashlocaldownloadserver.utils;

import com.panda.flashlocaldownloadserver.MainApplication;
import com.panda.flashlocaldownloadserver.controllers.DownloadController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.Objects;

public class UIUtility {
    public static void makeStageDraggable(Stage stage, Node dragNode) {
        final double[] xOffset = new double[1];
        final double[] yOffset = new double[1];

        dragNode.setOnMousePressed(event -> {
            xOffset[0] = event.getSceneX();
            yOffset[0] = event.getSceneY();
        });

        dragNode.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - xOffset[0]);
            stage.setY(event.getScreenY() - yOffset[0]);
        });
    }

    public static DownloadController getDownloadController() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource("/com/panda/flashlocaldownloadserver/views/download-view.fxml"));
        Parent root = loader.load();
        DownloadController controller = loader.getController();
        Stage downloadStage = new Stage();
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        downloadStage.setAlwaysOnTop(true);
        downloadStage.setScene(scene);
        downloadStage.initStyle(StageStyle.TRANSPARENT);
        downloadStage.getIcons().add(new Image(Objects.requireNonNull(MainApplication.class.getResourceAsStream("/com/panda/flashlocaldownloadserver/assets/img/app-logo.png"))));
        UIUtility.makeStageDraggable(downloadStage, root);
        StageSwitcher.addNewStage(StageSwitcher.Stages.DOWNLOAD_STAGE, downloadStage);
        return controller;
    }
}
