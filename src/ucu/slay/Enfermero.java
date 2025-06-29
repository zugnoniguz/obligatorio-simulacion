package ucu.slay;

import java.util.concurrent.BrokenBarrierException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Enfermero implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(Enfermero.class.getName());

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
            LOGGER.log(Level.FINER, "[{0}] Empezando el minuto", this.id);

            this.atenderPaciente();

            // y aviso que termine
            LOGGER.log(Level.FINER, "[{0}] Terminando el minuto", this.id);
            this.planificador.terminaronTodos.await();
            this.planificador.terminoElMinuto.release();
        }
    }

    private void atenderPaciente() {

        this.planificador.trancarEnfermeros();
        try {
            if (this.planificador.enfermeroOcupado(this.id)) {
                // Si estoy ocupado con un médico, no puedo hacer nada.
                return;
            }
        } finally {
            this.planificador.destrancarEnfermeros();
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
