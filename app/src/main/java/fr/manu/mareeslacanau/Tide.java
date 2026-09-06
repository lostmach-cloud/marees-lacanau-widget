package fr.manu.mareeslacanau;

public class Tide {
    public final String type;
    public final String time;
    public final String height;
    public final String coefficient;

    public Tide(String type, String time, String height, String coefficient) {
        this.type = type;
        this.time = time;
        this.height = height;
        this.coefficient = coefficient;
    }
}
