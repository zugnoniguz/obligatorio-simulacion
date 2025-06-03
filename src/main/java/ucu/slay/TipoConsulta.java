package ucu.slay;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;

public enum TipoConsulta {
	Dermatologia,
	Pediatria,
	Parto;

	public static TipoConsulta fromString(String s) {
		s = Normalizer.normalize(s, Normalizer.Form.NFD);
		s = s.replaceAll("[^\\p{ASCII}]", "");
		s = s.toLowerCase();
		switch (s) {
			case "dermatologia":
				return TipoConsulta.Dermatologia;
			case "pediatria":
				return TipoConsulta.Pediatria;
			case "parto":
				return TipoConsulta.Parto;
			default:
				System.err.printf("No existe el tipo de consulta %s\n", s);
				return null;
		}
	}

	private static HashMap<TipoConsulta, Integer> horarios = new HashMap<>(
			Map.ofEntries(
					Map.entry(TipoConsulta.Dermatologia, 5),
					Map.entry(TipoConsulta.Pediatria, 10),
					Map.entry(TipoConsulta.Parto, 30)));

	public int getTiempoEstimado() {
		if (!TipoConsulta.horarios.containsKey(this)) {
			throw new UnsupportedOperationException(
					String.format("El tipo %s no tiene tiempo estimado", this.toString()));
		}
		return TipoConsulta.horarios.get(this);
	}

	public boolean esEmergencia() {
		switch (this) {
			case Dermatologia:
				return false;
			case Pediatria:
				return false;
			case Parto:
				return true;
			default:
				throw new UnsupportedOperationException(
						String.format("No se sabe si %s es de emergencia o no\n", this.toString()));
		}
	}
}
