package com.example.demo;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.CheckBox;

public class InicioController {
    @FXML private Button btnAñadir;
    @FXML private Button btnInicio;
    @FXML private Button btnRR;
    @FXML private Button btnRandom;
    @FXML private Button btnRandomGrande;
    @FXML private Button btnEscenarioExtremo;
    @FXML private Button btnProcesosConHijos;
    @FXML private Button btnProgramasGrandes;
    @FXML private Button btnEscenarioQuantumBajo;
    @FXML private Button btnSwapLimitado;
    @FXML private Button btnSwapInteligente;
    @FXML private Button btnSJF;
    @FXML private TextField txtDuracion;
    @FXML private TextField txtSlot;
    @FXML private TextField txtTiempo;
    @FXML private TextField txtQuantum;
    @FXML private TextField txtNombrePrograma;
    @FXML private Label lblEstado;

    // Controles para configurar SWAP
    @FXML private ToggleButton toggleSwapLimitado;
    @FXML private Slider sliderTamañoSwap;
    @FXML private Label lblTamañoSwap;
    @FXML private Label lblEstadoSwap;
    @FXML private CheckBox checkSwapInteligente;

    Controlador controlador;
    HelloController controladorProcesos;
    boolean RR;
    boolean SJF;

    private static final int TAMAÑO_MAXIMO_PROCESO = 100;

    @FXML
    public void initialize() {
        try {
            Controlador instanciaAnterior = Controlador.getInstance();
            if (instanciaAnterior != null) {
                instanciaAnterior.reset();
            }
        } catch (Exception e) {
            System.err.println("Error al limpiar instancia anterior: " + e.getMessage());
        }

        this.controlador = Controlador.getInstance();
        this.RR = false;
        this.SJF = false;
        this.controladorProcesos = null;

        configurarControlesSwap();
        actualizarEstilosAlgoritmos();
        configurarEstiloLabel();

        mostrarAviso("Sistema iniciado. Configura SWAP, selecciona algoritmo y agrega procesos.", "#10b981");
    }

    private void configurarEstiloLabel() {
        if (lblEstado != null) {
            lblEstado.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-family: 'Consolas Bold';");
        }
    }

    private void configurarControlesSwap() {
        // Configurar toggle por defecto (SWAP limitado activado)
        toggleSwapLimitado.setSelected(true);

        // Configurar slider (rango 256-1024 slots)
        sliderTamañoSwap.setMin(256);
        sliderTamañoSwap.setMax(1024);
        sliderTamañoSwap.setValue(512);

        // NUEVO: Configurar checkbox para SWAP inteligente
        if (checkSwapInteligente != null) {
            checkSwapInteligente.setSelected(true); // Por defecto activado
            checkSwapInteligente.selectedProperty().addListener((observable, oldValue, newValue) -> {
                actualizarConfiguracionSwap();
            });
        }

        // Listeners existentes
        sliderTamañoSwap.valueProperty().addListener((observable, oldValue, newValue) -> {
            int tamaño = newValue.intValue();
            lblTamañoSwap.setText(tamaño + " slots");
            actualizarConfiguracionSwap();
        });

        toggleSwapLimitado.selectedProperty().addListener((observable, oldValue, newValue) -> {
            actualizarConfiguracionSwap();
        });

        // Configuración inicial
        lblTamañoSwap.setText("512 slots");
        actualizarConfiguracionSwap();
    }

    private void actualizarConfiguracionSwap() {
        boolean limitado = toggleSwapLimitado.isSelected();
        int tamaño = (int) sliderTamañoSwap.getValue();
        boolean inteligente = checkSwapInteligente != null ? checkSwapInteligente.isSelected() : true;

        // Actualizar interfaz
        sliderTamañoSwap.setDisable(!limitado);
        lblTamañoSwap.setDisable(!limitado);

        if (limitado) {
            toggleSwapLimitado.setText("LIMITADO");
            toggleSwapLimitado.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-weight: bold;");
            lblEstadoSwap.setText("SWAP Limitado: " + tamaño + " slots" +
                    (inteligente ? " | Modo: INTELIGENTE" : " | Modo: BÁSICO"));
            lblEstadoSwap.setStyle("-fx-text-fill: " + (inteligente ? "#27ae60" : "#e67e22") + "; -fx-font-weight: bold;");
        } else {
            toggleSwapLimitado.setText("ILIMITADO");
            toggleSwapLimitado.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
            lblEstadoSwap.setText("SWAP Ilimitado" +
                    (inteligente ? " | Modo: INTELIGENTE" : " | Modo: BÁSICO"));
            lblEstadoSwap.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
        }

        // Configurar controlador
        if (controlador != null) {
            controlador.configurarSwap(limitado, tamaño);
            controlador.configurarSwapInteligente(inteligente);
        }
    }

