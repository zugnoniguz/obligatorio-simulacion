package ucu.slay;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;

public class PlanificadorConsultas {

    private static final Hora horaInicial = new Hora(8, 0);

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

    public void correrSimulacion() {
        ArrayList<Thread> medicos = new ArrayList<>();
        ArrayList<Thread> enfermeros = new ArrayList<>();
        int totalHilos = 0;

        for (int i = 0; i < config.cantMedicos; ++i) {
            medicos.add(new Thread(new Medico()));
            totalHilos += 1;
        }
        for (int i = 0; i < config.cantEnfermeros; ++i) {
            enfermeros.add(new Thread(new Enfermero()));
            totalHilos += 1;
        }

        for (Hora hora = horaInicial; !hora.equals(new Hora(20, 0)); hora.increment()) {
            System.out.printf("Hora: %d:%d\n", hora.hora, hora.min);
            empezoElMinuto.release(totalHilos);

            try {
                terminoElMinuto.acquire(totalHilos);
            } catch (InterruptedException e) {
                System.err.printf("Me interrumpieron D: (%s)", e.getMessage());
            }
            
            System.out.println();
        }
        // while (true) {
        //     mando notificaciones
        //     digo que empezo el minuto
        //     espero a que todos terminen
        //     avanzo el minuto
        // }
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

    public Optional<Paciente> tomarPaciente() {
        Paciente p;
        p = this.consultasEmergencia.poll();
        if (p != null) {
            return Optional.of(p);
        }

        p = this.consultasUrgenciaAlta.poll();
        if (p != null) {
            return Optional.of(p);
        }

        p = this.consultasUrgenciaBaja.poll();
        if (p != null) {
            return Optional.of(p);
        }

        p = this.consultasNormales.poll();
        if (p != null) {
            return Optional.of(p);
        }

        return Optional.empty();

    }

    public Paciente esperarPaciente() {
        // TODO: Block until there is one
        return this.tomarPaciente().orElseThrow();
    }
}
