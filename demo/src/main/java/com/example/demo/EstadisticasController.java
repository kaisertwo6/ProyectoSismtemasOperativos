package com.example.demo;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.scene.control.TableRow;

import java.io.IOException;
import java.util.List;

public class EstadisticasController {
    @FXML private Button btnProcesos;
    @FXML private TableColumn<ProcesoEstadistica, String> columnaProcesos;
    @FXML private TableColumn<ProcesoEstadistica, Integer> columnaRespuesta;
    @FXML private TableColumn<ProcesoEstadistica, Integer> columnaEspera;
    @FXML private TableColumn<ProcesoEstadistica, Integer> columnaRetorno;
    @FXML private TableView<ProcesoEstadistica> tablaProcesos;
    @FXML private Text txtEspera;
    @FXML private Text txtRespuesta;
    @FXML private Text txtRetorno;

    private ObservableList<ProcesoEstadistica> listaEstadisticas;
    private Controlador controlador;


    @FXML
    public void initialize() {
        configurarTabla();
        listaEstadisticas = FXCollections.observableArrayList();
        tablaProcesos.setItems(listaEstadisticas);
        aplicarEstilosTabla();
    }



    private void aplicarEstilosTabla() {
        // Aplicar estilos CSS a la tabla
        tablaProcesos.setStyle(
                "-fx-background-color: #374151; " +
                        "-fx-border-color: #10b981; " +
                        "-fx-border-radius: 10; " +
                        "-fx-border-width: 2; " +
                        "-fx-background-radius: 10; " +
                        "-fx-text-fill: white;"
        );

        // Aplicar estilos a las columnas
        String columnStyle =
                "-fx-background-color: #4b5563; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-alignment: CENTER;";

        columnaProcesos.setStyle(columnStyle);
        columnaRespuesta.setStyle(columnStyle);
        columnaEspera.setStyle(columnStyle);
        columnaRetorno.setStyle(columnStyle);

        // Aplicar estilos a las filas de datos
        tablaProcesos.setRowFactory(tv -> {
            TableRow<ProcesoEstadistica> row = new TableRow<>();
            row.setStyle(
                    "-fx-background-color: #374151; " +
                            "-fx-text-fill: white; " +
                            "-fx-border-color: #4b5563;"
            );

            row.setOnMouseEntered(e -> {
                if (!row.isEmpty()) {
                    row.setStyle(
                            "-fx-background-color: #4b5563; " +
                                    "-fx-text-fill: white; " +
                                    "-fx-border-color: #10b981;"
                    );
                }
            });

            row.setOnMouseExited(e -> {
                if (!row.isEmpty()) {
                    row.setStyle(
                            "-fx-background-color: #374151; " +
                                    "-fx-text-fill: white; " +
                                    "-fx-border-color: #4b5563;"
                    );
                }
            });

            return row;
        });
    }

    private void configurarTabla() {
        columnaProcesos.setCellValueFactory(new PropertyValueFactory<>("nombreProceso"));
        columnaRespuesta.setCellValueFactory(new PropertyValueFactory<>("tiempoRespuesta"));
        columnaEspera.setCellValueFactory(new PropertyValueFactory<>("tiempoEspera"));
        columnaRetorno.setCellValueFactory(new PropertyValueFactory<>("tiempoRetorno"));
    }

    // Cargar estadísticas desde el controlador
    public void cargarEstadisticas(Controlador controlador) {
        this.controlador = controlador;

        if (controlador == null) {
            System.err.println("ERROR: Controlador es nulo en EstadisticasController");
            return;
        }

        System.out.println("=== CARGANDO ESTADÍSTICAS ===");

        List<Proceso> procesosTerminados = controlador.getProcesosTerminados();

        if (procesosTerminados.isEmpty()) {
            System.out.println("No hay procesos terminados para mostrar estadísticas");
            mostrarEstadisticasVacias();
            return;
        }

        cargarEstadisticasIndividuales(procesosTerminados);
        calcularYMostrarPromedios(procesosTerminados);

        System.out.println("Estadísticas cargadas exitosamente");
    }

