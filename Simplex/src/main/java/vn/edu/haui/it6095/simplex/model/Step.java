package vn.edu.haui.it6095.simplex.model;

import java.util.List;

/**
 * Sealed interface biểu diễn từng bước giải bài toán để HtmlSolutionRenderer xuất ra giao diện.
 */
public sealed interface Step permits
        Step.StandardizeStep,
        Step.InitialBasisStep,
        Step.IterationStep,
        Step.ConclusionStep {

    /**
     * Bước chuẩn hóa bài toán về dạng chính tắc (Phần B).
     */
    record StandardizeStep(
            LinearProgram originalLp,
            List<String> transformationLogs,
            int numOriginalVars,
            int numSlackVars,
            int numArtificialVars,
            int[] artificialVarIndices,
            BigM[] standardC,
            Fraction[][] standardA,
            Fraction[] standardB,
            boolean usesBigM
    ) implements Step {}

    /**
     * Bước tìm cơ sở và phương án cực biên xuất phát (Phần C).
     */
    record InitialBasisStep(
            int[] basisIndices,
            Fraction[] fullInitialX,
            boolean usesBigM,
            List<String> unitVectorDescriptions
    ) implements Step {}

    /**
     * Bước lặp trên một bảng đơn hình (Phần D).
     */
    record IterationStep(
            int tableNumber,
            SimplexTable table,
            Objective objective,
            boolean isOptimal,
            boolean isUnbounded,
            int unboundedCol,
            int enteringCol,      // -1 nếu dừng
            int leavingRow,       // -1 nếu dừng
            Fraction pivotElement,// null nếu dừng
            Fraction[] ratios,    // tỉ số θ = xj / zjs (null nếu zjs <= 0)
            Fraction minTheta,    // giá trị θ nhỏ nhất
            boolean degenerate,   // có biến cơ sở = 0 hay không
            List<String> rowOperations,
            int[] nextBasis
    ) implements Step {}

    /**
     * Bước kết luận cuối cùng (Phần E).
     */
    record ConclusionStep(
            Result result,
            Objective objective,
            boolean usesBigM
    ) implements Step {}
}