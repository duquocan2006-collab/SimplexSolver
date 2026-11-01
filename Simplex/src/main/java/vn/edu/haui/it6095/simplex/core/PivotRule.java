package vn.edu.haui.it6095.simplex.core;

import vn.edu.haui.it6095.simplex.model.BigM;
import vn.edu.haui.it6095.simplex.model.Fraction;
import vn.edu.haui.it6095.simplex.model.Objective;


public final class PivotRule {

    private PivotRule() {}


    public static boolean isOptimal(BigM[] delta, boolean[] hiddenColumns, Objective obj) {
        for (int k = 0; k < delta.length; k++) {
            if (hiddenColumns[k]) continue;
            if (obj == Objective.MAX && delta[k].isNegative()) {
                return false;
            }
            if (obj == Objective.MIN && delta[k].isPositive()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Bước 3: Kiểm tra điều kiện hàm mục tiêu không bị chặn.
     * Nếu tồn tại cột k vi phạm điều kiện tối ưu (Δk < 0 với MAX, hoặc Δk > 0 với MIN)
     * mà mọi zjk <= 0 trên cột k đó thì hàm mục tiêu không bị chặn.
     *
     * @return chỉ số cột k gây không bị chặn, hoặc -1 nếu không có.
     */
    public static int findUnboundedColumn(
            BigM[] delta, Fraction[][] z, boolean[] hiddenColumns, Objective obj) {
        int m = z.length;
        for (int k = 0; k < delta.length; k++) {
            if (hiddenColumns[k]) continue;
            boolean violates = (obj == Objective.MAX && delta[k].isNegative())
                    || (obj == Objective.MIN && delta[k].isPositive());
            if (violates) {
                boolean allNonPositive = true;
                for (int i = 0; i < m; i++) {
                    if (z[i][k].isPositive()) {
                        allNonPositive = false;
                        break;
                    }
                }
                if (allNonPositive) {
                    return k;
                }
            }
        }
        return -1;
    }

    /**
     * Bước 4: Chọn cột xoay s (vectơ As đưa vào cơ sở).
     * - Bài toán MAX: Δs = min { Δk | Δk < 0 }.
     * - Bài toán MIN: Δs = max { Δk | Δk > 0 }.
     * Khi có nhiều cột đạt cùng giá trị cực trị, chọn chỉ số cột k nhỏ nhất (quy tắc Bland).
     */
    public static int chooseEnteringColumn(BigM[] delta, boolean[] hiddenColumns, Objective obj) {
        int bestCol = -1;
        BigM bestVal = null;

        for (int k = 0; k < delta.length; k++) {
            if (hiddenColumns[k]) continue;
            if (obj == Objective.MAX && delta[k].isNegative()) {
                if (bestCol == -1 || delta[k].compareTo(bestVal) < 0) {
                    bestVal = delta[k];
                    bestCol = k;
                }
            } else if (obj == Objective.MIN && delta[k].isPositive()) {
                if (bestCol == -1 || delta[k].compareTo(bestVal) > 0) {
                    bestVal = delta[k];
                    bestCol = k;
                }
            }
        }
        return bestCol;
    }

    /**
     * Bước 5: Chọn dòng xoay r (vectơ Ar đưa ra khỏi cơ sở).
     * θ = min { xj / zjs | zjs > 0 } = xr / zrs.
     * Nếu hòa nhau giữa hai dòng, ưu tiên biến cơ sở là biến giả trước, sau đó chọn chỉ số biến nhỏ nhất (Bland).
     */
    public static int chooseLeavingRow(
            Fraction[] x, Fraction[][] z, int enteringCol, int[] basis, int firstArtificialCol) {
        int m = x.length;
        int bestRow = -1;
        Fraction minRatio = null;

        for (int i = 0; i < m; i++) {
            Fraction zis = z[i][enteringCol];
            if (zis.isPositive()) {
                Fraction ratio = x[i].divide(zis);
                if (bestRow == -1) {
                    minRatio = ratio;
                    bestRow = i;
                } else {
                    int cmp = ratio.compareTo(minRatio);
                    if (cmp < 0) {
                        minRatio = ratio;
                        bestRow = i;
                    } else if (cmp == 0) {
                        boolean currentIsArt = basis[i] >= firstArtificialCol;
                        boolean bestIsArt = basis[bestRow] >= firstArtificialCol;
                        if (currentIsArt && !bestIsArt) {
                            bestRow = i;
                        } else if (currentIsArt == bestIsArt && basis[i] < basis[bestRow]) {
                            bestRow = i;
                        }
                    }
                }
            }
        }
        return bestRow;
    }
}