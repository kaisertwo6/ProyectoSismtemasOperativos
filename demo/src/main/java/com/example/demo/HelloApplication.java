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
        Scene scene = new Scene(fxmlLoader.load());

        // Configurar la ventana para que inicie maximizada
        stage.setTitle("Simulador de Procesos - Terminal Mode");
        stage.setScene(scene);
        stage.setMaximized(true); // Esta línea hace que inicie maximizada
        stage.show();
    }

    // Cambiar a pantalla de simulación
    public static void cambiarSecenaProceso() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Scene newScene = new Scene(fxmlLoader.load());
        helloControllerInstance = fxmlLoader.getController();
        primaryStage.setTitle("Simulador de Procesos - Ejecutando");
        primaryStage.setScene(newScene);
        primaryStage.setMaximized(true); // Mantener maximizada
    }

    // Cambiar a pantalla de estadísticas
    public static void cambiarEscenaEstadisticas() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("estadisticas.fxml"));
        Scene newScene = new Scene(fxmlLoader.load());
        helloControllerInstance = fxmlLoader.getController();
        primaryStage.setTitle("Simulador de Procesos - Estadísticas");
        primaryStage.setScene(newScene);
        primaryStage.setMaximized(true); // Mantener maximizada
    }

    // Cambiar a pantalla de inicio
    public static void cambiarEscenaInicio() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("inicio.fxml"));
        Scene newScene = new Scene(fxmlLoader.load());
        primaryStage.setTitle("Simulador de Procesos - Terminal Mode");
        primaryStage.setScene(newScene);
        primaryStage.setMaximized(true); // Mantener maximizada

        helloControllerInstance = null;
    }

    public static HelloController getHelloControllerInstance() {
        return helloControllerInstance;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch();
    }
}