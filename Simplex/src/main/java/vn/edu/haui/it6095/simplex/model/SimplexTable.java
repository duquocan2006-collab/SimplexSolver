package vn.edu.haui.it6095.simplex.model;

import java.util.Arrays;

/**
 * Lưu trữ toàn bộ trạng thái của một bảng đơn hình ở bước lặp thứ k.
 *
 * @param basis         Mảng chỉ số biến cơ sở (0-based) của từng dòng i = 0..m-1
 * @param cBasis        Hệ số hàm mục tiêu cj của các biến cơ sở
 * @param cAll          Hệ số hàm mục tiêu của tất cả các biến (c1 .. cn+m)
 * @param x             Phương án cực biên hiện tại xj ứng với các dòng cơ sở
 * @param z             Ma trận hệ số khai triển zjk (kích thước m x totalVars)
 * @param delta         Mảng ước lượng Δk = zk - ck (với bài toán M: Δk = αk + ηk*M)
 * @param f             Giá trị hàm mục tiêu hiện tại f = Σ cj * xj
 * @param hiddenColumns Đánh dấu các cột biến giả đã ra khỏi cơ sở (bị gạch chéo)
 * @param bigMMode      Có phải bài toán phạt M hay không (để hiển thị 2 dòng αk, ηk)
 */
public record SimplexTable(
        int[] basis,
        BigM[] cBasis,
        BigM[] cAll,
        Fraction[] x,
        Fraction[][] z,
        BigM[] delta,
        BigM f,
        boolean[] hiddenColumns,
        boolean bigMMode
) {
    public SimplexTable {
        basis = Arrays.copyOf(basis, basis.length);
        cBasis = Arrays.copyOf(cBasis, cBasis.length);
        cAll = Arrays.copyOf(cAll, cAll.length);
        x = Arrays.copyOf(x, x.length);
        Fraction[][] zCopy = new Fraction[z.length][];
        for (int i = 0; i < z.length; i++) {
            zCopy[i] = Arrays.copyOf(z[i], z[i].length);
        }
        z = zCopy;
        delta = Arrays.copyOf(delta, delta.length);
        hiddenColumns = Arrays.copyOf(hiddenColumns, hiddenColumns.length);
    }
}