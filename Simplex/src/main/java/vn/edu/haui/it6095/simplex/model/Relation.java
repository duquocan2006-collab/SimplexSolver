package vn.edu.haui.it6095.simplex.model;

public enum Relation {
    LE("<="),
    EQ("="),
    GE(">=");

    private final String symbol;

    Relation(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() {
        return symbol;
    }

    public Relation flip() {
        return switch (this) {
            case LE -> GE;
            case GE -> LE;
            case EQ -> EQ;
        };
    }

    public static Relation fromSymbol(String s) {
        return switch (s.trim()) {
            case "<=", "≤" -> LE;
            case ">=", "≥" -> GE;
            case "=" -> EQ;
            default -> throw new IllegalArgumentException("Dấu quan hệ không hợp lệ: " + s);
        };
    }
}