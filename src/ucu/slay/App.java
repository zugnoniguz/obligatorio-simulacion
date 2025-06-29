package ucu.slay;

import ucu.utils.ClassFormatter;

import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.OperationNotSupportedException;

public class App {

    private static Configuracion readFromStdin() throws OperationNotSupportedException {
        throw new OperationNotSupportedException("No sé leer de la consola");
    }

    public static void main(String[] args) throws Exception {
        FileHandler fh = new FileHandler("simulacion.log", false);
        fh.setFormatter(new ClassFormatter());
        fh.setLevel(Level.INFO);
        FileHandler fhDebug = new FileHandler("simulacion_debug.log", false);
        fhDebug.setFormatter(new ClassFormatter());
        fhDebug.setLevel(Level.FINEST);
        ConsoleHandler ch = new ConsoleHandler();
        ch.setFormatter(new ClassFormatter());
        ch.setLevel(Level.FINEST);
        Logger l = Logger.getLogger("");
        l.addHandler(fh);
        l.addHandler(fhDebug);
        // l.addHandler(ch);
        l.setLevel(Level.FINEST);

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
