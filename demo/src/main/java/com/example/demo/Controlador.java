package com.example.demo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class Controlador extends Thread {
    private static Controlador instance;

    List<Integer> ram;
    TipoAlgoritmo algoritmo;
    private Queue<Proceso> procesosListos;
    private Queue<Proceso> procesosEnSwap;
    List<Core> cores;
    private List<Proceso> procesosPendientesDeLlegada;
    private List<Proceso> procesosTerminados;

    private final ReentrantLock memoriaLock = new ReentrantLock();
    private final ReentrantLock swapLock = new ReentrantLock();

    // Control de sincronización para procesos hijos
    private final Semaphore forkSemaphore = new Semaphore(10); // Máximo 10 operaciones fork simultáneas
    private final ReentrantLock forkLock = new ReentrantLock();

    int tiempoDeLlegadaProm;
    int tiempoDeEsperaProm;
    int tiempoDeRetornoProm;
    int quantum;

    Map<String, List<Integer>> listaDireccionesProcesMem;
    public Map<Integer, String> memoriaAProceso; // Mapear dirección de memoria -> ID de proceso (público para acceso desde HelloController)

    private int tiempoActual;
    private volatile boolean running;
    private volatile int velocidadSimulacion = 1000;

    // Control de pausa
    private volatile boolean pausado = false;
    private final Object pausaLock = new Object();

    private final int TAMAÑO_RAM = 1024;
    private final int NUMERO_CORES = 4;

    private Controlador() {
        this.ram = new ArrayList<>(Collections.nCopies(TAMAÑO_RAM, 0));
        this.procesosListos = new LinkedList<>();
        this.procesosEnSwap = new LinkedList<>();
        this.cores = new ArrayList<>();
        this.procesosPendientesDeLlegada = new ArrayList<>();
        this.procesosTerminados = new ArrayList<>();
        this.listaDireccionesProcesMem = new HashMap<>();
        this.memoriaAProceso = new HashMap<>(); // Inicializar el mapa de memoria a proceso

        // Crear e iniciar cores
        for (int i = 1; i <= NUMERO_CORES; i++) {
            Core core = new Core(i, this);
            cores.add(core);
            core.start();
        }

        this.tiempoDeLlegadaProm = 0;
        this.tiempoDeEsperaProm = 0;
        this.tiempoDeRetornoProm = 0;
        this.quantum = 0;
        this.tiempoActual = 0;
        this.running = false;
    }

    public static Controlador getInstance() {
        if (instance == null) {
            instance = new Controlador();
        }
        return instance;
    }

    // Pausar/reanudar simulación
    public void setPausado(boolean pausado) {
        synchronized (pausaLock) {
            this.pausado = pausado;

            // Notificar a cores sobre cambio de pausa
            for (Core core : cores) {
                core.setPausado(pausado);
            }

            if (!pausado) {
                pausaLock.notifyAll();
            }

            System.out.println("Simulación " + (pausado ? "PAUSADA" : "REANUDADA"));
        }
    }

    public boolean isPausado() {
        return pausado;
    }

    // Esperar cuando está pausado
    private void esperarSiPausado() {
        synchronized (pausaLock) {
            while (pausado && running) {
                try {
                    System.out.println("Controlador pausado, esperando...");
                    pausaLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    public void setVelocidadSimulacion(int nuevaVelocidad) {
        this.velocidadSimulacion = nuevaVelocidad;

        for (Core core : cores) {
            core.setVelocidadSimulacion(nuevaVelocidad);
        }

        System.out.println("Velocidad de simulación cambiada a: " + (1000.0/nuevaVelocidad) + "x");
    }

    public int getVelocidadSimulacion() {
        return velocidadSimulacion;
    }

    @Override
    public void run() {
        // Ordenar procesos por tiempo de llegada
        Collections.sort(procesosPendientesDeLlegada, Comparator.comparingInt(Proceso::getTiempoDeLlegada));
        System.out.println("=== INICIO SIMULACIÓN ===\n");

        while (running) {
            esperarSiPausado();

            if (!running) break;

            tiempoActual++;
            System.out.println("\n--- Tiempo: " + tiempoActual + " ---");

            // Flujo principal de simulación
            verificarLlegadaProcesos();
            cargarProcesosDesdeSwap();

            if (algoritmo == TipoAlgoritmo.SJF) {
                reordenarColaSJF();
            }

            asignarProcesosACores();
            mostrarEstadoActual();

            if (todosProcesosTerminados()) {
                System.out.println("\n=== SIMULACIÓN COMPLETADA ===");
                calcularEstadisticasFinales();
                break;
            }

            try {
                Thread.sleep(velocidadSimulacion);
            } catch (InterruptedException e) {
                System.out.println("Simulación interrumpida.");
                running = false;
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    // Verificar llegada de procesos y cargarlos (con prioridad a procesos hijos)
    private void verificarLlegadaProcesos() {
        Iterator<Proceso> iterator = procesosPendientesDeLlegada.iterator();

        while (iterator.hasNext()) {
            Proceso p = iterator.next();
            if (p.getTiempoDeLlegada() <= tiempoActual) {
                iterator.remove();

                // Dar prioridad a procesos hijos en la carga de memoria
                boolean esHijo = p.esProcesoHijo();
                boolean cargado = false;
                
                if (esHijo) {
                    // Intentar cargar proceso hijo con prioridad
                    if (cargarProcesoEnMemoria(p)) {
                        cargado = true;
                        System.out.println(">> 👶 HIJO " + p.getId() + " llegó y se cargó en RAM (PRIORIDAD).");
                    } else {
                        // Si no cabe, intentar hacer espacio liberando un proceso no-hijo
                        if (liberarEspacioParaHijo(p)) {
                            if (cargarProcesoEnMemoria(p)) {
                                cargado = true;
                                System.out.println(">> 👶 HIJO " + p.getId() + " llegó y se cargó en RAM (ESPACIO LIBERADO).");
                            }
                        }
                    }
                } else {
                    // Proceso normal
                    cargado = cargarProcesoEnMemoria(p);
                }
                
                if (cargado) {
                    procesosListos.offer(p);
                    p.setEstado(EstadoProceso.ESPERA);
                    p.setUltimaEntradaColaListos(tiempoActual);
                    if (!esHijo) {
                        System.out.println(">> " + p.getId() + " llegó y se cargó en RAM.");
                    }
                } else {
                    moverProcesoASwap(p);
                    String tipoMsg = esHijo ? "👶 HIJO " : "";
                    System.out.println(">> " + tipoMsg + p.getId() + " llegó pero fue a SWAP (RAM llena).");
                }
            } else {
                break;
            }
        }
    }

    private boolean cargarProcesoEnMemoria(Proceso proceso) {
        memoriaLock.lock();
        try {
            return asignarMemoriaProceso(proceso);
        } finally {
            memoriaLock.unlock();
        }
    }

    private void moverProcesoASwap(Proceso proceso) {
        swapLock.lock();
        try {
            procesosEnSwap.offer(proceso);
            proceso.setEstado(EstadoProceso.SWAP);

            List<Integer> direccionesSwap = new ArrayList<>();
            for (int i = 0; i < proceso.getTamanioSlot(); i++) {
                direccionesSwap.add(1000 + procesosEnSwap.size() * 100 + i);
            }
            listaDireccionesProcesMem.put(proceso.getId(), direccionesSwap);

            System.out.println("SWAP: " + proceso.getId() + " movido a disco en direcciones " + direccionesSwap);
        } finally {
            swapLock.unlock();
        }
    }

    // Cargar procesos desde SWAP a memoria cuando hay espacio
    private void cargarProcesosDesdeSwap() {
        if (procesosEnSwap.isEmpty()) return;

        swapLock.lock();
        memoriaLock.lock();
        try {
            Iterator<Proceso> iterator = procesosEnSwap.iterator();
            while (iterator.hasNext()) {
                Proceso proceso = iterator.next();

                if (asignarMemoriaProceso(proceso)) {
                    iterator.remove();
                    procesosListos.offer(proceso);
                    proceso.setEstado(EstadoProceso.ESPERA);
                    proceso.setUltimaEntradaColaListos(tiempoActual);

                    listaDireccionesProcesMem.remove(proceso.getId());

                    System.out.println("SWAP->RAM: " + proceso.getId() + " cargado desde disco a memoria.");
                    break;
                }
            }
        } finally {
            memoriaLock.unlock();
            swapLock.unlock();
        }
    }

    // Reordenar cola para SJF (menor duración primero)
    private void reordenarColaSJF() {
        if (!procesosListos.isEmpty()) {
            List<Proceso> listaTemp = new ArrayList<>(procesosListos);
            Collections.sort(listaTemp, Comparator.comparingInt(Proceso::getDuracion));
            procesosListos.clear();
            procesosListos.addAll(listaTemp);
            System.out.println("Cola reordenada por SJF (menor duración primero)");
        }
    }

    // Asignar procesos a cores disponibles
    private void asignarProcesosACores() {
        for (Core core : cores) {
            if (!core.isOcupado() && !procesosListos.isEmpty()) {
                Proceso proceso = procesosListos.poll();

                // Calcular tiempo de espera acumulado
                int tiempoEsperaEstaRonda = tiempoActual - proceso.getUltimaEntradaColaListos();
                int tiempoEsperaTotal = proceso.getTiempoDeEspera() + tiempoEsperaEstaRonda;
                proceso.setTiempoDeEspera(tiempoEsperaTotal);

                System.out.println("ASIGNACIÓN - " + proceso.getId() + ":");
                System.out.println("  Última entrada cola: " + proceso.getUltimaEntradaColaListos());
                System.out.println("  Tiempo actual: " + tiempoActual);
                System.out.println("  Espera esta ronda: " + tiempoEsperaEstaRonda);
                System.out.println("  Espera total acumulada: " + tiempoEsperaTotal);

                core.asignarProceso(proceso);
                System.out.println(">> " + proceso.getId() + " asignado a Core " + core.getCoreId());
            }
        }
    }

    // Liberar memoria cuando termina un proceso
    public void liberarMemoriaProceso(Proceso proceso) {
        memoriaLock.lock();
        try {
            String procesoIdStr = proceso.getId();
            List<Integer> direccionesLiberadas = new ArrayList<>();

            // Buscar direcciones que pertenecen a este proceso específico
            for (int i = 0; i < ram.size(); i++) {
                if (memoriaAProceso.containsKey(i) && memoriaAProceso.get(i).equals(procesoIdStr)) {
                    ram.set(i, 0);
                    memoriaAProceso.remove(i); // Limpiar el mapeo
                    direccionesLiberadas.add(i);
                }
            }

            System.out.println("Memoria liberada de " + proceso.getId() + ": " + direccionesLiberadas);
            cargarProcesosDesdeSwap();

        } finally {
            memoriaLock.unlock();
        }
    }

    // Asignar memoria a un proceso
    private boolean asignarMemoriaProceso(Proceso proceso) {
        int tamanioNecesario = proceso.getTamanioSlot();

        // Contar slots libres disponibles
        long slotsLibres = ram.stream().mapToLong(slot -> slot == 0 ? 1 : 0).sum();

        if (slotsLibres < tamanioNecesario) {
            return false;
        }

        // Crear lista de índices libres
        List<Integer> indicesLibres = new ArrayList<>();
        for (int i = 0; i < ram.size(); i++) {
            if (ram.get(i) == 0) {
                indicesLibres.add(i);
            }
        }

        // Mezclar aleatoriamente los índices libres (RAM = Random Access Memory)
        Collections.shuffle(indicesLibres);

        // Asignar los primeros N slots aleatorios
        String procesoIdStr = proceso.getId(); // ID completo (incluye .H1, .H2, etc.)
        int procesoIdNum = extraerNumeroIdProceso(procesoIdStr); // Solo número para compatibilidad
        List<Integer> direccionesAsignadas = new ArrayList<>();

        for (int i = 0; i < tamanioNecesario; i++) {
            int indiceAleatorio = indicesLibres.get(i);
            ram.set(indiceAleatorio, procesoIdNum);
            direccionesAsignadas.add(indiceAleatorio);
            memoriaAProceso.put(indiceAleatorio, procesoIdStr); // Mapear dirección a proceso completo
        }

        // Ordenar direcciones para mostrar mejor en logs
        direccionesAsignadas.sort(Integer::compareTo);

        System.out.println("Memoria ALEATORIA asignada a " + proceso.getId() +
                " en direcciones: " + direccionesAsignadas);
        return true;
    }

    private boolean realizarSwapping(Proceso procesoNuevo) {
        memoriaLock.lock();
        swapLock.lock();
        try {
            Set<String> procesosEjecutandose = cores.stream()
                    .filter(Core::isOcupado)
                    .filter(core -> core.getProcesoEjecucion() != null)
                    .map(core -> core.getProcesoEjecucion().getId())
                    .collect(Collectors.toSet());

            Iterator<Proceso> iterator = procesosListos.iterator();
            while (iterator.hasNext()) {
                Proceso candidatoSwap = iterator.next();

                if (!procesosEjecutandose.contains(candidatoSwap.getId())) {
                    liberarMemoriaProcesoSinCargarDesdeSwap(candidatoSwap);
                    iterator.remove();
                    moverProcesoASwap(candidatoSwap);

                    System.out.println("SWAP: " + candidatoSwap.getId() + " movido a disco para hacer espacio");

                    if (asignarMemoriaProceso(procesoNuevo)) {
                        return true;
                    }
                }
            }

            return false;
        } finally {
            swapLock.unlock();
            memoriaLock.unlock();
        }
    }

    private void liberarMemoriaProcesoSinCargarDesdeSwap(Proceso proceso) {
        int procesoId = Integer.parseInt(proceso.getId().replace("Proceso ", ""));
        for (int i = 0; i < ram.size(); i++) {
            if (ram.get(i) == procesoId) {
                ram.set(i, 0);
                memoriaAProceso.remove(i); // Desmapear dirección de proceso
            }
        }
    }

    // Verificar si todos los procesos han terminado
    private boolean todosProcesosTerminados() {
        return procesosPendientesDeLlegada.isEmpty() &&
                procesosListos.isEmpty() &&
                procesosEnSwap.isEmpty() &&
                cores.stream().noneMatch(Core::isOcupado);
    }

    // Calcular estadísticas finales
    private void calcularEstadisticasFinales() {
        if (procesosTerminados.isEmpty()) return;

        int sumaEspera = procesosTerminados.stream().mapToInt(Proceso::getTiempoDeEspera).sum();
        int sumaRetorno = procesosTerminados.stream().mapToInt(Proceso::getTiempoDeRetorno).sum();

        tiempoDeEsperaProm = sumaEspera / procesosTerminados.size();
        tiempoDeRetornoProm = sumaRetorno / procesosTerminados.size();

        System.out.println("\n=== ESTADÍSTICAS FINALES ===");
        System.out.println("Tiempo promedio de espera: " + tiempoDeEsperaProm);
        System.out.println("Tiempo promedio de retorno: " + tiempoDeRetornoProm);
    }

    private void mostrarEstadoActual() {
        System.out.println("Procesos en RAM (listos): " + procesosListos.size());
        System.out.println("Procesos en SWAP: " + procesosEnSwap.size());
        System.out.println("Cores ocupados: " + cores.stream().mapToInt(c -> c.isOcupado() ? 1 : 0).sum());

        Set<Integer> procesosEnMemoria = new HashSet<>();
        for (Integer procesoId : ram) {
            if (procesoId != 0) {
                procesosEnMemoria.add(procesoId);
            }
        }
        System.out.println("Procesos en memoria: " + procesosEnMemoria);
        System.out.println("Memoria libre: " + Collections.frequency(ram, 0) + "/" + TAMAÑO_RAM);
    }

    public void agregarProcesoTerminado(Proceso proceso) {
        procesosTerminados.add(proceso);
        System.out.println("CONTROLADOR: " + proceso.getId() + " agregado a lista de terminados");
    }

    public void agregarProcesoAlSistema(Proceso p) {
        procesosPendientesDeLlegada.add(p);
        System.out.println(p.getId() + " añadido al sistema. Llegada: " + p.getTiempoDeLlegada());
    }

    public void setAlgoritmo(TipoAlgoritmo algoritmo) {
        this.algoritmo = algoritmo;
        System.out.println("Algoritmo establecido: " + algoritmo.getNombre());
    }

    // Devolver proceso a cola (Round Robin)
    public synchronized void devolverProcesoACola(Proceso proceso) {
        this.procesosListos.offer(proceso);
        proceso.setUltimaEntradaColaListos(tiempoActual);
        System.out.println("CONTROLADOR: " + proceso.getId() + " devuelto a la cola de listos (RR).");
    }

    public void setQuantum(int quantum) {
        this.quantum = quantum;
        System.out.println("Quantum establecido: " + quantum);
    }

    // Método seguro para agregar procesos hijos con control de sincronización
    public boolean agregarProcesoHijoAlSistema(Proceso procesoHijo) {
        try {
            // Adquirir semáforo para limitar operaciones concurrentes
            forkSemaphore.acquire();
            
            forkLock.lock();
            try {
                // Verificar que el proceso hijo es válido
                if (procesoHijo == null) {
                    return false;
                }
                
                // Agregar a la lista de procesos pendientes
                procesosPendientesDeLlegada.add(procesoHijo);
                
                System.out.println("🚀 HIJO AGREGADO: " + procesoHijo.getId() + 
                                 " (Padre: " + (procesoHijo.getProcesoPadre() != null ? 
                                   procesoHijo.getProcesoPadre().getId() : "N/A") + 
                                 ", Llegada: " + procesoHijo.getTiempoDeLlegada() + ")");
                return true;
                
            } finally {
                forkLock.unlock();
            }
        } catch (InterruptedException e) {
            System.err.println("Error al agregar proceso hijo: " + e.getMessage());
            Thread.currentThread().interrupt();
            return false;
        } finally {
            forkSemaphore.release();
        }
    }

    // Liberar espacio en memoria para un proceso hijo prioritario
    private boolean liberarEspacioParaHijo(Proceso procesoHijo) {
        memoriaLock.lock();
        try {
            // Buscar procesos no-hijos en RAM que puedan moverse a SWAP
            Iterator<Proceso> iterator = procesosListos.iterator();
            while (iterator.hasNext()) {
                Proceso candidato = iterator.next();
                
                // Solo mover procesos que NO sean hijos y que no estén ejecutándose
                if (!candidato.esProcesoHijo() && !estaEjecutandose(candidato)) {
                    iterator.remove();
                    liberarMemoriaProcesoSinCargarDesdeSwap(candidato);
                    moverProcesoASwap(candidato);
                    
                    System.out.println("🔄 SWAP PARA HIJO: " + candidato.getId() + 
                                     " movido a SWAP para hacer espacio a " + procesoHijo.getId());
                    return true;
                }
            }
            return false;
        } finally {
            memoriaLock.unlock();
        }
    }
    
    // Verificar si un proceso está ejecutándose en algún core
    private boolean estaEjecutandose(Proceso proceso) {
        return cores.stream().anyMatch(core -> 
            core.isOcupado() && 
            core.getProcesoEjecucion() != null && 
            core.getProcesoEjecucion().getId().equals(proceso.getId())
        );
    }

    // Método auxiliar para extraer el número de ID de un proceso (maneja hijos)
    private int extraerNumeroIdProceso(String procesoId) {
        try {
            // Para "Proceso 1" devuelve 1
            // Para "Proceso 1.H1" devuelve 1 (el del padre)
            String[] partes = procesoId.replace("Proceso ", "").split("\\.");
            return Integer.parseInt(partes[0]);
        } catch (Exception e) {
            System.err.println("Error al extraer ID de proceso: " + procesoId);
            return 1; // Valor por defecto
        }
    }

    // Getters
    public Queue<Proceso> getProcesosListos() { return procesosListos; }
    public Queue<Proceso> getProcesosEnSwap() { return procesosEnSwap; }
    public void setRunning(boolean running) { this.running = running; }
    public int getTiempoActual() { return tiempoActual; }
    public List<Proceso> getProcesosPendientesDeLlegada() { return procesosPendientesDeLlegada; }
    public boolean getRunnin() { return running; }
    public List<Proceso> getProcesosTerminados() { return procesosTerminados; }

    // Reset completo del controlador
    public void reset() {
        System.out.println("=== INICIANDO RESET DEL CONTROLADOR ===");

        this.running = false;
        this.pausado = false;

        // Detener cores
        for (Core core : cores) {
            if (core != null) {
                core.detener();
            }
        }

        // Esperar terminación de hilos
        try {
            if (this.isAlive()) {
                this.join(2000);
            }

            for (Core core : cores) {
                if (core != null && core.isAlive()) {
                    core.join(1000);
                }
            }
        } catch (InterruptedException e) {
            System.err.println("Interrupción durante el reset");
            Thread.currentThread().interrupt();
        }

        // Limpiar estructuras de datos
        this.ram.clear();
        this.ram.addAll(Collections.nCopies(TAMAÑO_RAM, 0));
        this.procesosListos.clear();
        this.procesosEnSwap.clear();
        this.cores.clear();
        this.procesosPendientesDeLlegada.clear();
        this.procesosTerminados.clear();
        this.listaDireccionesProcesMem.clear();
        this.memoriaAProceso.clear(); // Limpiar mapeo de memoria a proceso
        
        // Limpiar semáforo (liberar todos los permisos)
        forkSemaphore.drainPermits();
        forkSemaphore.release(10); // Restaurar permisos iniciales

        // Reinicializar variables
        this.tiempoDeLlegadaProm = 0;
        this.tiempoDeEsperaProm = 0;
        this.tiempoDeRetornoProm = 0;
        this.quantum = 0;
        this.tiempoActual = 0;
        this.running = false;
        this.velocidadSimulacion = 1000;
        this.pausado = false;
        this.algoritmo = null;

        // Crear nuevos cores
        for (int i = 1; i <= NUMERO_CORES; i++) {
            Core core = new Core(i, this);
            cores.add(core);
            core.start();
        }

        Proceso.resetNextId();
        instance = null;

        System.out.println("=== RESET DEL CONTROLADOR COMPLETADO ===");
    }
}