package ucu.utils;

public class Hora {
    public final static int MAX_MIN = 60;

    public int hora;
    public int min;

    public Hora(int hora, int min) {
        this.hora = hora;
        this.min = min;
    }

    public void increment() {
        this.min += 1;
        if (this.min == MAX_MIN) {
            this.hora += 1;
            this.min = 0;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (this.getClass() != o.getClass()) {
            return false;
        }

        Hora horaObj = (Hora) o;
        return this.hora == horaObj.hora && this.min == horaObj.min;
    }

    @Override
    public int hashCode() {
        return Integer.valueOf(this.hora).hashCode() + Integer.valueOf(this.min).hashCode();
    }
}
