package ucu.slay;

import ucu.utils.ClassFormatter;
import ucu.utils.Hora;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class App {

    public static void main(String[] args) throws Exception {
        FileHandler fh = new FileHandler("simulacion.log", false);
        fh.setFormatter(new ClassFormatter());
        fh.setLevel(Level.INFO);
        FileHandler fhDebug = new FileHandler("simulacion_debug.log", false);
        fhDebug.setFormatter(new ClassFormatter());
        fhDebug.setLevel(Level.FINEST);
        ConsoleHandler ch = new ConsoleHandler();
        ch.setFormatter(new ClassFormatter());
        ch.setLevel(Level.FINEST);
        Logger l = Logger.getLogger("");
        l.addHandler(fh);
        l.addHandler(fhDebug);
        // l.addHandler(ch);
        l.setLevel(Level.FINEST);

        Configuracion config = null;
        switch (args.length) {
            case 0 -> {
                String fname = "simulacion-prueba.txt";
                System.out.printf("Leyendo de %s\n", fname);
                config = Configuracion.readFromFile(fname);
            }
            case 1 -> {
                String fname = args[0];
                System.out.printf("Leyendo de %s\n", fname);
                config = Configuracion.readFromFile(fname);
            }
            default -> {
                System.err.printf("Demasiados argumentos no sé qué hacer\n");
            }
        }

        if (config == null) {
            System.err.println("No pudimos leer la configuración papá");
            return;
        }

        PlanificadorConsultas planificador = new PlanificadorConsultas(config);
        Hora horaFinal = planificador.correrSimulacion();

        Stats stats = planificador.stats;
        {
            File csvFile = new File("stats.csv");
            BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile));
            try {
                writer.write("Hora,Espera emergencia,Espera urgencia,Espera normal,Siendo atendidos\n");
                for (Hora hora = PlanificadorConsultas.HORA_INICIAL.clone(); !hora
                        .equals(horaFinal); hora.increment()) {
                    Integer[] espera = stats.pacientesEsperando.get(hora);
                    int atendidos = stats.pacientesAtendidos.getOrDefault(hora, 0);
                    String s = String.format("%s,%d,%d,%d,%d",
                            hora.toString(),
                            espera[0],
                            espera[1] + espera[2],
                            espera[3],
                            atendidos);
                    writer.write(String.format("%s\n", s));
                }
                writer.write(String.format("%s,0,0,0,0\n", horaFinal));
            } finally {
                writer.close();
            }
        }

        {
            File csvFile = new File("pacientes.csv");
            BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile));
            try {
                writer.write(
                        "Hora de entrada, Hora de Salida,Tiempo esperado de atención,Tiempo real de atención,Tiempo de espera,Tiempo total\n");
                ArrayList<Paciente> newList = (ArrayList<Paciente>) stats.pacientesTerminados.clone();
                newList.sort(new Comparator<Paciente>() {
                    @Override
                    public int compare(Paciente o1, Paciente o2) {
                        return o1.horaLlegada.compareTo(o2.horaLlegada);
                    }
                });
                for (Paciente p : newList) {
                    Hora horaSalida = p.horaLlegada.clone();
                    for (int i = 0; i < p.tiempoDesdeLlegada; ++i) {
                        horaSalida.increment();
                    }
                    String s = String.format("%s,%s,%d,%d,%d,%d",
                            p.horaLlegada,
                            horaSalida,
                            p.consultaDeseada.getTiempoEstimado(),
                            p.tiempoDeAtencion,
                            p.tiempoDesdeLlegada - p.tiempoDeAtencion,
                            p.tiempoDesdeLlegada);
                    writer.write(String.format("%s\n", s));
                }
            } finally {
                writer.close();
            }
        }
    }
}
