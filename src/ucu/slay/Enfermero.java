package ucu.slay;

import java.util.concurrent.Semaphore;

public class Enfermero implements Runnable {

	private final int id;
	private final Semaphore empezoElMinuto;
	private final Semaphore terminoElMinuto;

	public Enfermero(int id, Semaphore empezoElMinuto, Semaphore terminoElMinuto) {
		this.id = id;
		this.empezoElMinuto = empezoElMinuto;
		this.terminoElMinuto = terminoElMinuto;
	}

	public void runPosta() throws InterruptedException {
		while (true) {
			// esperar a que avance el minuto
			this.empezoElMinuto.acquire();
			System.out.printf("[Enfermero %d] Empezando el minuto\n", this.id);

			// y aviso que termine
			System.out.printf("[Enfermero %d] Terminando el minuto\n", this.id);
			this.terminoElMinuto.release();
		}

	}

	@Override
	public void run() {
		try {
			runPosta();
		} catch (InterruptedException e) {
			System.err.printf("[Enfermero %d] Me interrumpieron D: (%s)\n", this.id, e.getMessage());
		}
	}

	// semaforo si esta ocupado o libre
	// si esta libre y el medico neceista algo que vaya bro
}
