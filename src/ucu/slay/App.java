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

        PlanificadorConsultas planificador = new PlanificadorConsultas(config);
        planificador.correrSimulacion();
    }
}
