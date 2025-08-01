module com.panda.flashlocaldownloadserver {
    requires javafx.controls;
    requires javafx.fxml;
    requires jdk.httpserver;
    requires com.google.gson;
    requires com.fasterxml.jackson.databind;


    opens com.panda.flashlocaldownloadserver to javafx.fxml;
    exports com.panda.flashlocaldownloadserver;
    exports com.panda.flashlocaldownloadserver.controllers;
    opens com.panda.flashlocaldownloadserver.controllers to javafx.fxml;
}