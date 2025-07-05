package com.example.demo;

public class Core extends Thread {
    int id;
    Proceso proceso_ejecucion;
    boolean ocupado;
    int tiempoEjecucion;
    private Controlador controlador;
    private volatile boolean running;

    // Control de velocidad y pausa
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

    // Pausar/reanudar core
    public void setPausado(boolean pausado) {
        synchronized (pausaLock) {
            this.pausado = pausado;
            if (!pausado) {
                pausaLock.notifyAll();
            }
            System.out.println("Core " + id + " " + (pausado ? "PAUSADO" : "REANUDADO"));
        }
    }

    // Esperar cuando está pausado
    private void esperarSiPausado() {
        synchronized (pausaLock) {
            while (pausado && running) {
                try {
                    System.out.println("Core " + id + " pausado, esperando...");
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

                    // Ejecutar proceso
                    int nuevaDuracion = proceso_ejecucion.getDuracion() - 1;
                    proceso_ejecucion.setDuracion(nuevaDuracion);
                    tiempoEjecucion++;
                    proceso_ejecucion.incrementarTiempoCpuAcumulado();

                    System.out.println("Core " + id + " ejecutando " + proceso_ejecucion.getId() +
                            " - Duración restante: " + nuevaDuracion);

                    // NUEVA FUNCIONALIDAD: Crear múltiples procesos hijos durante la ejecución
                    // Solo procesos habilitados, probabilidad reducida y crear entre 1-4 hijos
                    if (proceso_ejecucion.puedeCrearHijos() && tiempoEjecucion == 2 && 
                        Math.random() < 0.3 && proceso_ejecucion.cantidadHijos() == 0) {
                        synchronized (controlador) {
                            crearProcesosHijos();
                        }
                    }

                    // Verificar preempción por quantum (Round Robin)
                    if (controlador.algoritmo == TipoAlgoritmo.RR && controlador.quantum > 0 && tiempoEjecucion >= controlador.quantum) {
                        System.out.println("Core " + id + " - " + proceso_ejecucion.getId() + " PREEMPTO por quantum.");

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
                        System.out.println("Core " + id + " - " + proceso_ejecucion.getId() + " TERMINADO");

                        proceso_ejecucion.setEstado(EstadoProceso.TERMINADO);

                        // Calcular tiempos finales
                        int tiempoActualControlador = controlador.getTiempoActual();
                        int tiempoLlegada = proceso_ejecucion.getTiempoDeLlegada();
                        int tiempoCpuUsado = proceso_ejecucion.getTiempoCpuAcumulado();

                        // Tiempo de retorno = Tiempo actual - Tiempo de llegada
                        int tiempoRetorno = tiempoActualControlador - tiempoLlegada;

                        // Tiempo de espera = Tiempo de retorno - Tiempo de CPU usado
                        int tiempoEspera = tiempoRetorno - tiempoCpuUsado;

                        proceso_ejecucion.setTiempoDeRetorno(tiempoRetorno);
                        proceso_ejecucion.setTiempoDeEspera(Math.max(0, tiempoEspera));

                        System.out.println("DEBUG TIEMPOS - " + proceso_ejecucion.getId() + ":");
                        System.out.println("  Tiempo actual: " + tiempoActualControlador);
                        System.out.println("  Tiempo llegada: " + tiempoLlegada);
                        System.out.println("  CPU acumulado: " + tiempoCpuUsado);
                        System.out.println("  Tiempo retorno: " + tiempoRetorno);
                        System.out.println("  Tiempo espera: " + tiempoEspera);

                        synchronized (controlador) {
                            controlador.agregarProcesoTerminado(proceso_ejecucion);
                            System.out.println("DEBUG: " + proceso_ejecucion.getId() + " agregado a lista de terminados");
                            controlador.liberarMemoriaProceso(proceso_ejecucion);
                            
                            // Si es un proceso hijo, verificar si el padre puede terminar
                            if (proceso_ejecucion.esProcesoHijo() && proceso_ejecucion.getProcesoPadre() != null) {
                                Proceso padre = proceso_ejecucion.getProcesoPadre();
                                padre.verificarTerminacionPadre();
                                
                                // Si el padre terminó, agregarlo también a terminados y liberar memoria
                                if (padre.getEstado() == EstadoProceso.TERMINADO) {
                                    controlador.agregarProcesoTerminado(padre);
                                    controlador.liberarMemoriaProceso(padre);
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
        System.out.println("Core " + id + " terminó su ejecución");
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

        // Despertar hilo pausado para terminar correctamente
        synchronized (pausaLock) {
            pausaLock.notifyAll();
        }

        this.interrupt();
        System.out.println("Core " + id + " detenido");
    }
    
    // NUEVA FUNCIONALIDAD: Crear múltiples procesos hijos desde el proceso en ejecución
    private void crearProcesosHijos() {
        if (proceso_ejecucion == null || !proceso_ejecucion.puedeCrearHijos()) {
            return;
        }
        
        try {
            // Determinar cuántos hijos crear (entre 1 y 4)
            int numHijos = 1 + (int)(Math.random() * 4); // 1-4 hijos
            int tiempoActual = controlador.getTiempoActual();
            
            System.out.println("🚀 FORK MÚLTIPLE: " + proceso_ejecucion.getId() + 
                              " intentando crear " + numHijos + " hijos");
            
            int hijosCreados = 0;
            for (int i = 0; i < numHijos; i++) {
                // Parámetros aleatorios para cada hijo
                int duracionHijo = 2 + (int)(Math.random() * 6); // 2-7 segundos
                int tamanioHijo = 15 + (int)(Math.random() * 35); // 15-49 slots
                
                // Cada hijo llega con un pequeño retraso
                int tiempoLlegadaHijo = tiempoActual + 1 + i;
                
                Proceso procesoHijo = proceso_ejecucion.crearProcesoHijo(duracionHijo, tamanioHijo, tiempoLlegadaHijo);
                
                if (procesoHijo != null) {
                    // Usar el método seguro del controlador
                    if (controlador.agregarProcesoHijoAlSistema(procesoHijo)) {
                        hijosCreados++;
                        System.out.println("   ✅ Hijo " + (i+1) + ": " + procesoHijo.getId() + 
                                         " (D:" + duracionHijo + ", T:" + tamanioHijo + 
                                         ", Llegada:" + tiempoLlegadaHijo + ")");
                    }
                } else {
                    System.out.println("   ❌ No se pudo crear más hijos (límite alcanzado)");
                    break;
                }
            }
            
            if (hijosCreados > 0) {
                System.out.println("🎉 FORK EXITOSO: " + proceso_ejecucion.getId() + 
                                  " creó " + hijosCreados + " hijos");
            }
                              
        } catch (Exception e) {
            System.err.println("Error al crear procesos hijos: " + e.getMessage());
        }
    }

    // Getters
    public boolean isOcupado() { return ocupado; }
    public Proceso getProcesoEjecucion() { return proceso_ejecucion; }
    public int getCoreId() { return id; }
    public int getTiempoEjecucion() { return tiempoEjecucion; }
    public boolean isPausado() { return pausado; }
}