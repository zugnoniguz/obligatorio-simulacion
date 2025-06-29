package ucu.slay;

import java.util.ArrayList;
import java.util.HashMap;

import ucu.utils.Hora;

public class Stats {
    public final HashMap<Hora, Integer[]> pacientesEsperando;
    public final HashMap<Hora, Integer> pacientesAtendidos;
    public final ArrayList<Paciente> pacientesTerminados;

    public Stats() {
        this.pacientesEsperando = new HashMap<>();
        this.pacientesAtendidos = new HashMap<>();
        this.pacientesTerminados = new ArrayList<>();
    }
}
