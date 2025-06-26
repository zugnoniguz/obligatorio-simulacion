package ucu.slay;

import java.util.Random;
import java.util.concurrent.BrokenBarrierException;

public class GeneradorPacientes implements Runnable {
    private int semilla;
    private Random random;
    private PlanificadorConsultas planificador;

    public GeneradorPacientes(int semilla, PlanificadorConsultas planificador) {
        this.planificador = planificador;

        this.semilla = semilla;
        this.random = new Random(this.semilla);
    }

    public void runPosta() throws BrokenBarrierException, InterruptedException {
        while (true) {
            this.planificador.empezoElMinuto.acquire();

            if (this.random.nextBoolean()) {
                System.out.println("[GeneradorPacientes] bruh");
            }

            this.planificador.terminaronTodos.await();
            this.planificador.terminoElMinuto.release();
        }
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
