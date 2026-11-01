package vn.edu.haui.it6095.simplex.model;

import java.util.Arrays;
import java.util.List;

public record LinearProgram(Objective obj, Fraction[] c, List<Constraint> constraints) {
    public LinearProgram {
        c = Arrays.copyOf(c, c.length);
        constraints = List.copyOf(constraints);
    }

    public int numVars() {
        return c.length;
    }

    public int numConstraints() {
        return constraints.size();
    }
}