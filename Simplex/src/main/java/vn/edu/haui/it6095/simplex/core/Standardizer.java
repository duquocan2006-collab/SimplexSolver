package vn.edu.haui.it6095.simplex.core;

import vn.edu.haui.it6095.simplex.model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Đưa bài toán Quy hoạch tuyến tính tổng quát về dạng chính tắc (Mục 2.4 & 2.5):
 * Ax = b, x >= 0, b >= 0.
 * Tự động phát hiện các vectơ đơn vị có sẵn và chỉ thêm m - k biến giả khi cần thiết.
 */
public final class Standardizer {

    public record StandardizedModel(
            LinearProgram originalLp,
            int numOriginal,
            int numSlack,
            int numArtificial,
            int totalVars,
            Fraction[][] A,
            Fraction[] b,
            BigM[] c,
            int[] initialBasis,
            int[] artificialCols,
            boolean usesBigM,
            Step.StandardizeStep standardizeStep,
            Step.InitialBasisStep initialBasisStep
    ) {}

    public static StandardizedModel standardize(LinearProgram lp) {
        int n = lp.numVars();
        int m = lp.numConstraints();
        List<String> logs = new ArrayList<>();

        // Bước 1: Xử lý vế phải bi < 0
        Relation[] rels = new Relation[m];
        Fraction[][] aTemp = new Fraction[m][n];
        Fraction[] bTemp = new Fraction[m];

        for (int i = 0; i < m; i++) {
            Constraint con = lp.constraints().get(i);
            if (con.b().isNegative()) {
                for (int j = 0; j < n; j++) {
                    aTemp[i][j] = con.a()[j].negate();
                }
                bTemp[i] = con.b().negate();
                rels[i] = con.rel().flip();
                logs.add("Ràng buộc " + (i + 1) + " có vế phải b<sub>" + (i + 1) + "</sub> = " + con.b()
                        + " &lt; 0: nhân cả hai vế với (-1) và đổi chiều bất đẳng thức thành '"
                        + rels[i].symbol() + "'.");
            } else {
                aTemp[i] = Arrays.copyOf(con.a(), n);
                bTemp[i] = con.b();
                rels[i] = con.rel();
            }
        }

        // Bước 2: Đếm số biến phụ (slack/surplus) cho các ràng buộc <= và >=
        int numSlack = 0;
        int[] slackColOfRow = new int[m];
        Arrays.fill(slackColOfRow, -1);
        int[] slackSignOfRow = new int[m];

        for (int i = 0; i < m; i++) {
            if (rels[i] == Relation.LE) {
                slackColOfRow[i] = n + numSlack;
                slackSignOfRow[i] = 1;
                numSlack++;
                logs.add("Ràng buộc " + (i + 1) + " có dấu '&le;': cộng thêm biến phụ x<sub>"
                        + (slackColOfRow[i] + 1) + "</sub> &ge; 0 (hệ số 0 trong hàm mục tiêu).");
            } else if (rels[i] == Relation.GE) {
                slackColOfRow[i] = n + numSlack;
                slackSignOfRow[i] = -1;
                numSlack++;
                logs.add("Ràng buộc " + (i + 1) + " có dấu '&ge;': trừ đi biến phụ x<sub>"
                        + (slackColOfRow[i] + 1) + "</sub> &ge; 0 (hệ số 0 trong hàm mục tiêu).");
            } else {
                logs.add("Ràng buộc " + (i + 1) + " có dấu '=': giữ nguyên vế trái.");
            }
        }

        // Xây dựng ma trận sau khi thêm biến phụ (chưa có biến giả)
        int varsAfterSlack = n + numSlack;
        Fraction[][] aWithSlack = new Fraction[m][varsAfterSlack];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < varsAfterSlack; j++) {
                aWithSlack[i][j] = (j < n) ? aTemp[i][j] : Fraction.ZERO;
            }
            if (slackColOfRow[i] != -1) {
                aWithSlack[i][slackColOfRow[i]] = Fraction.of(slackSignOfRow[i]);
            }
        }

        // Bước 3: Duyệt các cột của A để tìm các vectơ đơn vị e1..em đã có sẵn (ưu tiên biến phụ trước, biến gốc sau)
        int[] basis = new int[m];
        Arrays.fill(basis, -1);

        // Ưu tiên quét từ cột biến phụ trước rồi đến biến gốc để khớp thứ tự thông thường của giáo trình
        List<Integer> scanOrder = new ArrayList<>();
        for (int j = n; j < varsAfterSlack; j++) scanOrder.add(j);
        for (int j = 0; j < n; j++) scanOrder.add(j);

        for (int col : scanOrder) {
            int unitRow = findUnitVectorRow(aWithSlack, col, m);
            if (unitRow != -1 && basis[unitRow] == -1) {
                basis[unitRow] = col;
            }
        }

        // Bước 4: Với mỗi dòng chưa có vectơ đơn vị, thêm biến giả (Lưu ý cuối mục 2.5.2)
        List<Integer> rowsNeedingArtificial = new ArrayList<>();
        for (int i = 0; i < m; i++) {
            if (basis[i] == -1) {
                rowsNeedingArtificial.add(i);
            }
        }

        int numArtificial = rowsNeedingArtificial.size();
        int totalVars = varsAfterSlack + numArtificial;
        boolean usesBigM = numArtificial > 0;

        Fraction[][] finalA = new Fraction[m][totalVars];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < totalVars; j++) {
                finalA[i][j] = (j < varsAfterSlack) ? aWithSlack[i][j] : Fraction.ZERO;
            }
        }

        int[] artificialCols = new int[numArtificial];
        BigM artificialCost = (lp.obj() == Objective.MAX) ? BigM.M_NEG : BigM.M_POS;
        String mSymbol = (lp.obj() == Objective.MAX) ? "-M" : "+M";

        for (int idx = 0; idx < numArtificial; idx++) {
            int row = rowsNeedingArtificial.get(idx);
            int artCol = varsAfterSlack + idx;
            finalA[row][artCol] = Fraction.ONE;
            basis[row] = artCol;
            artificialCols[idx] = artCol;
            logs.add("Phương trình " + (row + 1) + " chưa có vectơ đơn vị: thêm biến giả x<sub>"
                    + (artCol + 1) + "</sub> &ge; 0 với hệ số phạt " + mSymbol + " trong hàm mục tiêu.");
        }

        // Xây dựng mảng hệ số hàm mục tiêu chuẩn hóa c1 .. c_totalVars
        BigM[] finalC = new BigM[totalVars];
        for (int j = 0; j < n; j++) {
            finalC[j] = BigM.of(lp.c()[j]);
        }
        for (int j = n; j < varsAfterSlack; j++) {
            finalC[j] = BigM.ZERO;
        }
        for (int j = varsAfterSlack; j < totalVars; j++) {
            finalC[j] = artificialCost;
        }

        // Xây dựng phương án cực biên xuất phát x^(0)
        Fraction[] fullInitialX = new Fraction[totalVars];
        Arrays.fill(fullInitialX, Fraction.ZERO);
        List<String> unitDescs = new ArrayList<>();
        for (int i = 0; i < m; i++) {
            int bCol = basis[i];
            fullInitialX[bCol] = bTemp[i];
            unitDescs.add("x<sub>" + (bCol + 1) + "</sub> = e<sub>" + (i + 1)
                    + "</sub> (biến cơ sở x<sub>" + (bCol + 1) + "</sub> = " + bTemp[i] + ")");
        }

        Step.StandardizeStep stdStep = new Step.StandardizeStep(
                lp, logs, n, numSlack, numArtificial, artificialCols, finalC, finalA, bTemp, usesBigM
        );

        Step.InitialBasisStep initStep = new Step.InitialBasisStep(
                basis, fullInitialX, usesBigM, unitDescs
        );

        return new StandardizedModel(
                lp, n, numSlack, numArtificial, totalVars,
                finalA, bTemp, finalC, basis, artificialCols, usesBigM, stdStep, initStep
        );
    }

    private static int findUnitVectorRow(Fraction[][] A, int col, int m) {
        int oneRow = -1;
        for (int i = 0; i < m; i++) {
            if (A[i][col].isOne()) {
                if (oneRow != -1) return -1;
                oneRow = i;
            } else if (!A[i][col].isZero()) {
                return -1;
            }
        }
        return oneRow;
    }
}