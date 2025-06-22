package com.example.demo;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.util.Random;

public class InicioController {
    @FXML private Button btnAñadir;
    @FXML private Button btnInicio;
    @FXML private Button btnRR;
    @FXML private Button btnRandom;
    @FXML private Button btnSJF;
    @FXML private TextField txtDuracion;
    @FXML private TextField txtSlot;
    @FXML private TextField txtTiempo;

    Controlador controlador;
    HelloController controladorProcesos;

    boolean RR;
    boolean SJF;

    @FXML
    public void initialize() {
        this.controlador = Controlador.getInstance();
    }

    @FXML
    void añadirProceso(ActionEvent event) {
        try {
            int duracion = Integer.parseInt(txtDuracion.getText());
            int tamanioSlot = Integer.parseInt(txtSlot.getText());
            int tiempoLlegada = Integer.parseInt(txtTiempo.getText());

            Proceso procesoAux = new Proceso(duracion, tiempoLlegada, tamanioSlot);
            controlador.agregarProcesoAlSistema(procesoAux);
        } catch (NumberFormatException e) {
            System.err.println("Error: Asegúrate de introducir números válidos en los campos de texto.");
        }
    }

    @FXML
    void crearRandom(ActionEvent event) {
        Random random = new Random();
        int cantidadProcesos = random.nextInt(6) + 3; // 3-8 procesos

        System.out.println("=== GENERANDO " + cantidadProcesos + " PROCESOS ALEATORIOS ===");

        for (int i = 0; i < cantidadProcesos; i++) {
            int duracion = random.nextInt(15) + 1;        // 1-15 segundos
            int tiempoLlegada = random.nextInt(20);       // 0-19 segundos
            int tamanioSlot = random.nextInt(50) + 10;    // 10-59 slots

            Proceso procesoAleatorio = new Proceso(duracion, tiempoLlegada, tamanioSlot);
            controlador.agregarProcesoAlSistema(procesoAleatorio);

            System.out.println("Proceso generado: " + procesoAleatorio.getId() +
                    " - Duración: " + duracion +
                    ", Llegada: " + tiempoLlegada +
                    ", Tamaño: " + tamanioSlot);
        }

        System.out.println("=== " + cantidadProcesos + " PROCESOS ALEATORIOS CREADOS ===");
    }

    @FXML
    void iniciarSimulacion(ActionEvent event) throws IOException {
        TipoAlgoritmo tipo = null;
        if (RR) {
            tipo = TipoAlgoritmo.RR;
        } else if (SJF) {
            tipo = TipoAlgoritmo.SJF;
        }

        if (tipo == null) {
            System.err.println("Error: Debes seleccionar un algoritmo (RR o SJF) antes de iniciar la simulación.");
            return;
        }
        controlador.setAlgoritmo(tipo);

        HelloApplication.cambiarSecenaProceso();
        controladorProcesos = HelloApplication.getHelloControllerInstance();

        controlador.setRunning(true);
        if (!controlador.isAlive()) {
            controlador.start();
        } else {
            System.out.println("El hilo del Controlador de simulación ya está corriendo.");
        }

        if (controladorProcesos != null && !controladorProcesos.isAlive()) {
            controladorProcesos.start();
        } else if (controladorProcesos == null) {
            System.err.println("Error: La instancia de HelloController es nula después de cambiar la escena.");
        } else {
            System.out.println("El hilo de HelloController ya está corriendo.");
        }
    }

    @FXML
    void seleccionarRR(ActionEvent event) {
        this.RR = true;
        this.SJF = false;
        System.out.println("Algoritmo seleccionado: Round Robin");
        controlador.setQuantum(4);
    }

    @FXML
    void seleccionarSJF(ActionEvent event) {
        this.RR = false;
        this.SJF = true;
        System.out.println("Algoritmo seleccionado: Shortest Job First");
    }
}