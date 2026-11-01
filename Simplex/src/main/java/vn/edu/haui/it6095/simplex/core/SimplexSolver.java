package vn.edu.haui.it6095.simplex.core;

import vn.edu.haui.it6095.simplex.model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Bộ giải Quy hoạch tuyến tính bằng phương pháp đơn hình (dạng bảng) theo đúng
 * Đề cương bài giảng Tối ưu hóa - IT6095 (Bài 2, mục 2.4 - 2.5).
 */
public final class SimplexSolver {

    private static final int MAX_ITERATIONS = 500;

    public SolutionOutput solve(LinearProgram lp) {
        List<Step> steps = new ArrayList<>();

        // Giai đoạn chuẩn hóa & Giai đoạn 1: Tìm phương án cực biên xuất phát
        Standardizer.StandardizedModel model = Standardizer.standardize(lp);
        steps.add(model.standardizeStep());
        steps.add(model.initialBasisStep());

        int m = lp.numConstraints();
        int n = lp.numVars();
        int totalVars = model.totalVars();
        int firstArtificialCol = n + model.numSlack();

        int[] basis = Arrays.copyOf(model.initialBasis(), m);
        BigM[] cAll = Arrays.copyOf(model.c(), totalVars);
        Fraction[] x = Arrays.copyOf(model.b(), m);
        Fraction[][] z = new Fraction[m][totalVars];
        for (int i = 0; i < m; i++) {
            z[i] = Arrays.copyOf(model.A()[i], totalVars);
        }

        boolean[] hiddenColumns = new boolean[totalVars];
        Set<Integer> artificialSet = new HashSet<>();
        for (int col : model.artificialCols()) {
            artificialSet.add(col);
        }

        int tableNumber = 1;
        while (tableNumber <= MAX_ITERATIONS) {
            BigM[] cBasis = new BigM[m];
            for (int i = 0; i < m; i++) {
                cBasis[i] = cAll[basis[i]];
            }

            // Tính giá trị hàm mục tiêu: f = Σ cj * xj (j thuộc J)
            BigM f = BigM.ZERO;
            for (int i = 0; i < m; i++) {
                f = f.add(cBasis[i].multiply(x[i]));
            }

            // Tính các ước lượng Δk = zk - ck = Σ zjk * cj - ck (công thức mục 2.5)
            BigM[] delta = new BigM[totalVars];
            for (int k = 0; k < totalVars; k++) {
                if (hiddenColumns[k]) {
                    delta[k] = BigM.ZERO;
                    continue;
                }
                BigM zk = BigM.ZERO;
                for (int i = 0; i < m; i++) {
                    zk = zk.add(cBasis[i].multiply(z[i][k]));
                }
                delta[k] = zk.subtract(cAll[k]);
            }

            // Kiểm tra phương án cực biên hiện tại có suy biến (thoái hóa) hay không (tồn tại xj = 0 với j thuộc J)
            boolean degenerate = false;
            for (int i = 0; i < m; i++) {
                if (x[i].isZero()) {
                    degenerate = true;
                    break;
                }
            }

            SimplexTable currentTable = new SimplexTable(
                    basis, cBasis, cAll, x, z, delta, f, hiddenColumns, model.usesBigM()
            );

            // Bước 2: Kiểm tra điều kiện tối ưu
            if (PivotRule.isOptimal(delta, hiddenColumns, lp.obj())) {
                steps.add(new Step.IterationStep(
                        tableNumber, currentTable, lp.obj(),
                        true, false, -1, -1, -1, null, null, null,
                        degenerate, List.of(), basis
                ));

                Result finalResult = buildFinalResultOnOptimal(
                        lp, model, basis, x, delta, f, hiddenColumns, artificialSet, degenerate
                );
                steps.add(new Step.ConclusionStep(finalResult, lp.obj(), model.usesBigM()));
                return new SolutionOutput(steps, finalResult);
            }

            // Bước 3: Kiểm tra hàm mục tiêu không bị chặn
            int unboundedCol = PivotRule.findUnboundedColumn(delta, z, hiddenColumns, lp.obj());
            if (unboundedCol != -1) {
                steps.add(new Step.IterationStep(
                        tableNumber, currentTable, lp.obj(),
                        false, true, unboundedCol, -1, -1, null, null, null,
                        degenerate, List.of(), basis
                ));

                // Lưu ý: Trong bài toán M, nếu còn biến giả > 0 mà không bị chặn thì thực chất bài toán gốc có thể vô nghiệm
                Result finalResult = new Result.Unbounded(unboundedCol, lp.obj());
                steps.add(new Step.ConclusionStep(finalResult, lp.obj(), model.usesBigM()));
                return new SolutionOutput(steps, finalResult);
            }

            // Bước 4: Chọn cột xoay s (vectơ As đưa vào cơ sở)
            int s = PivotRule.chooseEnteringColumn(delta, hiddenColumns, lp.obj());

            // Bước 5: Chọn dòng xoay r (vectơ Ar đưa ra khỏi cơ sở)
            int r = PivotRule.chooseLeavingRow(x, z, s, basis, firstArtificialCol);
            Fraction pivot = z[r][s];

            Fraction[] ratios = new Fraction[m];
            for (int i = 0; i < m; i++) {
                ratios[i] = z[i][s].isPositive() ? x[i].divide(z[i][s]) : null;
            }
            Fraction minTheta = ratios[r];

            int leavingVar = basis[r];
            int[] nextBasis = Arrays.copyOf(basis, m);
            nextBasis[r] = s;

            // Mô tả phép biến đổi dòng cụ thể
            List<String> rowOps = new ArrayList<>();
            rowOps.add("Dòng chính D<sub>" + (r + 1) + "</sub>' = D<sub>" + (r + 1)
                    + "</sub> / (" + pivot + ") (chia dòng " + (r + 1) + " cho phần tử trục z<sub>"
                    + (leavingVar + 1) + "," + (s + 1) + "</sub> = " + pivot + ").");
            for (int i = 0; i < m; i++) {
                if (i == r) continue;
                Fraction factor = z[i][s];
                if (factor.isZero()) {
                    rowOps.add("D<sub>" + (i + 1) + "</sub>' = D<sub>" + (i + 1)
                            + "</sub> (giữ nguyên vì hệ số trên cột xoay bằng 0).");
                } else if (factor.isPositive()) {
                    rowOps.add("D<sub>" + (i + 1) + "</sub>' = D<sub>" + (i + 1)
                            + "</sub> - (" + factor + ") &middot; D<sub>" + (r + 1) + "</sub>'.");
                } else {
                    rowOps.add("D<sub>" + (i + 1) + "</sub>' = D<sub>" + (i + 1)
                            + "</sub> + (" + factor.abs() + ") &middot; D<sub>" + (r + 1) + "</sub>'.");
                }
            }

            steps.add(new Step.IterationStep(
                    tableNumber, currentTable, lp.obj(),
                    false, false, -1, s, r, pivot, ratios, minTheta,
                    degenerate, rowOps, nextBasis
            ));

            // Bước 6: Đổi cơ sở và tính bảng mới theo công thức 4.33 và 4.16
            Fraction[] nextX = new Fraction[m];
            Fraction[][] nextZ = new Fraction[m][totalVars];

            // Tính dòng chính r: x'r = xr / zrs; z'rk = zrk / zrs (công thức 4.33 & 4.16)
            nextX[r] = x[r].divide(pivot);
            for (int k = 0; k < totalVars; k++) {
                if (hiddenColumns[k]) {
                    nextZ[r][k] = Fraction.ZERO;
                } else {
                    nextZ[r][k] = z[r][k].divide(pivot);
                }
            }

            // Tính các dòng khác i != r:
            // x'j = xj - (xr / zrs) * zjs  (công thức 4.33)
            // z'jk = zjk - (zrk / zrs) * zjs (công thức 4.16)
            for (int i = 0; i < m; i++) {
                if (i == r) continue;
                Fraction zis = z[i][s];
                nextX[i] = x[i].subtract(nextX[r].multiply(zis));
                for (int k = 0; k < totalVars; k++) {
                    if (hiddenColumns[k]) {
                        nextZ[i][k] = Fraction.ZERO;
                    } else {
                        nextZ[i][k] = z[i][k].subtract(nextZ[r][k].multiply(zis));
                    }
                }
            }

            // Quy tắc bài toán M: Khi một biến giả bị đẩy ra khỏi cơ sở, từ đó trở đi ẩn cột của nó
            if (artificialSet.contains(leavingVar)) {
                hiddenColumns[leavingVar] = true;
            }

            basis = nextBasis;
            x = nextX;
            z = nextZ;
            tableNumber++;
        }

        Result loopLimitResult = new Result.Infeasible(
                "Vượt quá giới hạn " + MAX_ITERATIONS + " vòng lặp đơn hình.", -1, Fraction.ZERO
        );
        steps.add(new Step.ConclusionStep(loopLimitResult, lp.obj(), model.usesBigM()));
        return new SolutionOutput(steps, loopLimitResult);
    }

