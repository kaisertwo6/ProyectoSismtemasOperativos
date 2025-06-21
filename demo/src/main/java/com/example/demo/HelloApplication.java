package com.example.demo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class HelloApplication extends Application {

    private static Stage primaryStage;
    private static HelloController helloControllerInstance;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;

        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("inicio.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1080, 720);
        stage.setScene(scene);
        stage.show();
    }

    public static void cambiarSecenaProceso() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Scene newScene = new Scene(fxmlLoader.load(), 1080, 720);
        helloControllerInstance = fxmlLoader.getController();
        primaryStage.setScene(newScene);
    }

    public static HelloController getHelloControllerInstance() {
        return helloControllerInstance;
    }

    public static void main(String[] args) {
        launch();
    }
}