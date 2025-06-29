package ucu.slay;

import java.util.ArrayList;

import ucu.utils.FileUtils;

// El programa debe recibir por pantalla los datos necesarios para la simulación.
public class Configuracion {
    public final int semilla;

    // Número inicial de pacientes a la hora 8:00.
    public final int cantInicialPacientes;

    // Cantidad de pacientes que llegan por hora y motivo por el que vienen.
    // Cantidad de tiempo que demora en atender al paciente por especialidad.
    public final int pacientesPorHora;
    public final Consulta[] tiposConsulta;

    // Indicador si existe sala reservada por emergencia.
    public final boolean existeSalaReservada;

    public final int cantMedicos;
    public final int cantEnfermeros;
    public final int cantSalas;

    public Configuracion(
            int semilla,
            int cantInicialPacientes,
            int pacientesPorHora,
            Consulta[] tiposConsulta,
            boolean existeSalaReservada,
            int cantMedicos,
            int cantEnfermeros,
            int cantSalas) {
        this.semilla = semilla;
        this.cantInicialPacientes = cantInicialPacientes;
        this.pacientesPorHora = pacientesPorHora;
        this.tiposConsulta = tiposConsulta;
        this.existeSalaReservada = existeSalaReservada;
        this.cantMedicos = cantMedicos;
        this.cantEnfermeros = cantEnfermeros;
        this.cantSalas = cantSalas;
    }

    public static Configuracion readFromFile(String fpath) {
        String[] lines = FileUtils.readFileAsLines(fpath);

        Integer semilla = null;
        Integer cantInicialPacientes = null;
        Integer pacientesPorHora = null;
        ArrayList<Consulta> consultas = null;
        Boolean existeSalaReservada = null;
        Integer cantMedicos = null;
        Integer cantEnfermeros = null;
        Integer cantSalas = null;
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            int idx = line.indexOf("=");
            if (idx == -1) {
                System.err.printf("Ignorando línea inválida %s\n", line);
            }

            String name = line.substring(0, idx);
            String rest = line.substring(line.indexOf("=") + 1);
            switch (name.trim()) {
                case "semilla" -> {
                    if (semilla != null) {
                        System.err.printf("Valor duplicado de semilla, ignorando\n");
                        continue;
                    }
                    semilla = Integer.valueOf(rest.trim());
                }
                case "cantInicialPacientes" -> {
                    if (cantInicialPacientes != null) {
                        System.err.printf("Valor duplicado de cantInicialPacientes, ignorando\n");
                        continue;
                    }
                    cantInicialPacientes = Integer.valueOf(rest.trim());
                }
                case "pacientesPorHora" -> {
                    if (pacientesPorHora != null) {
                        System.err.printf("Valor duplicado de pacientesPorHora, ignorando\n");
                        continue;
                    }
                    pacientesPorHora = Integer.valueOf(rest.trim());
                }
                case "consulta" -> {
                    if (consultas == null) {
                        consultas = new ArrayList<>();
                    }

                    String[] vals = rest.trim().split(",");
                    String nombre = vals[0].trim();
                    TipoConsulta tipo = TipoConsulta.fromString(vals[1].trim());
                    int tiempoEstimado = Integer.valueOf(vals[2].trim());
                    Consulta cons = new Consulta(nombre, tipo, tiempoEstimado);
                    consultas.add(cons);
                }
                case "existeSalaReservada" -> {
                    if (existeSalaReservada != null) {
                        System.err.printf("Valor duplicado de existeSalaReservada, ignorando\n");
                        continue;
                    }
                    existeSalaReservada = Boolean.valueOf(rest.trim());
                }
                case "cantMedicos" -> {
                    if (cantMedicos != null) {
                        System.err.printf("Valor duplicado de cantMedicos, ignorando\n");
                        continue;
                    }
                    cantMedicos = Integer.valueOf(rest.trim());
                }
                case "cantEnfermeros" -> {
                    if (cantEnfermeros != null) {
                        System.err.printf("Valor duplicado de cantMedicos, ignorando\n");
                        continue;
                    }
                    cantEnfermeros = Integer.valueOf(rest.trim());
                }
                case "cantSalas" -> {
                    if (cantSalas != null) {
                        System.err.printf("Valor duplicado de cantSalas, ignorando\n");
                        continue;
                    }
                    cantSalas = Integer.valueOf(rest.trim());
                }
                default -> {
                    System.err.printf("Ignorando clave inválida %s\n", name);
                }
            }
        }

        if (semilla == null) {
            System.err.println("Falto semilla");
            return null;
        }

        if (cantInicialPacientes == null) {
            System.err.println("Falto cantInicialPacientes");
            return null;
        }

        if (pacientesPorHora == null) {
            System.err.println("Falto pacientesPorHora");
            return null;
        }

        if (consultas == null) {
            System.err.println("Falto consultas");
            return null;
        }

        if (existeSalaReservada == null) {
            System.err.println("Falto existeSalaReservada");
            return null;
        }

        if (cantMedicos == null) {
            System.err.println("Falto cantMedicos");
            return null;
        }

        if (cantEnfermeros == null) {
            System.err.println("Falto cantEnfermeros");
            return null;
        }

        if (cantSalas == null) {
            System.err.println("Falto cantSalas");
            return null;
        }

        return new Configuracion(
                semilla,
                cantInicialPacientes,
                pacientesPorHora,
                consultas.toArray(Consulta[]::new),
                existeSalaReservada,
                cantMedicos,
                cantEnfermeros,
                cantSalas);
    }
}
