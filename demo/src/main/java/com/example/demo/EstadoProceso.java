package com.example.demo;

enum EstadoProceso {
    ESPERA("Esperando"),        // En RAM, listo para ejecutar
    SWAP("En Swap"),           // En disco, esperando memoria
    EJECUCION("En proceso"),   // Ejecutándose en CPU
    TERMINADO("Terminado"),    // Proceso completado
    PENDIENTE("Pendiente");    // No ha llegado aún

    private final String descripcion;

    EstadoProceso(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}