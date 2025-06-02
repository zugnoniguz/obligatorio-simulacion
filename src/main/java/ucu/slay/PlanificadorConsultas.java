package ucu.slay;

import java.util.ArrayDeque;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

public class PlanificadorConsultas {
	// Las emergencias no tienen prioridad. Las atendemos y listo.
	private ArrayDeque<Paciente> consultasEmergencia;
	// Traba para la cola de emergencias.
	// Nota(guz): Que me la chupe synchronized.
	private ReentrantLock lockConsultasEmergencia;

	// Las urgencias tampoco tienen proridad entre sí.
	private ArrayDeque<Paciente> consultasUrgencia;
	// Traba para la cola de urgencias.
	private ReentrantLock lockConsultasUrgencia;

	// Entre las consultas normales, tampoco hay prioridad.
	//
	// Las urgencias tendrían más prioridad, en teoría, pero sin embargo una vez que
	// envejecen demasiado pasan a `consultasUrgencia`.
	private ArrayDeque<Paciente> consultasNormales;
	// Traba para la cola de consultas normales.
	private ReentrantLock lockConsultasNormales;

	private Semaphore cantidadPacientes;

	public PlanificadorConsultas() {
		this.consultasEmergencia = new ArrayDeque<>();
		this.lockConsultasEmergencia = new ReentrantLock();
		this.consultasUrgencia = new ArrayDeque<>();
		this.lockConsultasUrgencia = new ReentrantLock();
		this.consultasNormales = new ArrayDeque<>();
		this.lockConsultasNormales = new ReentrantLock();
		this.cantidadPacientes = new Semaphore(0);
	}

	public void recibirPaciente(Paciente p) {
		if (p.consultaDeseada.esEmergencia) {
			this.lockConsultasEmergencia.lock();
			this.consultasEmergencia.add(p);
			this.lockConsultasEmergencia.unlock();
		} else if (p.consultaDeseada.esUrgencia) {
			this.lockConsultasUrgencia.lock();
			this.consultasUrgencia.add(p);
			this.lockConsultasUrgencia.unlock();
		} else {
			this.lockConsultasNormales.lock();
			this.consultasNormales.add(p);
			this.lockConsultasNormales.unlock();
		}
		this.cantidadPacientes.release();
	}

	public Optional<Paciente> tomarPaciente() {
		this.lockConsultasEmergencia.lock();
		if (!this.consultasEmergencia.isEmpty()) {
			this.lockConsultasEmergencia.unlock();
			return Optional.of(this.consultasEmergencia.pop());
		}
		this.lockConsultasEmergencia.unlock();

		this.lockConsultasUrgencia.lock();
		if (!this.consultasUrgencia.isEmpty()) {
			this.lockConsultasUrgencia.unlock();
			return Optional.of(this.consultasUrgencia.pop());
		}
		this.lockConsultasUrgencia.unlock();

		this.lockConsultasNormales.lock();
		if (!this.consultasNormales.isEmpty()) {
			this.lockConsultasNormales.unlock();
			return Optional.of(this.consultasNormales.pop());
		}
		this.lockConsultasNormales.unlock();

		return Optional.empty();
	}

	public Paciente esperarPaciente() {
		try {
			this.cantidadPacientes.acquire();
		} catch (InterruptedException e) {
			System.err.println("No interrumpan al pana");
		}
		return this.tomarPaciente().orElseThrow();
	}
}
