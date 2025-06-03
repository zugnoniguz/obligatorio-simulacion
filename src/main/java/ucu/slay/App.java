package ucu.slay;

import javax.naming.OperationNotSupportedException;

public class App {

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
			config = Configuracion.readFromFile(args[1]);
		}

		if (config == null) {
			System.err.println("No pudimos leer la configuración papá");
			return;
		}

		System.out.printf("Vamos a atender %d pacientes a las 08:00\n", config.cantInicialPacientes);
		System.out.printf("Vamos a atender %d pacientes por hora\n", config.pacientesPorHora);
		for (TipoConsulta consulta : config.tiposConsulta) {
			System.out.printf("Consulta: %d min, nombre: %s, es emergencia: %b\n", consulta.getTiempoEstimado(),
					consulta.toString(),
					consulta.esEmergencia());
		}
		System.out.printf("Hay sala reservada: %b\n", config.existeSalaReservada);
	}
}
