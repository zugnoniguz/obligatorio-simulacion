package ucu.slay;

import java.util.Optional;
import java.util.concurrent.BrokenBarrierException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Medico implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(Medico.class.getName());

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
            this.planificador.empezoElMinuto.acquire();
            LOGGER.log(Level.FINER, "[{0}] Empezando el minuto", this.id);

            // me fijo que notificaciones hay (y hago lo que corresponda si pasa)
            if (this.pacienteActual == null) {
                this.conseguirPacienteNuevo();
            } else {
                this.verificarEmergencia();
            }

            if (this.pacienteActual != null && this.enfermeroId == null) {
                this.conseguirEnfermeroSiHay();
            }

            // hago lo que tengo que hacer
            if (this.puedoAtender()) {
                this.atenderPaciente();
            }

            // y aviso que termine
            LOGGER.log(Level.FINER, "[{0}] Terminando el minuto", this.id);
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

    private void conseguirPacienteNuevo() {
        LOGGER.log(Level.FINER, "[{0}] No tengo paciente, voy a buscar uno", this.id);
        this.planificador.trancarSalas();
        try {
            if (this.planificador.salasDisponibles.isEmpty()) {
                LOGGER.log(Level.FINER, "[{0}] No había sala disponible, no atiendo a nadie", this.id);
                // Si no tengo sala donde operar no puedo hacer nada.
                return;
            }

            int sala = this.planificador.salasDisponibles.removeFirst();
            this.planificador.salasOcupadas.put(this.id, sala);
            this.salaId = sala;
            LOGGER.log(Level.FINER, "[{0}] Atiendo en sala {1}", new Object[] { this.id, sala });
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
                LOGGER.log(Level.FINER, "[{0}] No había nadie para atender, no hago nada\n", this.id);
                this.planificador.trancarSalas();
                this.liberarSala();
                this.planificador.destrancarSalas();
                return;
            }

            LOGGER.log(Level.FINER, "[{0}] Tengo paciente a atender\n", this.id);
            // tengo que esperar a un enfermero y a una sala.
            this.planificador.trancarEnfermeros();
            try {
                if (this.planificador.enfermerosDisponibles.isEmpty()) {
                    LOGGER.log(
                            Level.FINER,
                            "[{0}] No había enfermeros disponibles pero me marco ocupado",
                            this.id);
                    // no hay nadie que me ayude, marco que necesito ayuda y sigo.
                    this.planificador.medicosEsperando += 1;
                    return;
                }

                Integer idEnfermero = this.planificador.enfermerosDisponibles.removeFirst();
                this.planificador.enfermerosOcupados.put(this.id, idEnfermero);
                this.enfermeroId = idEnfermero;
                LOGGER.log(Level.FINER, "[{0}] Atiendo con enfermero {1}", new Object[] { this.id, idEnfermero });
            } finally {
                this.planificador.destrancarEnfermeros();
            }
        } finally {
            this.planificador.destrancarColas();
        }
        return;
    }

    private void verificarEmergencia() throws InterruptedException {
        LOGGER.log(Level.FINER, "[{0}] Ya tengo paciente, me voy a fijar si hay emergencias", this.id);
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
                LOGGER.log(Level.FINER, "[{0}] Había una emergencia, la atiendo (llegó paciente {1})",
                        new Object[] {
                                this.id,
                                this.pacienteActual.id
                        });
            } else {
                // Si no hay nada que me interrumpa, directamente sigo
                LOGGER.log(Level.FINER, "[{0}] No había emergencia", this.id);
            }
        }
    }

    private void conseguirEnfermeroSiHay() {
        this.planificador.trancarEnfermeros();
        LOGGER.log(Level.FINER, "[{0}] No hay enfermero, voy a ver si se liberó alguno", this.id);
        try {
            if (this.planificador.enfermerosDisponibles.isEmpty()) {
                LOGGER.log(
                        Level.FINER,
                        "[{0}] No hay enfermeros, espero con paciente {1} en sala 2}",
                        new Object[] {
                                this.id,
                                this.pacienteActual.id,
                                this.salaId
                        });
                return;
            }

            Integer idEnfermero = this.planificador.enfermerosDisponibles.removeFirst();
            this.planificador.enfermerosOcupados.put(this.id, idEnfermero);
            this.enfermeroId = idEnfermero;
            this.planificador.medicosEsperando -= 1;
            LOGGER.log(Level.FINER, "[{0}] Atiendo con enfermero {1}", new Object[] { this.id, idEnfermero });
        } finally {
            this.planificador.destrancarEnfermeros();
        }
    }

    private boolean puedoAtender() {
        if (this.pacienteActual == null) {
            return false;
        }

        if (this.salaId == null) {
            return false;
        }

        if (this.enfermeroId == null) {
            return false;
        }

        return true;
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
        this.pacienteActual.tiempoRestante -= 1;
        LOGGER.log(
                Level.FINER,
                "[{0}] Atendiendo a paciente {1} con enfermero {2} en sala {3}, quedan {4}mins",
                new Object[] {
                        this.id,
                        this.pacienteActual.id,
                        this.enfermeroId,
                        this.salaId,
                        this.pacienteActual.tiempoRestante
                });
        if (pacienteActual.tiempoRestante == 0) {
            LOGGER.log(
                    Level.FINER,
                    "[{0}] Terminé de atender a paciente {1}. Libero a enfermero {2} y sala {3}",
                    new Object[] {
                            this.id,
                            this.pacienteActual.id,
                            this.enfermeroId,
                            this.salaId,
                    });

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
