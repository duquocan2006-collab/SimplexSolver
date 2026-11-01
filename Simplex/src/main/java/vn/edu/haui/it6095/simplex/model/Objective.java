package vn.edu.haui.it6095.simplex.model;

public enum Objective {
    MAX("max"),
    MIN("min");

    private final String label;

    Objective(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}