    @FXML
    void crearSwapInteligente(ActionEvent event) {
        // Activar SWAP inteligente
        if (checkSwapInteligente != null) {
            checkSwapInteligente.setSelected(true);
        }

        // Crear escenario que demuestre SWAP bidireccional
        Random random = new Random();

        // Procesos grandes que van a SWAP inicialmente
        for (int i = 0; i < 3; i++) {
            int duracion = 15 + random.nextInt(10);
            int tiempoLlegada = i;
            int tamanioSlot = 200 + random.nextInt(100);

            Proceso proceso = new Proceso(duracion, tiempoLlegada, tamanioSlot);
            controlador.agregarProcesoAlSistema(proceso);
        }

        // Procesos pequeños prioritarios que llegan después
        for (int i = 3; i < 8; i++) {
            int duracion = 3 + random.nextInt(5);
            int tiempoLlegada = 5 + i;
            int tamanioSlot = 20 + random.nextInt(30);

            Proceso proceso = new Proceso(duracion, tiempoLlegada, tamanioSlot);
            controlador.agregarProcesoAlSistema(proceso);
        }

        // Procesos de tamaño medio que llegan al final
        for (int i = 8; i < 12; i++) {
            int duracion = 8 + random.nextInt(8);
            int tiempoLlegada = 12 + i;
            int tamanioSlot = 80 + random.nextInt(60);

            Proceso proceso = new Proceso(duracion, tiempoLlegada, tamanioSlot);
            controlador.agregarProcesoAlSistema(proceso);
        }

        mostrarAviso("Escenario SWAP Inteligente: Procesos grandes → pequeños prioritarios → medianos. Verás intercambio bidireccional.", "#9b59b6");
    }

    private void mostrarAviso(String mensaje, String color) {
        lblEstado.setText("✓ " + mensaje);
        lblEstado.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
    }

    private void actualizarEstilosAlgoritmos() {
        if (RR) {
            btnRR.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 15;");
            btnSJF.setStyle("-fx-background-color: #5284FF; -fx-text-fill: white; -fx-background-radius: 15;");
        } else if (SJF) {
            btnSJF.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 15;");
            btnRR.setStyle("-fx-background-color: #5284FF; -fx-text-fill: white; -fx-background-radius: 15;");
        } else {
            btnRR.setStyle("-fx-background-color: #5284FF; -fx-text-fill: white; -fx-background-radius: 15;");
            btnSJF.setStyle("-fx-background-color: #5284FF; -fx-text-fill: white; -fx-background-radius: 15;");
        }
    }

    @FXML
    void añadirProceso(ActionEvent event) {
        try {
            int duracion = Integer.parseInt(txtDuracion.getText());
            int tamanioSlot = Integer.parseInt(txtSlot.getText());
            int tiempoLlegada = Integer.parseInt(txtTiempo.getText());

            if (txtNombrePrograma.getText() != null && !txtNombrePrograma.getText().trim().isEmpty()) {
                String nombrePrograma = txtNombrePrograma.getText().trim();
                agregarPrograma(nombrePrograma, duracion, tiempoLlegada, tamanioSlot);
                mostrarAviso("Programa '" + nombrePrograma + "' agregado exitosamente.", "#27ae60");
            } else {
                Proceso procesoAux = new Proceso(duracion, tiempoLlegada, tamanioSlot);
                controlador.agregarProcesoAlSistema(procesoAux);
                mostrarAviso("Proceso individual agregado (D:" + duracion + ", L:" + tiempoLlegada + ", T:" + tamanioSlot + ").", "#27ae60");
            }

            txtDuracion.clear();
            txtSlot.clear();
            txtTiempo.clear();
            txtNombrePrograma.clear();

        } catch (NumberFormatException e) {
            mostrarAviso("Error: Introduce números válidos en todos los campos.", "#e74c3c");
        }
    }

