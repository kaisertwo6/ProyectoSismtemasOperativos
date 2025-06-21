package com.example.demo;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.TextField;


public class InicioController {
    @FXML
    private Button btnAñadir;

    @FXML
    private Button btnInicio;

    @FXML
    private Button btnRR;

    @FXML
    private Button btnRandom;

    @FXML
    private Button btnSJF;

    @FXML
    private TextField txtDuracion;

    @FXML
    private TextField txtSlot;

    @FXML
    private TextField txtTiempo;

    Controlador controlador ;

    boolean RR ;
    boolean SJF;


    @FXML
    public void initialize(){
         this.controlador = Controlador.getInstance();

    }

    @FXML
    void añadirProceso(ActionEvent event) {

        int  duracion = Integer.parseInt(txtDuracion.getText());
        int tamanioSlot = Integer.parseInt(txtSlot.getText());
        int tiempoLlegada = Integer.parseInt(txtTiempo.getText());

        Proceso procesoAux = new Proceso(duracion , tiempoLlegada , tamanioSlot);
        controlador.agregarProcesoAlSistema(procesoAux);
    }

    @FXML
    void crearRandom(ActionEvent event) {

    }

    @FXML
    void iniciarSimulacion(ActionEvent event) {

        TipoAlgoritmo tipo;
        if (RR){
            tipo = TipoAlgoritmo.RR;
            controlador.setAlgoritmo(tipo);

        } else if (SJF) {
            tipo = TipoAlgoritmo.SJF;
            controlador.setAlgoritmo(tipo);

        }
        controlador.setRunning(true);
        controlador.run();

    }

    @FXML
    void seleccionarRR(ActionEvent event) {

        this.RR = true;
        this.SJF = false;

    }

    @FXML
    void seleccionarSJF(ActionEvent event) {
        this.RR = false;
        this.SJF = true;
    }
}
