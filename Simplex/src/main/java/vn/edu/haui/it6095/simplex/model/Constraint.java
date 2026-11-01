package vn.edu.haui.it6095.simplex.model;

import java.util.Arrays;

public record Constraint(Fraction[] a, Relation rel, Fraction b) {
    public Constraint {
        a = Arrays.copyOf(a, a.length);
    }
}