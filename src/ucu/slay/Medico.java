package ucu.slay;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;

public class Medico implements Runnable {

    private final int id;
    private final Semaphore empezoElMinuto;
    private final Semaphore terminoElMinuto;
    private final ArrayBlockingQueue<Paciente> colaPacientesInterrupcion;
    private final ArrayBlockingQueue<Paciente> colaPacientesNormales;

    private Paciente pacienteActual;

    public Medico(
            int id,
            Semaphore start,
            Semaphore end,
            ArrayBlockingQueue<Paciente> colaPacientesInterrupcion,
            ArrayBlockingQueue<Paciente> colaPacientesNormales) {
        this.id = id;
        this.colaPacientesInterrupcion = colaPacientesInterrupcion;
        this.colaPacientesNormales = colaPacientesNormales;
        this.empezoElMinuto = start;
        this.terminoElMinuto = end;
    }

    private void conseguirPacienteNuevoSiCorresponde() throws InterruptedException {
        if (this.pacienteActual == null) {
            Paciente p = this.colaPacientesInterrupcion.poll();
            if (p != null) {
                this.pacienteActual = p;
            } else {
                p = this.colaPacientesNormales.poll();
                if (p != null) {
                    this.pacienteActual = p;
                }
            }

            if (this.pacienteActual != null) {
                // loEstoyAtendiendo(pacienteActual);
                // tengo que esperar a un enfermero (probablemente un semaforo o algo (estaria
                // bueno que el medico sepa quien es el enfermero))
            }
            return;
        }

        // Si ya tengo un paciente, pero no es emergencia, puedo interrumpirlo y poner
        // una emergencia
        if (!this.pacienteActual.consultaDeseada.esEmergencia()) {
            // Solo me importa si hay pacientes nuevos si ya no estoy atendiendo una
            // emergencia

            Paciente p = colaPacientesInterrupcion.poll();
            if (p != null) {
                // devuelvoPacienteASalaDeEspera(pacienteActual);
                // loEstoyAtendiendo(pacienteActual);

                // no necesito esperar a un enfermero porque ya tengo uno al estar atendiendo ya
                // un paciente
            }

            // Si no hay nada que me interrumpa, directamente sigo
        }
    }

    private void atenderPaciente() throws InterruptedException {
        pacienteActual.tiempoRestante -= 1;
        if (pacienteActual.tiempoRestante == 0) {
            pacienteActual = null;
        }
    }

    private void runPosta() throws InterruptedException {
        while (true) {
            // esperar a que avance el minuto
            this.empezoElMinuto.acquire();
            System.out.printf("[Médico %d] Empezando el minuto\n", this.id);

            // me fijo que notificaciones hay (y hago lo que corresponda si pasa)
            this.conseguirPacienteNuevoSiCorresponde();

            // hago lo que tengo que hacer
            if (this.pacienteActual != null) {
                this.atenderPaciente();
            }

            // y aviso que termine
            System.out.printf("[Médico %d] Terminando el minuto\n", this.id);
            this.terminoElMinuto.release();
        }
    }

    @Override
    public void run() {
        try {
            runPosta();
        } catch (InterruptedException e) {
            System.err.printf("[Medico %d] Me interrumpieron D: (%s)\n", this.id, e.getMessage());
        }
    }

}
