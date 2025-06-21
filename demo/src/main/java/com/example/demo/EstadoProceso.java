package com.example.demo;

enum EstadoProceso {
    ESPERA("Esperando"),
    TERMINADO("Terminado"),
    EJECUCION("En proceso");  // Cambié "EJECUTANDO" a "EJECUCION" para mejor legibilidad (opcional)

    private final String descripcion;

    EstadoProceso(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}

