package ucu.slay;

import java.util.Optional;
import java.util.concurrent.BrokenBarrierException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Enfermero implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(Enfermero.class.getName());

    private final int id;
    private Integer salaId;
    private Paciente paciente;
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

            boolean disponible = this.puedoTrabajar();

            if (disponible) {
                if (this.paciente == null) {
                    this.conseguirPacienteNuevo();
                } else {
                    this.atenderPaciente();
                }
            }

            // y aviso que termine
            LOGGER.log(Level.FINER, "[{0}] Terminando el minuto", this.id);
            this.planificador.terminaronTodos.await();
            this.planificador.terminoElMinuto.release();
        }
    }

    private boolean puedoTrabajar() {
        boolean disponible = true;
        this.planificador.trancarEnfermeros();
        try {
            if (this.paciente == null) {
                LOGGER.log(Level.FINER, "[{0}] No tengo paciente, me voy a fijar si estoy disponible", this.id);
                disponible = this.planificador.enfermerosDisponibles.contains(this.id);
                if (disponible) {
                    if (planificador.hayMedicosEsperando()) {
                        LOGGER.log(Level.FINER, "[{0}] Estaba disponible, pero hay médicos esperando", this.id);
                        disponible = false;
                    } else {
                        LOGGER.log(Level.FINER, "[{0}] Estoy disponible, voy a ver si hay pacientes", this.id);
                        this.planificador.enfermerosDisponibles.remove(Integer.valueOf(this.id));
                    }
                } else {
                    LOGGER.log(Level.FINER, "[{0}] No estaba disponible, no hago nada", this.id);
                }
            } else {
                // Si tengo paciente pero hay alguien esperando emergencia entonces tengo que
                // dejar lo que estoy haciendo
                LOGGER.log(Level.FINER, "[{0}] Tengo un paciente, voy a ver si hay emergencias", this.id);
                if (this.planificador.medicosEsperandoEmergencia > 0) {
                    LOGGER.log(Level.FINER, "[{0}] Había una, dejo mi paciente", this.id);
                    this.planificador.recibirPacienteDeSala(this.paciente);
                    this.paciente = null;
                    this.planificador.enfermerosDisponibles.add(this.id);
                    this.planificador.enfermerosOcupadosSolos.remove(this.id);

                    this.planificador.trancarSalas();
                    this.liberarSala();
                    this.planificador.destrancarSalas();

                    disponible = false;
                } else {
                    LOGGER.log(Level.FINER, "[{0}] No había emergencias, sigo con el mío", this.id);
                }
            }
        } finally {
            this.planificador.destrancarEnfermeros();
        }

        return disponible;
    }

    private void conseguirPacienteNuevo() {
        this.planificador.trancarSalas();
        try {
            if (this.planificador.salasDisponiblesEnfermero.isEmpty()) {
                LOGGER.log(Level.FINER, "[{0}] No tengo dónde atender", this.id);
                // Si no tengo sala donde operar no puedo hacer nada.
                return;
            }

            int sala = this.planificador.salasDisponiblesEnfermero.removeLast();
            this.salaId = sala;
            this.planificador.salasOcupadasEnfermero.put(this.id, sala);
            LOGGER.log(Level.FINER, "[{0}] Atiendo en sala {1}", new Object[] { this.id, sala });
        } finally {
            this.planificador.destrancarSalas();
        }

        this.planificador.trancarColas();
        try {
            Optional<Paciente> p = this.planificador.conseguirPacienteEnfermeria();
            if (p.isPresent()) {
                this.paciente = p.orElseThrow();
                this.planificador.enfermerosOcupadosSolos.add(this.id);
                LOGGER.log(
                        Level.FINER,
                        "[{0}] Atiendo a {1} en sala {2}",
                        new Object[] {
                                this.id,
                                this.paciente.id,
                                this.salaId
                        });
            } else {
                LOGGER.log(Level.FINER, "[{0}] No tengo a quién atender, cedo sala {1}",
                        new Object[] { this.id, this.salaId });
                this.planificador.trancarSalas();
                this.planificador.salasDisponiblesEnfermero.add(this.salaId);
                this.salaId = null;
                this.planificador.destrancarSalas();

                this.planificador.trancarEnfermeros();
                this.planificador.enfermerosDisponibles.add(this.id);
                this.planificador.destrancarEnfermeros();
            }
        } finally {
            this.planificador.destrancarColas();
        }
    }

    private void atenderPaciente() {
        paciente.tiempoRestante -= 1;
        paciente.tiempoDesdeLlegada += 1;

        if (paciente.tiempoRestante == 0) {
            this.planificador.trancarSalas();
            this.liberarSala();
            this.planificador.destrancarSalas();

            this.planificador.trancarEnfermeros();
            // this.liberarEnfermero();
            this.planificador.enfermerosDisponibles.add(this.id);
            this.planificador.enfermerosOcupadosSolos.remove(this.id);
            this.planificador.destrancarEnfermeros();

            this.paciente = null;
        }
    }

    private void liberarSala() {
        int salaId = this.planificador.salasOcupadasEnfermero.remove(this.id);
        this.planificador.salasDisponiblesEnfermero.add(salaId);
        this.salaId = null;
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
