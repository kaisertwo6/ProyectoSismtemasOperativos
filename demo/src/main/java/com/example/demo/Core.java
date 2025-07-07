package com.example.demo;

public class Core extends Thread {
    int id;
    Proceso proceso_ejecucion;
    boolean ocupado;
    int tiempoEjecucion;
    private Controlador controlador;
    private volatile boolean running;

    private volatile int velocidadSimulacion = 1000;
    private volatile boolean pausado = false;
    private final Object pausaLock = new Object();

    public Core(int id, Controlador controlador) {
        this.id = id;
        this.proceso_ejecucion = null;
        this.ocupado = false;
        this.tiempoEjecucion = 0;
        this.controlador = controlador;
        this.running = true;
        this.velocidadSimulacion = controlador.getVelocidadSimulacion();
    }

    public void setVelocidadSimulacion(int nuevaVelocidad) {
        this.velocidadSimulacion = nuevaVelocidad;
    }

    public void setPausado(boolean pausado) {
        synchronized (pausaLock) {
            this.pausado = pausado;
            if (!pausado) {
                pausaLock.notifyAll();
            }
        }
    }

    private void esperarSiPausado() {
        synchronized (pausaLock) {
            while (pausado && running) {
                try {
                    pausaLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    @Override
    public void run() {
        while (running) {
            try {
                esperarSiPausado();
                if (!running) break;

                if (ocupado && proceso_ejecucion != null) {
                    Thread.sleep(velocidadSimulacion);

                    int nuevaDuracion = proceso_ejecucion.getDuracion() - 1;
                    proceso_ejecucion.setDuracion(nuevaDuracion);
                    tiempoEjecucion++;
                    proceso_ejecucion.incrementarTiempoCpuAcumulado();

                    controlador.registrarEjecucionProceso(proceso_ejecucion.getId());

                    System.out.println("Core " + id + " ejecutando " + proceso_ejecucion.getId() +
                            " - Duración restante: " + nuevaDuracion);

                    // Crear procesos hijos durante ejecución (solo para procesos normales, no programas)
                    if (proceso_ejecucion.puedeCrearHijos() && tiempoEjecucion == 2 &&
                            Math.random() < 0.3 && proceso_ejecucion.cantidadHijos() == 0 &&
                            !proceso_ejecucion.esPrograma()) {
                        synchronized (controlador) {
                            crearProcesosHijos();
                        }
                    }

                    // Verificar preempción por quantum (Round Robin)
                    if (controlador.algoritmo == TipoAlgoritmo.RR && controlador.quantum > 0 &&
                            tiempoEjecucion >= controlador.quantum) {

                        System.out.println("🔄 PREEMPCIÓN RR: " + proceso_ejecucion.getId() +
                                " quantum agotado (" + controlador.quantum + " unidades)");

                        proceso_ejecucion.setEstado(EstadoProceso.ESPERA);
                        proceso_ejecucion.setUltimaEntradaColaListos(controlador.getTiempoActual());

                        synchronized (controlador) {
                            controlador.devolverProcesoACola(proceso_ejecucion);
                        }
                        liberarCore();
                        continue;
                    }

                    // Verificar si proceso terminó
                    if (nuevaDuracion <= 0) {
                        proceso_ejecucion.setEstado(EstadoProceso.TERMINADO);

                        // Calcular tiempos finales
                        int tiempoActualControlador = controlador.getTiempoActual();
                        int tiempoLlegada = proceso_ejecucion.getTiempoDeLlegada();
                        int tiempoCpuUsado = proceso_ejecucion.getTiempoCpuAcumulado();

                        int tiempoRetorno = tiempoActualControlador - tiempoLlegada;
                        int tiempoEspera = tiempoRetorno - tiempoCpuUsado;

                        proceso_ejecucion.setTiempoDeRetorno(tiempoRetorno);
                        proceso_ejecucion.setTiempoDeEspera(Math.max(0, tiempoEspera));

                        System.out.println("✅ TERMINADO: " + proceso_ejecucion.getId() +
                                " (TR:" + tiempoRetorno + ", TE:" + tiempoEspera + ")");

                        synchronized (controlador) {
                            controlador.agregarProcesoTerminado(proceso_ejecucion);
                            controlador.liberarMemoriaProceso(proceso_ejecucion);

                            // Verificar terminación de proceso padre o programa
                            if (proceso_ejecucion.esProcesoHijo() && proceso_ejecucion.getProcesoPadre() != null) {
                                Proceso padre = proceso_ejecucion.getProcesoPadre();

                                // Si es un programa, verificar terminación del programa
                                if (padre.esPrograma()) {
                                    padre.verificarTerminacionPrograma();
                                    if (padre.getEstado() == EstadoProceso.TERMINADO) {
                                        controlador.agregarProcesoTerminado(padre);
                                    }
                                } else {
                                    // Proceso padre normal (fork)
                                    if (padre.todosLosHijosTerminaron()) {
                                        padre.setEstado(EstadoProceso.TERMINADO);
                                        controlador.agregarProcesoTerminado(padre);
                                        controlador.liberarMemoriaProceso(padre);
                                    }
                                }
                            }
                        }
                        liberarCore();
                    }
                }

                Thread.sleep(velocidadSimulacion / 2);

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

            // NUEVO: Registrar que el proceso inicia ejecución
            controlador.registrarEjecucionProceso(proceso.getId());

            return true;
        }
        return false;
    }

    public synchronized void liberarCore() {
        this.proceso_ejecucion = null;
        this.ocupado = false;
        this.tiempoEjecucion = 0;
    }

    public void detener() {
        this.running = false;
        synchronized (pausaLock) {
            pausaLock.notifyAll();
        }
        this.interrupt();
    }

    private void crearProcesosHijos() {
        if (proceso_ejecucion == null || !proceso_ejecucion.puedeCrearHijos()) {
            return;
        }

        try {
            int numHijos = 1 + (int)(Math.random() * 4);
            int tiempoActual = controlador.getTiempoActual();

            int hijosCreados = 0;
            for (int i = 0; i < numHijos; i++) {
                int duracionHijo = 2 + (int)(Math.random() * 6);
                int tamanioHijo = 15 + (int)(Math.random() * 35);
                int tiempoLlegadaHijo = tiempoActual + 1 + i;

                Proceso procesoHijo = proceso_ejecucion.crearProcesoHijo(duracionHijo, tamanioHijo, tiempoLlegadaHijo);

                if (procesoHijo != null) {
                    if (controlador.agregarProcesoHijoAlSistema(procesoHijo)) {
                        hijosCreados++;
                        System.out.println("👶 FORK: " + proceso_ejecucion.getId() +
                                " creó hijo " + procesoHijo.getId());
                    }
                } else {
                    break;
                }
            }

        } catch (Exception e) {
            System.err.println("Error al crear procesos hijos: " + e.getMessage());
        }
    }

    public boolean isOcupado() { return ocupado; }
    public Proceso getProcesoEjecucion() { return proceso_ejecucion; }
    public int getCoreId() { return id; }
    public int getTiempoEjecucion() { return tiempoEjecucion; }
    public boolean isPausado() { return pausado; }
}