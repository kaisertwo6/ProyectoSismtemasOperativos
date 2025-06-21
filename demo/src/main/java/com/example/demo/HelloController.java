package com.example.demo;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory; // Todavía útil para algunos casos, pero no directamente para Integer si solo es un Integer
import javafx.scene.control.cell.MapValueFactory; // Puede ser útil, pero usaremos setCellValueFactory directamente
import javafx.scene.control.cell.TextFieldTableCell; // Para editar si se desea, no requerido para mostrar
import javafx.util.Callback; // Necesario para CellValueFactory si no usas PropertyValueFactory

import javafx.beans.property.SimpleStringProperty;

public class HelloController {

    @FXML
    private Button botonTerminar;

    @FXML
    private TableColumn<Integer, String> colum1Procesos;

    @FXML
    private Label labelMemoriaRam;

    @FXML
    private TableView<Integer> tablaProcesos;

    private ObservableList<Integer> listaDeNumeros;

    @FXML
    void ActionInButtonTerminar(ActionEvent event) {

        // 1. Inicializa la lista con los números 1, 2 y 3
        listaDeNumeros = FXCollections.observableArrayList(1, 2, 3);

        // 2. Asigna la lista a la TableView
        tablaProcesos.setItems(listaDeNumeros);

        // 3. Configura la TableColumn para mostrar los números
        // Cuando el tipo de la fila es directamente el tipo de la columna (ej. Integer -> Integer),
        // o un tipo primitivo que se mostrará como String,
        // no se necesita PropertyValueFactory. Se usa un CellValueFactory personalizado.
        colum1Procesos.setCellValueFactory(cellData -> {

            return new SimpleStringProperty(cellData.getValue().toString());
        });

    }

}