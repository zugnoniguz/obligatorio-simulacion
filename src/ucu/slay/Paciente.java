package ucu.slay;

import ucu.utils.Hora;

public class Paciente {
    public final int id;
    public final Consulta consultaDeseada;
    public int tiempoRestante;
    public int tiempoDeAtencion;
    public int tiempoDesdeLlegada;
    public boolean interrumpido;
    public final Hora horaLlegada;

    public Paciente(int id, Consulta consultaDeseada, Hora horaLlegada, int tiempoRestante) {
        this.id = id;
        this.consultaDeseada = consultaDeseada;
        this.horaLlegada = horaLlegada;
        this.tiempoRestante = tiempoRestante;
        this.tiempoDeAtencion = 0;
        this.tiempoDesdeLlegada = 0;
        this.interrumpido = false;
    }
}
