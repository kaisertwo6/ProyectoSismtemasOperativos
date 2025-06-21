package com.example.demo;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty; // Importar si lo vas a usar para el ID String

public class Proceso {

    // CAMBIO IMPORTANTE: Usamos una variable estática para generar IDs únicos y secuenciales.
    private static int nextId = 1; // Comienza los IDs desde 1, 2, 3...

    // Propiedades de JavaFX para que la TableView funcione correctamente
    private final StringProperty id; // Será el ID visible en la tabla (ej. "P1", "P2")
    private final IntegerProperty duracion; // milisegundos
    private final IntegerProperty tiempoDeLlegada;
    private final IntegerProperty tiempoDeEspera;
    private final IntegerProperty tiempoDeRetorno;
    private final IntegerProperty tamanioSlot;
    private EstadoProceso estado; // Asumo que EstadoProceso es una enum


    public Proceso(int duracion, int tiempoDeLlegada, int tamanioSlot) {

        this.id = new SimpleStringProperty("Proceso " + nextId++);
        this.duracion = new SimpleIntegerProperty(duracion);
        this.tiempoDeLlegada = new SimpleIntegerProperty(tiempoDeLlegada);
        this.tamanioSlot = new SimpleIntegerProperty(tamanioSlot);

        this.tiempoDeEspera = new SimpleIntegerProperty(0);
        this.tiempoDeRetorno = new SimpleIntegerProperty(0);
        this.estado = EstadoProceso.ESPERA;
    }

    // --- Getters para las Propiedades (¡CRUCIAL para PropertyValueFactory!) ---
    public StringProperty idProperty() {
        return id;
    }

    public IntegerProperty duracionProperty() {
        return duracion;
    }

    public IntegerProperty tiempoDeLlegadaProperty() {
        return tiempoDeLlegada;
    }

    public IntegerProperty tiempoDeEsperaProperty() {
        return tiempoDeEspera;
    }

    public IntegerProperty tiempoDeRetornoProperty() {
        return tiempoDeRetorno;
    }

    public IntegerProperty tamanioSlotProperty() {
        return tamanioSlot;
    }

    // --- Getters para los valores directos (opcionales, pero útiles) ---
    public String getId() {
        return id.get();
    }

    public int getDuracion() {
        return duracion.get();
    }

    public int getTiempoDeLlegada() {
        return tiempoDeLlegada.get();
    }

    public int getTiempoDeEspera() {
        return tiempoDeEspera.get();
    }

    public int getTiempoDeRetorno() {
        return tiempoDeRetorno.get();
    }

    public int getTamanioSlot() {
        return tamanioSlot.get();
    }

    public EstadoProceso getEstado() {
        return estado;
    }


    // --- Setters para modificar las propiedades (importante si los valores cambian durante la simulación) ---
    public void setDuracion(int duracion) {
        this.duracion.set(duracion);
    }

    public void setTiempoDeEspera(int tiempoDeEspera) {
        this.tiempoDeEspera.set(tiempoDeEspera);
    }

    public void setTiempoDeRetorno(int tiempoDeRetorno) {
        this.tiempoDeRetorno.set(tiempoDeRetorno);
    }

    public void setTamanioSlot(int tamanioSlot) {
        this.tamanioSlot.set(tamanioSlot);
    }

    public void setEstado(EstadoProceso estado) {
        this.estado = estado;
    }

    public static void resetNextId() {
        nextId = 1;
    }
}

