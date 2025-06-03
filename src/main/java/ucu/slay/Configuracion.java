package ucu.slay;

// El programa debe recibir por pantalla los datos necesarios para la simulación.
public class Configuracion {
	// Número inicial de pacientes a la hora 8:00.
	public int cantInicialPacientes;

	// Cantidad de pacientes que llegan por hora y motivo por el que vienen.
	// Cantidad de tiempo que demora en atender al paciente por especialidad.
	// Nota(guz): "motivo por el que vienen" me lo paso por el orto, y en la
	// simulación sería en realidad aleatoria, con cierto peso para cada una.
	public int pacientesPorHora;
	public InfoConsulta[] tiposConsulta;

	// Indicador si existe sala reservada por emergencia.
	public boolean existeSalaReservada;

	public Configuracion(int cantInicialPacientes, int pacientesPorHora, InfoConsulta[] tiposConsulta,
			boolean existeSalaReservada) {
		this.cantInicialPacientes = cantInicialPacientes;
		this.pacientesPorHora = pacientesPorHora;
		this.tiposConsulta = tiposConsulta;
		this.existeSalaReservada = existeSalaReservada;
	}
}
