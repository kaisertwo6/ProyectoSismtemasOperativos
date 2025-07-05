package com.example.demo;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.scene.layout.HBox;
import javafx.scene.layout.AnchorPane;
import javafx.geometry.Pos;
import javafx.stage.Stage;
import java.util.stream.Collectors;

import java.io.IOException;
import java.util.*;

public class HelloController extends Thread {

    private static HelloController instance;

    @FXML private Button botonTerminar;
    @FXML private Button botonPausar;
    @FXML private Label labelTiempoActual;
    @FXML private Label labelQuantumValor;
    @FXML private AnchorPane panelQuantum;

    // Botones de velocidad
    @FXML private Button btnVelocidadX1;
    @FXML private Button btnVelocidadX2;
    @FXML private Button btnVelocidadX5;
    @FXML private Button btnVelocidadX10;

    // Tablas de la interfaz
    @FXML private TableView<CoreInfo> tablaNucleos;
    @FXML private TableColumn<CoreInfo, String> columnaCoreId;
    @FXML private TableColumn<CoreInfo, String> columnaProceso;
    @FXML private TableColumn<CoreInfo, String> columnaEstado;

    @FXML private TableView<MemoriaInfo> tablaMemoriaRAM;
    @FXML private TableColumn<MemoriaInfo, Integer> columnaDireccion;
    @FXML private TableColumn<MemoriaInfo, String> columnaProcesoId;
    @FXML private TableColumn<MemoriaInfo, String> columnaEstadoMemoria;
    @FXML private TableColumn<MemoriaInfo, String> columnaFragmentacion;

    @FXML private TableView<ProcesoInfo> tablaProcesos;
    @FXML private TableColumn<ProcesoInfo, String> columnaProcesoInfo;
    @FXML private TableColumn<ProcesoInfo, String> columnaEstadoProceso;
    @FXML private TableColumn<ProcesoInfo, Button> columnaAcciones;

    @FXML private TableView<SwapInfo> tablaSwap;
    @FXML private TableColumn<SwapInfo, String> columnaProcesoSwap;
    @FXML private TableColumn<SwapInfo, String> columnaDireccionesSwap;

    // Listas observables para las tablas
    private ObservableList<CoreInfo> listaNucleos;
    private ObservableList<MemoriaInfo> listaMemoria;
    private ObservableList<ProcesoInfo> listaProcesos;
    private ObservableList<SwapInfo> listaSwap;

    private Set<String> procesosConocidos;
    private Controlador controlador;
    private boolean pausado = false;
    private int velocidadSimulacion = 1000;

    public HelloController() {
        if (instance != null && instance != this) {
            System.err.println("ADVERTENCIA: Constructor de HelloController llamado de nuevo. Posible doble instancia.");
        }
        instance = this;
        procesosConocidos = new HashSet<>();
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
        inicializarTablas();
        configurarTablas();
        inicializarInterfaz();
        configurarBotonesVelocidad();
    }

    private void configurarBotonesVelocidad() {
        btnVelocidadX1.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");
        btnVelocidadX2.setStyle("-fx-background-color: #ecf0f1; -fx-text-fill: #2c3e50;");
        btnVelocidadX5.setStyle("-fx-background-color: #ecf0f1; -fx-text-fill: #2c3e50;");
        btnVelocidadX10.setStyle("-fx-background-color: #ecf0f1; -fx-text-fill: #2c3e50;");
    }

    @FXML
    void cambiarVelocidadX1(ActionEvent event) {
        cambiarVelocidad(1000, btnVelocidadX1);
    }

    @FXML
    void cambiarVelocidadX2(ActionEvent event) {
        cambiarVelocidad(500, btnVelocidadX2);
    }

    @FXML
    void cambiarVelocidadX5(ActionEvent event) {
        cambiarVelocidad(200, btnVelocidadX5);
    }

