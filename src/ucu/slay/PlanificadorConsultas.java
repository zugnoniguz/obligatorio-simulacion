package ucu.slay;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import ucu.utils.Hora;

public class PlanificadorConsultas {
    private static final Logger LOGGER = Logger.getLogger(PlanificadorConsultas.class.getName());

    public static final Hora HORA_INICIAL = new Hora(8, 0);
    public static final Hora HORA_FINAL = new Hora(20, 0);

    private final Configuracion config;

    // La tranca para acceder a todas las colas
    private final ReentrantLock mutexColas;
    // Las emergencias no tienen prioridad. Las atendemos y listo.
    private final ArrayDeque<Paciente> consultasEmergencia;
    // Las urgencias tienen proridad entre sí, según cuanto tiempo van sin ser
    // atendidas.
    private final ArrayDeque<Paciente> consultasUrgenciaAlta;
    private final ArrayDeque<Paciente> consultasUrgenciaBaja;
    // Entre las consultas normales, tampoco hay prioridad.
    private final ArrayDeque<Paciente> consultasNormales;

    public final Semaphore empezoElMinuto;
    public final Semaphore terminoElMinuto;
    public int medicosEsperando;
    public CyclicBarrier terminaronTodos;
    public Hora horaActual;

    private final ReentrantLock mutexEnfermeros;
    public ArrayList<Integer> enfermerosDisponibles;
    public HashMap<Integer, Integer> enfermerosOcupados;

    private final ReentrantLock mutexSalas;
    public ArrayList<Integer> salasDisponibles;
    public HashMap<Integer, Integer> salasOcupadas;

    public ReentrantLock statsLock;
    public Stats stats;

    public PlanificadorConsultas(Configuracion config) {
        this.config = config;

        this.consultasEmergencia = new ArrayDeque<>();
        this.consultasUrgenciaAlta = new ArrayDeque<>();
        this.consultasUrgenciaBaja = new ArrayDeque<>();
        this.consultasNormales = new ArrayDeque<>();

        this.mutexColas = new ReentrantLock();
        this.mutexEnfermeros = new ReentrantLock();
        this.mutexSalas = new ReentrantLock();

        this.medicosEsperando = 0;
        this.enfermerosDisponibles = new ArrayList<>();
        this.enfermerosOcupados = new HashMap<>();
        this.salasDisponibles = new ArrayList<>();
        this.salasOcupadas = new HashMap<>();

        this.stats = new Stats();
        this.statsLock = new ReentrantLock();

        this.empezoElMinuto = new Semaphore(0);
        this.terminoElMinuto = new Semaphore(0);
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

        Thread hiloGenerador = new Thread(
                new GeneradorPacientes(
                        config.semilla,
                        config.cantInicialPacientes,
                        config.pacientesPorHora,
                        config.tiposConsulta,
                        this));
        hiloGenerador.start();
        totalHilos += 1;

        for (int i = 0; i < config.cantMedicos; ++i) {
            Thread t = new Thread(new Medico(i + 1, this));
            t.start();
            medicos.add(t);
            totalHilos += 1;
        }

        for (int i = 0; i < config.cantEnfermeros; ++i) {
            int id = i + 1;
            Thread t = new Thread(new Enfermero(id, this));
            t.start();
            enfermeros.add(t);
            enfermerosDisponibles.add(id);
            totalHilos += 1;
        }

        this.terminaronTodos = new CyclicBarrier(totalHilos);
        for (int i = 0; i < config.cantSalas; ++i) {
            this.salasDisponibles.add(i + 1);
        }

        return new InitResult(
                totalHilos,
                medicos.toArray(new Thread[0]),
                enfermeros.toArray(new Thread[0]),
                hiloGenerador);
    }