    private void agregarPrograma(String nombrePrograma, int duracion, int tiempoLlegada, int tamanioPrograma) {
        if (tamanioPrograma <= TAMAÑO_MAXIMO_PROCESO) {
            System.out.println("📄 Programa pequeño: " + nombrePrograma + " (" + tamanioPrograma + " slots) - No se divide");

            Proceso programa = new Proceso(duracion, tiempoLlegada, tamanioPrograma);
            programa.setNombrePersonalizado(nombrePrograma);
            controlador.agregarProcesoAlSistema(programa);

        } else {
            System.out.println("📦 Programa grande: " + nombrePrograma + " (" + tamanioPrograma + " slots) - Se divide automáticamente");

            Proceso programaPadre = new Proceso(nombrePrograma, duracion, tiempoLlegada, tamanioPrograma);
            List<Proceso> procesosHijos = programaPadre.dividirPrograma(TAMAÑO_MAXIMO_PROCESO);

            for (Proceso procesoHijo : procesosHijos) {
                controlador.agregarProcesoAlSistema(procesoHijo);
            }
        }
    }

    @FXML
    void crearRandom(ActionEvent event) {
        Random random = new Random();
        int cantidadProcesos = random.nextInt(6) + 3;

        for (int i = 0; i < cantidadProcesos; i++) {
            int duracion = random.nextInt(15) + 1;
            int tiempoLlegada = random.nextInt(20);
            int tamanioSlot = random.nextInt(50) + 10;

            Proceso procesoAleatorio = new Proceso(duracion, tiempoLlegada, tamanioSlot);
            controlador.agregarProcesoAlSistema(procesoAleatorio);
        }

        mostrarAviso(cantidadProcesos + " procesos aleatorios normales creados (10-59 slots cada uno).", "#3498db");
    }

    @FXML
    void crearRandomGrande(ActionEvent event) {
        Random random = new Random();
        int cantidadProcesos = random.nextInt(8) + 7;

        for (int i = 0; i < cantidadProcesos; i++) {
            int duracion = random.nextInt(20) + 5;
            int tiempoLlegada = random.nextInt(15);

            int tamanioSlot;
            if (i < 3) {
                tamanioSlot = random.nextInt(200) + 150;
            } else if (i < 6) {
                tamanioSlot = random.nextInt(150) + 80;
            } else {
                tamanioSlot = random.nextInt(100) + 50;
            }

            Proceso procesoAleatorio = new Proceso(duracion, tiempoLlegada, tamanioSlot);
            controlador.agregarProcesoAlSistema(procesoAleatorio);
        }

        mostrarAviso(cantidadProcesos + " procesos grandes creados. Forzarán uso intensivo de SWAP.", "#e67e22");
    }

    @FXML
    void crearEscenarioExtremo(ActionEvent event) {
        for (int i = 0; i < 5; i++) {
            int duracion = 15 + i * 2;
            int tiempoLlegada = i;
            int tamanioSlot = 250 + i * 50;

            Proceso proceso = new Proceso(duracion, tiempoLlegada, tamanioSlot);
            controlador.agregarProcesoAlSistema(proceso);
        }

        for (int i = 5; i < 10; i++) {
            int duracion = 8 + i;
            int tiempoLlegada = 3 + i;
            int tamanioSlot = 80 + i * 10;

            Proceso proceso = new Proceso(duracion, tiempoLlegada, tamanioSlot);
            controlador.agregarProcesoAlSistema(proceso);
        }

        mostrarAviso("Escenario extremo creado: 10 procesos para swapping intensivo.", "#e74c3c");
    }

