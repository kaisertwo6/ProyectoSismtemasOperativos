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
                    proceso_ejecucion.incrementarTiempoCpuAcumulado();

                    System.out.println("Core " + id + " ejecutando " + proceso_ejecucion.getId() +
                            " - Duración restante: " + nuevaDuracion);
                    if (controlador.algoritmo == TipoAlgoritmo.RR && controlador.quantum > 0 && tiempoEjecucion >= controlador.quantum) {
                        System.out.println("Core " + id + " - " + proceso_ejecucion.getId() + " PREEMPTO por quantum.");
                        proceso_ejecucion.setEstado(EstadoProceso.ESPERA); // Vuelve a estado de espera

                        // Actualiza el tiempo en que el proceso entra a la cola de listos de nuevo
                        proceso_ejecucion.setUltimaEntradaColaListos(controlador.getTiempoActual()); // ¡Nuevo!

                        // Se usa 'devolverProcesoACola' en Controlador para asegurar FIFO en la cola
                        synchronized (controlador) { // Sincroniza el acceso al controlador
                            controlador.devolverProcesoACola(proceso_ejecucion); // Nuevo método en Controlador
                        }
                        liberarCore();
                        continue;
                    }

                    if (nuevaDuracion <= 0) {
                        System.out.println("Core " + id + " - " + proceso_ejecucion.getId() + " TERMINADO");

                        proceso_ejecucion.setEstado(EstadoProceso.TERMINADO);
                        int tiempoEsperaParcial = controlador.getTiempoActual() - proceso_ejecucion.getUltimaEntradaColaListos();
                        proceso_ejecucion.setTiempoDeEspera(proceso_ejecucion.getTiempoDeEspera() + tiempoEsperaParcial);
                        int tiempoRetorno = controlador.getTiempoActual() - proceso_ejecucion.getTiempoDeLlegada();
                        int tiempoEspera = tiempoRetorno - tiempoEjecucion;
                        proceso_ejecucion.setTiempoDeEspera(tiempoRetorno - proceso_ejecucion.getTiempoCpuAcumulado());
                        proceso_ejecucion.setTiempoDeEspera(tiempoEspera);


                        synchronized (controlador) {
                            controlador.agregarProcesoTerminado(proceso_ejecucion);
                            System.out.println("DEBUG: " + proceso_ejecucion.getId() + " agregado a lista de terminados");
                            controlador.liberarMemoriaProceso(proceso_ejecucion); // Esto libera la memoria del proceso terminado
                        }
                        liberarCore();
                    }
                }
                Thread.sleep(500);
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