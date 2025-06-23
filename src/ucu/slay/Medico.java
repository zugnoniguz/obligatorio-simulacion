package ucu.slay;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;

public class Medico implements Runnable {

    private final Semaphore empezoElMinuto;
    private final Semaphore terminoElMinuto;
    private final ArrayBlockingQueue<ArrayList<Paciente>> colaPacientesInterrupcion;
    private final ArrayBlockingQueue<ArrayList<Paciente>> colaPacientesNormales;

    private Paciente pacienteActual;

    // TODO: estaria bueno ponerles un id o algo
    public Medico(
            Semaphore start,
            Semaphore end,
            ArrayBlockingQueue<ArrayList<Paciente>> colaPacientesInterrupcion,
            ArrayBlockingQueue<ArrayList<Paciente>> colaPacientesNormales
    ) {
        this.colaPacientesInterrupcion = colaPacientesInterrupcion;
        this.colaPacientesNormales = colaPacientesNormales;
        this.empezoElMinuto = start;
        this.terminoElMinuto = end;
    }

    private void runPosta() throws InterruptedException {
        // esperar a que avance el minuto
        this.empezoElMinuto.acquire();

        // me fijo que notificaciones hay (y hago lo que corresponda si pasa)
        if (pacienteActual == null) {
            // Agarrar el que venga
            // tengo que esperar a un enfermero (probablemente un semaforo o algo (estaria bueno que el medico sepa quien es el enfermero))
        }

        if (!pacienteActual.consultaDeseada.esEmergencia()) {
            var msj = colaPacientesInterrupcion.take();
            if (!msj.isEmpty()) {
                // devuelvoPacienteASalaDeEspera(pacienteActual);
                pacienteActual = msj.get(0);
                // loEstoyAtendiendo(pacienteActual);
                // tengo que esperar a un enfermero (probablemente un semaforo o algo (estaria bueno que el medico sepa quien es el enfermero))
            }
            // si esta vacio es porque no hay emergencias para atender
        }

        // hago lo que tengo que hacer
        pacienteActual.tiempoRestante -= 1;
        if (pacienteActual.tiempoRestante == 0) {
            pacienteActual = null;
        }

        // y aviso que termine
        this.terminoElMinuto.release();
    }

    @Override
    public void run() {
        try {
            runPosta();
        } catch (InterruptedException e) {
            System.err.printf("[Medico] Me interrumpieron D: (%s)", e.getMessage());
        }
    }

}