    @FXML
    void cambiarVelocidadX10(ActionEvent event) {
        cambiarVelocidad(100, btnVelocidadX10);
    }

    private void cambiarVelocidad(int nuevaVelocidad, Button botonActivo) {
        this.velocidadSimulacion = nuevaVelocidad;

        if (controlador != null) {
            controlador.setVelocidadSimulacion(nuevaVelocidad);
        }

        // Resetear estilos de botones
        btnVelocidadX1.setStyle("-fx-background-color: #ecf0f1; -fx-text-fill: #2c3e50;");
        btnVelocidadX2.setStyle("-fx-background-color: #ecf0f1; -fx-text-fill: #2c3e50;");
        btnVelocidadX5.setStyle("-fx-background-color: #ecf0f1; -fx-text-fill: #2c3e50;");
        btnVelocidadX10.setStyle("-fx-background-color: #ecf0f1; -fx-text-fill: #2c3e50;");

        // Activar botón seleccionado
        botonActivo.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");

        String velocidadTexto = "";
        switch (nuevaVelocidad) {
            case 1000: velocidadTexto = "1x"; break;
            case 500: velocidadTexto = "2x"; break;
            case 200: velocidadTexto = "5x"; break;
            case 100: velocidadTexto = "10x"; break;
        }
        System.out.println("Velocidad cambiada a: " + velocidadTexto);
    }

    private void inicializarTablas() {
        listaNucleos = FXCollections.observableArrayList();
        listaMemoria = FXCollections.observableArrayList();
        listaProcesos = FXCollections.observableArrayList();
        listaSwap = FXCollections.observableArrayList();

        tablaNucleos.setItems(listaNucleos);
        tablaMemoriaRAM.setItems(listaMemoria);
        tablaProcesos.setItems(listaProcesos);
        tablaSwap.setItems(listaSwap);
    }

