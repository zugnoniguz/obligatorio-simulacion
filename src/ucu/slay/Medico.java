package ucu.slay;

import java.util.Optional;
import java.util.concurrent.BrokenBarrierException;

public class Medico implements Runnable {

    private final int id;
    private Integer salaId;
    private Integer enfermeroId;
    private final PlanificadorConsultas planificador;

    private Paciente pacienteActual;

    public Medico(
            int id,
            PlanificadorConsultas planificador) {
        this.id = id;
        this.planificador = planificador;
    }

    private void runPosta() throws BrokenBarrierException, InterruptedException {
        while (true) {
            // esperar a que avance el minuto
            System.out.printf("[Médico %d] Empezando el minuto\n", this.id);
            this.planificador.empezoElMinuto.acquire();

            // me fijo que notificaciones hay (y hago lo que corresponda si pasa)
            this.conseguirPacienteNuevoSiCorresponde();

            // hago lo que tengo que hacer
            if (this.pacienteActual != null) {
                this.atenderPaciente();
            }

            // y aviso que termine
            System.out.printf("[Médico %d] Terminando el minuto\n", this.id);
            this.planificador.terminaronTodos.await();
            this.planificador.terminoElMinuto.release();
        }
    }

    @Override
    public void run() {
        try {
            runPosta();
        } catch (InterruptedException e) {
            System.err.printf("[Medico %d] Me interrumpieron D: (%s)\n", this.id, e.getMessage());
        } catch (BrokenBarrierException e) {
            System.err.printf("[Medico %d] Barrera rota D: (%s)\n", this.id, e.getMessage());
        }
    }

    private void conseguirPacienteNuevoSiCorresponde() throws InterruptedException {
        if (this.pacienteActual == null) {
            System.err.printf("[Medico %d] No tengo paciente, voy a buscar uno\n", this.id);
            this.planificador.trancarSalas();
            try {
                if (this.planificador.salasDisponibles.isEmpty()) {
                    System.err.printf("[Medico %d] No había sala disponible, no atiendo a nadie\n", this.id);
                    // Si no tengo sala donde operar no puedo hacer nada.
                    return;
                }

                int sala = this.planificador.salasDisponibles.removeLast();
                this.planificador.salasOcupadas.put(this.id, sala);
                this.salaId = sala;
                System.err.printf("[Medico %d] Atiendo en sala %d\n", this.id, sala);
            } finally {
                this.planificador.destrancarSalas();
            }

            this.planificador.trancarColas();
            try {
                Optional<Paciente> p = this.planificador.conseguirPaciente();
                if (p.isPresent()) {
                    this.pacienteActual = p.orElseThrow();
                }

                if (this.pacienteActual == null) {
                    // no hay nadie para atender, tengo que ceder la sala que conseguí
                    System.err.printf("[Medico %d] No había nadie para atender, no hago nada\n", this.id);
                    this.planificador.trancarSalas();
                    this.liberarSala();
                    this.planificador.destrancarSalas();
                    return;
                }

                System.err.printf("[Medico %d] Atiendo a un paciente\n", this.id);
                // tengo que esperar a un enfermero y a una sala.
                this.planificador.trancarEnfermeros();
                try {
                    if (this.planificador.enfermerosDisponibles.isEmpty()) {
                        System.err.printf("[Medico %d] No había enfermeros disponibles pero me marco ocupado\n",
                                this.id);
                        // no hay nadie que me ayude, marco que necesito ayuda y sigo.
                        this.planificador.medicosEsperando += 1;
                        return;
                    }

                    Integer idEnfermero = this.planificador.enfermerosDisponibles.removeLast();
                    this.planificador.enfermerosOcupados.put(this.id, idEnfermero);
                    this.enfermeroId = idEnfermero;
                    System.err.printf("[Medico %d] Atiendo con enfermero %d\n",
                            this.id, idEnfermero);
                } finally {
                    this.planificador.destrancarEnfermeros();
                }
            } finally {
                this.planificador.destrancarColas();
            }
            return;
        }

        // Si ya tengo un paciente, pero no es emergencia, puedo interrumpirlo y poner
        // una emergencia
        if (!this.pacienteActual.consultaDeseada.esEmergencia()) {
            // Solo me importa si hay pacientes nuevos si ya no estoy atendiendo una
            // emergencia

            Optional<Paciente> p = this.planificador.conseguirPacienteInterruptor();
            if (p.isPresent()) {
                this.planificador.recibirPacienteDeSala(this.pacienteActual);
                this.pacienteActual = p.orElseThrow();

                // no necesito esperar a un enfermero ni sala porque ya tengo uno al estar
                // atendiendo ya un paciente
            }

            // Si no hay nada que me interrumpa, directamente sigo
        }
    }

    private void liberarSala() {
        int salaId = this.planificador.salasOcupadas.remove(this.id);
        this.planificador.salasDisponibles.add(salaId);
        this.salaId = null;
    }

    private void liberarEnfermero() {
        int enfermeroId = this.planificador.enfermerosOcupados.remove(this.id);
        this.planificador.enfermerosDisponibles.add(enfermeroId);
        this.enfermeroId = null;
    }

    private void atenderPaciente() throws InterruptedException {
        pacienteActual.tiempoRestante -= 1;
        if (pacienteActual.tiempoRestante == 0) {
            pacienteActual = null;

            this.planificador.trancarSalas();
            this.liberarSala();
            this.planificador.destrancarSalas();

            this.planificador.trancarEnfermeros();
            this.liberarEnfermero();
            this.planificador.destrancarEnfermeros();
        }
    }
}
