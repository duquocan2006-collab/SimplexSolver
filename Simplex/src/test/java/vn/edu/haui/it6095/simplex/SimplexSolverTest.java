package vn.edu.haui.it6095.simplex;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edu.haui.it6095.simplex.core.SimplexSolver;
import vn.edu.haui.it6095.simplex.core.TextLpParser;
import vn.edu.haui.it6095.simplex.model.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SimplexSolverTest {

    private final SimplexSolver solver = new SimplexSolver();

    @Test
    @DisplayName("1. Kiểm tra số học phân số Fraction và so sánh BigM")
    void testFractionAndBigM() {
        Fraction f1 = Fraction.of(164, 35);
        Fraction f2 = Fraction.of(118, 35);
        assertEquals(Fraction.of(282, 35), f1.add(f2));
        assertEquals(Fraction.of(-3, 4), Fraction.of(6, -8));

        // So sánh BigM: ưu tiên hệ số của M (b) trước, sau đó mới so hệ số tự do (a)
        BigM m1 = new BigM(Fraction.of(100), Fraction.of(-2)); // 100 - 2M
        BigM m2 = new BigM(Fraction.of(-50), Fraction.of(-1)); // -50 - M
        BigM m3 = new BigM(Fraction.of(-10), Fraction.of(-1)); // -10 - M

        assertTrue(m1.compareTo(m2) < 0, "100 - 2M phải nhỏ hơn -50 - M");
        assertTrue(m2.compareTo(m3) < 0, "-50 - M phải nhỏ hơn -10 - M");
        assertTrue(m1.isNegative());
    }

    @Test
    @DisplayName("2. Ví dụ 2.5 (Đề cương IT6095): Khớp phương án tối ưu, f*, Δ bảng cuối và cơ sở cuối")
    void testExample2_5() {
        LinearProgram lp = new LinearProgram(
                Objective.MAX,
                new Fraction[]{Fraction.of(3), Fraction.of(-1), Fraction.of(2)},
                List.of(
                        new Constraint(new Fraction[]{Fraction.of(-1), Fraction.of(3), Fraction.of(1)}, Relation.LE, Fraction.of(7)),
                        new Constraint(new Fraction[]{Fraction.of(3), Fraction.of(-4), Fraction.of(8)}, Relation.LE, Fraction.of(10)),
                        new Constraint(new Fraction[]{Fraction.of(4), Fraction.of(-2), Fraction.of(0)}, Relation.LE, Fraction.of(12))
                )
        );

        SolutionOutput out = solver.solve(lp);
        assertInstanceOf(Result.Optimal.class, out.result());
        Result.Optimal opt = (Result.Optimal) out.result();

        // Kỳ vọng: x* = (164/35, 118/35, 55/35) = (164/35, 118/35, 11/7), f* = 484/35
        assertArrayEquals(
                new Fraction[]{Fraction.of(164, 35), Fraction.of(118, 35), Fraction.of(55, 35)},
                opt.originalX()
        );
        assertEquals(Fraction.of(484, 35), opt.optimalValue());

        // Lấy bảng cuối cùng
        List<Step.IterationStep> iterSteps = out.steps().stream()
                .filter(Step.IterationStep.class::isInstance)
                .map(Step.IterationStep.class::cast)
                .toList();
        Step.IterationStep lastStep = iterSteps.getLast();
        SimplexTable lastTable = lastStep.table();

        // Cơ sở cuối: A2, A3, A1 (tương ứng chỉ số 0-based: 1, 2, 0)
        assertArrayEquals(new int[]{1, 2, 0}, lastTable.basis());

        // Bảng cuối có Δ = (0, 0, 0, 16/35, 9/35, 157/140)
        Fraction[] actualDeltas = java.util.Arrays.stream(lastTable.delta())
                .map(BigM::a)
                .toArray(Fraction[]::new);
        assertArrayEquals(
                new Fraction[]{
                        Fraction.ZERO, Fraction.ZERO, Fraction.ZERO,
                        Fraction.of(16, 35), Fraction.of(9, 35), Fraction.of(157, 140)
                },
                actualDeltas
        );
    }

    @Test
    @DisplayName("3. Ví dụ 2.6 (Phương pháp phạt M): Đi qua 4 bảng, cột x5 bị loại sau bảng 1, x*=(3,3,1,0), f*=8")
    void testExample2_6() {
        LinearProgram lp = new LinearProgram(
                Objective.MAX,
                new Fraction[]{Fraction.of(2), Fraction.of(1), Fraction.of(-1), Fraction.of(-1)},
                List.of(
                        new Constraint(new Fraction[]{Fraction.of(1), Fraction.of(-1), Fraction.of(2), Fraction.of(-1)}, Relation.EQ, Fraction.of(2)),
                        new Constraint(new Fraction[]{Fraction.of(2), Fraction.of(1), Fraction.of(-3), Fraction.of(1)}, Relation.EQ, Fraction.of(6)),
                        new Constraint(new Fraction[]{Fraction.of(1), Fraction.of(1), Fraction.of(1), Fraction.of(1)}, Relation.EQ, Fraction.of(7))
                )
        );

        SolutionOutput out = solver.solve(lp);
        assertInstanceOf(Result.Optimal.class, out.result());
        Result.Optimal opt = (Result.Optimal) out.result();

        assertArrayEquals(
                new Fraction[]{Fraction.of(3), Fraction.of(3), Fraction.of(1), Fraction.ZERO},
                opt.originalX()
        );
        assertEquals(Fraction.of(8), opt.optimalValue());

        List<Step.IterationStep> iterSteps = out.steps().stream()
                .filter(Step.IterationStep.class::isInstance)
                .map(Step.IterationStep.class::cast)
                .toList();

        // Đi qua đúng 4 bảng đơn hình
        assertEquals(4, iterSteps.size());

        // Cơ sở xuất phát ở Bảng 1 gồm x5, x6, x7 (chỉ số 4, 5, 6)
        assertArrayEquals(new int[]{4, 5, 6}, iterSteps.get(0).table().basis());

        // Sau Bảng 1, biến giả x5 (cột 4) bị loại và đánh dấu ẩn ở Bảng 2
        assertTrue(iterSteps.get(1).table().hiddenColumns()[4]);
    }

    @Test
    @DisplayName("4. Bài toán không bị chặn: max x1 + x2 với x1 - x2 <= 1")
    void testUnboundedProblem() {
        LinearProgram lp = new LinearProgram(
                Objective.MAX,
                new Fraction[]{Fraction.ONE, Fraction.ONE},
                List.of(
                        new Constraint(new Fraction[]{Fraction.ONE, Fraction.MINUS_ONE}, Relation.LE, Fraction.ONE)
                )
        );
        SolutionOutput out = solver.solve(lp);
        assertInstanceOf(Result.Unbounded.class, out.result());
    }

    @Test
    @DisplayName("5. Bài toán vô nghiệm: x1 + x2 <= 1 và x1 + x2 >= 3")
    void testInfeasibleProblem() {
        LinearProgram lp = new LinearProgram(
                Objective.MAX,
                new Fraction[]{Fraction.ONE, Fraction.ONE},
                List.of(
                        new Constraint(new Fraction[]{Fraction.ONE, Fraction.ONE}, Relation.LE, Fraction.ONE),
                        new Constraint(new Fraction[]{Fraction.ONE, Fraction.ONE}, Relation.GE, Fraction.of(3))
                )
        );
        SolutionOutput out = solver.solve(lp);
        assertInstanceOf(Result.Infeasible.class, out.result());
    }

    @Test
    @DisplayName("6. Bài toán MIN có ràng buộc >=")
    void testMinProblemWithGE() {
        // min f = 2x1 + 3x2, với x1 + x2 >= 4, x1 + 3x2 >= 6, xj >= 0 -> x* = (3, 1), f_min = 9
        LinearProgram lp = new LinearProgram(
                Objective.MIN,
                new Fraction[]{Fraction.of(2), Fraction.of(3)},
                List.of(
                        new Constraint(new Fraction[]{Fraction.ONE, Fraction.ONE}, Relation.GE, Fraction.of(4)),
                        new Constraint(new Fraction[]{Fraction.ONE, Fraction.of(3)}, Relation.GE, Fraction.of(6))
                )
        );
        SolutionOutput out = solver.solve(lp);
        assertInstanceOf(Result.Optimal.class, out.result());
        Result.Optimal opt = (Result.Optimal) out.result();
        assertArrayEquals(new Fraction[]{Fraction.of(3), Fraction.ONE}, opt.originalX());
        assertEquals(Fraction.of(9), opt.optimalValue());
    }

    @Test
    @DisplayName("7. Ràng buộc có vế phải âm (nhân -1 đổi chiều)")
    void testNegativeRightHandSide() {
        // max f = x1 + 2x2 với -x1 - x2 >= -3 (tương đương x1 + x2 <= 3) -> x* = (0, 3), f* = 6
        LinearProgram lp = new LinearProgram(
                Objective.MAX,
                new Fraction[]{Fraction.ONE, Fraction.of(2)},
                List.of(
                        new Constraint(new Fraction[]{Fraction.MINUS_ONE, Fraction.MINUS_ONE}, Relation.GE, Fraction.of(-3))
                )
        );
        SolutionOutput out = solver.solve(lp);
        assertInstanceOf(Result.Optimal.class, out.result());
        Result.Optimal opt = (Result.Optimal) out.result();
        assertArrayEquals(new Fraction[]{Fraction.ZERO, Fraction.of(3)}, opt.originalX());
        assertEquals(Fraction.of(6), opt.optimalValue());
    }

    @Test
    @DisplayName("8. Bài toán có phương án cực biên suy biến (thoái hóa)")
    void testDegenerateProblem() {
        // max f = 2x1 + x2 với x1 <= 2, x1 + x2 <= 2 -> tại đỉnh tối ưu (2, 0), biến phụ s1 = 0 nằm trong cơ sở
        LinearProgram lp = new LinearProgram(
                Objective.MAX,
                new Fraction[]{Fraction.of(2), Fraction.ONE},
                List.of(
                        new Constraint(new Fraction[]{Fraction.ONE, Fraction.ZERO}, Relation.LE, Fraction.of(2)),
                        new Constraint(new Fraction[]{Fraction.ONE, Fraction.ONE}, Relation.LE, Fraction.of(2))
                )
        );
        SolutionOutput out = solver.solve(lp);
        assertInstanceOf(Result.Optimal.class, out.result());
        Result.Optimal opt = (Result.Optimal) out.result();
        assertTrue(opt.degenerate(), "Phương án tối ưu phải được nhận diện là suy biến");
        assertEquals(Fraction.of(4), opt.optimalValue());
    }

    @Test
    @DisplayName("9. Ví dụ mẫu của phần mềm: MIN 6 ẩn, 3 ràng buộc =, x*=(92,0,0,30,0,36), f*=-214")
    void testBuiltInSampleExample() {
        LinearProgram lp = TextLpParser.parse(
                "f(x) = -2x1 + 5x2 -4x3 - x4 +5x5 -> min\n" +
                "x1 + 6x2 -2x4 -9x5 = 32\n" +
                "2x2 + x3 + x4 + 3x5 = 30\n" +
                "3x2 + x5 + x6 = 36");
        assertEquals(Objective.MIN, lp.obj());
        assertEquals(6, lp.numVars());
        assertEquals(3, lp.numConstraints());

        SolutionOutput out = solver.solve(lp);
        assertInstanceOf(Result.Optimal.class, out.result());
        Result.Optimal opt = (Result.Optimal) out.result();
        assertArrayEquals(
                new Fraction[]{Fraction.of(92), Fraction.ZERO, Fraction.ZERO, Fraction.of(30), Fraction.ZERO, Fraction.of(36)},
                opt.originalX()
        );
        assertEquals(Fraction.of(-214), opt.optimalValue());
    }
}