    @FXML
    void crearProgramasGrandes(ActionEvent event) {
        String[] nombresPrograma = {"Word", "Chrome", "Photoshop", "LeagueOfLegends"};
        Random random = new Random();

        for (int i = 0; i < nombresPrograma.length; i++) {
            int duracion = 12 + random.nextInt(8);
            int tiempoLlegada = i * 3;
            int tamanioPrograma = 250 + random.nextInt(300);

            agregarPrograma(nombresPrograma[i], duracion, tiempoLlegada, tamanioPrograma);
        }

        mostrarAviso("4 programas grandes creados. Se dividirán automáticamente en procesos pequeños.", "#2c3e50");
    }

    @FXML
    void crearEscenarioQuantumBajo(ActionEvent event) {
        if (txtQuantum.getText().isEmpty()) {
            txtQuantum.setText("2");
        }

        Random random = new Random();

        for (int i = 0; i < 8; i++) {
            int duracion = 10 + random.nextInt(6);
            int tiempoLlegada = random.nextInt(5);
            int tamanioSlot = 30 + random.nextInt(20);

            Proceso proceso = new Proceso(duracion, tiempoLlegada, tamanioSlot);
            controlador.agregarProcesoAlSistema(proceso);
        }

        mostrarAviso("8 procesos largos creados. Usa quantum=2 para ver muchos cambios de contexto.", "#16a085");
    }

    @FXML
    void crearSwapLimitado(ActionEvent event) {
        Random random = new Random();
        int memoriaTotal = 0;

        for (int i = 0; i < 15; i++) {
            int duracion = 8 + random.nextInt(12);
            int tiempoLlegada = i;

            int tamanioSlot;
            if (i < 5) {
                tamanioSlot = 200;
            } else if (i < 10) {
                tamanioSlot = 100;
            } else {
                tamanioSlot = 80;
            }

            memoriaTotal += tamanioSlot;

            Proceso proceso = new Proceso(duracion, tiempoLlegada, tamanioSlot);
            controlador.agregarProcesoAlSistema(proceso);
        }

        if (toggleSwapLimitado.isSelected()) {
            int tamanioSwap = (int) sliderTamañoSwap.getValue();
            int totalDisponible = 1024 + tamanioSwap;
            mostrarAviso("15 procesos creados (" + memoriaTotal + " slots). Disponible=" + totalDisponible + ", exceso irá a cola.", "#8e44ad");
        } else {
            mostrarAviso("15 procesos creados (" + memoriaTotal + " slots). SWAP ilimitado se expandirá automáticamente.", "#27ae60");
        }
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
            mostrarAviso("ERROR: Debes seleccionar un algoritmo (Round Robin o SJF).", "#e74c3c");
            return;
        }

        if (controlador.getProcesosPendientesDeLlegada().isEmpty()) {
            mostrarAviso("ERROR: Agrega al menos un proceso antes de iniciar la simulación.", "#e74c3c");
            return;
        }

        controlador.setAlgoritmo(tipo);

        HelloApplication.cambiarSecenaProceso();
        controladorProcesos = HelloApplication.getHelloControllerInstance();

        controlador.setRunning(true);
        if (!controlador.isAlive()) {
            controlador.start();
        }

        if (controladorProcesos != null && !controladorProcesos.isAlive()) {
            controladorProcesos.start();
        }
    }

    @FXML
    void seleccionarRR(ActionEvent event) {
        this.RR = true;
        this.SJF = false;
        int valor_Quantum = 4;
        try{
            valor_Quantum = Integer.parseInt(txtQuantum.getText());
            if(valor_Quantum < 2){
                valor_Quantum = 2;
            }
        }catch(Exception e) {
            valor_Quantum = 4;
        }
        controlador.setQuantum(valor_Quantum);
        actualizarEstilosAlgoritmos();
        mostrarAviso("Algoritmo Round Robin seleccionado con quantum=" + valor_Quantum + ".", "#2980b9");
    }

    @FXML
    void seleccionarSJF(ActionEvent event) {
        this.RR = false;
        this.SJF = true;
        actualizarEstilosAlgoritmos();
        mostrarAviso("Algoritmo Shortest Job First (SJF) seleccionado.", "#2980b9");
    }
}