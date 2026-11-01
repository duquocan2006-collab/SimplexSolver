package vn.edu.haui.it6095.simplex.model;

import java.util.List;

public sealed interface Result permits Result.Optimal, Result.Unbounded, Result.Infeasible {

    /**
     * Có phương án tối ưu x*.
     */
    record Optimal(
            Fraction[] originalX,
            Fraction[] fullX,
            Fraction optimalValue,
            boolean degenerate,
            boolean multipleSolutions,
            List<Integer> zeroDeltaNonBasicVars
    ) implements Result {}

    /**
     * Hàm mục tiêu không bị chặn.
     */
    record Unbounded(
            int problematicColumnIndex,
            Objective objective
    ) implements Result {}

    /**
     * Bài toán vô nghiệm (miền ràng buộc rỗng hoặc vượt số vòng lặp).
     */
    record Infeasible(
            String reason,
            int remainingArtificialVarIndex,
            Fraction remainingArtificialValue
    ) implements Result {}
}