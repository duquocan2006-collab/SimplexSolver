package vn.edu.haui.it6095.simplex.render;

import vn.edu.haui.it6095.simplex.model.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Sinh tài liệu HTML5 + CSS trình bày chi tiết từng bước giải bài toán Quy hoạch tuyến tính
 * theo đúng ký hiệu và bố cục bảng của giáo trình "Tối ưu hóa - IT6095, Bài 2, mục 2.4 - 2.5".
 */
public final class HtmlSolutionRenderer {

    public String render(SolutionOutput output) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                <meta charset="UTF-8">
                <title>Lời giải chi tiết - Phương pháp đơn hình</title>
                <style>
                    body {
                        font-family: "Segoe UI", "Times New Roman", sans-serif;
                        font-size: 14.5px;
                        line-height: 1.6;
                        color: #1e293b;
                        background-color: #ffffff;
                        margin: 0;
                        padding: 18px 24px;
                    }
                    h2 {
                        color: #0f172a;
                        margin-top: 22px;
                        margin-bottom: 10px;
                        font-size: 17px;
                    }
                    h3 {
                        color: #1d4ed8;
                        margin-top: 16px;
                        margin-bottom: 8px;
                        font-size: 15.5px;
                    }
                    .math-box {
                        background: #f8fafc;
                        border-left: 4px solid #3b82f6;
                        padding: 10px 16px;
                        margin: 10px 0;
                        font-family: "Cambria Math", "Times New Roman", serif;
                        font-size: 16px;
                    }
                    table.simplex-table {
                        border-collapse: collapse;
                        margin: 12px 0;
                        font-family: "Cambria Math", "Times New Roman", serif;
                        font-size: 15.5px;
                        min-width: 580px;
                    }
                    table.simplex-table th, table.simplex-table td {
                        border: 1px solid #334155;
                        padding: 6px 12px;
                        text-align: center;
                    }
                    table.simplex-table th {
                        background-color: #f1f5f9;
                        font-weight: bold;
                    }
                    td.pivot-col, th.pivot-col {
                        background-color: #e2e8f0 !important;
                    }
                    td.pivot-cell {
                        background-color: #fdba74 !important;
                        font-weight: bold;
                        color: #7c2d12;
                        border: 2px solid #ea580c !important;
                    }
                    td.slashed-cell {
                        background: linear-gradient(
                            to top left,
                            rgba(241,245,249,0.7) 0%,
                            rgba(241,245,249,0.7) calc(50% - 1.2px),
                            #64748b 50%,
                            rgba(241,245,249,0.7) calc(50% + 1.2px),
                            rgba(241,245,249,0.7) 100%
                        );
                        min-width: 38px;
                    }
                    tr.delta-row td {
                        background-color: #f8fafc;
                        font-weight: bold;
                    }
                    .commentary {
                        background-color: #fffbeb;
                        border: 1px solid #fde68a;
                        border-radius: 6px;
                        padding: 10px 16px;
                        margin-bottom: 18px;
                    }
                    ul {
                        list-style: none;
                        margin: 4px 0;
                        padding-left: 4px;
                    }
                    li {
                        position: relative;
                        padding-left: 16px;
                    }
                    li::before {
                        content: "-";
                        position: absolute;
                        left: 0;
                    }
                    .conclusion-box {
                        background-color: #ecfdf5;
                        border: 2px solid #10b981;
                        border-radius: 8px;
                        padding: 14px 18px;
                        margin-top: 16px;
                    }
                    .conclusion-box.error {
                        background-color: #fef2f2;
                        border-color: #ef4444;
                    }
                    .note-warn {
                        color: #b45309;
                        font-weight: 600;
                    }
                </style>
                </head>
                <body>
                """);

        for (Step step : output.steps()) {
            switch (step) {
                case Step.StandardizeStep s -> renderPartAAndB(sb, s);
                case Step.InitialBasisStep s -> renderPartC(sb, s);
                case Step.IterationStep s -> renderPartD(sb, s);
                case Step.ConclusionStep s -> renderPartE(sb, s);
            }
        }

        sb.append("</body></html>");
        return sb.toString();
    }

    private void renderPartAAndB(StringBuilder sb, Step.StandardizeStep s) {
        LinearProgram lp = s.originalLp();

        // PHẦN A: ĐỀ BÀI
        sb.append("<h2>Đề bài</h2>");
        sb.append("<div class='math-box'>");
        sb.append("<div><b>Hàm mục tiêu:</b> f(x) = ")
                .append(formatPolynomial(lp.c()))
                .append(" &rarr; <b>").append(lp.obj().label()).append("</b></div>");
        sb.append("<div style='margin-top:6px;'><b>Hệ ràng buộc:</b></div>");
        for (int i = 0; i < lp.numConstraints(); i++) {
            Constraint c = lp.constraints().get(i);
            sb.append("<div style='margin-left:18px;'>(").append(i + 1).append(") ")
                    .append(formatPolynomial(c.a())).append(" ")
                    .append(htmlRelation(c.rel())).append(" ")
                    .append(c.b()).append("</div>");
        }
        sb.append("<div style='margin-top:6px;'><b>Điều kiện dấu:</b> x<sub>j</sub> &ge; 0 (j = 1, 2, ..., ")
                .append(lp.numVars()).append(")</div>");
        sb.append("</div>");

        // PHẦN B: ĐƯA VỀ DẠNG CHÍNH TẮC
        sb.append("<h2>Đưa bài toán về dạng chính tắc</h2>");
        sb.append("<ul>");
        for (String log : s.transformationLogs()) {
            sb.append("<li>").append(log).append("</li>");
        }
        sb.append("</ul>");

        String problemTitle = s.usesBigM()
                ? "Bài toán mở rộng (bài toán M) ở dạng chính tắc:"
                : "Bài toán ở dạng chính tắc tương đương:";
        sb.append("<div class='math-box'>");
        sb.append("<div><b>").append(problemTitle).append("</b></div>");
        sb.append("<div>f(x) = ").append(formatBigMPolynomial(s.standardC()))
                .append(" &rarr; <b>").append(lp.obj().label()).append("</b></div>");
        for (int i = 0; i < s.standardA().length; i++) {
            sb.append("<div style='margin-left:18px;'>")
                    .append(formatPolynomial(s.standardA()[i]))
                    .append(" = ").append(s.standardB()[i]).append("</div>");
        }
        sb.append("<div style='margin-top:6px;'>x<sub>j</sub> &ge; 0 (j = 1, 2, ..., ")
                .append(s.standardC().length).append(")");
        if (s.usesBigM()) {
            String artList = java.util.Arrays.stream(s.artificialVarIndices())
                    .mapToObj(idx -> "x<sub>" + (idx + 1) + "</sub>")
                    .collect(Collectors.joining(", "));
            sb.append(" &mdash; <i>trong đó ").append(artList)
                    .append(" là biến giả, M là số dương rất lớn.</i>");
        }
        sb.append("</div></div>");
    }

    private void renderPartC(StringBuilder sb, Step.InitialBasisStep s) {
        sb.append("<h2>Tìm phương án cực biên xuất phát và cơ sở</h2>");
        String jSet = java.util.Arrays.stream(s.basisIndices())
                .mapToObj(idx -> "x<sub>" + (idx + 1) + "</sub>")
                .collect(Collectors.joining(", "));
        String xVec = java.util.Arrays.stream(s.fullInitialX())
                .map(Fraction::toString)
                .collect(Collectors.joining(", "));
        String xNames = java.util.stream.IntStream.rangeClosed(1, s.fullInitialX().length)
                .mapToObj(k -> "x<sub>" + k + "</sub>")
                .collect(Collectors.joining(", "));

        sb.append("<ul>");
        sb.append("<li><b>Hệ vectơ cơ sở đơn vị xuất phát:</b> B = {").append(jSet).append("}.</li>");
        sb.append("<li><b>Phương án cực biên xuất phát:</b> x<sub>ij</sub> = (").append(xNames)
                .append(") = (").append(xVec).append(").</li>");
        sb.append("</ul>");
        sb.append("<h2>Lặp trên bảng đơn hình</h2>");
    }

    private void renderPartD(StringBuilder sb, Step.IterationStep step) {
        SimplexTable t = step.table();
        int m = t.basis().length;
        int totalVars = t.cAll().length;
        int sCol = step.enteringCol();
        int rRow = step.leavingRow();

        sb.append("<h3>Bảng ").append(step.tableNumber()).append("</h3>");
        sb.append("<table class='simplex-table'>");

        // Hàng tiêu đề 1: cj | Cơ sở | Phương án | c1 ... cn+m
        sb.append("<tr>");
        sb.append("<th rowspan='2'>c<sub>j</sub></th>");
        sb.append("<th rowspan='2'>Cơ sở</th>");
        sb.append("<th rowspan='2'>Phương án</th>");
        for (int k = 0; k < totalVars; k++) {
            String cls = (k == sCol) ? " class='pivot-col'" : "";
            sb.append("<th").append(cls).append(">").append(formatBigMHeader(t.cAll()[k])).append("</th>");
        }
        sb.append("</tr>");

        // Hàng tiêu đề 2: A1 ... An+m
        sb.append("<tr>");
        for (int k = 0; k < totalVars; k++) {
            String cls = (k == sCol) ? " class='pivot-col'" : "";
            sb.append("<th").append(cls).append(">x<sub>").append(k + 1).append("</sub></th>");
        }
        sb.append("</tr>");

        // m hàng cơ sở
        for (int i = 0; i < m; i++) {
            sb.append("<tr>");
            sb.append("<td>").append(formatBigMHeader(t.cBasis()[i])).append("</td>");
            sb.append("<td><b>x<sub>").append(t.basis()[i] + 1).append("</sub></b></td>");
            sb.append("<td><b>").append(t.x()[i]).append("</b></td>");

            for (int k = 0; k < totalVars; k++) {
                if (t.hiddenColumns()[k]) {
                    sb.append("<td class='slashed-cell'></td>");
                } else if (i == rRow && k == sCol) {
                    sb.append("<td class='pivot-cell'>(").append(t.z()[i][k]).append(")</td>");
                } else if (k == sCol) {
                    sb.append("<td class='pivot-col'>").append(t.z()[i][k]).append("</td>");
                } else {
                    sb.append("<td>").append(t.z()[i][k]).append("</td>");
                }
            }
            sb.append("</tr>");
        }

        // Hàng cuối: Với bài toán thông thường hiển thị 1 dòng Δk; với bài toán M hiển thị 2 dòng αk và ηk
        if (!t.bigMMode()) {
            sb.append("<tr class='delta-row'>");
            sb.append("<td></td>");
            sb.append("<td>&Delta;<sub>ij</sub></td>");
            sb.append("<td>f = ").append(t.f().a()).append("</td>");
            for (int k = 0; k < totalVars; k++) {
                String cls = (k == sCol) ? " class='pivot-col'" : "";
                sb.append("<td").append(cls).append(">").append(t.delta()[k].a()).append("</td>");
            }
            sb.append("</tr>");
        } else {
            // Dòng αk (phần tự do không chứa M)
            sb.append("<tr class='delta-row'>");
            sb.append("<td></td>");
            sb.append("<td>&alpha;<sub>k</sub></td>");
            sb.append("<td>").append(t.f().a()).append("</td>");
            for (int k = 0; k < totalVars; k++) {
                if (t.hiddenColumns()[k]) {
                    sb.append("<td class='slashed-cell'></td>");
                } else {
                    String cls = (k == sCol) ? " class='pivot-col'" : "";
                    sb.append("<td").append(cls).append(">").append(t.delta()[k].a()).append("</td>");
                }
            }
            sb.append("</tr>");

            // Dòng ηk (hệ số của M trong Δk = αk + ηk*M)
            sb.append("<tr class='delta-row'>");
            sb.append("<td></td>");
            sb.append("<td>&eta;<sub>k</sub> (M)</td>");
            sb.append("<td>").append(t.f().b()).append("</td>");
            for (int k = 0; k < totalVars; k++) {
                if (t.hiddenColumns()[k]) {
                    sb.append("<td class='slashed-cell'></td>");
                } else {
                    String cls = (k == sCol) ? " class='pivot-col'" : "";
                    sb.append("<td").append(cls).append(">").append(t.delta()[k].b()).append("</td>");
                }
            }
            sb.append("</tr>");
        }
        sb.append("</table>");

        // Nhận xét dưới bảng
        sb.append("<div class='commentary'><b>Nhận xét Bảng ").append(step.tableNumber()).append(":</b><ul>");
        if (step.degenerate()) {
            sb.append("<li class='note-warn'>Phương án cực biên hiện tại bị <b>suy biến (thoái hóa)</b> vì có biến cơ sở nhận giá trị bằng 0 (|J*| &lt; m).</li>");
        }

        if (step.isOptimal()) {
            if (step.objective() == Objective.MAX) {
                sb.append("<li><b>Kiểm tra điều kiện tối ưu:</b> Ta thấy mọi ước lượng &Delta;<sub>ij</sub> &ge; 0 (với mọi cột k đang xét). Thuật toán dừng!</li>");
            } else {
                sb.append("<li><b>Kiểm tra điều kiện tối ưu:</b> Ta thấy mọi ước lượng &Delta;<sub>ij</sub> &le; 0 (với mọi cột k đang xét). Thuật toán dừng!</li>");
            }
        } else if (step.isUnbounded()) {
            int col = step.unboundedCol() + 1;
            String sign = (step.objective() == Objective.MAX) ? "&lt; 0" : "&gt; 0";
            sb.append("<li><b>Kiểm tra không bị chặn:</b> Trên cột x<sub>").append(col)
                    .append("</sub> có &Delta;<sub>").append(col).append("</sub> = ")
                    .append(t.delta()[step.unboundedCol()]).append(" ").append(sign)
                    .append(" nhưng mọi hệ số z<sub>j,").append(col).append("</sub> &le; 0. ")
                    .append("Do đó hàm mục tiêu không bị chặn, thuật toán dừng!</li>");
        } else {
            // Liệt kê các cột vi phạm điều kiện tối ưu
            StringBuilder violatingList = new StringBuilder();
            for (int k = 0; k < totalVars; k++) {
                if (t.hiddenColumns()[k]) continue;
                boolean v = (step.objective() == Objective.MAX && t.delta()[k].isNegative())
                        || (step.objective() == Objective.MIN && t.delta()[k].isPositive());
                if (v) {
                    if (!violatingList.isEmpty()) violatingList.append("; ");
                    violatingList.append("&Delta;<sub>").append(k + 1).append("</sub> = ").append(t.delta()[k]);
                }
            }
            String cmpSym = (step.objective() == Objective.MAX) ? "&lt; 0" : "&gt; 0";
            String extrName = (step.objective() == Objective.MAX) ? "min" : "max";

            sb.append("<li><b>Kiểm tra điều kiện tối ưu:</b> Chưa tối ưu vì còn các ước lượng ")
                    .append(cmpSym).append(": ").append(violatingList).append(".</li>");
            sb.append("<li><b>Chọn cột xoay (vectơ đưa vào cơ sở):</b> &Delta;<sub>").append(sCol + 1)
                    .append("</sub> = ").append(extrName).append("{&Delta;<sub>ij</sub> | &Delta;<sub>ij</sub> ")
                    .append(cmpSym).append("} = ").append(t.delta()[sCol])
                    .append(" &rArr; đưa vectơ <b>x<sub>").append(sCol + 1).append("</sub></b> vào cơ sở.</li>");

            // Tính tỉ số θ
            StringBuilder ratioStr = new StringBuilder();
            for (int i = 0; i < m; i++) {
                if (step.ratios()[i] != null) {
                    if (!ratioStr.isEmpty()) ratioStr.append(", ");
                    ratioStr.append(t.x()[i]).append("/").append(t.z()[i][sCol])
                            .append(" = ").append(step.ratios()[i]);
                }
            }
            int leavingVar = t.basis()[rRow];
            sb.append("<li><b>Chọn dòng xoay (vectơ đưa ra khỏi cơ sở):</b> &theta; = min{x<sub>j</sub> / z<sub>j,")
                    .append(sCol + 1).append("</sub> | z<sub>j,").append(sCol + 1).append("</sub> &gt; 0} = min{")
                    .append(ratioStr).append("} = <b>").append(step.minTheta()).append("</b> (đạt tại dòng cơ sở x<sub>")
                    .append(leavingVar + 1).append("</sub>) &rArr; đưa vectơ <b>x<sub>")
                    .append(leavingVar + 1).append("</sub></b> ra khỏi cơ sở.</li>");

            sb.append("<li><b>Phần tử trục:</b> z<sub>").append(leavingVar + 1).append(",").append(sCol + 1)
                    .append("</sub> = <b>(").append(step.pivotElement()).append(")</b>.</li>");

            sb.append("<li><b>Phép biến đổi sơ cấp sang Bảng ").append(step.tableNumber() + 1).append(":</b><ul>");
            for (String op : step.rowOperations()) {
                sb.append("<li>").append(op).append("</li>");
            }
            sb.append("</ul></li>");

            String newBasisStr = java.util.Arrays.stream(step.nextBasis())
                    .mapToObj(idx -> "x<sub>" + (idx + 1) + "</sub>")
                    .collect(Collectors.joining(", "));
            sb.append("<li><b>Cơ sở mới:</b> B' = {").append(newBasisStr).append("}.</li>");
        }
        sb.append("</ul></div>");
    }

    private void renderPartE(StringBuilder sb, Step.ConclusionStep s) {
        sb.append("<h2>Kết luận</h2>");
        switch (s.result()) {
            case Result.Optimal opt -> {
                sb.append("<div class='conclusion-box'>");
                String condType = opt.degenerate()
                        ? "điều kiện đủ (do phương án cực biên suy biến, xem Chú ý 1 mục 2.5)"
                        : "điều kiện cần và đủ (do phương án cực biên không suy biến)";
                String sign = (s.objective() == Objective.MAX) ? "&ge; 0" : "&le; 0";

                sb.append("<p><b>1. Lý do dừng thuật toán:</b> Tại bảng cuối, mọi ước lượng &Delta;<sub>ij</sub> ")
                        .append(sign).append(" thỏa mãn ").append(condType).append(" của phương án tối ưu");
                if (s.usesBigM()) {
                    sb.append(", đồng thời mọi biến giả trong phương án tối ưu đều bằng 0");
                }
                sb.append(".</p>");

                String xStar = java.util.Arrays.stream(opt.originalX())
                        .map(Fraction::toString)
                        .collect(Collectors.joining(", "));
                sb.append("<p><b>2. Phương án tối ưu của bài toán gốc:</b><br>");
                sb.append("<span style='font-size:17px;'><b>x<sub>ij</sub> = (").append(xStar).append(")</b></span> (với ");
                for (int j = 0; j < opt.originalX().length; j++) {
                    if (j > 0) sb.append(", ");
                    sb.append("x<sub>").append(j + 1).append("</sub> = ").append(opt.originalX()[j]);
                }
                sb.append(").</p>");

                Fraction fVal = opt.optimalValue();
                sb.append("<p><b>3. Giá trị tối ưu của hàm mục tiêu:</b><br>");
                sb.append("<span style='font-size:17px;'><b>f<sub>").append(s.objective().label())
                        .append("</sub> = ").append(fVal).append("</b></span>");
                if (!fVal.den().equals(java.math.BigInteger.ONE)) {
                    sb.append(String.format(" <i>(&approx; %.4f)</i>", fVal.toDouble()));
                }
                sb.append(".</p>");

                if (opt.degenerate()) {
                    sb.append("<p class='note-warn'>- <b>Ghi chú suy biến:</b> Phương án tối ưu x<sub>ij</sub> là phương án cực biên suy biến (thoái hóa) vì có biến cơ sở nhận giá trị bằng 0.</p>");
                }
                if (opt.multipleSolutions()) {
                    String vars = opt.zeroDeltaNonBasicVars().stream()
                            .map(idx -> "x<sub>" + (idx + 1) + "</sub> (&Delta;<sub>" + (idx + 1) + "</sub> = 0)")
                            .collect(Collectors.joining(", "));
                    sb.append("<p class='note-warn'>- <b>Ghi chú nghiệm:</b> Ở bảng tối ưu tồn tại biến phi cơ sở có ước lượng bằng 0 (")
                            .append(vars).append("), do đó <b>bài toán có thể có vô số phương án tối ưu</b>.</p>");
                }
                sb.append("</div>");
            }
            case Result.Unbounded unb -> {
                sb.append("<div class='conclusion-box error'>");
                String boundType = (unb.objective() == Objective.MAX) ? "chặn trên (f(x) &rarr; +&infin;)" : "chặn dưới (f(x) &rarr; -&infin;)";
                sb.append("<p><b>Kết luận:</b> Tại cột x<sub>").append(unb.problematicColumnIndex() + 1)
                        .append("</sub> vi phạm điều kiện tối ưu nhưng mọi hệ số khai triển z<sub>j,")
                        .append(unb.problematicColumnIndex() + 1).append("</sub> &le; 0.</p>");
                sb.append("<p>Do đó hàm mục tiêu f(x) <b>không bị ").append(boundType)
                        .append("</b> trên miền ràng buộc. <b>Bài toán không có phương án tối ưu!</b></p>");
                sb.append("</div>");
            }
            case Result.Infeasible inf -> {
                sb.append("<div class='conclusion-box error'>");
                sb.append("<p><b>Kết luận:</b> ").append(inf.reason()).append("</p>");
                sb.append("<p>Do đó miền ràng buộc của bài toán gốc là tập rỗng (D = &empty;). <b>Bài toán không có phương án chấp nhận được (vô nghiệm) và không có phương án tối ưu!</b></p>");
                sb.append("</div>");
            }
        }
    }

    private static String formatPolynomial(Fraction[] coeffs) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (int j = 0; j < coeffs.length; j++) {
            Fraction c = coeffs[j];
            if (c.isZero()) continue;
            if (first) {
                if (c.isMinusOne()) sb.append("-");
                else if (!c.isOne()) sb.append(c);
                sb.append("x<sub>").append(j + 1).append("</sub>");
                first = false;
            } else {
                if (c.isPositive()) {
                    sb.append(" + ");
                    if (!c.isOne()) sb.append(c);
                } else {
                    sb.append(" - ");
                    if (!c.abs().isOne()) sb.append(c.abs());
                }
                sb.append("x<sub>").append(j + 1).append("</sub>");
            }
        }
        return first ? "0" : sb.toString();
    }

    private static String formatBigMPolynomial(BigM[] coeffs) {
        StringBuilder sb = new StringBuilder();
        for (int j = 0; j < coeffs.length; j++) {
            BigM c = coeffs[j];
            if (j == 0) {
                sb.append(formatSingleBigMTerm(c, j + 1, true));
            } else {
                sb.append(formatSingleBigMTerm(c, j + 1, false));
            }
        }
        return sb.toString();
    }

    private static String formatSingleBigMTerm(BigM c, int idx, boolean first) {
        if (!c.b().isZero()) {
            String sign = c.b().isPositive() ? (first ? "" : " + ") : (first ? "-" : " - ");
            return sign + "M&middot;x<sub>" + idx + "</sub>";
        }
        Fraction a = c.a();
        if (a.isZero()) {
            return (first ? "" : " + ") + "0&middot;x<sub>" + idx + "</sub>";
        }
        if (first) {
            if (a.isOne()) return "x<sub>" + idx + "</sub>";
            if (a.isMinusOne()) return "-x<sub>" + idx + "</sub>";
            return a + "x<sub>" + idx + "</sub>";
        } else {
            if (a.isPositive()) {
                return " + " + (a.isOne() ? "" : a) + "x<sub>" + idx + "</sub>";
            } else {
                return " - " + (a.abs().isOne() ? "" : a.abs()) + "x<sub>" + idx + "</sub>";
            }
        }
    }

    private static String formatBigMHeader(BigM c) {
        if (c.b().isZero()) {
            return c.a().toString();
        }
        return c.b().isPositive() ? "M" : "-M";
    }

    private static String htmlRelation(Relation r) {
        return switch (r) {
            case LE -> "&le;";
            case GE -> "&ge;";
            case EQ -> "=";
        };
    }
}