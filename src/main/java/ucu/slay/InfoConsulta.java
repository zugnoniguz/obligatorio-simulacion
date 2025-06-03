package ucu.slay;

public class InfoConsulta {
	// La duración estimada en minutos.
	public int duracion;

	// El nombre de consulta.
	public String tipo;

	// Si es emergencia
	public boolean esEmergencia;

	// Si es urgencia
	public boolean esUrgencia;

	public InfoConsulta(int duracion, String tipo, boolean esEmergencia, boolean esUrgencia) {
		this.duracion = duracion;
		this.tipo = tipo;
		this.esEmergencia = esEmergencia;
		this.esUrgencia = esUrgencia;
	}
}
