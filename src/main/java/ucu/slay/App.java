package ucu.slay;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import javax.naming.OperationNotSupportedException;

public class App {
	private static Configuracion readFromFile(String fpath) throws OperationNotSupportedException {
		FileReader fReader;
		try {
			fReader = new FileReader(new File(fpath));
		} catch (FileNotFoundException e) {
			System.err.printf("No existe el archivo `%s`, papanatas\n", fpath);
			return null;
		}
		BufferedReader reader = new BufferedReader(fReader);
		ArrayList<String> lines = new ArrayList<>();
		try {
			try {
				while (true) {
					String s = reader.readLine();
					if (s == null) {
						break;
					}
					lines.add(s);
				}
			} catch (IOException e) {
				System.err.printf("Error al leer el archivo: `%s`\n", fpath);
				return null;
			}
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				System.err.printf("Hubo un error al cerrar el archivo: %s\n", e.getMessage());
				return null;
			}
		}

		Integer cantInicialPacientes = null;
		Integer pacientesPorHora = null;
		ArrayList<InfoConsulta> consultas = null;
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

					String[] splitVal = rest.split(",");
					int duracion = Integer.valueOf(splitVal[0].trim());
					String nombre = splitVal[1].trim();
					boolean esEmergencia = Boolean.valueOf(splitVal[2].trim());
					boolean esUrgencia = Boolean.valueOf(splitVal[3].trim());
					InfoConsulta consulta = new InfoConsulta(duracion, nombre, esEmergencia, esUrgencia);
					consultas.add(consulta);
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
				consultas.toArray(new InfoConsulta[consultas.size()]), existeSalaReservada);
	}

	private static Configuracion readFromStdin() throws OperationNotSupportedException {
		throw new OperationNotSupportedException("No sé leer de la consola");
	}

	public static void main(String[] args) throws Exception {
		Configuracion config;
		if (args.length == 0) {
			// No me pasaron nada
			config = readFromStdin();
		} else {
			assert args[0].equals("-f");
			config = readFromFile(args[1]);
		}

		if (config == null) {
			System.err.println("No pudimos leer la configuración papá");
			return;
		}

		System.out.printf("Vamos a atender %d pacientes a las 08:00\n", config.cantInicialPacientes);
		System.out.printf("Vamos a atender %d pacientes por hora\n", config.pacientesPorHora);
		for (InfoConsulta consulta : config.tiposConsulta) {
			System.out.printf("Consulta: %d min, nombre: %s, es emergencia: %b\n", consulta.duracion, consulta.tipo,
					consulta.esEmergencia);
		}
		System.out.printf("Hay sala reservada: %b\n", config.existeSalaReservada);
	}
}