    public Hora correrSimulacion() throws InterruptedException {
        var result = this.initSimulacion();

        var totalHilos = result.totalHilos;
        var generadorPacientes = result.generadorPacientes;
        var medicos = result.medicos;
        var enfermeros = result.enfermeros;

        LOGGER.log(Level.FINER, "Empezando la simulación con {0} hilos", totalHilos);
        this.horaActual = HORA_INICIAL.clone();
        while (true) {
            LOGGER.log(Level.FINEST, "");
            // digo que empezo el minuto
            LOGGER.log(Level.FINER,
                    "Empezó el minuto: {0}",
                    this.horaActual.toString());
            this.empezoElMinuto.release(totalHilos);

            // espero a que todos terminen
            this.terminoElMinuto.acquire(totalHilos);
            int total = this.totalEsperando();
            LOGGER.log(Level.FINER,
                    "Terminó el minuto: {0} con {1} en espera",
                    new Object[] {
                            this.horaActual.toString(),
                            total,
                    });
            LOGGER.log(Level.FINER,
                    "Emer: {0}, Urg/Alta: {1}, Urg/Baja: {2}, Norm: {3}",
                    new Object[] {
                            this.consultasEmergencia.size(),
                            this.consultasUrgenciaAlta.size(),
                            this.consultasUrgenciaBaja.size(),
                            this.consultasNormales.size(),
                    });
            int n = this.terminaronTodos.getNumberWaiting();
            if (n != 0) {
                LOGGER.log(Level.FINER,
                        "Terminaron {0} hilos pero {1} esperan para seguir",
                        new Object[] {
                                totalHilos,
                                n
                        });
            }

            // TODO: Promover urgencias
            this.envejecerPacientes();

            this.actualizarStats();

            // avanzo el minuto
            if (this.horaActual.compareTo(PlanificadorConsultas.HORA_FINAL) >= 0) {
                int siendoAtendidos = this.stats.pacientesAtendidos.getOrDefault(this.horaActual, 0);
                int esperando = this.totalEsperando();
                if (siendoAtendidos != 0) {
                    LOGGER.log(Level.FINER,
                            "Siguiendo porque quedan {0} pacientes siendo atendidos",
                            siendoAtendidos);
                } else {
                    if (esperando != 0) {
                        LOGGER.log(Level.FINER,
                                "Siguiendo porque quedan {0} pacientes en espera",
                                esperando);

                    } else {
                        break;
                    }
                }
            }
            this.horaActual.increment();
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

        return this.horaActual.clone();
    }

    public int totalEsperando() {
        int total = 0;
        total += this.consultasEmergencia.size();
        total += this.consultasUrgenciaAlta.size();
        total += this.consultasUrgenciaBaja.size();
        total += this.consultasNormales.size();

        return total;
    }

    // TODO: Informar que puede estar llena y capaz trackear como resultado cuantos
    // pacientes no pueden entrar en la sala
    // de espera
    public void recibirPaciente(Paciente p) {
        if (this.horaActual.compareTo(PlanificadorConsultas.HORA_FINAL) >= 0) {
            LOGGER.log(Level.FINER, "Ignorando paciente porque cerró el hospital");
            return;
        }

        switch (p.consultaDeseada.tipo) {
            case TipoConsulta.Emergencia -> {
                this.consultasEmergencia.addLast(p);
            }
            case TipoConsulta.Urgencia -> {
                // TODO: Es alta o baja?
                this.consultasUrgenciaBaja.addLast(p);
            }
            case TipoConsulta.Normal -> {
                this.consultasNormales.addLast(p);
            }
        }
    }

    public void recibirPacienteDeSala(Paciente p) {
        switch (p.consultaDeseada.tipo) {
            case TipoConsulta.Emergencia -> {
                this.consultasEmergencia.addFirst(p);
            }
            case TipoConsulta.Urgencia -> {
                // TODO: Es alta o baja?
                this.consultasUrgenciaBaja.addFirst(p);
            }
            case TipoConsulta.Normal -> {
                this.consultasNormales.addFirst(p);
            }
        }
    }

    public boolean enfermeroOcupado(Integer idEnfermero) {
        for (int i : this.enfermerosOcupados.values()) {
            if (idEnfermero.equals(i)) {
                return true;
            }
        }

        return false;
    }

    public void trancarColas() {
        this.mutexColas.lock();
    }

    public void destrancarColas() {
        this.mutexColas.unlock();
    }

    public void trancarEnfermeros() {
        this.mutexEnfermeros.lock();
    }

    public void destrancarEnfermeros() {
        this.mutexEnfermeros.unlock();
    }

    public void trancarSalas() {
        this.mutexSalas.lock();
    }

    public void destrancarSalas() {
        this.mutexSalas.unlock();
    }

    public Optional<Paciente> conseguirPaciente() {
        Optional<Paciente> pInterruptor = this.conseguirPacienteInterruptor();
        if (pInterruptor.isPresent()) {
            return pInterruptor;
        }

        Optional<Paciente> pNormal = this.conseguirPacienteNormal();
        if (pNormal.isPresent()) {
            return pNormal;
        }

        return Optional.empty();
    }

    public Optional<Paciente> conseguirPacienteInterruptor() {
        Paciente pEmergencia = this.consultasEmergencia.poll();
        if (pEmergencia != null) {
            return Optional.of(pEmergencia);
        }

        return Optional.empty();
    }

    public Optional<Paciente> conseguirPacienteNormal() {
        Paciente pUrgenciaAlta = this.consultasUrgenciaAlta.poll();
        if (pUrgenciaAlta != null) {
            return Optional.of(pUrgenciaAlta);
        }

        Paciente pUrgenciaBaja = this.consultasUrgenciaBaja.poll();
        if (pUrgenciaBaja != null) {
            return Optional.of(pUrgenciaBaja);
        }

        Paciente p = this.consultasNormales.poll();
        if (p != null) {
            return Optional.of(p);
        }

        return Optional.empty();
    }

    public void aumentarPrioridadUrgencia(Paciente p) {
        this.consultasUrgenciaBaja.remove(p);
        this.consultasUrgenciaAlta.add(p);
    }

    private void envejecerPacientes() {
        for (Paciente p : this.consultasEmergencia) {
            p.tiempoDesdeLlegada += 1;
        }

        for (Paciente p : this.consultasUrgenciaAlta) {
            p.tiempoDesdeLlegada += 1;
        }

        for (Paciente p : this.consultasUrgenciaBaja) {
            p.tiempoDesdeLlegada += 1;
        }

        for (Paciente p : this.consultasNormales) {
            p.tiempoDesdeLlegada += 1;
        }
    }

    private void actualizarStats() {
        this.statsLock.lock();
        try {
            this.stats.pacientesEsperando.put(this.horaActual.clone(),
                    new Integer[] {
                            this.consultasEmergencia.size(),
                            this.consultasUrgenciaAlta.size(),
                            this.consultasUrgenciaBaja.size(),
                            this.consultasNormales.size()
                    });
        } finally {
            this.statsLock.unlock();
        }
    }
}
