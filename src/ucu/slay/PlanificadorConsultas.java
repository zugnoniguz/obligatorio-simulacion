package ucu.slay;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;

import ucu.utils.Hora;

public class PlanificadorConsultas {

    private static final Hora horaInicial = new Hora(8, 0);
    private static final Hora horaFinal = new Hora(20, 0);

    public final Configuracion config;

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

    public final Semaphore empezoElMinuto;
    public final Semaphore terminoElMinuto;
    public CyclicBarrier terminaronTodos;
    public Hora horaActual;

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

    class InitResult {
        int totalHilos;
        Thread[] medicos;
        Thread[] enfermeros;
        Thread generadorPacientes;

        public InitResult(
                int totalHilos,
                Thread[] medicos,
                Thread[] enfermeros,
                Thread generadorPacientes) {
            this.totalHilos = totalHilos;
            this.medicos = medicos;
            this.enfermeros = enfermeros;
            this.generadorPacientes = generadorPacientes;
        }
    }

    private InitResult initSimulacion() {
        ArrayList<Thread> medicos = new ArrayList<>();
        ArrayList<Thread> enfermeros = new ArrayList<>();

        int totalHilos = 0;

        Thread hiloGenerador = new Thread(new GeneradorPacientes(config.semilla, this));
        hiloGenerador.start();
        totalHilos += 1;

        for (int i = 0; i < config.cantMedicos; ++i) {
            Thread t = new Thread(new Medico(i + 1, this));
            t.start();
            medicos.add(t);
            totalHilos += config.cantMedicos;
        }

        for (int i = 0; i < config.cantEnfermeros; ++i) {
            Thread t = new Thread(new Enfermero(i + 1, this));
            t.start();
            enfermeros.add(t);
            totalHilos += config.cantEnfermeros;
        }

        this.terminaronTodos = new CyclicBarrier(totalHilos);

        return new InitResult(
                totalHilos,
                medicos.toArray(new Thread[0]),
                enfermeros.toArray(new Thread[0]),
                hiloGenerador);
    }

    public void correrSimulacion() throws InterruptedException {
        var result = this.initSimulacion();

        var totalHilos = result.totalHilos;
        var generadorPacientes = result.generadorPacientes;
        var medicos = result.medicos;
        var enfermeros = result.medicos;

        System.out.printf("[PlanificadorConsultas] Empezando la simulación con %d hilos\n", totalHilos);
        for (this.horaActual = horaInicial; !this.horaActual.equals(horaFinal); this.horaActual.increment()) {
            // digo que empezo el minuto
            System.out.printf("[PlanificadorConsultas] Hora: %02d:%02d\n", this.horaActual.hora, this.horaActual.min);
            this.empezoElMinuto.release(totalHilos);

            // espero a que todos terminen
            this.terminoElMinuto.acquire(totalHilos);
            System.out.printf("[PlanificadorConsultas] Terminó el minuto %02d:%02d\n", this.horaActual.hora,
                    this.horaActual.min);
            int n = this.terminaronTodos.getNumberWaiting();
            if (n != 0) {
                System.err.printf("[PlanificadorConsultas] Terminaron %d hilos pero %d esperan para seguir\n",
                        totalHilos, n);
            }

            System.out.println();

            // avanzo el minuto (el for)
        }

        generadorPacientes.interrupt();
        generadorPacientes.join();
        for (Thread t : medicos) {
            t.interrupt();
            t.join();
        }

        for (Thread t : enfermeros) {
            t.interrupt();
            t.join();
        }
    }

    // TODO: Informar que puede estar llena y capaz trackear como resultado cuantos
    // pacientes no pueden entrar en la sala
    // de espera
    public void recibirPaciente(Paciente p) {
        if (p.consultaDeseada.esEmergencia()) {
            this.consultasEmergencia.add(p);
        } else if (p.consultaDeseada.esUrgencia()) {
            this.consultasUrgenciaBaja.add(p);
        } else {
            this.consultasNormales.add(p);
        }
    }

    public Optional<Paciente> conseguirPacienteInterruptor() {
        Paciente pEmergencia = this.consultasEmergencia.poll();
        if (pEmergencia != null) {
            return Optional.of(pEmergencia);
        }

        Paciente pUrgenciaAlta = this.consultasUrgenciaAlta.poll();
        if (pUrgenciaAlta != null) {
            return Optional.of(pUrgenciaAlta);
        }

        Paciente pUrgenciaBaja = this.consultasUrgenciaBaja.poll();
        if (pUrgenciaBaja != null) {
            return Optional.of(pUrgenciaBaja);
        }

        return Optional.empty();
    }

    public boolean hayPacienteNormal() {
        Paciente p = this.consultasNormales.poll();
        return p != null;
    }

    public void aumentarPrioridadUrgencia(Paciente p) {
        this.consultasUrgenciaBaja.remove(p);
        this.consultasUrgenciaAlta.add(p);
    }
}
