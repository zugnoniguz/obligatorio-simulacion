package ucu.slay;

import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.logging.Level;
import java.util.logging.Logger;

import ucu.utils.Hora;

public class GeneradorPacientes implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(GeneradorPacientes.class.getName());

    // Should be in [0,1]
    private static final double MAX_TIME_VARIANCE = 0.5;

    private final int semilla;
    private final Random random;
    private final int cantInicialPacientes;
    private final int pacientesPorHora;
    private int currentId;
    private final Consulta[] consultas;
    private PlanificadorConsultas planificador;

    public GeneradorPacientes(
            int semilla,
            int cantInicialPacientes,
            int pacientesPorHora,
            Consulta[] consultas,
            PlanificadorConsultas planificador) {
        this.planificador = planificador;

        this.semilla = semilla;
        this.random = new Random(this.semilla);
        this.pacientesPorHora = pacientesPorHora;
        this.cantInicialPacientes = cantInicialPacientes;
        this.consultas = consultas;
        this.currentId = 1;
    }

    public void runPosta() throws BrokenBarrierException, InterruptedException {
        int[] mins = new int[this.pacientesPorHora];
        boolean firstRun = true;
        while (true) {
            this.planificador.empezoElMinuto.acquire();

            if (firstRun) {
                firstRun = false;
                for (int i = 0; i < this.cantInicialPacientes; ++i) {
                    this.generarPaciente();
                }
            }

            if (this.planificador.horaActual.min == 0) {
                for (int i = 0; i < mins.length; ++i) {
                    mins[i] = this.random.nextInt(0, Hora.MAX_MIN);
                }
            }

            for (int i = 0; i < mins.length; ++i) {
                if (mins[i] == this.planificador.horaActual.min) {
                    this.generarPaciente();
                }
            }

            this.planificador.terminaronTodos.await();
            this.planificador.terminoElMinuto.release();
        }
    }

    public void generarPaciente() {
        int idx = this.random.nextInt(consultas.length);
        Consulta consulta = consultas[idx];
        int id = this.currentId++;
        int tiempoRestante = consulta.tiempoEstimado;
        tiempoRestante *= 1 + this.random.nextDouble(-MAX_TIME_VARIANCE, MAX_TIME_VARIANCE);
        Paciente p = new Paciente(id, consulta, this.planificador.horaActual.clone(), tiempoRestante);

        char nivel = '%';
        switch (p.consultaDeseada.tipo) {
            case TipoConsulta.Emergencia -> {
                nivel = 'E';
            }
            case TipoConsulta.Urgencia -> {
                nivel = 'U';
            }
            case TipoConsulta.Normal -> {
                nivel = 'N';
            }
        }

        LOGGER.log(
                Level.FINER,
                "Generando paciente [{0}] ({1}) {2}min",
                new Object[] { p.id, nivel, p.tiempoRestante });
        this.planificador.trancarColas();
        this.planificador.recibirPaciente(p);
        this.planificador.destrancarColas();
    }

    @Override
    public void run() {
        try {
            runPosta();
        } catch (InterruptedException e) {
            System.err.printf("[GeneradorPacientes] Me interrumpieron D: (%s)\n", e.getMessage());
        } catch (BrokenBarrierException e) {
            System.err.printf("[GeneradorPacientes] Barrera rota D: (%s)\n", e.getMessage());
        }

    }
}
