package com.example.demo;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.text.Text;

public class EstadisticasController {
    @FXML
    private Button btnProcesos;

    @FXML
    private TableColumn<?, ?> columnaEspera;

    @FXML
    private TableColumn<?, ?> columnaProcesos;

    @FXML
    private TableColumn<?, ?> columnaRespuesta;

    @FXML
    private TableColumn<?, ?> columnaRetorno;

    @FXML
    private TableView<?> tablaProcesos;

    @FXML
    private Text txtEspera;

    @FXML
    private Text txtRespuesta;

    @FXML
    private Text txtRetorno;

    @FXML
    void irInicio(ActionEvent event) {

    }
}
