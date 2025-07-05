package com.example.demo;

import java.io.IOException;
import java.util.Random;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

public class InicioController {
    @FXML private Button btnAñadir;
    @FXML private Button btnInicio;
    @FXML private Button btnRR;
    @FXML private Button btnRandom;
    @FXML private Button btnRandomGrande;
    @FXML private Button btnEscenarioExtremo;
    @FXML private Button btnProcesosConHijos; // NUEVO BOTÓN
    @FXML private Button btnSJF;
    @FXML private TextField txtDuracion;
    @FXML private TextField txtSlot;
    @FXML private TextField txtTiempo;
    @FXML private TextField txtQuantum;

    Controlador controlador;
    HelloController controladorProcesos;

    boolean RR;
    boolean SJF;

    @FXML
    public void initialize() {
        // Limpiar instancia anterior del controlador
        try {
            Controlador instanciaAnterior = Controlador.getInstance();
            if (instanciaAnterior != null) {
                System.out.println("Limpiando instancia anterior del controlador...");
                instanciaAnterior.reset();
            }
        } catch (Exception e) {
            System.err.println("Error al limpiar instancia anterior: " + e.getMessage());
        }

        this.controlador = Controlador.getInstance();

        // Resetear variables de selección de algoritmo
        this.RR = false;
        this.SJF = false;
        this.controladorProcesos = null;

        System.out.println("InicioController inicializado correctamente");
    }

    // Añadir proceso individual
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

    // Crear procesos aleatorios normales
    @FXML
    void crearRandom(ActionEvent event) {
        Random random = new Random();
        int cantidadProcesos = random.nextInt(6) + 3; // 3-8 procesos

        System.out.println("=== GENERANDO " + cantidadProcesos + " PROCESOS ALEATORIOS PEQUEÑOS ===");

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

        System.out.println("=== " + cantidadProcesos + " PROCESOS ALEATORIOS PEQUEÑOS CREADOS ===");
    }

    // Crear procesos grandes para forzar SWAP
    @FXML
    void crearRandomGrande(ActionEvent event) {
        Random random = new Random();
        int cantidadProcesos = random.nextInt(8) + 7; // 7-14 procesos

        System.out.println("=== GENERANDO " + cantidadProcesos + " PROCESOS ALEATORIOS GRANDES (PARA SWAP) ===");

        for (int i = 0; i < cantidadProcesos; i++) {
            int duracion = random.nextInt(20) + 5;        // 5-24 segundos
            int tiempoLlegada = random.nextInt(15);       // 0-14 segundos

            // Tamaños grandes para forzar swapping
            int tamanioSlot;
            if (i < 3) {
                tamanioSlot = random.nextInt(200) + 150;  // 150-349 slots
            } else if (i < 6) {
                tamanioSlot = random.nextInt(150) + 80;   // 80-229 slots
            } else {
                tamanioSlot = random.nextInt(100) + 50;   // 50-149 slots
            }

            Proceso procesoAleatorio = new Proceso(duracion, tiempoLlegada, tamanioSlot);
            controlador.agregarProcesoAlSistema(procesoAleatorio);

            System.out.println("Proceso GRANDE generado: " + procesoAleatorio.getId() +
                    " - Duración: " + duracion +
                    ", Llegada: " + tiempoLlegada +
                    ", Tamaño: " + tamanioSlot + " slots");
        }

        System.out.println("=== " + cantidadProcesos + " PROCESOS GRANDES CREADOS ===");
        System.out.println("RAM disponible: 1024 slots");

        int memoriaTotal = controlador.getProcesosPendientesDeLlegada().stream()
                .mapToInt(Proceso::getTamanioSlot)
                .sum();

        System.out.println("Memoria total requerida: " + memoriaTotal + " slots");
        System.out.println("Exceso de memoria: " + (memoriaTotal - 1024) + " slots -> Irán a SWAP");
    }

