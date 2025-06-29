package ucu.slay;

import ucu.utils.Hora;

public class Paciente {
    public final int id;
    public final TipoConsulta consultaDeseada;
    public int tiempoRestante;
    public int tiempoDesdeLlegada;
    public final Hora horaLlegada;

    public Paciente(int id, TipoConsulta consultaDeseada, Hora horaLlegada, int tiempoRestante) {
        this.id = id;
        this.consultaDeseada = consultaDeseada;
        this.horaLlegada = horaLlegada;
        this.tiempoRestante = tiempoRestante;
        this.tiempoDesdeLlegada = 0;
    }
}
