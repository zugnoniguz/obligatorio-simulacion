package ucu.slay;

import java.util.Random;
import java.util.concurrent.BrokenBarrierException;

public class GeneradorPacientes implements Runnable {
    // Should be in [0,1]
    private static final double MAX_TIME_VARIANCE = 0.5;

    private final int semilla;
    private final Random random;
    private PlanificadorConsultas planificador;

    public GeneradorPacientes(int semilla, PlanificadorConsultas planificador) {
        this.planificador = planificador;

        this.semilla = semilla;
        this.random = new Random(this.semilla);

        for (int i = 0; i < this.planificador.config.cantInicialPacientes; ++i) {
            this.generarPaciente();
        }
    }

    public void runPosta() throws BrokenBarrierException, InterruptedException {
        int[] mins = new int[this.planificador.config.pacientesPorHora];
        while (true) {
            this.planificador.empezoElMinuto.acquire();

            if (this.planificador.horaActual.min == 0) {
                for (int i = 0; i < mins.length; ++i) {
                    mins[i] = this.random.nextInt(0, 60);
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

        if (p.consultaDeseada.esEmergencia()) {
            System.out.printf("[GeneradorPacientes] Generando paciente (E) %dmin\n", p.tiempoRestante);
        } else if (p.consultaDeseada.esUrgencia()) {
            System.out.printf("[GeneradorPacientes] Generando paciente (U) %dmin\n", p.tiempoRestante);
        } else {
            System.out.printf("[GeneradorPacientes] Generando paciente (N) %dmin\n", p.tiempoRestante);
        }
        this.planificador.recibirPaciente(p);
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
