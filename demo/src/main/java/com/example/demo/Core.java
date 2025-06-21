package com.example.demo;

public class Core extends Thread  {

    int id;
    Proceso proceso_ejecucion;
    boolean ocupado;
    int tiempoEjecucion;

    Core(int id , Proceso proceso_ejecucion, boolean ocupado , int tiempoEjecucion){
        this.id  = id ;
        this.ocupado = ocupado;
        this.proceso_ejecucion = proceso_ejecucion;
        this.tiempoEjecucion = tiempoEjecucion;
    }
    @Override
    public void run() {

    }


}
