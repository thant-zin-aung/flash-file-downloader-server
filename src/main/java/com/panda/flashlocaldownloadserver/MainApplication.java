package com.panda.flashlocaldownloadserver;

import com.panda.flashlocaldownloadserver.utils.UnsafeSSL;
import com.panda.flashlocaldownloadserver.utils.network.LocalDownloadServer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        Platform.setImplicitExit(false);
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("views/main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);
        stage.setTitle("Flash Download Server!");
        stage.setScene(scene);
    }

    public static void main(String[] args) {
        startServer();
        launch();
    }

    private static void startServer() {
        try {
            UnsafeSSL.disableCertificateValidation();
            LocalDownloadServer.startServer(12345);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}