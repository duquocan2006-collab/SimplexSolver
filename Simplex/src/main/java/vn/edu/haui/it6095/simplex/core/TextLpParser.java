package vn.edu.haui.it6095.simplex.core;

import vn.edu.haui.it6095.simplex.model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bộ phân tích cú pháp (Parser) cho ô nhập nhanh dạng văn bản.
 * Hỗ trợ 2 định dạng:
 *
 * Định dạng mới (ưu tiên):
 *   f(x) = -2x1 + 5x2 -4x3 - x4 +5x5 -> min
 *   x1 + 6x2 -2x4 -9x5 = 32
 *   2x2 + x3 + x4 + 3x5 = 30
 *   3x2 + x5 + x6 = 36
 *
 * Định dạng cũ (vẫn tương thích):
 *   max 3x1 - x2 + 2x3
 *   -x1 + 3x2 + x3 <= 7
 *   3x1 - 4x2 + 8x3 <= 10
 */
public final class TextLpParser {

    // Định dạng mới: f(x) = ... -> max/min/số
    private static final Pattern NEW_OBJ_PATTERN = Pattern.compile(
            "^\\s*f\\s*(?:\\(\\s*x\\s*\\))?\\s*=\\s*(.+?)\\s*(?:->|→)\\s*(max|min|.+?)\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    // Định dạng cũ: max/min ...
    private static final Pattern OBJ_PATTERN = Pattern.compile(
            "^\\s*(max|min)\\s+(?:f\\s*(?:\\(\\s*x\\s*\\))?\\s*=\\s*)?(.+)$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern CON_PATTERN = Pattern.compile(
            "^(.+?)(<=|>=|=|≤|≥)(.+)$"
    );

    private static final Pattern TERM_PATTERN = Pattern.compile(
            "([+-]?\\s*(?:\\d+(?:/\\d+|\\.\\d+)?\\s*)?)\\*?\\s*x_?(\\d+)",
            Pattern.CASE_INSENSITIVE
    );

    private TextLpParser() {}

    public static LinearProgram parse(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Nội dung văn bản nhập nhanh đang trống.");
        }

        String[] rawLines = text.split("\\R");
        List<String> lines = new ArrayList<>();
        for (String line : rawLines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("#") && !trimmed.startsWith("//")) {
                // Bỏ qua dòng điều kiện dấu mặc định x >= 0 nếu người dùng gõ vào
                if (trimmed.matches("(?i)^x\\d*\\s*(>=|≥)\\s*0.*$")) {
                    continue;
                }
                lines.add(trimmed);
            }
        }

        if (lines.size() < 2) {
            throw new IllegalArgumentException(
                    "Cần ít nhất 1 dòng hàm mục tiêu và 1 dòng ràng buộc."
            );
        }

        // Tìm chỉ số biến lớn nhất x_n xuất hiện trong toàn bộ văn bản
        int maxVarIndex = 0;
        for (String line : lines) {
            Matcher m = TERM_PATTERN.matcher(line);
            while (m.find()) {
                int idx = Integer.parseInt(m.group(2));
                if (idx > maxVarIndex) {
                    maxVarIndex = idx;
                }
            }
        }
        if (maxVarIndex <= 0 || maxVarIndex > 10) {
            throw new IllegalArgumentException("Số biến phát hiện được phải nằm trong khoảng từ 1 đến 10.");
        }

        // Phân tích dòng đầu tiên: Hàm mục tiêu
        Objective obj;
        Fraction[] c;

        // Thử định dạng mới trước: f(x) = ... -> max/min/số
        Matcher newObjMatcher = NEW_OBJ_PATTERN.matcher(lines.getFirst());
        if (newObjMatcher.matches()) {
            String expr = newObjMatcher.group(1);
            String target = newObjMatcher.group(2).trim();
            if (target.equalsIgnoreCase("max")) {
                obj = Objective.MAX;
            } else if (target.equalsIgnoreCase("min")) {
                obj = Objective.MIN;
            } else {
                // Giá trị số bất kỳ -> mặc định xử lý như max (người dùng nhập giá trị tùy chỉnh)
                obj = Objective.MAX;
            }
            c = parseLinearExpression(expr, maxVarIndex);
        } else {
            // Thử định dạng cũ: max/min ...
            Matcher objMatcher = OBJ_PATTERN.matcher(lines.getFirst());
            if (!objMatcher.matches()) {
                throw new IllegalArgumentException(
                        "Dòng đầu tiên phải là hàm mục tiêu, ví dụ: 'f(x) = -2x1 + 5x2 -4x3 - x4 + 5x5 -> min'."
                );
            }
            obj = objMatcher.group(1).equalsIgnoreCase("max") ? Objective.MAX : Objective.MIN;
            c = parseLinearExpression(objMatcher.group(2), maxVarIndex);
        }

        // Phân tích các dòng tiếp theo: Hệ ràng buộc
        List<Constraint> constraints = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            Matcher conMatcher = CON_PATTERN.matcher(line);
            if (!conMatcher.matches()) {
                throw new IllegalArgumentException("Ràng buộc dòng " + (i + 1) + " không hợp lệ: " + line);
            }
            String lhs = conMatcher.group(1).trim();
            Relation rel = Relation.fromSymbol(conMatcher.group(2));
            Fraction rhs = Fraction.parse(conMatcher.group(3).trim());
            Fraction[] a = parseLinearExpression(lhs, maxVarIndex);
            constraints.add(new Constraint(a, rel, rhs));
        }

        if (constraints.size() > 10) {
            throw new IllegalArgumentException("Số lượng ràng buộc tối đa là 10.");
        }

        return new LinearProgram(obj, c, constraints);
    }

    private static Fraction[] parseLinearExpression(String expr, int numVars) {
        Fraction[] coeffs = new Fraction[numVars];
        Arrays.fill(coeffs, Fraction.ZERO);

        Matcher m = TERM_PATTERN.matcher(expr);
        boolean foundAny = false;
        while (m.find()) {
            foundAny = true;
            String rawCoeff = m.group(1).replace(" ", "");
            int varIdx = Integer.parseInt(m.group(2)) - 1;
            if (varIdx < 0 || varIdx >= numVars) {
                throw new IllegalArgumentException("Chỉ số biến không hợp lệ: x" + (varIdx + 1));
            }
            Fraction coeff;
            if (rawCoeff.isEmpty() || rawCoeff.equals("+")) {
                coeff = Fraction.ONE;
            } else if (rawCoeff.equals("-")) {
                coeff = Fraction.MINUS_ONE;
            } else {
                coeff = Fraction.parse(rawCoeff);
            }
            coeffs[varIdx] = coeffs[varIdx].add(coeff);
        }

        if (!foundAny) {
            throw new IllegalArgumentException("Không tìm thấy biến xj nào trong biểu thức: " + expr);
        }
        return coeffs;
    }
}