    // Cargar estadísticas de cada proceso individual
    private void cargarEstadisticasIndividuales(List<Proceso> procesosTerminados) {
        listaEstadisticas.clear();

        System.out.println("--- ESTADÍSTICAS INDIVIDUALES ---");

        for (Proceso proceso : procesosTerminados) {
            int tiempoRespuesta = Math.max(0, proceso.getTiempoDeEspera());

            ProcesoEstadistica estadistica = new ProcesoEstadistica(
                    proceso.getId(),
                    tiempoRespuesta,
                    proceso.getTiempoDeEspera(),
                    proceso.getTiempoDeRetorno()
            );

            listaEstadisticas.add(estadistica);

            System.out.println(proceso.getId() + " - Respuesta: " + tiempoRespuesta +
                    ", Espera: " + proceso.getTiempoDeEspera() +
                    ", Retorno: " + proceso.getTiempoDeRetorno());
        }
    }

    // Calcular y mostrar promedios generales
    private void calcularYMostrarPromedios(List<Proceso> procesosTerminados) {
        if (procesosTerminados.isEmpty()) return;

        double promedioRespuesta = listaEstadisticas.stream()
                .mapToInt(ProcesoEstadistica::getTiempoRespuesta)
                .average()
                .orElse(0.0);

        double promedioEspera = procesosTerminados.stream()
                .mapToInt(Proceso::getTiempoDeEspera)
                .average()
                .orElse(0.0);

        double promedioRetorno = procesosTerminados.stream()
                .mapToInt(Proceso::getTiempoDeRetorno)
                .average()
                .orElse(0.0);

        txtRespuesta.setText(String.format("%.2f unidades", promedioRespuesta));
        txtEspera.setText(String.format("%.2f unidades", promedioEspera));
        txtRetorno.setText(String.format("%.2f unidades", promedioRetorno));

        System.out.println("--- PROMEDIOS CALCULADOS ---");
        System.out.println("Tiempo promedio de respuesta: " + String.format("%.2f", promedioRespuesta));
        System.out.println("Tiempo promedio de espera: " + String.format("%.2f", promedioEspera));
        System.out.println("Tiempo promedio de retorno: " + String.format("%.2f", promedioRetorno));
    }

    private void mostrarEstadisticasVacias() {
        txtRespuesta.setText("0.00 unidades");
        txtEspera.setText("0.00 unidades");
        txtRetorno.setText("0.00 unidades");

        listaEstadisticas.add(new ProcesoEstadistica("Sin procesos", 0, 0, 0));
    }

    // Volver a la pantalla de inicio
    @FXML
    void irInicio(ActionEvent event) {
        try {
            System.out.println("=== REGRESANDO AL INICIO ===");

            // Reset completo del controlador
            if (controlador != null) {
                controlador.reset();
                System.out.println("Controlador reseteado");
            }

            Controlador.getInstance().reset();

            // Cargar pantalla de inicio
            FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("inicio.fxml"));
            Scene newScene = new Scene(fxmlLoader.load());

            Stage stage = (Stage) btnProcesos.getScene().getWindow();
            stage.setScene(newScene);
            stage.setMaximized(true); // AGREGADO: Mantener maximizada

            System.out.println("Navegación al inicio completada");

        } catch (IOException e) {
            System.err.println("Error al regresar al inicio: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Error inesperado al regresar al inicio: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Clase para datos de la tabla de estadísticas
    public static class ProcesoEstadistica {
        private String nombreProceso;
        private int tiempoRespuesta;
        private int tiempoEspera;
        private int tiempoRetorno;

        public ProcesoEstadistica(String nombreProceso, int tiempoRespuesta, int tiempoEspera, int tiempoRetorno) {
            this.nombreProceso = nombreProceso;
            this.tiempoRespuesta = tiempoRespuesta;
            this.tiempoEspera = tiempoEspera;
            this.tiempoRetorno = tiempoRetorno;
        }

        public String getNombreProceso() { return nombreProceso; }
        public int getTiempoRespuesta() { return tiempoRespuesta; }
        public int getTiempoEspera() { return tiempoEspera; }
        public int getTiempoRetorno() { return tiempoRetorno; }

        public void setNombreProceso(String nombreProceso) { this.nombreProceso = nombreProceso; }
        public void setTiempoRespuesta(int tiempoRespuesta) { this.tiempoRespuesta = tiempoRespuesta; }
        public void setTiempoEspera(int tiempoEspera) { this.tiempoEspera = tiempoEspera; }
        public void setTiempoRetorno(int tiempoRetorno) { this.tiempoRetorno = tiempoRetorno; }
    }
}