    private void configurarTablas() {
        // Configurar tabla de núcleos
        columnaCoreId.setCellValueFactory(new PropertyValueFactory<>("coreId"));
        columnaProceso.setCellValueFactory(new PropertyValueFactory<>("procesoId"));
        columnaEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        // Configurar tabla de memoria RAM
        columnaDireccion.setCellValueFactory(new PropertyValueFactory<>("direccion"));
        columnaProcesoId.setCellValueFactory(new PropertyValueFactory<>("procesoId"));
        columnaEstadoMemoria.setCellValueFactory(new PropertyValueFactory<>("estado"));
        columnaFragmentacion.setCellValueFactory(new PropertyValueFactory<>("fragmentacion"));

        // Configurar tabla de procesos
        columnaProcesoInfo.setCellValueFactory(new PropertyValueFactory<>("descripcion"));

        // Configurar columna de estado con círculos de colores
        columnaEstadoProceso.setCellFactory(column -> {
            return new TableCell<ProcesoInfo, String>() {
                private final Circle circulo = new Circle(6);
                private final Label labelEstado = new Label();
                private final HBox contenedor = new HBox(8);

                {
                    contenedor.setAlignment(Pos.CENTER_LEFT);
                    labelEstado.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
                    contenedor.getChildren().addAll(circulo, labelEstado);
                }

                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setGraphic(null);
                    } else {
                        ProcesoInfo procesoInfo = getTableRow().getItem();
                        EstadoProceso estado = procesoInfo.getProceso().getEstado();

                        switch (estado) {
                            case PENDIENTE:
                                circulo.setFill(Color.web("#95a5a6"));
                                labelEstado.setText("Pendiente");
                                labelEstado.setTextFill(Color.web("#95a5a6"));
                                break;
                            case ESPERA:
                                circulo.setFill(Color.web("#3498db"));
                                labelEstado.setText("En RAM");
                                labelEstado.setTextFill(Color.web("#3498db"));
                                break;
                            case SWAP:
                                circulo.setFill(Color.web("#f39c12"));
                                labelEstado.setText("En Swap");
                                labelEstado.setTextFill(Color.web("#f39c12"));
                                break;
                            case EJECUCION:
                                circulo.setFill(Color.web("#2ecc71"));
                                labelEstado.setText("Ejecutando");
                                labelEstado.setTextFill(Color.web("#2ecc71"));
                                break;
                            case TERMINADO:
                                circulo.setFill(Color.web("#e74c3c"));
                                labelEstado.setText("Terminado");
                                labelEstado.setTextFill(Color.web("#e74c3c"));
                                break;
                            default:
                                circulo.setFill(Color.web("#95a5a6"));
                                labelEstado.setText("Desconocido");
                                labelEstado.setTextFill(Color.web("#95a5a6"));
                                break;
                        }

                        setGraphic(contenedor);
                    }
                }
            };
        });

        // Configurar columna de botones "Ver"
        columnaAcciones.setCellFactory(column -> {
            return new TableCell<ProcesoInfo, Button>() {
                private final Button btn = new Button("Ver");

                {
                    btn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 12px;");
                    btn.setOnAction(event -> {
                        ProcesoInfo procesoInfo = getTableView().getItems().get(getIndex());
                        mostrarDetallesProceso(procesoInfo.getProceso());
                    });
                }

                @Override
                protected void updateItem(Button item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(btn);
                    }
                }
            };
        });

        // Configurar tabla de swap
        columnaProcesoSwap.setCellValueFactory(new PropertyValueFactory<>("procesoId"));
        columnaDireccionesSwap.setCellValueFactory(new PropertyValueFactory<>("direcciones"));
    }

    private void inicializarInterfaz() {
        listaNucleos.add(new CoreInfo("Core 1", "Libre", "Disponible"));
        listaNucleos.add(new CoreInfo("Core 2", "Libre", "Disponible"));
        listaNucleos.add(new CoreInfo("Core 3", "Libre", "Disponible"));
        listaNucleos.add(new CoreInfo("Core 4", "Libre", "Disponible"));
        inicializarMemoriaRAM(1024);
        configurarPanelQuantum();
        actualizarInformacionGeneral();
    }

    private void configurarPanelQuantum() {
        if (controlador != null && controlador.algoritmo == TipoAlgoritmo.RR) {
            panelQuantum.setVisible(true);
            labelQuantumValor.setText("Valor: " + controlador.quantum);
        } else {
            panelQuantum.setVisible(false);
        }
    }

    private void inicializarMemoriaRAM(int cantidadSlots) {
        listaMemoria.clear();
        for (int i = 0; i < cantidadSlots; i++) {
            listaMemoria.add(new MemoriaInfo(i, "Libre", "Disponible", "No"));
        }
    }

    public void configurarTamanioRAM(int cantidadSlots) {
        Platform.runLater(() -> {
            inicializarMemoriaRAM(cantidadSlots);
        });
    }

    // Actualizar vista de memoria RAM
    public void actualizarMemoriaRAM() {
        if (controlador == null) return;

        Platform.runLater(() -> {
            List<Integer> ram = controlador.ram;
            Map<Integer, String> memoriaAProceso = controlador.memoriaAProceso;

            Map<String, List<Integer>> procesosBloques = new HashMap<>();
            Map<String, EstadoProceso> estadosProcesos = new HashMap<>();

            // Obtener estados de procesos activos usando el ID completo
            for (Core core : controlador.cores) {
                if (core.isOcupado() && core.getProcesoEjecucion() != null) {
                    String procesoId = core.getProcesoEjecucion().getId();
                    estadosProcesos.put(procesoId, EstadoProceso.EJECUCION);
                }
            }

            // Procesos en RAM (cola de listos)
            for (Proceso proceso : controlador.getProcesosListos()) {
                String procesoId = proceso.getId();
                estadosProcesos.put(procesoId, EstadoProceso.ESPERA);
            }

            // Usar el nuevo mapeo de memoria a proceso
            for (int i = 0; i < ram.size(); i++) {
                if (memoriaAProceso.containsKey(i)) {
                    String procesoId = memoriaAProceso.get(i);
                    procesosBloques.computeIfAbsent(procesoId, k -> new ArrayList<>()).add(i);
                }
            }

            listaMemoria.clear();

            // Mostrar procesos activos como rangos
            for (Map.Entry<String, List<Integer>> entry : procesosBloques.entrySet()) {
                List<Integer> direcciones = entry.getValue();
                if (!direcciones.isEmpty()) {
                    String procesoId = entry.getKey();
                    int inicio = direcciones.get(0);
                    int fin = direcciones.get(direcciones.size() - 1);
                    String rango = (inicio == fin) ? String.valueOf(inicio) : inicio + "-" + fin;

                    EstadoProceso estadoProceso = estadosProcesos.getOrDefault(procesoId, EstadoProceso.ESPERA);
                    String estadoTexto;
                    switch (estadoProceso) {
                        case EJECUCION:
                            estadoTexto = "Ejecutando";
                            break;
                        case ESPERA:
                            estadoTexto = "En espera";
                            break;
                        default:
                            estadoTexto = "Ocupado";
                    }

                    // Mostrar ID completo para procesos hijos (ej: "Proceso 1.H1")
                    String displayId = procesoId.contains(".H") ? procesoId : procesoId.replace("Proceso ", "P");

                    listaMemoria.add(new MemoriaInfo(
                            inicio,
                            displayId,
                            estadoTexto,
                            "Rango: " + rango + " (" + direcciones.size() + " slots)"
                    ));
                }
            }

            // Mostrar memoria libre total
            int memoriaLibre = Collections.frequency(ram, 0);
            if (memoriaLibre > 0) {
                listaMemoria.add(new MemoriaInfo(
                        -1,
                        "LIBRE",
                        "Disponible",
                        memoriaLibre + " slots libres"
                ));
            }

            tablaMemoriaRAM.refresh();
        });
    }

    // Actualizar estado de los núcleos
    public void actualizarNucleos() {
        if (controlador == null) return;

        Platform.runLater(() -> {
            List<Core> cores = controlador.cores;

            for (int i = 0; i < cores.size() && i < listaNucleos.size(); i++) {
                Core core = cores.get(i);
                CoreInfo info = listaNucleos.get(i);

                if (core.isOcupado() && core.getProcesoEjecucion() != null) {
                    info.setProcesoId(core.getProcesoEjecucion().getId());
                    info.setEstado("Ejecutando");
                } else {
                    info.setProcesoId("Libre");
                    info.setEstado("Disponible");
                }
            }

            tablaNucleos.refresh();
        });
    }

    // Actualizar cola de procesos
    public void actualizarColaDeprocesos() {
        if (controlador == null) return;

        Platform.runLater(() -> {
            List<ProcesoInfo> procesosEjecutandose = new ArrayList<>();
            List<ProcesoInfo> procesosEnRamEspera = new ArrayList<>();
            List<ProcesoInfo> procesosEnSwap = new ArrayList<>();
            List<ProcesoInfo> procesosPendientes = new ArrayList<>();
            List<ProcesoInfo> procesosTerminados = new ArrayList<>();

            Map<String, ProcesoInfo> procesosEnTabla = new HashMap<>();
            for (ProcesoInfo procesoInfo : listaProcesos) {
                procesosEnTabla.put(procesoInfo.getProceso().getId(), procesoInfo);
            }

            // OBTENER TIEMPO ACTUAL PARA VERIFICAR
            int tiempoActual = controlador.getTiempoActual();
            System.out.println("=== ACTUALIZANDO ESTADOS - Tiempo actual: " + tiempoActual + " ===");

            // 1. Procesos EJECUTANDO
            for (Core core : controlador.cores) {
                if (core.isOcupado() && core.getProcesoEjecucion() != null) {
                    Proceso procesoEjecutandose = core.getProcesoEjecucion();
                    procesosConocidos.add(procesoEjecutandose.getId());

                    String descripcionLimpia = procesoEjecutandose.getId() + " (D:" +
                            procesoEjecutandose.getDuracion() + ", T:" + procesoEjecutandose.getTamanioSlot() + ")";

                    if (procesosEnTabla.containsKey(procesoEjecutandose.getId())) {
                        ProcesoInfo procesoInfo = procesosEnTabla.get(procesoEjecutandose.getId());
                        procesoInfo.setProceso(procesoEjecutandose);
                        procesoInfo.setDescripcion(descripcionLimpia);
                        procesosEjecutandose.add(procesoInfo);
                    } else {
                        ProcesoInfo info = new ProcesoInfo(procesoEjecutandose);
                        info.setDescripcion(descripcionLimpia);
                        procesosEjecutandose.add(info);
                    }
                }
            }

            // 2. TODOS LOS DEMÁS PROCESOS - Clasificar correctamente por tiempo de llegada
            Set<String> procesosYaEjecutando = procesosEjecutandose.stream()
                    .map(info -> info.getProceso().getId())
                    .collect(Collectors.toSet());

            // Obtener todos los procesos del sistema
            Set<Proceso> todosProcesos = new HashSet<>();
            todosProcesos.addAll(controlador.getProcesosPendientesDeLlegada());
            todosProcesos.addAll(controlador.getProcesosListos());
            todosProcesos.addAll(controlador.getProcesosEnSwap());
            todosProcesos.addAll(controlador.getProcesosTerminados());

            for (Proceso proceso : todosProcesos) {
                // Saltar si ya está ejecutando
                if (procesosYaEjecutando.contains(proceso.getId())) {
                    continue;
                }

                procesosConocidos.add(proceso.getId());

                // VERIFICAR TIEMPO DE LLEGADA VS TIEMPO ACTUAL
                boolean yaLlego = proceso.getTiempoDeLlegada() <= tiempoActual;

                System.out.println("Proceso " + proceso.getId() + " - Llegada: " + proceso.getTiempoDeLlegada() +
                        ", Actual: " + tiempoActual + ", Ya llegó: " + yaLlego + ", Estado: " + proceso.getEstado());

                ProcesoInfo info;
                String descripcionLimpia;

                if (proceso.getEstado() == EstadoProceso.TERMINADO) {
                    // TERMINADO
                    descripcionLimpia = proceso.getId() + " (TE:" +
                            proceso.getTiempoDeEspera() + ", TR:" + proceso.getTiempoDeRetorno() + ")";

                    if (procesosEnTabla.containsKey(proceso.getId())) {
                        info = procesosEnTabla.get(proceso.getId());
                        info.setProceso(proceso);
                        info.setDescripcion(descripcionLimpia);
                    } else {
                        info = new ProcesoInfo(proceso);
                        info.setDescripcion(descripcionLimpia);
                    }
                    procesosTerminados.add(info);

                } else if (!yaLlego) {
                    // NO HA LLEGADO AÚN - FORZAR ESTADO PENDIENTE
                    descripcionLimpia = proceso.getId() + " (D:" +
                            proceso.getDuracion() + ", L:" + proceso.getTiempoDeLlegada() +
                            ", T:" + proceso.getTamanioSlot() + ")";

                    if (procesosEnTabla.containsKey(proceso.getId())) {
                        info = procesosEnTabla.get(proceso.getId());
                        info.setProceso(proceso);
                        info.setDescripcion(descripcionLimpia);
                    } else {
                        info = new ProcesoInfo(proceso);
                        info.setDescripcion(descripcionLimpia);
                    }

                    // FORZAR ESTADO PENDIENTE TEMPORALMENTE PARA LA VISTA
                    proceso.setEstado(EstadoProceso.PENDIENTE);
                    procesosPendientes.add(info);
                    System.out.println("  → CLASIFICADO COMO PENDIENTE");

                } else {
                    // YA LLEGÓ - Verificar dónde está realmente
                    if (controlador.getProcesosListos().contains(proceso)) {
                        // EN RAM
                        descripcionLimpia = proceso.getId() + " (D:" +
                                proceso.getDuracion() + ", T:" + proceso.getTamanioSlot() + ")";

                        if (procesosEnTabla.containsKey(proceso.getId())) {
                            info = procesosEnTabla.get(proceso.getId());
                            info.setProceso(proceso);
                            info.setDescripcion(descripcionLimpia);
                        } else {
                            info = new ProcesoInfo(proceso);
                            info.setDescripcion(descripcionLimpia);
                        }
                        procesosEnRamEspera.add(info);
                        System.out.println("  → CLASIFICADO COMO EN RAM");

                    } else if (controlador.getProcesosEnSwap().contains(proceso)) {
                        // EN SWAP
                        descripcionLimpia = proceso.getId() + " (D:" +
                                proceso.getDuracion() + ", T:" + proceso.getTamanioSlot() + ")";

                        if (procesosEnTabla.containsKey(proceso.getId())) {
                            info = procesosEnTabla.get(proceso.getId());
                            info.setProceso(proceso);
                            info.setDescripcion(descripcionLimpia);
                        } else {
                            info = new ProcesoInfo(proceso);
                            info.setDescripcion(descripcionLimpia);
                        }
                        procesosEnSwap.add(info);
                        System.out.println("  → CLASIFICADO COMO EN SWAP");

                    } else {
                        // CASO EXTRAÑO - Llegó pero no está en ninguna cola
                        descripcionLimpia = proceso.getId() + " (D:" +
                                proceso.getDuracion() + ", L:" + proceso.getTiempoDeLlegada() +
                                ", T:" + proceso.getTamanioSlot() + ")";

                        if (procesosEnTabla.containsKey(proceso.getId())) {
                            info = procesosEnTabla.get(proceso.getId());
                            info.setProceso(proceso);
                            info.setDescripcion(descripcionLimpia);
                        } else {
                            info = new ProcesoInfo(proceso);
                            info.setDescripcion(descripcionLimpia);
                        }
                        proceso.setEstado(EstadoProceso.PENDIENTE);
                        procesosPendientes.add(info);
                        System.out.println("  → CLASIFICADO COMO PENDIENTE (caso extraño)");
                    }
                }
            }

            // Ordenar listas
            if (controlador.algoritmo == TipoAlgoritmo.SJF) {
                procesosEnRamEspera.sort((p1, p2) -> Integer.compare(p1.getProceso().getDuracion(), p2.getProceso().getDuracion()));
            }
            procesosPendientes.sort((p1, p2) -> Integer.compare(p1.getProceso().getTiempoDeLlegada(), p2.getProceso().getTiempoDeLlegada()));
            procesosTerminados.sort((p1, p2) -> p1.getProceso().getId().compareTo(p2.getProceso().getId()));

            // Construir lista final ordenada
            List<ProcesoInfo> procesosOrdenadosFinales = new ArrayList<>();
            procesosOrdenadosFinales.addAll(procesosEjecutandose);
            procesosOrdenadosFinales.addAll(procesosEnRamEspera);
            procesosOrdenadosFinales.addAll(procesosEnSwap);
            procesosOrdenadosFinales.addAll(procesosPendientes);
            procesosOrdenadosFinales.addAll(procesosTerminados);

            System.out.println("RESUMEN CLASIFICACIÓN:");
            System.out.println("  Ejecutando: " + procesosEjecutandose.size());
            System.out.println("  En RAM: " + procesosEnRamEspera.size());
            System.out.println("  En SWAP: " + procesosEnSwap.size());
            System.out.println("  Pendientes: " + procesosPendientes.size());
            System.out.println("  Terminados: " + procesosTerminados.size());

            listaProcesos.setAll(procesosOrdenadosFinales);
            tablaProcesos.refresh();
        });
    }

    // Mostrar detalles de un proceso en ventana emergente
    private void mostrarDetallesProceso(Proceso proceso) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ver_proceso.fxml"));
            Scene scene = new Scene(loader.load());

            VerProcesoController controller = loader.getController();
            controller.cargarDatosProceso(proceso);

            Stage stage = new Stage();
            stage.setTitle("Detalles del " + proceso.getId());
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error al abrir la ventana de detalles del proceso: " + e.getMessage());
        }
    }

    // Actualizar tabla de SWAP
    public void actualizarSwap() {
        if (controlador == null) return;

        Platform.runLater(() -> {
            listaSwap.clear();

            Queue<Proceso> procesosEnSwap = controlador.getProcesosEnSwap();

            if (procesosEnSwap.isEmpty()) {
                listaSwap.add(new SwapInfo("💾 SWAP VACÍO", "Sin procesos en disco"));
            } else {
                for (Proceso proceso : procesosEnSwap) {
                    String procesoInfo = proceso.getId();
                    String tamanioInfo = "📦 " + proceso.getTamanioSlot() + " slots";
                    String duracionInfo = "⏱️ " + proceso.getDuracion() + "s";

                    String detalles = tamanioInfo + " | " + duracionInfo;
                    listaSwap.add(new SwapInfo(procesoInfo, detalles));
                }

                int totalSlots = procesosEnSwap.stream().mapToInt(Proceso::getTamanioSlot).sum();
                listaSwap.add(new SwapInfo(
                        "📊 TOTAL EN SWAP",
                        procesosEnSwap.size() + " procesos | " + totalSlots + " slots"
                ));
            }

            tablaSwap.refresh();
        });
    }

    // Actualizar información general de la interfaz
    private void actualizarInformacionGeneral() {
        if (controlador == null) return;

        Platform.runLater(() -> {
            int tiempoActual = controlador.getTiempoActual();
            int procesosEnSwap = controlador.getProcesosEnSwap().size();
            int procesosTerminados = controlador.getProcesosTerminados().size();

            String infoCompleta = String.format(
                    "Tiempo: %d | SWAP: %d proc | Terminados: %d",
                    tiempoActual, procesosEnSwap, procesosTerminados
            );

            labelTiempoActual.setText(infoCompleta);
            configurarPanelQuantum();
        });
    }

    // Terminar simulación e ir a estadísticas
    @FXML
    void ActionInButtonTerminar(ActionEvent event) {
        System.out.println("=== TERMINANDO SIMULACIÓN ===");

        if (controlador != null) {
            controlador.setRunning(false);
            controlador.setPausado(false);

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        this.interrupt();

        try {
            irAEstadisticas();
        } catch (IOException e) {
            System.err.println("Error al ir a estadísticas: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Navegar a pantalla de estadísticas
    private void irAEstadisticas() throws IOException {
        System.out.println("Navegando a pantalla de estadísticas...");

        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("estadisticas.fxml"));
        Scene newScene = new Scene(fxmlLoader.load(), 1080, 720);

        EstadisticasController estadisticasController = fxmlLoader.getController();
        if (controlador != null) {
            estadisticasController.cargarEstadisticas(controlador);
        }

        Stage stage = (Stage) botonTerminar.getScene().getWindow();
        stage.setScene(newScene);
    }

    // Pausar/reanudar simulación
    @FXML
    void ActionPausarSimulacion(ActionEvent event) {
        pausado = !pausado;
        botonPausar.setText(pausado ? "Reanudar" : "Pausar");

        if (controlador != null) {
            controlador.setPausado(pausado);
        }

        if (pausado) {
            botonPausar.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
            System.out.println("=== SIMULACIÓN PAUSADA ===");
        } else {
            botonPausar.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold;");
            System.out.println("=== SIMULACIÓN REANUDADA ===");
        }
    }

    @Override
    public void run() {
        if (this.controlador == null) {
            System.err.println("ERROR FATAL: 'controlador' es nulo en HelloController.run() ANTES del bucle. Saliendo.");
            return;
        }

        while (controlador.getRunnin()) {
            try {
                if (!pausado) {
                    actualizarColaDeprocesos();
                    actualizarMemoriaRAM();
                    actualizarNucleos();
                    actualizarSwap();
                    actualizarInformacionGeneral();
                }

                Thread.sleep(velocidadSimulacion / 2);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
    }

    // Clases auxiliares para las tablas
    public static class CoreInfo {
        private String coreId;
        private String procesoId;
        private String estado;

        public CoreInfo(String coreId, String procesoId, String estado) {
            this.coreId = coreId;
            this.procesoId = procesoId;
            this.estado = estado;
        }

        public String getCoreId() { return coreId; }
        public void setCoreId(String coreId) { this.coreId = coreId; }
        public String getProcesoId() { return procesoId; }
        public void setProcesoId(String procesoId) { this.procesoId = procesoId; }
        public String getEstado() { return estado; }
        public void setEstado(String estado) { this.estado = estado; }
    }

    public static class MemoriaInfo {
        private int direccion;
        private String procesoId;
        private String estado;
        private String fragmentacion;

        public MemoriaInfo(int direccion, String procesoId, String estado, String fragmentacion) {
            this.direccion = direccion;
            this.procesoId = procesoId;
            this.estado = estado;
            this.fragmentacion = fragmentacion;
        }

        public int getDireccion() { return direccion; }
        public void setDireccion(int direccion) { this.direccion = direccion; }
        public String getProcesoId() { return procesoId; }
        public void setProcesoId(String procesoId) { this.procesoId = procesoId; }
        public String getEstado() { return estado; }
        public void setEstado(String estado) { this.estado = estado; }
        public String getFragmentacion() { return fragmentacion; }
        public void setFragmentacion(String fragmentacion) { this.fragmentacion = fragmentacion; }
    }

    public static class ProcesoInfo {
        private Proceso proceso;
        private String descripcion;

        public ProcesoInfo(Proceso proceso) {
            this.proceso = proceso;
            actualizarDescripcion();
        }
        
        private void actualizarDescripcion() {
            StringBuilder desc = new StringBuilder();
            desc.append(proceso.getId());
            
            // Mostrar si es proceso hijo
            if (proceso.esProcesoHijo()) {
                desc.append(" 👶"); // Emoji para proceso hijo
            }
            
            // Mostrar si tiene hijos
            if (proceso.esProcesoConHijos()) {
                desc.append(" 👨‍👩‍👧‍👦").append(proceso.cantidadHijos()); // Emoji familia + cantidad
            }
            
            desc.append(" (D:").append(proceso.getDuracion())
                .append(", L:").append(proceso.getTiempoDeLlegada())
                .append(", T:").append(proceso.getTamanioSlot()).append(")");
                
            this.descripcion = desc.toString();
        }

        public Proceso getProceso() { return proceso; }
        public void setProceso(Proceso proceso) {
            this.proceso = proceso;
            actualizarDescripcion();
        }

        public String getDescripcion() { return descripcion; }
        public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    }

    public static class SwapInfo {
        private String procesoId;
        private String direcciones;

        public SwapInfo(String procesoId, String direcciones) {
            this.procesoId = procesoId;
            this.direcciones = direcciones;
        }

        public String getProcesoId() { return procesoId; }
        public void setProcesoId(String procesoId) { this.procesoId = procesoId; }
        public String getDirecciones() { return direcciones; }
        public void setDirecciones(String direcciones) { this.direcciones = direcciones; }
    }
}