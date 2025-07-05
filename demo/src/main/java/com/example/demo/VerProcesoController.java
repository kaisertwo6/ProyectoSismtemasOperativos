package com.example.demo;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class VerProcesoController {

    @FXML private Button btnSalir;
    @FXML private Text labelTitulo;
    @FXML private Text labelLlegada;
    @FXML private Text labelEjecucion;
    @FXML private Text labelEspera;
    @FXML private Text labelRetorno;
    @FXML private Text labelTamaño;
    @FXML private Text labelProcesoPadre;  // NUEVO
    @FXML private Text labelProcesosHijos; // NUEVO

    private Proceso procesoActual;

    @FXML
    public void initialize() {
        // Configuración inicial si es necesaria
    }

    public void cargarDatosProceso(Proceso proceso) {
        this.procesoActual = proceso;

        if (proceso != null) {
            // Actualizar título
            String titulo = proceso.getId();
            if (proceso.esProcesoHijo()) {
                titulo += " (PROCESO HIJO)";
            } else if (proceso.esProcesoConHijos()) {
                titulo += " (PROCESO PADRE)";
            }
            labelTitulo.setText(titulo);

            // Actualizar información del proceso
            labelLlegada.setText(String.valueOf(proceso.getTiempoDeLlegada()));
            labelEjecucion.setText(String.valueOf(proceso.getDuracion()));
            labelEspera.setText(String.valueOf(proceso.getTiempoDeEspera()));
            labelRetorno.setText(String.valueOf(proceso.getTiempoDeRetorno()));
            labelTamaño.setText(String.valueOf(proceso.getTamanioSlot()));
            
            // NUEVA INFORMACIÓN: Proceso padre
            if (proceso.esProcesoHijo() && proceso.getProcesoPadre() != null) {
                labelProcesoPadre.setText(proceso.getProcesoPadre().getId());
            } else {
                labelProcesoPadre.setText("Ninguno (Proceso raíz)");
            }
            
            // NUEVA INFORMACIÓN: Procesos hijos
            if (proceso.esProcesoConHijos()) {
                StringBuilder hijosInfo = new StringBuilder();
                for (int i = 0; i < proceso.getProcesosHijos().size(); i++) {
                    if (i > 0) hijosInfo.append(", ");
                    hijosInfo.append(proceso.getProcesosHijos().get(i).getId());
                }
                labelProcesosHijos.setText(hijosInfo.toString() + " (" + proceso.cantidadHijos() + " hijos)");
            } else {
                labelProcesosHijos.setText("Ninguno");
            }
            
        } else {
            // Valores por defecto si el proceso es nulo
            labelTitulo.setText("Proceso Desconocido");
            labelLlegada.setText("N/A");
            labelEjecucion.setText("N/A");
            labelEspera.setText("N/A");
            labelRetorno.setText("N/A");
            labelTamaño.setText("N/A");
            labelProcesoPadre.setText("N/A");
            labelProcesosHijos.setText("N/A");
        }
    }

    @FXML
    void cerrarVentana(ActionEvent event) {
        // Obtener la ventana actual y cerrarla
        Stage stage = (Stage) btnSalir.getScene().getWindow();
        stage.close();
    }
}