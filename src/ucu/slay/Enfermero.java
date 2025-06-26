package ucu.slay;

import java.util.concurrent.BrokenBarrierException;

public class Enfermero implements Runnable {

    private final int id;
    private final PlanificadorConsultas planificador;

    public Enfermero(int id, PlanificadorConsultas planificador) {
        this.id = id;
        this.planificador = planificador;
    }

    public void runPosta() throws BrokenBarrierException, InterruptedException {
        while (true) {
            // esperar a que avance el minuto
            this.planificador.empezoElMinuto.acquire();
            System.out.printf("[Enfermero %d] Empezando el minuto\n", this.id);

            // y aviso que termine
            System.out.printf("[Enfermero %d] Terminando el minuto\n", this.id);
            this.planificador.terminaronTodos.await();
            this.planificador.terminoElMinuto.release();
        }
    }

    @Override
    public void run() {
        try {
            runPosta();
        } catch (InterruptedException e) {
            System.err.printf("[Enfermero %d] Me interrumpieron D: (%s)\n", this.id, e.getMessage());
        } catch (BrokenBarrierException e) {
            System.err.printf("[Enfermero %d] Barrera rota D: (%s)\n", this.id, e.getMessage());
        }
    }

    // semaforo si esta ocupado o libre
    // si esta libre y el medico neceista algo que vaya bro
}
