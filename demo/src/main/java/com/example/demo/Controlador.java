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
    List<Integer> swap;
    TipoAlgoritmo algoritmo;
    private Queue<Proceso> procesosListos;
    private Queue<Proceso> procesosEnSwap;
    private Queue<Proceso> procesosEnColaEspera;
    List<Core> cores;
    private List<Proceso> procesosPendientesDeLlegada;
    private List<Proceso> procesosTerminados;

    private final ReentrantLock memoriaLock = new ReentrantLock();
    private final ReentrantLock swapLock = new ReentrantLock();

    private final Semaphore forkSemaphore = new Semaphore(10);
    private final ReentrantLock forkLock = new ReentrantLock();

    int tiempoDeLlegadaProm;
    int tiempoDeEsperaProm;
    int tiempoDeRetornoProm;
    int quantum;

    Map<String, List<Integer>> listaDireccionesProcesMem;
    public Map<Integer, String> memoriaAProceso;
    public Map<Integer, String> swapAProceso;

    private int tiempoActual;
    private volatile boolean running;
    private volatile int velocidadSimulacion = 1000;

    private volatile boolean pausado = false;
    private final Object pausaLock = new Object();

    private final int TAMAÑO_RAM = 1024;
    private int tamaño_swap = 512; // MODIFICADO: Ahora es variable
    private boolean swapLimitado = true; // NUEVO: Flag para controlar si SWAP es limitado
    private final int NUMERO_CORES = 4;

    private Controlador() {
        this.ram = new ArrayList<>(Collections.nCopies(TAMAÑO_RAM, 0));
        this.swap = new ArrayList<>(Collections.nCopies(tamaño_swap, 0));
        this.procesosListos = new LinkedList<>();
        this.procesosEnSwap = new LinkedList<>();
        this.procesosEnColaEspera = new LinkedList<>();
        this.cores = new ArrayList<>();
        this.procesosPendientesDeLlegada = new ArrayList<>();
        this.procesosTerminados = new ArrayList<>();
        this.listaDireccionesProcesMem = new HashMap<>();
        this.memoriaAProceso = new HashMap<>();
        this.swapAProceso = new HashMap<>();

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

    // NUEVO: Configurar modo de SWAP
    public void configurarSwap(boolean limitado, int tamaño) {
        this.swapLimitado = limitado;
        if (limitado) {
            this.tamaño_swap = tamaño;
            // Redimensionar SWAP si es necesario
            if (swap.size() != tamaño) {
                swap.clear();
                swap.addAll(Collections.nCopies(tamaño, 0));
                swapAProceso.clear();
            }
        } else {
            // SWAP ilimitado: usar un tamaño muy grande
            this.tamaño_swap = Integer.MAX_VALUE;
        }
        System.out.println("SWAP configurado: " + (limitado ? "Limitado (" + tamaño + " slots)" : "Ilimitado"));
    }

    public void setPausado(boolean pausado) {
        synchronized (pausaLock) {
            this.pausado = pausado;

            for (Core core : cores) {
                core.setPausado(pausado);
            }

            if (!pausado) {
                pausaLock.notifyAll();
            }
        }
    }

    public boolean isPausado() {
        return pausado;
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

    public void setVelocidadSimulacion(int nuevaVelocidad) {
        this.velocidadSimulacion = nuevaVelocidad;

        for (Core core : cores) {
            core.setVelocidadSimulacion(nuevaVelocidad);
        }
    }

    public int getVelocidadSimulacion() {
        return velocidadSimulacion;
    }

    @Override
    public void run() {
        Collections.sort(procesosPendientesDeLlegada, Comparator.comparingInt(Proceso::getTiempoDeLlegada));
        System.out.println("=== INICIO SIMULACIÓN ===");
        System.out.println("SWAP: " + (swapLimitado ? "Limitado (" + tamaño_swap + " slots)" : "Ilimitado"));

        while (running) {
            esperarSiPausado();

            if (!running) break;

            tiempoActual++;
            System.out.println("\n--- Tiempo: " + tiempoActual + " ---");

            verificarLlegadaProcesos();
            cargarProcesosDesdeSwap();
            procesarColaEspera();

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
                running = false;
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void verificarLlegadaProcesos() {
        Iterator<Proceso> iterator = procesosPendientesDeLlegada.iterator();

        while (iterator.hasNext()) {
            Proceso p = iterator.next();
            if (p.getTiempoDeLlegada() <= tiempoActual) {
                iterator.remove();

                if (p.esPrograma()) {
                    System.out.println(">> PROGRAMA " + p.getId() + " llegó (controlador, no se ejecuta directamente)");
                    continue;
                }

                boolean esHijo = p.esProcesoHijo();
                boolean cargado = false;

                if (esHijo) {
                    if (cargarProcesoEnMemoria(p)) {
                        cargado = true;
                        System.out.println(">> 👶 HIJO " + p.getId() + " llegó y se cargó en RAM (PRIORIDAD).");
                    } else {
                        if (liberarEspacioParaHijo(p)) {
                            if (cargarProcesoEnMemoria(p)) {
                                cargado = true;
                                System.out.println(">> 👶 HIJO " + p.getId() + " llegó y se cargó en RAM (ESPACIO LIBERADO).");
                            }
                        }
                    }
                } else {
                    cargado = cargarProcesoEnMemoria(p);
                }

                if (cargado) {
                    procesosListos.offer(p);
                    p.setEstado(EstadoProceso.ESPERA);
                    p.setUltimaEntradaColaListos(tiempoActual);
                    if (!esHijo) {
                        String tipoMsg = p.getNombrePrograma() != null ?
                                " (Parte de " + p.getNombrePrograma() + ")" : "";
                        System.out.println(">> " + p.getId() + " llegó y se cargó en RAM" + tipoMsg);
                    }
                } else {
                    intentarMoverASwapOColaEspera(p);
                }
            } else {
                break;
            }
        }
    }

    private void intentarMoverASwapOColaEspera(Proceso proceso) {
        if (moverProcesoASwap(proceso)) {
            String tipoMsg = proceso.esProcesoHijo() ? "👶 HIJO " : "";
            if (proceso.getNombrePrograma() != null) {
                tipoMsg += "(Parte de " + proceso.getNombrePrograma() + ") ";
            }
            System.out.println(">> " + tipoMsg + proceso.getId() + " llegó y fue a SWAP.");
        } else {
            if (swapLimitado) {
                moverProcesoAColaEspera(proceso);
                String tipoMsg = proceso.esProcesoHijo() ? "👶 HIJO " : "";
                if (proceso.getNombrePrograma() != null) {
                    tipoMsg += "(Parte de " + proceso.getNombrePrograma() + ") ";
                }
                System.out.println(">> " + tipoMsg + proceso.getId() + " llegó y fue a COLA DE ESPERA (SWAP lleno).");
            } else {
                // SWAP ilimitado: expandir automáticamente
                expandirSwap(proceso.getTamanioSlot());
                moverProcesoASwap(proceso);
                String tipoMsg = proceso.esProcesoHijo() ? "👶 HIJO " : "";
                if (proceso.getNombrePrograma() != null) {
                    tipoMsg += "(Parte de " + proceso.getNombrePrograma() + ") ";
                }
                System.out.println(">> " + tipoMsg + proceso.getId() + " llegó y fue a SWAP (expandido automáticamente).");
            }
        }
    }

    // NUEVO: Expandir SWAP cuando es ilimitado
    private void expandirSwap(int tamanioNecesario) {
        int slotsLibres = Collections.frequency(swap, 0);
        if (slotsLibres < tamanioNecesario) {
            int slotsAgregar = tamanioNecesario - slotsLibres + 100; // Agregar un poco extra
            for (int i = 0; i < slotsAgregar; i++) {
                swap.add(0);
            }
            System.out.println("SWAP expandido: +" + slotsAgregar + " slots (total: " + swap.size() + ")");
        }
    }

    private void moverProcesoAColaEspera(Proceso proceso) {
        procesosEnColaEspera.offer(proceso);
        proceso.setEstado(EstadoProceso.PENDIENTE);
    }

    private void procesarColaEspera() {
        if (procesosEnColaEspera.isEmpty()) return;

        Iterator<Proceso> iterator = procesosEnColaEspera.iterator();
        while (iterator.hasNext()) {
            Proceso proceso = iterator.next();

            if (cargarProcesoEnMemoria(proceso)) {
                iterator.remove();
                procesosListos.offer(proceso);
                proceso.setEstado(EstadoProceso.ESPERA);
                proceso.setUltimaEntradaColaListos(tiempoActual);
                System.out.println("COLA->RAM: " + proceso.getId() + " movido de cola de espera a RAM");
                break;
            } else if (moverProcesoASwap(proceso)) {
                iterator.remove();
                System.out.println("COLA->SWAP: " + proceso.getId() + " movido de cola de espera a SWAP");
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

    private boolean moverProcesoASwap(Proceso proceso) {
        swapLock.lock();
        try {
            if (swapLimitado && !asignarSwapProceso(proceso)) {
                return false;
            } else if (!swapLimitado) {
                expandirSwap(proceso.getTamanioSlot());
                asignarSwapProceso(proceso);
            }

            procesosEnSwap.offer(proceso);
            proceso.setEstado(EstadoProceso.SWAP);

            if (swapLimitado) {
                System.out.println("SWAP: " + proceso.getId() + " movido a disco (" +
                        obtenerSlotsLibresSwap() + "/" + tamaño_swap + " slots libres)");
            } else {
                System.out.println("SWAP: " + proceso.getId() + " movido a disco (SWAP ilimitado - " + swap.size() + " slots totales)");
            }
            return true;
        } finally {
            swapLock.unlock();
        }
    }

    private boolean asignarSwapProceso(Proceso proceso) {
        int tamanioNecesario = proceso.getTamanioSlot();

        if (swapLimitado) {
            long slotsLibresSwap = swap.stream().mapToLong(slot -> slot == 0 ? 1 : 0).sum();
            if (slotsLibresSwap < tamanioNecesario) {
                return false;
            }
        }

        List<Integer> indicesLibres = new ArrayList<>();
        for (int i = 0; i < swap.size(); i++) {
            if (swap.get(i) == 0) {
                indicesLibres.add(i);
            }
        }

        Collections.shuffle(indicesLibres);

        String procesoIdStr = proceso.getId();
        int procesoIdNum = extraerNumeroIdProceso(procesoIdStr);
        List<Integer> direccionesAsignadas = new ArrayList<>();

        for (int i = 0; i < tamanioNecesario; i++) {
            int indiceAleatorio = indicesLibres.get(i);
            swap.set(indiceAleatorio, procesoIdNum);
            direccionesAsignadas.add(1000 + indiceAleatorio);
            swapAProceso.put(indiceAleatorio, procesoIdStr);
        }

        direccionesAsignadas.sort(Integer::compareTo);
        listaDireccionesProcesMem.put(proceso.getId(), direccionesAsignadas);

        return true;
    }

    private int obtenerSlotsLibresSwap() {
        return Collections.frequency(swap, 0);
    }

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
                    liberarSwapProceso(proceso);
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

    private void liberarSwapProceso(Proceso proceso) {
        String procesoIdStr = proceso.getId();
        List<Integer> direccionesLiberadas = new ArrayList<>();

        for (int i = 0; i < swap.size(); i++) {
            if (swapAProceso.containsKey(i) && swapAProceso.get(i).equals(procesoIdStr)) {
                swap.set(i, 0);
                swapAProceso.remove(i);
                direccionesLiberadas.add(i);
            }
        }
    }

    private void reordenarColaSJF() {
        if (!procesosListos.isEmpty()) {
            List<Proceso> listaTemp = new ArrayList<>(procesosListos);
            Collections.sort(listaTemp, Comparator.comparingInt(Proceso::getDuracion));
            procesosListos.clear();
            procesosListos.addAll(listaTemp);
        }
    }

    private void asignarProcesosACores() {
        for (Core core : cores) {
            if (!core.isOcupado() && !procesosListos.isEmpty()) {
                Proceso proceso = procesosListos.poll();

                int tiempoEsperaEstaRonda = tiempoActual - proceso.getUltimaEntradaColaListos();
                int tiempoEsperaTotal = proceso.getTiempoDeEspera() + tiempoEsperaEstaRonda;
                proceso.setTiempoDeEspera(tiempoEsperaTotal);

                core.asignarProceso(proceso);
                System.out.println(">> " + proceso.getId() + " asignado a Core " + core.getCoreId());
            }
        }
    }

    public void liberarMemoriaProceso(Proceso proceso) {
        memoriaLock.lock();
        try {
            String procesoIdStr = proceso.getId();
            List<Integer> direccionesLiberadas = new ArrayList<>();

            for (int i = 0; i < ram.size(); i++) {
                if (memoriaAProceso.containsKey(i) && memoriaAProceso.get(i).equals(procesoIdStr)) {
                    ram.set(i, 0);
                    memoriaAProceso.remove(i);
                    direccionesLiberadas.add(i);
                }
            }

            cargarProcesosDesdeSwap();
            procesarColaEspera();

        } finally {
            memoriaLock.unlock();
        }
    }

    private boolean asignarMemoriaProceso(Proceso proceso) {
        int tamanioNecesario = proceso.getTamanioSlot();

        long slotsLibres = ram.stream().mapToLong(slot -> slot == 0 ? 1 : 0).sum();

        if (slotsLibres < tamanioNecesario) {
            return false;
        }

        List<Integer> indicesLibres = new ArrayList<>();
        for (int i = 0; i < ram.size(); i++) {
            if (ram.get(i) == 0) {
                indicesLibres.add(i);
            }
        }

        Collections.shuffle(indicesLibres);

        String procesoIdStr = proceso.getId();
        int procesoIdNum = extraerNumeroIdProceso(procesoIdStr);
        List<Integer> direccionesAsignadas = new ArrayList<>();

        for (int i = 0; i < tamanioNecesario; i++) {
            int indiceAleatorio = indicesLibres.get(i);
            ram.set(indiceAleatorio, procesoIdNum);
            direccionesAsignadas.add(indiceAleatorio);
            memoriaAProceso.put(indiceAleatorio, procesoIdStr);
        }

        direccionesAsignadas.sort(Integer::compareTo);

        return true;
    }

    private boolean todosProcesosTerminados() {
        boolean hayProcesosEjecutablesPendientes = procesosPendientesDeLlegada.stream()
                .anyMatch(p -> !p.esPrograma());

        boolean hayProcesosEjecutablesEnRam = procesosListos.stream()
                .anyMatch(p -> !p.esPrograma());

        boolean hayProcesosEjecutablesEnSwap = procesosEnSwap.stream()
                .anyMatch(p -> !p.esPrograma());

        boolean hayProcesosEnColaEspera = !procesosEnColaEspera.isEmpty();

        return !hayProcesosEjecutablesPendientes &&
                !hayProcesosEjecutablesEnRam &&
                !hayProcesosEjecutablesEnSwap &&
                !hayProcesosEnColaEspera &&
                cores.stream().noneMatch(Core::isOcupado);
    }

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
        if (swapLimitado) {
            System.out.println("Procesos en COLA ESPERA: " + procesosEnColaEspera.size());
        }
        System.out.println("Cores ocupados: " + cores.stream().mapToInt(c -> c.isOcupado() ? 1 : 0).sum());

        System.out.println("Memoria libre: " + Collections.frequency(ram, 0) + "/" + TAMAÑO_RAM);
        if (swapLimitado) {
            System.out.println("SWAP libre: " + Collections.frequency(swap, 0) + "/" + tamaño_swap);
        } else {
            System.out.println("SWAP ilimitado: " + Collections.frequency(swap, 0) + " libres / " + swap.size() + " totales");
        }
    }

    public void agregarProcesoTerminado(Proceso proceso) {
        procesosTerminados.add(proceso);
    }

    public void agregarProcesoAlSistema(Proceso p) {
        procesosPendientesDeLlegada.add(p);
    }

    public void setAlgoritmo(TipoAlgoritmo algoritmo) {
        this.algoritmo = algoritmo;
    }

    public synchronized void devolverProcesoACola(Proceso proceso) {
        this.procesosListos.offer(proceso);
        proceso.setUltimaEntradaColaListos(tiempoActual);
    }

    public void setQuantum(int quantum) {
        this.quantum = quantum;
    }

    public boolean agregarProcesoHijoAlSistema(Proceso procesoHijo) {
        try {
            forkSemaphore.acquire();

            forkLock.lock();
            try {
                if (procesoHijo == null) {
                    return false;
                }

                procesosPendientesDeLlegada.add(procesoHijo);
                return true;

            } finally {
                forkLock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            forkSemaphore.release();
        }
    }

    private boolean liberarEspacioParaHijo(Proceso procesoHijo) {
        memoriaLock.lock();
        try {
            Iterator<Proceso> iterator = procesosListos.iterator();
            while (iterator.hasNext()) {
                Proceso candidato = iterator.next();

                if (!candidato.esProcesoHijo() && !estaEjecutandose(candidato)) {
                    iterator.remove();
                    liberarMemoriaProcesoSinCargarDesdeSwap(candidato);

                    if (!moverProcesoASwap(candidato)) {
                        if (swapLimitado) {
                            moverProcesoAColaEspera(candidato);
                        }
                    }

                    return true;
                }
            }
            return false;
        } finally {
            memoriaLock.unlock();
        }
    }

    private boolean estaEjecutandose(Proceso proceso) {
        return cores.stream().anyMatch(core ->
                core.isOcupado() &&
                        core.getProcesoEjecucion() != null &&
                        core.getProcesoEjecucion().getId().equals(proceso.getId())
        );
    }

    private void liberarMemoriaProcesoSinCargarDesdeSwap(Proceso proceso) {
        int procesoId = Integer.parseInt(proceso.getId().replace("Proceso ", ""));
        for (int i = 0; i < ram.size(); i++) {
            if (ram.get(i) == procesoId) {
                ram.set(i, 0);
                memoriaAProceso.remove(i);
            }
        }
    }

    private int extraerNumeroIdProceso(String procesoId) {
        try {
            String[] partes = procesoId.replace("Proceso ", "").split("\\.");
            return Integer.parseInt(partes[0]);
        } catch (Exception e) {
            return 1;
        }
    }

    // Getters
    public Queue<Proceso> getProcesosListos() { return procesosListos; }
    public Queue<Proceso> getProcesosEnSwap() { return procesosEnSwap; }
    public Queue<Proceso> getProcesosEnColaEspera() { return procesosEnColaEspera; }
    public void setRunning(boolean running) { this.running = running; }
    public int getTiempoActual() { return tiempoActual; }
    public List<Proceso> getProcesosPendientesDeLlegada() { return procesosPendientesDeLlegada; }
    public boolean getRunnin() { return running; }
    public List<Proceso> getProcesosTerminados() { return procesosTerminados; }
    public int getTamañoSwap() { return swapLimitado ? tamaño_swap : swap.size(); } // MODIFICADO
    public List<Integer> getSwap() { return swap; }
    public boolean isSwapLimitado() { return swapLimitado; } // NUEVO

    public void reset() {
        this.running = false;
        this.pausado = false;

        for (Core core : cores) {
            if (core != null) {
                core.detener();
            }
        }

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
            Thread.currentThread().interrupt();
        }

        this.ram.clear();
        this.ram.addAll(Collections.nCopies(TAMAÑO_RAM, 0));
        this.swap.clear();
        this.swap.addAll(Collections.nCopies(swapLimitado ? tamaño_swap : 512, 0)); // MODIFICADO: Respetar configuración
        this.procesosListos.clear();
        this.procesosEnSwap.clear();
        this.procesosEnColaEspera.clear();
        this.cores.clear();
        this.procesosPendientesDeLlegada.clear();
        this.procesosTerminados.clear();
        this.listaDireccionesProcesMem.clear();
        this.memoriaAProceso.clear();
        this.swapAProceso.clear();

        forkSemaphore.drainPermits();
        forkSemaphore.release(10);

        this.tiempoDeLlegadaProm = 0;
        this.tiempoDeEsperaProm = 0;
        this.tiempoDeRetornoProm = 0;
        this.quantum = 0;
        this.tiempoActual = 0;
        this.running = false;
        this.velocidadSimulacion = 1000;
        this.pausado = false;
        this.algoritmo = null;

        for (int i = 1; i <= NUMERO_CORES; i++) {
            Core core = new Core(i, this);
            cores.add(core);
            core.start();
        }

        Proceso.resetNextId();
        instance = null;
    }
}