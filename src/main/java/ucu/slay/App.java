package ucu.slay;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

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

		try {
			reader.close();
		} catch (IOException e) {
			System.err.printf("Hubo un error al cerrar el archivo: %s\n", e.getMessage());
			return null;
		}
		throw new OperationNotSupportedException("No sé leer de un archivo");
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
	}
}
