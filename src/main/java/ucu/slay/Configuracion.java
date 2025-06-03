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

	public Configuracion(int cantInicialPacientes, int pacientesPorHora, TipoConsulta[] tiposConsulta,
			boolean existeSalaReservada) {
		this.cantInicialPacientes = cantInicialPacientes;
		this.pacientesPorHora = pacientesPorHora;
		this.tiposConsulta = tiposConsulta;
		this.existeSalaReservada = existeSalaReservada;
	}

	public static Configuracion readFromFile(String fpath) {
		String[] lines = FileUtils.readFileAsLines(fpath);

		Integer cantInicialPacientes = null;
		Integer pacientesPorHora = null;
		ArrayList<TipoConsulta> consultas = null;
		Boolean existeSalaReservada = null;
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
				case "cantInicialPacientes": {
					if (cantInicialPacientes != null) {
						System.err.printf("Valor duplicado de cantInicialPacientes, ignorando\n");
					}
					cantInicialPacientes = Integer.valueOf(rest.trim());
				}
					break;
				case "pacientesPorHora": {
					if (pacientesPorHora != null) {
						System.err.printf("Valor duplicado de pacientesPorHora, ignorando\n");
					}
					pacientesPorHora = Integer.valueOf(rest.trim());
				}
					break;
				case "consulta": {
					if (consultas == null) {
						consultas = new ArrayList<>();
					}

					TipoConsulta tipo = TipoConsulta.fromString(rest.trim());
					consultas.add(tipo);
				}
					break;
				case "existeSalaReservada": {
					if (existeSalaReservada != null) {
						System.err.printf("Valor duplicado de existeSalaReservada, ignorando\n");
					}
					existeSalaReservada = Boolean.valueOf(rest.trim());
				}
					break;
				default: {
					System.err.printf("Ignorando clave inválida %s\n", name);
				}
					break;
			}
		}

		return new Configuracion(cantInicialPacientes, pacientesPorHora,
				consultas.toArray(new TipoConsulta[consultas.size()]), existeSalaReservada);
	}
}
