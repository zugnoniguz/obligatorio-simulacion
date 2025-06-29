package ucu.slay;

import java.text.Normalizer;

public enum TipoConsulta {
    Emergencia,
    Urgencia,
    Normal;

    public static TipoConsulta fromString(String s) {
        s = Normalizer.normalize(s, Normalizer.Form.NFD);
        s = s.replaceAll("[^\\p{ASCII}]", "");
        s = s.toLowerCase();
        switch (s) {
            case "emergencia" -> {
                return TipoConsulta.Emergencia;
            }
            case "urgencia" -> {
                return TipoConsulta.Urgencia;
            }
            case "normal" -> {
                return TipoConsulta.Normal;
            }
            default -> {
                System.err.printf("No existe el tipo de consulta %s\n", s);
                return null;
            }
        }
    }

}
