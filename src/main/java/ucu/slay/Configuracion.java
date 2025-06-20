package ucu.slay;

import java.util.ArrayList;

import ucu.utils.FileUtils;

// El programa debe recibir por pantalla los datos necesarios para la simulación.
public class Configuracion {
    // Número inicial de pacientes a la hora 8:00.

    public int cantInicialPacientes;

    // Cantidad de pacientes que llegan por hora y motivo por el que vienen.
    // Cantidad de tiempo que demora en atender al paciente por especialidad.
    public int pacientesPorHora;
    public TipoConsulta[] tiposConsulta;

    // Indicador si existe sala reservada por emergencia.
    public boolean existeSalaReservada;

    public int cantMedicos;
    public int cantEnfermeros;

    public Configuracion(int cantInicialPacientes, int pacientesPorHora, TipoConsulta[] tiposConsulta,
            boolean existeSalaReservada, int cantMedicos, int cantEnfermeros) {
        this.cantInicialPacientes = cantInicialPacientes;
        this.pacientesPorHora = pacientesPorHora;
        this.tiposConsulta = tiposConsulta;
        this.existeSalaReservada = existeSalaReservada;
        this.cantMedicos = cantMedicos;
        this.cantEnfermeros = cantEnfermeros;
    }

    public static Configuracion readFromFile(String fpath) {
        String[] lines = FileUtils.readFileAsLines(fpath);

        Integer cantInicialPacientes = null;
        Integer pacientesPorHora = null;
        ArrayList<TipoConsulta> consultas = null;
        Boolean existeSalaReservada = null;
        Integer cantMedicos = null;
        Integer cantEnfermeros = null;
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

                    TipoConsulta tipo = TipoConsulta.fromString(rest.trim());
                    consultas.add(tipo);
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
                default -> {
                    System.err.printf("Ignorando clave inválida %s\n", name);
                }
            }
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

        return new Configuracion(
                cantInicialPacientes,
                pacientesPorHora,
                consultas.toArray(TipoConsulta[]::new),
                existeSalaReservada,
                cantMedicos,
                cantEnfermeros);
    }
}
