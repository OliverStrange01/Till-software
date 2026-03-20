package com.till;

import com.till.database.DatabaseConnection;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("/main.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1200, 800);
        stage.setTitle("Till POS System");
        stage.setScene(scene);
        stage.show();
    }

    // ONLY close here — when the app is fully shutting down
    @Override
    public void stop() {
        DatabaseConnection.close();
        System.out.println("App stopped – DB connection closed");
    }

    public static void main(String[] args) {
        launch();
    }
}