    // Crear escenario extremo para swapping intensivo
    @FXML
    void crearEscenarioExtremo(ActionEvent event) {
        System.out.println("=== CREANDO ESCENARIO EXTREMO DE SWAPPING ===");

        // Procesos que lleguen casi al mismo tiempo y sean muy grandes
        for (int i = 0; i < 5; i++) {
            int duracion = 15 + i * 2;                   // 15, 17, 19, 21, 23 segundos
            int tiempoLlegada = i;                       // 0, 1, 2, 3, 4 segundos
            int tamanioSlot = 250 + i * 50;              // 250, 300, 350, 400, 450 slots

            Proceso proceso = new Proceso(duracion, tiempoLlegada, tamanioSlot);
            controlador.agregarProcesoAlSistema(proceso);

            System.out.println("Proceso EXTREMO: " + proceso.getId() +
                    " - Duración: " + duracion + ", Llegada: " + tiempoLlegada +
                    ", Tamaño: " + tamanioSlot + " slots");
        }

        // Procesos más pequeños que compitan por memoria
        for (int i = 5; i < 10; i++) {
            int duracion = 8 + i;                        // 13, 14, 15, 16, 17 segundos
            int tiempoLlegada = 3 + i;                   // 8, 9, 10, 11, 12 segundos
            int tamanioSlot = 80 + i * 10;               // 130, 140, 150, 160, 170 slots

            Proceso proceso = new Proceso(duracion, tiempoLlegada, tamanioSlot);
            controlador.agregarProcesoAlSistema(proceso);

            System.out.println("Proceso COMPETIDOR: " + proceso.getId() +
                    " - Duración: " + duracion + ", Llegada: " + tiempoLlegada +
                    ", Tamaño: " + tamanioSlot + " slots");
        }

        System.out.println("=== ESCENARIO EXTREMO CREADO ===");
        System.out.println("Total memoria requerida: ~2750 slots para RAM de 1024 slots");
        System.out.println("Resultado esperado: Swapping intensivo y competencia por memoria");
    }
    
    // NUEVA FUNCIONALIDAD: Crear procesos que pueden generar hijos
    @FXML
    void crearProcesosConHijos(ActionEvent event) {
        System.out.println("=== CREANDO PROCESOS CON CAPACIDAD DE TENER HIJOS ===");

        // Crear 3-4 procesos padre que pueden generar hijos
        Random random = new Random();
        int cantidadProcesos = 3 + random.nextInt(2); // 3-4 procesos

        for (int i = 0; i < cantidadProcesos; i++) {
            int duracion = 8 + random.nextInt(7);      // 8-14 segundos (duración suficiente para crear hijos)
            int tiempoLlegada = i * 2;                 // 0, 2, 4, 6 segundos (escalonado)
            int tamanioSlot = 40 + random.nextInt(40); // 40-79 slots (tamaño moderado)

            Proceso procesoPadre = new Proceso(duracion, tiempoLlegada, tamanioSlot);
            
            // HABILITAR la capacidad de crear hijos
            procesoPadre.habilitarCreacionHijos();
            
            controlador.agregarProcesoAlSistema(procesoPadre);

            System.out.println("Proceso PADRE creado: " + procesoPadre.getId() +
                    " - Duración: " + duracion + ", Llegada: " + tiempoLlegada +
                    ", Tamaño: " + tamanioSlot + " slots [PUEDE CREAR HIJOS]");
        }

        System.out.println("=== PROCESOS CON CAPACIDAD DE HIJOS CREADOS ===");
        System.out.println("Durante la ejecución, estos procesos tendrán una probabilidad de crear procesos hijos (FORK)");
        System.out.println("Los procesos hijos aparecerán con nombres como 'Proceso X.H1', 'Proceso X.H2', etc.");
    }

    // Iniciar simulación
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

    // Seleccionar algoritmo Round Robin
    @FXML
    void seleccionarRR(ActionEvent event) {
        this.RR = true;
        this.SJF = false;
        System.out.println("Algoritmo seleccionado: Round Robin");
        int valor_Quantum = 4;
        try{
            valor_Quantum = Integer.parseInt(txtQuantum.getText());
            if(valor_Quantum < 4){
                System.out.println("Este valor no es valido para el Quantum, se ingresa el valor predeterminado");
                valor_Quantum = 4;
            }
        }catch(Exception e){
            System.out.println("Lo que se ingreso no es valido como valor para el Quantum");
        }
        controlador.setQuantum(valor_Quantum);
    }

    // Seleccionar algoritmo Shortest Job First
    @FXML
    void seleccionarSJF(ActionEvent event) {
        this.RR = false;
        this.SJF = true;
        System.out.println("Algoritmo seleccionado: Shortest Job First");
    }
}