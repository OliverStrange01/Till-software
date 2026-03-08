package com.till;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;

public class App extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        String fxmlPath = "/main.fxml";

        URL url = App.class.getResource(fxmlPath);

        // Debug prints – very important right now
        System.out.println("Current class loader: " + App.class.getClassLoader());
        System.out.println("Attempting FXML path: " + fxmlPath);
        System.out.println("Resolved URL: " + url);

        if (url == null) {
            throw new IOException("FXML file NOT FOUND at classpath: " + fxmlPath +
                    "\nCheck: src/main/resources/main.fxml must exist and be spelled exactly 'main.fxml'");
        }

        FXMLLoader fxmlLoader = new FXMLLoader(url);

        // Optional: set controller factory if needed later
        // fxmlLoader.setControllerFactory(c -> new MainController());

        Scene scene = new Scene(fxmlLoader.load(), 1000, 700);

        stage.setTitle("Till POS System");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}