package com.example.demo;

enum TipoAlgoritmo {
    SJF("Shortest Job First"),
    RR("Round Robin");

    private final String nombre;

    private TipoAlgoritmo(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }
}
