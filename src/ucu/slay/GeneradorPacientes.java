package ucu.slay;

import java.util.Random;
import java.util.concurrent.Semaphore;

public class GeneradorPacientes implements Runnable {
    private int semilla;
    private Random random;
    private Semaphore empezoElMinuto;
    private Semaphore terminoElMinuto;

    public GeneradorPacientes(Semaphore empezoElMinuto, Semaphore terminoElMinuto, int semilla) {
        this.empezoElMinuto = empezoElMinuto;
        this.terminoElMinuto = terminoElMinuto;

        this.semilla = semilla;
        this.random = new Random(this.semilla);
    }

    public void runPosta() throws InterruptedException {
        while (true) {
            this.empezoElMinuto.acquire();

            if (this.random.nextBoolean()) {
                System.out.println("[GeneradorPacientes] bruh");
            }

            this.terminoElMinuto.release();
        }
    }

    @Override
    public void run() {
        try {
            runPosta();
        } catch (InterruptedException e) {
            System.err.printf("[GeneradorPacientes] Me interrumpieron D: (%s)\n", e.getMessage());
        }

    }
}
