package com.example.demo;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty; // Importar si lo vas a usar para el ID String
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Proceso {


    private static int nextId = 1; // Comienza los IDs desde 1, 2, 3...


    private final StringProperty id; // Será el ID visible en la tabla (ej. "P1", "P2")
    private final IntegerProperty duracion; // milisegundos
    private final IntegerProperty tiempoDeLlegada;
    private final IntegerProperty tiempoDeEspera;
    private final IntegerProperty tiempoDeRetorno;
    private final IntegerProperty tamanioSlot;
    private EstadoProceso estado; // Asumo que EstadoProceso es una enum

    private IntegerProperty tiempoCpuAcumulado;
    private int ultimaEntradaColaListos;
    
    // NUEVOS CAMPOS PARA PROCESOS HIJOS
    private Proceso procesoPadre; // Referencia al proceso padre (null si es proceso raíz)
    private List<Proceso> procesosHijos; // Lista de procesos hijos
    private boolean tieneHijos; // Flag para indicar si este proceso puede crear hijos


    public Proceso(int duracion, int tiempoDeLlegada, int tamanioSlot) {

        this.id = new SimpleStringProperty("Proceso " + nextId++);
        this.duracion = new SimpleIntegerProperty(duracion);
        this.tiempoDeLlegada = new SimpleIntegerProperty(tiempoDeLlegada);
        this.tamanioSlot = new SimpleIntegerProperty(tamanioSlot);

        this.tiempoDeEspera = new SimpleIntegerProperty(0);
        this.tiempoDeRetorno = new SimpleIntegerProperty(0);
        this.estado = EstadoProceso.ESPERA;
        this.tiempoCpuAcumulado = new SimpleIntegerProperty(0); // Nueva inicialización
        this.ultimaEntradaColaListos = tiempoDeLlegada; // Nueva inicialización
        
        // INICIALIZAR CAMPOS DE PROCESOS HIJOS
        this.procesoPadre = null;
        this.procesosHijos = new ArrayList<>();
        this.tieneHijos = false; // Por defecto no puede crear hijos
    }
    
    // Constructor para procesos hijos (llamado por el proceso padre)
    public Proceso(int duracion, int tiempoDeLlegada, int tamanioSlot, Proceso procesoPadre) {
        this(duracion, tiempoDeLlegada, tamanioSlot); // Llamar al constructor principal
        this.procesoPadre = procesoPadre;
        // Cambiar el ID para mostrar relación padre-hijo
        this.id.set(procesoPadre.getId() + ".H" + (procesoPadre.procesosHijos.size() + 1));
    }

    // --- Getters para las Propiedades (¡CRUCIAL para PropertyValueFactory!) ---
    // Dentro de la clase Proceso
// ... (Tus getters y setters existentes) ...

    public int getTiempoCpuAcumulado() {
        return tiempoCpuAcumulado.get();
    }

    public void setTiempoCpuAcumulado(int tiempoCpuAcumulado) {
        this.tiempoCpuAcumulado.set(tiempoCpuAcumulado);
    }

    public int getUltimaEntradaColaListos() {
        return ultimaEntradaColaListos;
    }

    public void setUltimaEntradaColaListos(int ultimaEntradaColaListos) {
        this.ultimaEntradaColaListos = ultimaEntradaColaListos;
    }

    public void incrementarTiempoCpuAcumulado() {
        this.tiempoCpuAcumulado.set(this.tiempoCpuAcumulado.get() + 1);
    }


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
    
    // ======== Metodos de manejo de proceso hijos ========
    
    // Activar la capacidad de crear hijos para este proceso
    public void habilitarCreacionHijos() {
        this.tieneHijos = true;
    }
    
    // Verificar si este proceso puede crear hijos
    public boolean puedeCrearHijos() {
        return this.tieneHijos;
    }
    
    // Crear un proceso hijo
    public Proceso crearProcesoHijo(int duracion, int tamanioSlot) {
        if (!this.tieneHijos) {
            throw new IllegalStateException("El proceso " + this.getId() + " no puede crear hijos");
        }
        
        // Limitar el número de hijos que puede crear un proceso (máximo 4)
        if (this.procesosHijos.size() >= 4) {
            System.out.println("FORK BLOQUEADO: " + this.getId() + " ya tiene el máximo de hijos permitidos (4)");
            return null;
        }
        
        // El proceso hijo llega al mismo tiempo que el padre + un pequeño delay
        int tiempoLlegadaHijo = this.getTiempoDeLlegada() + 1;
        
        Proceso procesoHijo = new Proceso(duracion, tiempoLlegadaHijo, tamanioSlot, this);
        this.procesosHijos.add(procesoHijo);
        
        System.out.println("FORK: " + this.getId() + " creó proceso hijo " + procesoHijo.getId());
        return procesoHijo;
    }
    
    // Crear un proceso hijo con tiempo de llegada específico
    public Proceso crearProcesoHijo(int duracion, int tamanioSlot, int tiempoLlegada) {
        if (!this.tieneHijos) {
            throw new IllegalStateException("El proceso " + this.getId() + " no puede crear hijos");
        }
        
        // Limitar el número de hijos que puede crear un proceso (máximo 4)
        if (this.procesosHijos.size() >= 4) {
            System.out.println("FORK BLOQUEADO: " + this.getId() + " ya tiene el máximo de hijos permitidos (4)");
            return null;
        }
        
        Proceso procesoHijo = new Proceso(duracion, tiempoLlegada, tamanioSlot, this);
        this.procesosHijos.add(procesoHijo);
        
        System.out.println("FORK: " + this.getId() + " creó proceso hijo " + procesoHijo.getId() + 
                          " con llegada en tiempo " + tiempoLlegada);
        return procesoHijo;
    }
    
    // Obtener el proceso padre
    public Proceso getProcesoPadre() {
        return this.procesoPadre;
    }
    
    // Obtener lista de procesos hijos
    public List<Proceso> getProcesosHijos() {
        return new ArrayList<>(this.procesosHijos); // Devolver copia para evitar modificaciones externas
    }
    
    // Verificar si es un proceso hijo (tiene padre)
    public boolean esProcesoHijo() {
        return this.procesoPadre != null;
    }
    
    // Verificar si es un proceso padre (tiene hijos)
    public boolean esProcesoConHijos() {
        return !this.procesosHijos.isEmpty();
    }
    
    // Contar total de procesos hijos
    public int cantidadHijos() {
        return this.procesosHijos.size();
    }
    
    // Verificar si el proceso padre puede terminar (todos los hijos han terminado)
    public boolean puedeTerminarProcesoPadre() {
        if (!this.esProcesoConHijos()) {
            return true; // No tiene hijos, puede terminar normalmente
        }
        
        // Verificar que todos los hijos hayan terminado
        for (Proceso hijo : this.procesosHijos) {
            if (hijo.getEstado() != EstadoProceso.TERMINADO) {
                return false; // Aún hay hijos sin terminar
            }
        }
        return true; // Todos los hijos han terminado
    }
    
    // Marcar proceso padre como terminado cuando todos los hijos terminen
    public void verificarTerminacionPadre() {
        if (this.esProcesoConHijos() && this.puedeTerminarProcesoPadre() && 
            this.getEstado() != EstadoProceso.TERMINADO) {
            this.setEstado(EstadoProceso.TERMINADO);
            System.out.println("👨‍👩‍👧‍👦 PADRE TERMINADO: " + this.getId() + 
                             " terminó porque todos sus " + this.cantidadHijos() + " hijos terminaron");
        }
    }
}

