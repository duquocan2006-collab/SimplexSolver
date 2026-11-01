package vn.edu.haui.it6095.simplex.model;

public record BigM(Fraction a, Fraction b) implements Comparable<BigM> {
    public static final BigM ZERO = new BigM(Fraction.ZERO, Fraction.ZERO);
    public static final BigM M_POS = new BigM(Fraction.ZERO, Fraction.ONE);
    public static final BigM M_NEG = new BigM(Fraction.ZERO, Fraction.MINUS_ONE);

    public static BigM of(Fraction a) {
        return new BigM(a, Fraction.ZERO);
    }

    public static BigM of(long a) {
        return new BigM(Fraction.of(a), Fraction.ZERO);
    }

    public BigM add(BigM other) {
        return new BigM(this.a.add(other.a), this.b.add(other.b));
    }

    public BigM subtract(BigM other) {
        return new BigM(this.a.subtract(other.a), this.b.subtract(other.b));
    }

    public BigM multiply(Fraction scalar) {
        return new BigM(this.a.multiply(scalar), this.b.multiply(scalar));
    }

    public BigM negate() {
        return new BigM(this.a.negate(), this.b.negate());
    }

    public int signum() {
        int bSign = b.signum();
        return (bSign != 0) ? bSign : a.signum();
    }

    public boolean isZero() {
        return a.isZero() && b.isZero();
    }

    public boolean isNegative() {
        return signum() < 0;
    }

    public boolean isPositive() {
        return signum() > 0;
    }

    @Override
    public int compareTo(BigM other) {
        int cmpB = this.b.compareTo(other.b);
        if (cmpB != 0) {
            return cmpB;
        }
        return this.a.compareTo(other.a);
    }

    @Override
    public String toString() {
        if (b.isZero()) {
            return a.toString();
        }
        String mPart;
        if (b.isOne()) {
            mPart = "M";
        } else if (b.isMinusOne()) {
            mPart = "-M";
        } else {
            mPart = b + "M";
        }

        if (a.isZero()) {
            return mPart;
        }
        if (b.isPositive()) {
            return a + " + " + mPart;
        } else {
            String absM = b.abs().isOne() ? "M" : b.abs() + "M";
            return a + " - " + absM;
        }
    }
}