package ucu.utils;

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

public class FileUtils {
	public static String[] readFileAsLines(String fpath) {
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

		return lines.toArray(new String[lines.size()]);
	}
}
