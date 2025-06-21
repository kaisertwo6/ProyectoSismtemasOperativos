package com.example.demo;

public class Core extends Thread {
    int id;
    Proceso proceso_ejecucion;
    boolean ocupado;
    int tiempoEjecucion;
    private Controlador controlador;
    private volatile boolean running;

    public Core(int id, Controlador controlador) {
        this.id = id;
        this.proceso_ejecucion = null;
        this.ocupado = false;
        this.tiempoEjecucion = 0;
        this.controlador = controlador;
        this.running = true;
    }

    @Override
    public void run() {
        while (running) {
            try {
                if (ocupado && proceso_ejecucion != null) {
                    Thread.sleep(1000);

                    int nuevaDuracion = proceso_ejecucion.getDuracion() - 1;
                    proceso_ejecucion.setDuracion(nuevaDuracion);
                    tiempoEjecucion++;

                    System.out.println("Core " + id + " ejecutando " + proceso_ejecucion.getId() +
                            " - Duración restante: " + nuevaDuracion);

                    if (nuevaDuracion <= 0) {
                        System.out.println("Core " + id + " - " + proceso_ejecucion.getId() + " TERMINADO");

                        proceso_ejecucion.setEstado(EstadoProceso.TERMINADO);

                        int tiempoRetorno = controlador.getTiempoActual() - proceso_ejecucion.getTiempoDeLlegada();
                        int tiempoEspera = tiempoRetorno - tiempoEjecucion;
                        proceso_ejecucion.setTiempoDeRetorno(tiempoRetorno);
                        proceso_ejecucion.setTiempoDeEspera(tiempoEspera);

                        controlador.agregarProcesoTerminado(proceso_ejecucion);
                        System.out.println("DEBUG: " + proceso_ejecucion.getId() + " agregado a lista de terminados");

                        controlador.liberarMemoriaProceso(proceso_ejecucion);
                        liberarCore();
                    }
                } else {
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                running = false;
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public synchronized boolean asignarProceso(Proceso proceso) {
        if (!ocupado) {
            this.proceso_ejecucion = proceso;
            this.ocupado = true;
            this.tiempoEjecucion = 0;
            proceso.setEstado(EstadoProceso.EJECUCION);

            System.out.println("Core " + id + " asignado al " + proceso.getId());
            return true;
        }
        return false;
    }

    public synchronized void liberarCore() {
        this.proceso_ejecucion = null;
        this.ocupado = false;
        this.tiempoEjecucion = 0;
        System.out.println("Core " + id + " liberado");
    }

    public void detener() {
        this.running = false;
        this.interrupt();
    }

    public boolean isOcupado() { return ocupado; }
    public Proceso getProcesoEjecucion() { return proceso_ejecucion; }
    public int getCoreId() { return id; }
    public int getTiempoEjecucion() { return tiempoEjecucion; }
}