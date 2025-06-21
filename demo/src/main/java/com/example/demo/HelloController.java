package com.example.demo;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.beans.property.SimpleStringProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

public class HelloController extends Thread {

    private static HelloController instance;

    @FXML private Button botonTerminar;
    @FXML private TableColumn<String, String> colum1Procesos;
    @FXML private Label labelMemoriaRam;
    @FXML private TableView<String> tablaProcesos;

    private ObservableList<String> listaDeProcesosEnTabla;
    private Controlador controlador;

    public HelloController() {
        if (instance != null && instance != this) {
            System.err.println("ADVERTENCIA: Constructor de HelloController llamado de nuevo. Posible doble instancia.");
        }
        instance = this;
    }

    public static HelloController getInstance() {
        return instance;
    }

    @FXML
    public void initialize() {
        if (instance != null && instance != this) {
            System.err.println("ADVERTENCIA: Se está inicializando una segunda instancia de HelloController. El Singleton debería ser único.");
        }
        instance = this;

        this.controlador = Controlador.getInstance();
        this.listaDeProcesosEnTabla = FXCollections.observableArrayList();
        this.tablaProcesos.setItems(listaDeProcesosEnTabla);
        colum1Procesos.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue()));
    }

    @FXML
    void ActionInButtonTerminar(ActionEvent event) {
        if (controlador != null) {
            controlador.setRunning(false);
        }
        this.interrupt();
    }

    @Override
    public void run() {
        if (this.controlador == null) {
            System.err.println("ERROR FATAL: 'controlador' es nulo en HelloController.run() ANTES del bucle. Saliendo.");
            return;
        }
        if (this.listaDeProcesosEnTabla == null) {
            System.err.println("ERROR FATAL: 'listaDeProcesosEnTabla' es nula en HelloController.run() ANTES del bucle. Saliendo.");
            return;
        }
        if (this.tablaProcesos == null) {
            System.err.println("ERROR FATAL: 'tablaProcesos' es nula en HelloController.run() ANTES del bucle. Saliendo.");
            return;
        }

        while (controlador.getRunnin()) {
            try {
                List<String> nuevosDatos;
                synchronized (controlador.getProcesosListos()) {
                    nuevosDatos = controlador.getProcesosListos().stream()
                            .map(p -> "P" + p.getId() + " (D:" + p.getDuracion() + ")")
                            .collect(Collectors.toList());
                }

                Platform.runLater(() -> {
                    listaDeProcesosEnTabla.setAll(nuevosDatos);
                    labelMemoriaRam.setText("Tiempo actual: " + controlador.getTiempoActual() + " | Procesos en cola: " + nuevosDatos.size());
                });

                Thread.sleep(500);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
    }
}