    private Result buildFinalResultOnOptimal(
            LinearProgram lp,
            Standardizer.StandardizedModel model,
            int[] basis,
            Fraction[] x,
            BigM[] delta,
            BigM f,
            boolean[] hiddenColumns,
            Set<Integer> artificialSet,
            boolean degenerate
    ) {
        int m = basis.length;
        int n = lp.numVars();
        int totalVars = model.totalVars();

        // Kiểm tra xem trong cơ sở tối ưu của bài toán M còn biến giả dương hay không (mục 2.5.2)
        for (int i = 0; i < m; i++) {
            int varIdx = basis[i];
            if (artificialSet.contains(varIdx) && x[i].isPositive()) {
                return new Result.Infeasible(
                        "Trong phương án tối ưu của bài toán mở rộng (M) vẫn còn biến giả x<sub>"
                                + (varIdx + 1) + "</sub> = " + x[i] + " &gt; 0.",
                        varIdx,
                        x[i]
                );
            }
        }

        Fraction[] fullX = new Fraction[totalVars];
        Arrays.fill(fullX, Fraction.ZERO);
        for (int i = 0; i < m; i++) {
            fullX[basis[i]] = x[i];
        }

        Fraction[] origX = Arrays.copyOf(fullX, n);

        // Kiểm tra dấu hiệu có vô số phương án tối ưu: tồn tại biến phi cơ sở (không phải biến giả) có Δk = 0
        Set<Integer> basisSet = new HashSet<>();
        for (int b : basis) basisSet.add(b);

        List<Integer> zeroDeltaNonBasic = new ArrayList<>();
        for (int k = 0; k < totalVars; k++) {
            if (!basisSet.contains(k) && !artificialSet.contains(k) && !hiddenColumns[k]) {
                if (delta[k].isZero()) {
                    zeroDeltaNonBasic.add(k);
                }
            }
        }

        return new Result.Optimal(
                origX,
                fullX,
                f.a(),
                degenerate,
                !zeroDeltaNonBasic.isEmpty(),
                zeroDeltaNonBasic
        );
    }
}