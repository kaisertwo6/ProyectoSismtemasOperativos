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
    @FXML private Text labelTipoProceso;

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
            labelTitulo.setText(titulo);

            // Actualizar información básica del proceso
            labelLlegada.setText(String.valueOf(proceso.getTiempoDeLlegada()));
            labelEjecucion.setText(String.valueOf(proceso.getDuracionOriginal()) + " unidades");
            labelEspera.setText(String.valueOf(proceso.getTiempoDeEspera()));
            labelRetorno.setText(String.valueOf(proceso.getTiempoDeRetorno()));
            labelTamaño.setText(String.valueOf(proceso.getTamanioSlot()) + " slots");

            // NUEVA LÓGICA SIMPLIFICADA: Determinar tipo de proceso
            String tipoProceso = determinarTipoProceso(proceso);
            labelTipoProceso.setText(tipoProceso);

        } else {
            // Valores por defecto si el proceso es nulo
            labelTitulo.setText("Proceso Desconocido");
            labelLlegada.setText("N/A");
            labelEjecucion.setText("N/A");
            labelEspera.setText("N/A");
            labelRetorno.setText("N/A");
            labelTamaño.setText("N/A");
            labelTipoProceso.setText("N/A");
        }
    }

    private String determinarTipoProceso(Proceso proceso) {
        String id = proceso.getId();

        // Verificar si es parte de un programa dividido (contiene .P)
        if (id.contains(".P") && !id.contains(".H")) {
            // Es parte de un programa grande dividido
            String nombrePrograma = id.substring(0, id.indexOf(".P"));
            String numeroParte = id.substring(id.indexOf(".P") + 2);
            return "📦 Parte del programa: " + nombrePrograma + "\n" +
                    "🔢 Fragmento número: " + numeroParte + "\n" +
                    "⚡ Programa dividido automáticamente por tamaño";
        }

        // Verificar si es un proceso hijo creado por fork (contiene .H)
        else if (id.contains(".H")) {
            String procesoPadre = id.substring(0, id.indexOf(".H"));
            String numeroHijo = id.substring(id.indexOf(".H") + 2);
            return "👶 Proceso hijo creado durante ejecución\n" +
                    "👨‍💼 Proceso padre: " + procesoPadre + "\n" +
                    "🔢 Hijo número: " + numeroHijo;
        }

        // Verificar si tiene nombre personalizado (no es "Proceso X")
        else if (!id.startsWith("Proceso ")) {
            return "📄 Programa completo: " + id + "\n" +
                    "✨ Programa pequeño que no requirió división\n" +
                    "💾 Tamaño: " + proceso.getTamanioSlot() + " slots";
        }

        // Es un proceso normal sin nombre
        else {
            return "⚙️ Proceso independiente\n" +
                    "🔧 Creado sin nombre específico\n" +
                    "💾 Tamaño: " + proceso.getTamanioSlot() + " slots";
        }
    }

    @FXML
    void cerrarVentana(ActionEvent event) {
        // Obtener la ventana actual y cerrarla
        Stage stage = (Stage) btnSalir.getScene().getWindow();
        stage.close();
    }
}