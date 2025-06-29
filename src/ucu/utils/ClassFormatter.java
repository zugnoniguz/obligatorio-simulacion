package ucu.utils;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class ClassFormatter extends Formatter {
    @Override
    public String format(LogRecord record) {
        String fqcn = record.getLoggerName();
        String message = formatMessage(record);
        if (message == null || message.isEmpty()) {
            return System.lineSeparator();
        }

        String name = fqcn.substring(fqcn.lastIndexOf('.') + 1);
        // String name = fqcn;
        return String.format("[%s] %s%s", name, message, System.lineSeparator());
    }
}
