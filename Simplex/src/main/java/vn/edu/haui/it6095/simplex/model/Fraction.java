package vn.edu.haui.it6095.simplex.model;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Objects;

public final class Fraction implements Comparable<Fraction> {
    public static final Fraction ZERO = new Fraction(BigInteger.ZERO, BigInteger.ONE);
    public static final Fraction ONE = new Fraction(BigInteger.ONE, BigInteger.ONE);
    public static final Fraction MINUS_ONE = new Fraction(BigInteger.ONE.negate(), BigInteger.ONE);

    private final BigInteger num;
    private final BigInteger den;

    public Fraction(BigInteger num, BigInteger den) {
        Objects.requireNonNull(num, "Tử số không được null");
        Objects.requireNonNull(den, "Mẫu số không được null");
        if (den.equals(BigInteger.ZERO)) {
            throw new ArithmeticException("Mẫu số không được bằng 0.");
        }
        if (den.signum() < 0) {
            num = num.negate();
            den = den.negate();
        }
        BigInteger gcd = num.gcd(den);
        this.num = num.divide(gcd);
        this.den = den.divide(gcd);
    }

    public static Fraction of(long n) {
        return new Fraction(BigInteger.valueOf(n), BigInteger.ONE);
    }

    public static Fraction of(long n, long d) {
        return new Fraction(BigInteger.valueOf(n), BigInteger.valueOf(d));
    }

    public static Fraction parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Ô nhập trống.");
        }
        String s = raw.trim().replace(" ", "");
        if (s.contains("/")) {
            String[] parts = s.split("/");
            if (parts.length != 2 || parts[0].isEmpty() || parts[1].isEmpty()) {
                throw new IllegalArgumentException("Định dạng phân số không hợp lệ: " + raw);
            }
            return new Fraction(new BigInteger(parts[0]), new BigInteger(parts[1]));
        }
        if (s.contains(".") || s.contains(",")) {
            s = s.replace(",", ".");
            BigDecimal bd = new BigDecimal(s);
            int scale = bd.scale();
            if (scale <= 0) {
                return new Fraction(bd.toBigIntegerExact(), BigInteger.ONE);
            }
            BigInteger unscaled = bd.unscaledValue();
            BigInteger tenPow = BigInteger.TEN.pow(scale);
            return new Fraction(unscaled, tenPow);
        }
        return new Fraction(new BigInteger(s), BigInteger.ONE);
    }

    public BigInteger num() { return num; }
    public BigInteger den() { return den; }

    public Fraction add(Fraction other) {
        return new Fraction(
                this.num.multiply(other.den).add(other.num.multiply(this.den)),
                this.den.multiply(other.den)
        );
    }

    public Fraction subtract(Fraction other) {
        return new Fraction(
                this.num.multiply(other.den).subtract(other.num.multiply(this.den)),
                this.den.multiply(other.den)
        );
    }

    public Fraction multiply(Fraction other) {
        return new Fraction(this.num.multiply(other.num), this.den.multiply(other.den));
    }

    public Fraction divide(Fraction other) {
        if (other.isZero()) {
            throw new ArithmeticException("Chia cho phân số 0.");
        }
        return new Fraction(this.num.multiply(other.den), this.den.multiply(other.num));
    }

    public Fraction negate() {
        return new Fraction(this.num.negate(), this.den);
    }

    public Fraction abs() {
        return new Fraction(this.num.abs(), this.den);
    }

    public int signum() {
        return this.num.signum();
    }

    public boolean isZero() { return this.num.signum() == 0; }
    public boolean isPositive() { return this.num.signum() > 0; }
    public boolean isNegative() { return this.num.signum() < 0; }
    public boolean isOne() { return this.num.equals(BigInteger.ONE) && this.den.equals(BigInteger.ONE); }
    public boolean isMinusOne() { return this.num.equals(BigInteger.ONE.negate()) && this.den.equals(BigInteger.ONE); }

    public double toDouble() {
        return new BigDecimal(num).divide(new BigDecimal(den), 6, RoundingMode.HALF_UP).doubleValue();
    }

    @Override
    public int compareTo(Fraction other) {
        return this.num.multiply(other.den).compareTo(other.num.multiply(this.den));
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof Fraction f) && this.num.equals(f.num) && this.den.equals(f.den);
    }

    @Override
    public int hashCode() {
        return Objects.hash(num, den);
    }

    @Override
    public String toString() {
        if (den.equals(BigInteger.ONE)) {
            return num.toString();
        }
        return num + "/" + den;
    }
}