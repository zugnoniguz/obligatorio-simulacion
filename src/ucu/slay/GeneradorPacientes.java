package ucu.slay;

import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.logging.Level;
import java.util.logging.Logger;

import ucu.utils.Hora;

public class GeneradorPacientes implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(PlanificadorConsultas.class.getName());

    // Should be in [0,1]
    private static final double MAX_TIME_VARIANCE = 0.5;

    private final int semilla;
    private final Random random;
    private final int cantInicialPacientes;
    private final int pacientesPorHora;
    private int currentId;
    private PlanificadorConsultas planificador;

    public GeneradorPacientes(
            int semilla,
            int cantInicialPacientes,
            int pacientesPorHora,
            PlanificadorConsultas planificador) {
        this.planificador = planificador;

        this.semilla = semilla;
        this.random = new Random(this.semilla);
        this.pacientesPorHora = pacientesPorHora;
        this.cantInicialPacientes = cantInicialPacientes;
        this.currentId = 1;

        for (int i = 0; i < this.cantInicialPacientes; ++i) {
            this.generarPaciente();
        }
    }

    public void runPosta() throws BrokenBarrierException, InterruptedException {
        int[] mins = new int[this.pacientesPorHora];
        while (true) {
            this.planificador.empezoElMinuto.acquire();

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
        int idx = this.random.nextInt(TipoConsulta.getCount());
        TipoConsulta consulta = TipoConsulta.fromIdx(idx);
        Paciente p = new Paciente();
        p.consultaDeseada = consulta;
        p.tiempoDesdeLlegada = 0;
        p.tiempoRestante = p.consultaDeseada.getTiempoEstimado();
        double variance = this.random.nextDouble(-MAX_TIME_VARIANCE, MAX_TIME_VARIANCE);
        p.tiempoRestante += p.tiempoRestante * variance;
        p.id = this.currentId++;

        char nivel = '%';
        if (p.consultaDeseada.esEmergencia()) {
            nivel = 'E';
        } else if (p.consultaDeseada.esUrgencia()) {
            nivel = 'U';
        } else {
            nivel = 'N';
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
