package ucu.slay;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;

public class PlanificadorConsultas {

    private static final Hora horaInicial = new Hora(8, 0);
    private static final Hora horaFinal = new Hora(20, 0);

    private final Configuracion config;

    // Las emergencias no tienen prioridad. Las atendemos y listo.
    private final ArrayBlockingQueue<Paciente> consultasEmergencia;

    // Las urgencias tampoco tienen proridad entre sí.
    private final ArrayBlockingQueue<Paciente> consultasUrgenciaAlta;
    private final ArrayBlockingQueue<Paciente> consultasUrgenciaBaja;

    // Entre las consultas normales, tampoco hay prioridad.
    //
    // Las urgencias tendrían más prioridad, en teoría, pero sin embargo una vez que
    // envejecen demasiado pasan a `consultasUrgencia`.
    private final ArrayBlockingQueue<Paciente> consultasNormales;

    private final Semaphore empezoElMinuto;
    private final Semaphore terminoElMinuto;

    public PlanificadorConsultas(Configuracion config) {
        this.config = config;

        int cap = config.pacientesPorHora * 2;
        this.consultasEmergencia = new ArrayBlockingQueue<>(cap, true);
        this.consultasUrgenciaAlta = new ArrayBlockingQueue<>(cap, true);
        this.consultasUrgenciaBaja = new ArrayBlockingQueue<>(cap, true);
        this.consultasNormales = new ArrayBlockingQueue<>(cap, true);

        this.empezoElMinuto = new Semaphore(0, true);
        this.terminoElMinuto = new Semaphore(0, true);
    }

    public void correrSimulacion() throws InterruptedException {
        ArrayList<Thread> medicos = new ArrayList<>();
        ArrayList<Thread> enfermeros = new ArrayList<>();
        ArrayBlockingQueue<Paciente> colaPacientesInterrupcion = new ArrayBlockingQueue<>(2, true);
        ArrayBlockingQueue<Paciente> colaPacientesNormales = new ArrayBlockingQueue<>(2, true);

        int totalHilos = 0;

        Thread hiloGenerador = new Thread(
                new GeneradorPacientes(
                        this.empezoElMinuto,
                        this.terminoElMinuto,
                        config.semilla));
        hiloGenerador.start();
        totalHilos += 1;

        for (int i = 0; i < config.cantMedicos; ++i) {
            Thread t = new Thread(
                    new Medico(i + 1,
                            this.empezoElMinuto,
                            this.terminoElMinuto,
                            colaPacientesInterrupcion,
                            colaPacientesNormales));
            t.start();
            medicos.add(t);
            totalHilos += 1;
        }
        for (int i = 0; i < config.cantEnfermeros; ++i) {
            Thread t = new Thread(
                    new Enfermero(i + 1,
                            this.empezoElMinuto,
                            this.terminoElMinuto));
            t.start();
            enfermeros.add(t);
            totalHilos += 1;
        }

        System.out.printf("[PlanificadorConsultas] Empezando la simulación con %d hilos\n", totalHilos);
        for (Hora hora = horaInicial; !hora.equals(horaFinal); hora.increment()) {
            // mando notificaciones
            colaPacientesInterrupcion.clear();
            colaPacientesInterrupcion.addAll(this.consultasEmergencia);
            colaPacientesInterrupcion.addAll(this.consultasUrgenciaAlta);
            colaPacientesNormales.clear();
            colaPacientesNormales.addAll(this.consultasUrgenciaBaja);
            colaPacientesNormales.addAll(this.consultasNormales);

            // digo que empezo el minuto
            System.out.printf("[PlanificadorConsultas] Hora: %d:%d\n", hora.hora, hora.min);
            empezoElMinuto.release(totalHilos);

            // espero a que todos terminen
            terminoElMinuto.acquire(totalHilos);
            System.out.printf("[PlanificadorConsultas] Terminó el minuto %d:%d\n", hora.hora, hora.min);

            System.out.println();

            // avanzo el minuto (el for)
        }

        hiloGenerador.interrupt();
        hiloGenerador.join();
        for (Thread t : medicos) {
            t.interrupt();
            t.join();
        }

        for (Thread t : enfermeros) {
            t.interrupt();
            t.join();
        }
    }

    public void recibirPaciente(Paciente p) {
        if (p.consultaDeseada.esEmergencia()) {
            this.consultasEmergencia.add(p);
        } else if (p.consultaDeseada.esUrgencia()) {
            this.consultasUrgenciaBaja.add(p);
        } else {
            this.consultasNormales.add(p);
        }
    }
}
