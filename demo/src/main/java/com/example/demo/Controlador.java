package com.example.demo;

import java.util.*;


public class Controlador extends Thread {
    private static Controlador instance;

    List<Integer> ram;
    TipoAlgoritmo algoritmo;
    private Queue<Proceso> procesosListos; // Cola de proceso que estan listos para su ejecucion
    List<Core> cores;
    private List<Proceso> procesosPendientesDeLlegada;

    int tiempoDeLlegadaProm;
    int tiempoDeEsperaProm;
    int tiempoDeRetornoProm;
    int quantum;

    Map<String, List<Integer>> listaDireccionesProcesMem;

    private int tiempoActual;
    private volatile boolean running;

    private Controlador() {
        this.ram = new ArrayList<>();
        this.procesosListos = new LinkedList<>();
        this.cores = new ArrayList<>();
        this.procesosPendientesDeLlegada = new ArrayList<>();

        this.listaDireccionesProcesMem = new HashMap<>();
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
        System.out.println("inicio simulacion \n");
        mostrarCola();
        while (running) {

            tiempoActual++;
            System.out.println("\n--- Tiempo: " + tiempoActual + " ---");

            Iterator<Proceso> iterator = procesosPendientesDeLlegada.iterator();
            boolean nuevosProcesosLlegaron = false;

            while (iterator.hasNext()) {
                Proceso p = iterator.next();
                if (p.getTiempoDeLlegada() <= tiempoActual) {
                    procesosListos.offer(p);
                    iterator.remove();
                    System.out.println(">> Proceso " + p.getId() + " llegó (T.Llegada: " + p.getTiempoDeLlegada() + ") y se añadió a la cola de listos.");
                    nuevosProcesosLlegaron = true;
                } else {
                    break;
                }
            }

            if (nuevosProcesosLlegaron || algoritmo == TipoAlgoritmo.SJF) {
                List<Proceso> listaTemporalParaOrdenar = new ArrayList<>(procesosListos);

                if (algoritmo == TipoAlgoritmo.SJF) {
                    Collections.sort(listaTemporalParaOrdenar, Comparator.comparingInt(Proceso::getDuracion));
                    System.out.println("Cola de listos reordenada por SJF (menor duración primero).");
                } else if (algoritmo == TipoAlgoritmo.RR) {
                    System.out.println("Cola de listos (RR): El orden por llegada se mantiene automáticamente.");
                }

                procesosListos.clear();
                procesosListos.addAll(listaTemporalParaOrdenar);

                // tibleviw actualizar ----- >
            }

            if (!procesosListos.isEmpty()) {
                Proceso procesoEnEjecucion = procesosListos.peek();
                System.out.println("Proceso actual en cabeza de cola de listos: " + procesoEnEjecucion.getId());
            } else {
                System.out.println("Cola de procesos listos vacía. Esperando nuevas llegadas...");
            }

            mostrarCola();
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                System.out.println("El hilo de simulación fue interrumpido.");
                running = false;
                Thread.currentThread().interrupt();
            }

        }

    }

    public void agregarProcesoAlSistema(Proceso p) {
        procesosPendientesDeLlegada.add(p);
        System.out.println("Proceso " + p.getId() + " añadido a la lista de pendientes de llegada. Tiempo de llegada: " + p.getTiempoDeLlegada());
    }

    public void setAlgoritmo(TipoAlgoritmo algoritmo) {
        this.algoritmo = algoritmo;

    }

    public void setQuantum(int quantum) {
        this.quantum = quantum;
        System.out.println("Quantum establecido: " + quantum);
    }

    public Queue<Proceso> getProcesosListos() {
        return procesosListos;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public void reset() {

        try {
            this.join(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        this.ram.clear();
        this.procesosListos.clear();
        this.cores.clear();
        this.procesosPendientesDeLlegada.clear();
        this.listaDireccionesProcesMem.clear();
        this.tiempoDeLlegadaProm = 0;
        this.tiempoDeEsperaProm = 0;
        this.tiempoDeRetornoProm = 0;
        this.quantum = 0;
        this.tiempoActual = 0;
        this.running = false;
        Proceso.resetNextId();

        instance = null;
        System.out.println("Controlador reseteado. Listo para una nueva simulación.");
    }

    public int getTiempoActual() {
        return tiempoActual;
    }

    public List<Proceso> getProcesosPendientesDeLlegada() {
        return procesosPendientesDeLlegada;
    }
    public void mostrarCola(){
        if (procesosListos.isEmpty()) {
            System.out.println("[Cola vacía]");
            return;
        }

        System.out.print("[");
        boolean first = true;
        for (Proceso p : procesosListos) {
            if (!first) {
                System.out.print(", ");
            }
            System.out.print("P" + p.getId() + " (D:" + p.getDuracion() + ")");
            first = false;
        }
        System.out.println("]");
    }
    public boolean getRunnin(){
        return running;

    }

}