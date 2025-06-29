package ucu.slay;

public class Consulta {
    public final String nombre;
    public final TipoConsulta tipo;
    public final int tiempoEstimado;

    public Consulta(String nombre, TipoConsulta tipo, int tiempoEstimado) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.tiempoEstimado = tiempoEstimado;
    }
}
