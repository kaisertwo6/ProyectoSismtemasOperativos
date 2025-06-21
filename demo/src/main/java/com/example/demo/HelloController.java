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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class HelloController extends Thread {

    private static HelloController instance;

    @FXML private Button botonTerminar;
    @FXML private Button botonPausar;
    @FXML private Label labelTiempoActual;
    @FXML private Label labelQuantumValor;
    @FXML private AnchorPane panelQuantum;

    // Tabla de núcleos
    @FXML private TableView<CoreInfo> tablaNucleos;
    @FXML private TableColumn<CoreInfo, String> columnaCoreId;
    @FXML private TableColumn<CoreInfo, String> columnaProceso;
    @FXML private TableColumn<CoreInfo, String> columnaEstado;

    // Tabla de memoria RAM
    @FXML private TableView<MemoriaInfo> tablaMemoriaRAM;
    @FXML private TableColumn<MemoriaInfo, Integer> columnaDireccion;
    @FXML private TableColumn<MemoriaInfo, String> columnaProcesoId;
    @FXML private TableColumn<MemoriaInfo, String> columnaEstadoMemoria;
    @FXML private TableColumn<MemoriaInfo, String> columnaFragmentacion;

    // Tabla de procesos
    @FXML private TableView<ProcesoInfo> tablaProcesos;
    @FXML private TableColumn<ProcesoInfo, String> columnaProcesoInfo;
    @FXML private TableColumn<ProcesoInfo, String> columnaEstadoProceso;
    @FXML private TableColumn<ProcesoInfo, Button> columnaAcciones;

    // Tabla de swap
    @FXML private TableView<SwapInfo> tablaSwap;
    @FXML private TableColumn<SwapInfo, String> columnaProcesoSwap;
    @FXML private TableColumn<SwapInfo, String> columnaDireccionesSwap;

    // Listas observables
    private ObservableList<CoreInfo> listaNucleos;
    private ObservableList<MemoriaInfo> listaMemoria;
    private ObservableList<ProcesoInfo> listaProcesos;
    private ObservableList<SwapInfo> listaSwap;

    private Set<String> procesosConocidos;
    private Controlador controlador;
    private boolean pausado = false;

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
                            case ESPERA:
                                circulo.setFill(Color.web("#f1c40f")); // Amarillo
                                labelEstado.setText("Espera");
                                labelEstado.setTextFill(Color.web("#f1c40f"));
                                break;
                            case EJECUCION:
                                circulo.setFill(Color.web("#e67e22")); // Naranja
                                labelEstado.setText("En Proceso");
                                labelEstado.setTextFill(Color.web("#e67e22"));
                                break;
                            case TERMINADO:
                                circulo.setFill(Color.web("#e74c3c")); // Rojo
                                labelEstado.setText("Terminado");
                                labelEstado.setTextFill(Color.web("#e74c3c"));
                                break;
                            default:
                                circulo.setFill(Color.web("#95a5a6")); // Gris
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

    public void actualizarMemoriaRAM() {
        if (controlador == null) return;

        Platform.runLater(() -> {
            for (MemoriaInfo info : listaMemoria) {
                info.setProcesoId("Libre");
                info.setEstado("Disponible");
                info.setFragmentacion("No");
            }

            List<Integer> ram = controlador.ram;
            for (int i = 0; i < ram.size() && i < listaMemoria.size(); i++) {
                MemoriaInfo info = listaMemoria.get(i);
                Integer procesoId = ram.get(i);

                if (procesoId != null && procesoId != 0) {
                    info.setProcesoId("P" + procesoId);
                    info.setEstado("Ocupado");
                } else {
                    info.setProcesoId("Libre");
                    info.setEstado("Disponible");
                }
            }

            tablaMemoriaRAM.refresh();
        });
    }

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

    public void actualizarColaDeprocesos() {
        if (controlador == null) return;

        Platform.runLater(() -> {
            // Listas separadas por estado para controlar el orden
            List<ProcesoInfo> procesosEjecutandose = new ArrayList<>();
            List<ProcesoInfo> procesosEnEspera = new ArrayList<>();
            List<ProcesoInfo> procesosPendientes = new ArrayList<>();
            List<ProcesoInfo> procesosTerminados = new ArrayList<>();

            // Mapa para rastrear procesos existentes en la tabla
            Map<String, ProcesoInfo> procesosEnTabla = new HashMap<>();
            for (ProcesoInfo procesoInfo : listaProcesos) {
                procesosEnTabla.put(procesoInfo.getProceso().getId(), procesoInfo);
            }

            // 1. Procesos en ejecución (arriba)
            for (Core core : controlador.cores) {
                if (core.isOcupado() && core.getProcesoEjecucion() != null) {
                    Proceso procesoEjecutandose = core.getProcesoEjecucion();
                    procesosConocidos.add(procesoEjecutandose.getId());

                    if (procesosEnTabla.containsKey(procesoEjecutandose.getId())) {
                        ProcesoInfo procesoInfo = procesosEnTabla.get(procesoEjecutandose.getId());
                        procesoInfo.setProceso(procesoEjecutandose);
                        procesosEjecutandose.add(procesoInfo);
                    } else {
                        procesosEjecutandose.add(new ProcesoInfo(procesoEjecutandose));
                    }
                }
            }

            // 2. Procesos en cola de listos (ordenados por SJF)
            List<Proceso> procesosListosOrdenados = new ArrayList<>(controlador.getProcesosListos());
            procesosListosOrdenados.sort((p1, p2) -> Integer.compare(p1.getDuracion(), p2.getDuracion()));

            for (Proceso proceso : procesosListosOrdenados) {
                procesosConocidos.add(proceso.getId());

                if (procesosEnTabla.containsKey(proceso.getId())) {
                    ProcesoInfo procesoInfo = procesosEnTabla.get(proceso.getId());
                    procesoInfo.setProceso(proceso);
                    procesosEnEspera.add(procesoInfo);
                } else {
                    procesosEnEspera.add(new ProcesoInfo(proceso));
                }
            }

            // 3. Procesos pendientes de llegada
            List<Proceso> procesosPendientesOrdenados = new ArrayList<>(controlador.getProcesosPendientesDeLlegada());
            procesosPendientesOrdenados.sort((p1, p2) -> Integer.compare(p1.getTiempoDeLlegada(), p2.getTiempoDeLlegada()));

            for (Proceso procesoPendiente : procesosPendientesOrdenados) {
                procesosConocidos.add(procesoPendiente.getId());

                if (procesosEnTabla.containsKey(procesoPendiente.getId())) {
                    ProcesoInfo procesoInfo = procesosEnTabla.get(procesoPendiente.getId());
                    procesoInfo.setProceso(procesoPendiente);
                    procesosPendientes.add(procesoInfo);
                } else {
                    procesosPendientes.add(new ProcesoInfo(procesoPendiente));
                }
            }

            // 4. Procesos terminados (al final)
            List<Proceso> procesosTerminadosOrdenados = new ArrayList<>(controlador.getProcesosTerminados());
            procesosTerminadosOrdenados.sort((p1, p2) -> p1.getId().compareTo(p2.getId()));

            for (Proceso procesoTerminado : procesosTerminadosOrdenados) {
                procesosConocidos.add(procesoTerminado.getId());

                if (procesosEnTabla.containsKey(procesoTerminado.getId())) {
                    ProcesoInfo procesoInfo = procesosEnTabla.get(procesoTerminado.getId());
                    procesoInfo.setProceso(procesoTerminado);
                    procesosTerminados.add(procesoInfo);
                } else {
                    procesosTerminados.add(new ProcesoInfo(procesoTerminado));
                }
            }

            // 5. Construir lista final con orden lógico
            List<ProcesoInfo> procesosOrdenadosFinales = new ArrayList<>();
            procesosOrdenadosFinales.addAll(procesosEjecutandose);    // En ejecución
            procesosOrdenadosFinales.addAll(procesosEnEspera);        // En espera
            procesosOrdenadosFinales.addAll(procesosPendientes);      // Pendientes
            procesosOrdenadosFinales.addAll(procesosTerminados);      // Terminados

            listaProcesos.setAll(procesosOrdenadosFinales);
            tablaProcesos.refresh();
        });
    }

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

    public void actualizarSwap() {
        if (controlador == null) return;

        Platform.runLater(() -> {
            listaSwap.clear();

            Map<String, List<Integer>> swap = controlador.listaDireccionesProcesMem;
            for (Map.Entry<String, List<Integer>> entry : swap.entrySet()) {
                String procesoId = entry.getKey();
                List<Integer> direcciones = entry.getValue();
                String direccionesStr = direcciones.toString();

                listaSwap.add(new SwapInfo(procesoId, direccionesStr));
            }
        });
    }

    private void actualizarInformacionGeneral() {
        if (controlador == null) return;

        Platform.runLater(() -> {
            int tiempoActual = controlador.getTiempoActual();
            int procesosEnCola = controlador.getProcesosListos().size();
            int procesosTerminados = controlador.getProcesosTerminados().size();

            labelTiempoActual.setText("Tiempo: " + tiempoActual + " | En cola: " + procesosEnCola + " | Terminados: " + procesosTerminados);
            configurarPanelQuantum();
        });
    }

    @FXML
    void ActionInButtonTerminar(ActionEvent event) {
        if (controlador != null) {
            controlador.setRunning(false);
        }
        this.interrupt();
    }

    @FXML
    void ActionPausarSimulacion(ActionEvent event) {
        pausado = !pausado;
        botonPausar.setText(pausado ? "Reanudar" : "Pausar");
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

    // Clases auxiliares
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
            this.descripcion = proceso.getId() + " (D:" + proceso.getDuracion() +
                    ", L:" + proceso.getTiempoDeLlegada() +
                    ", T:" + proceso.getTamanioSlot() + ")";
        }

        public Proceso getProceso() { return proceso; }
        public void setProceso(Proceso proceso) {
            this.proceso = proceso;
            this.descripcion = proceso.getId() + " (D:" + proceso.getDuracion() +
                    ", L:" + proceso.getTiempoDeLlegada() +
                    ", T:" + proceso.getTamanioSlot() + ")";
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