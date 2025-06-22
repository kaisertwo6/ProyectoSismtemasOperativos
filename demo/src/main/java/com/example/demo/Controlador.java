package com.example.demo;

import java.util.*;
import java.util.stream.Collectors;

public class Controlador extends Thread {
    private static Controlador instance;

    List<Integer> ram;
    TipoAlgoritmo algoritmo;
    private Queue<Proceso> procesosListos;
    List<Core> cores;
    private List<Proceso> procesosPendientesDeLlegada;
    private List<Proceso> procesosTerminados;

    int tiempoDeLlegadaProm;
    int tiempoDeEsperaProm;
    int tiempoDeRetornoProm;
    int quantum;

    Map<String, List<Integer>> listaDireccionesProcesMem; // Para swapping

    private int tiempoActual;
    private volatile boolean running;

    // Configuración del sistema
    private final int TAMAÑO_RAM = 1024; // 1024 slots = 1GB
    private final int NUMERO_CORES = 2;

    private Controlador() {
        this.ram = new ArrayList<>(Collections.nCopies(TAMAÑO_RAM, 0)); // 0 = libre
        this.procesosListos = new LinkedList<>();
        this.cores = new ArrayList<>();
        this.procesosPendientesDeLlegada = new ArrayList<>();
        this.procesosTerminados = new ArrayList<>();
        this.listaDireccionesProcesMem = new HashMap<>();

        // Inicializar cores
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

    @Override
    public void run() {
        Collections.sort(procesosPendientesDeLlegada, Comparator.comparingInt(Proceso::getTiempoDeLlegada));
        System.out.println("=== INICIO SIMULACIÓN SJF ===\n");

        while (running) {
            tiempoActual++;
            System.out.println("\n--- Tiempo: " + tiempoActual + " ---");

            verificarLlegadaProcesos();

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
                sleep(1000);
            } catch (InterruptedException e) {
                System.out.println("Simulación interrumpida.");
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
                procesosListos.offer(p);
                p.setEstado(EstadoProceso.ESPERA);
                p.setUltimaEntradaColaListos(tiempoActual); // ¡Nuevo!
                iterator.remove();
                System.out.println(">> " + p.getId() + " llegó y se añadió a cola de listos.");
            } else {
                break;
            }
        }
    }

    private void reordenarColaSJF() {
        if (!procesosListos.isEmpty()) {
            List<Proceso> listaTemp = new ArrayList<>(procesosListos);
            Collections.sort(listaTemp, Comparator.comparingInt(Proceso::getDuracion));
            procesosListos.clear();
            procesosListos.addAll(listaTemp);
            System.out.println("Cola reordenada por SJF (menor duración primero)");
        }
    }

    private void asignarProcesosACores() {
        for (Core core : cores) {
            if (!core.isOcupado() && !procesosListos.isEmpty()) {
                Proceso proceso = procesosListos.poll();

                int tiempoEsperaEstaRonda = tiempoActual - proceso.getUltimaEntradaColaListos();
                proceso.setTiempoDeEspera(proceso.getTiempoDeEspera() + tiempoEsperaEstaRonda);

                if (asignarMemoriaProceso(proceso)) {
                    core.asignarProceso(proceso);
                    System.out.println(">> " + proceso.getId() + " asignado a Core " + core.getCoreId() + " con memoria");
                } else {
                    if (realizarSwapping(proceso)) {
                        core.asignarProceso(proceso);
                        System.out.println(">> " + proceso.getId() + " asignado a Core " + core.getCoreId() + " tras swapping");
                    } else {
                        procesosListos.offer(proceso);
                        System.out.println(">> " + proceso.getId() + " devuelto a cola (sin memoria disponible)");
                        break;
                    }
                }
            }
        }
    }

    private boolean asignarMemoriaProceso(Proceso proceso) {
        int tamanioNecesario = proceso.getTamanioSlot();
        List<Integer> direccionesAsignadas = new ArrayList<>();

        // Buscar bloques libres
        int espaciosEncontrados = 0;
        for (int i = 0; i < ram.size() && espaciosEncontrados < tamanioNecesario; i++) {
            if (ram.get(i) == 0) {
                direccionesAsignadas.add(i);
                espaciosEncontrados++;
            }
        }

        if (espaciosEncontrados >= tamanioNecesario) {
            int procesoId = Integer.parseInt(proceso.getId().replace("Proceso ", ""));
            for (int direccion : direccionesAsignadas) {
                ram.set(direccion, procesoId);
            }
            System.out.println("Memoria asignada a " + proceso.getId() +
                    " en direcciones: " + direccionesAsignadas.subList(0, tamanioNecesario));
            return true;
        }

        return false;
    }

    private boolean realizarSwapping(Proceso procesoNuevo) {
        List<Integer> direccionesLiberadas = new ArrayList<>();

        for (int i = 0; i < ram.size(); i++) {
            if (ram.get(i) != 0) {
                int procesoIdEnMemoria = ram.get(i);
                String procesoIdString = "Proceso " + procesoIdEnMemoria;

                boolean estaEjecutandose = cores.stream()
                        .anyMatch(core -> core.isOcupado() &&
                                core.getProcesoEjecucion() != null &&
                                core.getProcesoEjecucion().getId().equals(procesoIdString));

                if (!estaEjecutandose) {
                    direccionesLiberadas.add(i);
                    ram.set(i, 0);

                    if (!listaDireccionesProcesMem.containsKey(procesoIdString)) {
                        List<Integer> direccionesProceso = new ArrayList<>();
                        direccionesProceso.add(i);
                        listaDireccionesProcesMem.put(procesoIdString, direccionesProceso);
                    }
                }

                if (direccionesLiberadas.size() >= procesoNuevo.getTamanioSlot()) {
                    break;
                }
            }
        }

        if (!direccionesLiberadas.isEmpty()) {
            System.out.println("SWAP: Liberadas " + direccionesLiberadas.size() + " direcciones de memoria");
            return asignarMemoriaProceso(procesoNuevo);
        }

        return false;
    }

