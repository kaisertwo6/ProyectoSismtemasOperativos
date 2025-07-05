package com.example.demo;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Proceso {

    private static int nextId = 1;

    private final StringProperty id;
    private final IntegerProperty duracion;
    private final IntegerProperty tiempoDeLlegada;
    private final IntegerProperty tiempoDeEspera;
    private final IntegerProperty tiempoDeRetorno;
    private final IntegerProperty tamanioSlot;
    private EstadoProceso estado;

    private IntegerProperty tiempoCpuAcumulado;
    private int ultimaEntradaColaListos;

    // Gestión de procesos padre-hijo
    private Proceso procesoPadre;
    private List<Proceso> procesosHijos;
    private boolean tieneHijos;
    private boolean esPrograma; // Indica si es un programa completo o solo un proceso
    private String nombrePrograma; // Nombre del programa original
    private int tamanioOriginalPrograma; // Tamaño total del programa

    // Constructor para procesos normales
    public Proceso(int duracion, int tiempoDeLlegada, int tamanioSlot) {
        this.id = new SimpleStringProperty("Proceso " + nextId++);
        this.duracion = new SimpleIntegerProperty(duracion);
        this.tiempoDeLlegada = new SimpleIntegerProperty(tiempoDeLlegada);
        this.tamanioSlot = new SimpleIntegerProperty(tamanioSlot);
        this.tiempoDeEspera = new SimpleIntegerProperty(0);
        this.tiempoDeRetorno = new SimpleIntegerProperty(0);
        this.estado = EstadoProceso.ESPERA;
        this.tiempoCpuAcumulado = new SimpleIntegerProperty(0);
        this.ultimaEntradaColaListos = tiempoDeLlegada;

        this.procesoPadre = null;
        this.procesosHijos = new ArrayList<>();
        this.tieneHijos = false;
        this.esPrograma = false;
        this.nombrePrograma = null;
        this.tamanioOriginalPrograma = tamanioSlot;
    }

    // Constructor para programas que se dividen automáticamente
    public Proceso(String nombrePrograma, int duracion, int tiempoDeLlegada, int tamanioPrograma) {
        this.id = new SimpleStringProperty(nombrePrograma);
        this.duracion = new SimpleIntegerProperty(duracion);
        this.tiempoDeLlegada = new SimpleIntegerProperty(tiempoDeLlegada);
        this.tamanioSlot = new SimpleIntegerProperty(tamanioPrograma);
        this.tiempoDeEspera = new SimpleIntegerProperty(0);
        this.tiempoDeRetorno = new SimpleIntegerProperty(0);
        this.estado = EstadoProceso.PENDIENTE;
        this.tiempoCpuAcumulado = new SimpleIntegerProperty(0);
        this.ultimaEntradaColaListos = tiempoDeLlegada;

        this.procesoPadre = null;
        this.procesosHijos = new ArrayList<>();
        this.tieneHijos = true;
        this.esPrograma = true;
        this.nombrePrograma = nombrePrograma;
        this.tamanioOriginalPrograma = tamanioPrograma;
    }

    // Constructor para procesos hijos (división automática)
    private Proceso(String nombrePrograma, int duracion, int tiempoDeLlegada, int tamanioSlot, Proceso procesoPadre, int numeroHijo) {
        this.duracion = new SimpleIntegerProperty(duracion);
        this.tiempoDeLlegada = new SimpleIntegerProperty(tiempoDeLlegada);
        this.tamanioSlot = new SimpleIntegerProperty(tamanioSlot);
        this.tiempoDeEspera = new SimpleIntegerProperty(0);
        this.tiempoDeRetorno = new SimpleIntegerProperty(0);
        this.estado = EstadoProceso.PENDIENTE;
        this.tiempoCpuAcumulado = new SimpleIntegerProperty(0);
        this.ultimaEntradaColaListos = tiempoDeLlegada;

        this.procesoPadre = procesoPadre;
        this.procesosHijos = new ArrayList<>();
        this.tieneHijos = false;
        this.esPrograma = false;
        this.nombrePrograma = nombrePrograma;
        this.tamanioOriginalPrograma = procesoPadre.tamanioOriginalPrograma;

        this.id = new SimpleStringProperty(nombrePrograma + ".P" + numeroHijo);
    }

    // Dividir programa automáticamente en procesos más pequeños
    public List<Proceso> dividirPrograma(int tamanioMaximoPorProceso) {
        if (!this.esPrograma) {
            throw new IllegalStateException("Solo los programas pueden ser divididos");
        }

        List<Proceso> procesosResultantes = new ArrayList<>();
        int tamanioRestante = this.getTamanioSlot();
        int duracionPorProceso = Math.max(1, this.getDuracion() / Math.max(1, (tamanioRestante / tamanioMaximoPorProceso)));
        int numeroHijo = 1;

        while (tamanioRestante > 0) {
            int tamanioEsteProceso = Math.min(tamanioMaximoPorProceso, tamanioRestante);
            int tiempoLlegadaHijo = this.getTiempoDeLlegada() + (numeroHijo - 1);

            Proceso procesoHijo = new Proceso(
                    this.nombrePrograma,
                    duracionPorProceso,
                    tiempoLlegadaHijo,
                    tamanioEsteProceso,
                    this,
                    numeroHijo
            );

            this.procesosHijos.add(procesoHijo);
            procesosResultantes.add(procesoHijo);

            tamanioRestante -= tamanioEsteProceso;
            numeroHijo++;
        }

        // El programa padre no se ejecuta, solo controla los hijos
        this.setEstado(EstadoProceso.ESPERA);

        return procesosResultantes;
    }

    // Crear proceso hijo manual (fork durante ejecución)
    public Proceso crearProcesoHijo(int duracion, int tamanioSlot, int tiempoLlegada) {
        if (this.procesosHijos.size() >= 4) {
            return null;
        }

        String idHijo = this.getId() + ".H" + (this.procesosHijos.size() + 1);
        Proceso procesoHijo = new Proceso(duracion, tiempoLlegada, tamanioSlot);
        procesoHijo.id.set(idHijo);
        procesoHijo.procesoPadre = this;
        procesoHijo.nombrePrograma = this.nombrePrograma != null ? this.nombrePrograma : this.getId();

        this.procesosHijos.add(procesoHijo);
        this.tieneHijos = true;

        return procesoHijo;
    }

    // Verificar si todos los procesos hijos han terminado
    public boolean todosLosHijosTerminaron() {
        return this.procesosHijos.stream().allMatch(hijo -> hijo.getEstado() == EstadoProceso.TERMINADO);
    }

    // Terminar programa padre cuando todos los hijos terminen
    public void verificarTerminacionPrograma() {
        if (this.esPrograma && todosLosHijosTerminaron() && this.getEstado() != EstadoProceso.TERMINADO) {
            this.setEstado(EstadoProceso.TERMINADO);
            System.out.println("📋 PROGRAMA TERMINADO: " + this.getId() + " - Todos los procesos completados");
        }
    }

    // Getters para las propiedades
    public StringProperty idProperty() { return id; }
    public IntegerProperty duracionProperty() { return duracion; }
    public IntegerProperty tiempoDeLlegadaProperty() { return tiempoDeLlegada; }
    public IntegerProperty tiempoDeEsperaProperty() { return tiempoDeEspera; }
    public IntegerProperty tiempoDeRetornoProperty() { return tiempoDeRetorno; }
    public IntegerProperty tamanioSlotProperty() { return tamanioSlot; }

    public String getId() { return id.get(); }
    public int getDuracion() { return duracion.get(); }
    public int getTiempoDeLlegada() { return tiempoDeLlegada.get(); }
    public int getTiempoDeEspera() { return tiempoDeEspera.get(); }
    public int getTiempoDeRetorno() { return tiempoDeRetorno.get(); }
    public int getTamanioSlot() { return tamanioSlot.get(); }
    public EstadoProceso getEstado() { return estado; }
    public int getTiempoCpuAcumulado() { return tiempoCpuAcumulado.get(); }
    public int getUltimaEntradaColaListos() { return ultimaEntradaColaListos; }

    public Proceso getProcesoPadre() { return procesoPadre; }
    public List<Proceso> getProcesosHijos() { return new ArrayList<>(procesosHijos); }
    public boolean esProcesoHijo() { return procesoPadre != null; }
    public boolean esProcesoConHijos() { return !procesosHijos.isEmpty(); }
    public boolean puedeCrearHijos() { return tieneHijos && !esPrograma; }
    public int cantidadHijos() { return procesosHijos.size(); }
    public boolean esPrograma() { return esPrograma; }
    public String getNombrePrograma() { return nombrePrograma; }
    public int getTamanioOriginalPrograma() { return tamanioOriginalPrograma; }

    // Setters
    public void setDuracion(int duracion) { this.duracion.set(duracion); }
    public void setTiempoDeEspera(int tiempoDeEspera) { this.tiempoDeEspera.set(tiempoDeEspera); }
    public void setTiempoDeRetorno(int tiempoDeRetorno) { this.tiempoDeRetorno.set(tiempoDeRetorno); }
    public void setTamanioSlot(int tamanioSlot) { this.tamanioSlot.set(tamanioSlot); }
    public void setEstado(EstadoProceso estado) { this.estado = estado; }
    public void setTiempoCpuAcumulado(int tiempoCpuAcumulado) { this.tiempoCpuAcumulado.set(tiempoCpuAcumulado); }
    public void setUltimaEntradaColaListos(int ultimaEntradaColaListos) { this.ultimaEntradaColaListos = ultimaEntradaColaListos; }
    public void incrementarTiempoCpuAcumulado() { this.tiempoCpuAcumulado.set(this.tiempoCpuAcumulado.get() + 1); }
    public void habilitarCreacionHijos() { this.tieneHijos = true; }

    public static void resetNextId() { nextId = 1; }
}