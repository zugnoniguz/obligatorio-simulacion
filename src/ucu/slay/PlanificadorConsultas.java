package ucu.slay;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;

import ucu.utils.Hora;

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

    public PlanificadorConsultas(Configuracion config) {
        this.config = config;

        int cap = config.pacientesPorHora * 2;
        this.consultasEmergencia = new ArrayBlockingQueue<>(cap, true);
        this.consultasUrgenciaAlta = new ArrayBlockingQueue<>(cap, true);
        this.consultasUrgenciaBaja = new ArrayBlockingQueue<>(cap, true);
        this.consultasNormales = new ArrayBlockingQueue<>(cap, true);
    }

    class InitResult {
        int totalHilos;
        Thread[] medicos;
        Thread[] enfermeros;
        Thread generadorPacientes;
        ArrayBlockingQueue<Paciente> colaPacientesInterrupcion;
        ArrayBlockingQueue<Paciente> colaPacientesNormales;
        Semaphore empezoElMinuto;
        Semaphore terminoElMinuto;

        public InitResult(
                int totalHilos,
                Thread[] medicos,
                Thread[] enfermeros,
                Thread generadorPacientes,
                ArrayBlockingQueue<Paciente> colaPacientesInterrupcion,
                ArrayBlockingQueue<Paciente> colaPacientesNormales,
                Semaphore empezoElMinuto,
                Semaphore terminoElMinuto) {
            this.totalHilos = totalHilos;
            this.medicos = medicos;
            this.enfermeros = enfermeros;
            this.generadorPacientes = generadorPacientes;
            this.colaPacientesInterrupcion = colaPacientesInterrupcion;
            this.colaPacientesNormales = colaPacientesNormales;
            this.empezoElMinuto = empezoElMinuto;
            this.terminoElMinuto = terminoElMinuto;
        }
    }

    private InitResult initSimulacion() {
        ArrayList<Thread> medicos = new ArrayList<>();
        ArrayList<Thread> enfermeros = new ArrayList<>();
        ArrayBlockingQueue<Paciente> colaPacientesNormales = new ArrayBlockingQueue<>(2, true);
        ArrayBlockingQueue<Paciente> colaPacientesInterrupcion = new ArrayBlockingQueue<>(2, true);

        int totalHilos = 0;
        totalHilos += 1; // GeneradorPacientes
        totalHilos += config.cantMedicos;
        totalHilos += config.cantEnfermeros;

        Semaphore empezoElMinuto = new Semaphore(0, true);
        Semaphore terminoElMinuto = new Semaphore(0, true);

        Thread hiloGenerador = new Thread(
                new GeneradorPacientes(
                        empezoElMinuto,
                        terminoElMinuto,
                        config.semilla));
        hiloGenerador.start();

        for (int i = 0; i < config.cantMedicos; ++i) {
            Thread t = new Thread(
                    new Medico(i + 1,
                            empezoElMinuto,
                            terminoElMinuto,
                            colaPacientesInterrupcion,
                            colaPacientesNormales));
            t.start();
            medicos.add(t);
        }
        for (int i = 0; i < config.cantEnfermeros; ++i) {
            Thread t = new Thread(
                    new Enfermero(i + 1,
                            empezoElMinuto,
                            terminoElMinuto));
            t.start();
            enfermeros.add(t);
        }

        return new InitResult(
                totalHilos,
                medicos.toArray(new Thread[0]),
                enfermeros.toArray(new Thread[0]),
                hiloGenerador,
                colaPacientesInterrupcion,
                colaPacientesNormales,
                empezoElMinuto,
                terminoElMinuto);
    }

    public void correrSimulacion() throws InterruptedException {
        var result = this.initSimulacion();

        var totalHilos = result.totalHilos;
        var colaPacientesInterrupcion = result.colaPacientesInterrupcion;
        var colaPacientesNormales = result.colaPacientesNormales;
        var empezoElMinuto = result.empezoElMinuto;
        var terminoElMinuto = result.terminoElMinuto;
        var generadorPacientes = result.generadorPacientes;
        var medicos = result.medicos;
        var enfermeros = result.medicos;

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
            // FIXME: esto no espera a que todos terminen. como el hilo no puede esperar a
            // que todos terminen para seguir, puede pasar que un mismo hilo empieze el
            // minuto dos veces
            empezoElMinuto.release(totalHilos);

            // espero a que todos terminen
            terminoElMinuto.acquire(totalHilos);
            System.out.printf("[PlanificadorConsultas] Terminó el minuto %d:%d\n", hora.hora, hora.min);

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