    public void liberarMemoriaProceso(Proceso proceso) {
        int procesoId = Integer.parseInt(proceso.getId().replace("Proceso ", ""));
        List<Integer> direccionesLiberadas = new ArrayList<>();

        for (int i = 0; i < ram.size(); i++) {
            if (ram.get(i) == procesoId) {
                ram.set(i, 0);
                direccionesLiberadas.add(i);
            }
        }

        System.out.println("Memoria liberada de " + proceso.getId() + ": " + direccionesLiberadas);
        cargarProcesoDesdeSwap();
    }

    private void cargarProcesoDesdeSwap() {
        for (Map.Entry<String, List<Integer>> entry : listaDireccionesProcesMem.entrySet()) {
            String procesoId = entry.getKey();
            listaDireccionesProcesMem.remove(procesoId);
            System.out.println("Proceso " + procesoId + " cargado desde swap");
            break;
        }
    }

    private boolean todosProcesosTerminados() {
        return procesosPendientesDeLlegada.isEmpty() &&
                procesosListos.isEmpty() &&
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

    private void limpiarMemoriaInactiva() {
        Set<String> procesosEjecutandose = cores.stream()
                .filter(Core::isOcupado)
                .filter(core -> core.getProcesoEjecucion() != null)
                .map(core -> core.getProcesoEjecucion().getId())
                .collect(Collectors.toSet());

        for (int i = 0; i < ram.size(); i++) {
            if (ram.get(i) != 0) {
                String procesoIdEnMemoria = "Proceso " + ram.get(i);

                if (!procesosEjecutandose.contains(procesoIdEnMemoria)) {
                    System.out.println("Liberando memoria de " + procesoIdEnMemoria + " (no está ejecutándose)");
                    ram.set(i, 0);
                }
            }
        }
    }

    private void mostrarEstadoActual() {
        limpiarMemoriaInactiva();

        System.out.println("Procesos en cola: " + procesosListos.size());
        System.out.println("Cores ocupados: " + cores.stream().mapToInt(c -> c.isOcupado() ? 1 : 0).sum());
        System.out.println("Procesos en swap: " + listaDireccionesProcesMem.size());

        Set<Integer> procesosEnMemoria = new HashSet<>();
        for (Integer procesoId : ram) {
            if (procesoId != 0) {
                procesosEnMemoria.add(procesoId);
            }
        }
        System.out.println("Procesos en memoria: " + procesosEnMemoria);
    }

    public void agregarProcesoTerminado(Proceso proceso) {
        procesosTerminados.add(proceso);
        System.out.println("CONTROLADOR: " + proceso.getId() + " agregado a lista de terminados");
        System.out.println("CONTROLADOR: Total procesos terminados ahora: " + procesosTerminados.size());

        System.out.print("CONTROLADOR: Lista completa terminados: [");
        for (int i = 0; i < procesosTerminados.size(); i++) {
            if (i > 0) System.out.print(", ");
            System.out.print(procesosTerminados.get(i).getId());
        }
        System.out.println("]");
    }

    public void agregarProcesoAlSistema(Proceso p) {
        procesosPendientesDeLlegada.add(p);
        System.out.println(p.getId() + " añadido al sistema. Llegada: " + p.getTiempoDeLlegada());
    }

    public void setAlgoritmo(TipoAlgoritmo algoritmo) {
        this.algoritmo = algoritmo;
        System.out.println("Algoritmo establecido: " + algoritmo.getNombre());
    }

    public synchronized void devolverProcesoACola(Proceso proceso) {
        this.procesosListos.offer(proceso);
        System.out.println("CONTROLADOR: " + proceso.getId() + " devuelto a la cola de listos (RR).");
    }

    public void setQuantum(int quantum) {
        this.quantum = quantum;
        System.out.println("Quantum establecido: " + quantum);
    }

    public Queue<Proceso> getProcesosListos() { return procesosListos; }
    public void setRunning(boolean running) { this.running = running; }
    public int getTiempoActual() { return tiempoActual; }
    public List<Proceso> getProcesosPendientesDeLlegada() { return procesosPendientesDeLlegada; }
    public boolean getRunnin() { return running; }
    public List<Proceso> getProcesosTerminados() { return procesosTerminados; }

    public void reset() {
        for (Core core : cores) {
            core.detener();
        }

        try {
            this.join(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        this.ram.clear();
        this.ram.addAll(Collections.nCopies(TAMAÑO_RAM, 0));
        this.procesosListos.clear();
        this.cores.clear();
        this.procesosPendientesDeLlegada.clear();
        this.procesosTerminados.clear();
        this.listaDireccionesProcesMem.clear();

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
        Proceso.resetNextId();

        instance = null;
        System.out.println("Controlador reseteado completamente.");
    }

    public void mostrarCola() {
        if (procesosListos.isEmpty()) {
            System.out.println("[Cola vacía]");
            return;
        }

        System.out.print("[");
        boolean first = true;
        for (Proceso p : procesosListos) {
            if (!first) System.out.print(", ");
            System.out.print("P" + p.getId() + " (D:" + p.getDuracion() + ")");
            first = false;
        }
        System.out.println("]");
    }
}