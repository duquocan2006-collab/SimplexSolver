package vn.edu.haui.it6095.simplex.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import vn.edu.haui.it6095.simplex.core.TextLpParser;
import vn.edu.haui.it6095.simplex.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Giao diện nhập dữ liệu bài toán Quy hoạch tuyến tính:
 * - Bảng nhập trực quan hệ số c1..cn, aij, dấu quan hệ và vế phải bi.
 * - Ô nhập nhanh văn bản đồng bộ hai chiều với bảng nhập.
 * - Nút nạp sẵn ví dụ mẫu có nghiệm đẹp.
 */
public class InputPanel extends VBox {

    private final Spinner<Integer> spinnerVars = new Spinner<>(1, 10, 3);
    private final Spinner<Integer> spinnerCons = new Spinner<>(1, 10, 3);
    private final ComboBox<String> comboObjective = new ComboBox<>();
    private final Label lblObjectivePreview = new Label();

    private final GridPane matrixGrid = new GridPane();
    private TextField[] objFields;
    private TextField[][] conFields;
    @SuppressWarnings("unchecked")
    private ComboBox<String>[] relBoxes;
    private TextField[] rhsFields;

    private final TextArea txtQuickInput = new TextArea();
    private final Button btnSolve = new Button("Giải bài toán");

    private final Consumer<LinearProgram> onSolveRequested;
    private final Runnable onClearRequested;

    // Theo dõi người dùng sửa ở đâu gần nhất (bảng hay ô văn bản) để "Giải bài toán" đọc đúng nguồn.
    private boolean textIsSource = false;
    private int suppressDirty = 0;

    public InputPanel(Consumer<LinearProgram> onSolveRequested, Runnable onClearRequested) {
        this.onSolveRequested = onSolveRequested;
        this.onClearRequested = onClearRequested;

        setSpacing(10);
        setPadding(new Insets(12));
        setStyle("-fx-background-color: #f8fafc; -fx-border-color: #cbd5e1; -fx-border-width: 0 0 1 0;");

        // Hàng điều khiển trên cùng
        FlowPane topBar = new FlowPane(12, 8);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setRowValignment(javafx.geometry.VPos.CENTER);

        spinnerVars.setPrefWidth(72);
        spinnerCons.setPrefWidth(72);
        Button btnGenerateGrid = new Button("Tạo bảng nhập");
        btnGenerateGrid.setOnAction(e -> {
            buildMatrixGrid(spinnerVars.getValue(), spinnerCons.getValue());
            textIsSource = false;
        });

        comboObjective.getItems().addAll("Tìm Max", "Tìm Min");
        comboObjective.getSelectionModel().selectFirst();
        comboObjective.setOnAction(e -> {
            updateObjectivePreview();
            markGridEdited();
        });

        Button btnExample = new Button("Ví dụ mẫu");
        btnExample.setStyle("-fx-background-color: #e0f2fe; -fx-text-fill: #0369a1; -fx-font-weight: bold;");
        btnExample.setOnAction(e -> loadExampleNice());

        Button btnClear = new Button("Xóa");
        btnClear.setOnAction(e -> clearAll());

        btnSolve.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 16;");
        btnSolve.setOnAction(e -> triggerSolve());

        topBar.getChildren().addAll(
                new Label("Số biến (1–10):"), spinnerVars,
                new Label("Số ràng buộc (1–10):"), spinnerCons,
                btnGenerateGrid,
                new Label("Mục tiêu:"), comboObjective,
                btnSolve, btnClear, btnExample
        );

        // Khu vực chia 2 cột: Bên trái là Bảng nhập hệ số, bên phải là Ô nhập nhanh bằng văn bản
        HBox mainInputArea = new HBox(16);
        VBox leftBox = new VBox(6);
        lblObjectivePreview.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e40af;");

        ScrollPane gridScroll = new ScrollPane(matrixGrid);
        gridScroll.setFitToHeight(true);
        gridScroll.setPrefHeight(170);
        gridScroll.setStyle("-fx-background: #ffffff; -fx-border-color: #cbd5e1;");
        leftBox.getChildren().addAll(lblObjectivePreview, gridScroll);
        HBox.setHgrow(leftBox, Priority.ALWAYS);

        VBox rightBox = new VBox(6);
        rightBox.setPrefWidth(300);
        rightBox.setMinWidth(240);
        Label lblQuick = new Label("Nhập nhanh bằng văn bản:");
        lblQuick.setStyle("-fx-font-weight: bold;");
        txtQuickInput.setPromptText("f(x) = -2x1 + 5x2 -4x3 - x4 +5x5 -> min\nx1 + 6x2 -2x4 -9x5 = 32\n2x2 + x3 + x4 + 3x5 = 30\n3x2 + x5 + x6 = 36");
        txtQuickInput.setPrefRowCount(5);
        txtQuickInput.textProperty().addListener((obs, oldV, newV) -> {
            if (suppressDirty == 0) textIsSource = true;
        });
        txtQuickInput.setStyle("-fx-font-family: 'Consolas', monospace;");

        FlowPane syncButtons = new FlowPane(8, 6);
        Button btnParseTextToGrid = new Button("Đồng bộ Văn bản → Bảng");
        btnParseTextToGrid.setOnAction(e -> syncTextToGrid());
        Button btnSyncGridToText = new Button("Đồng bộ Bảng → Văn bản");
        btnSyncGridToText.setOnAction(e -> syncGridToText());
        syncButtons.getChildren().addAll(btnParseTextToGrid, btnSyncGridToText);

        rightBox.getChildren().addAll(lblQuick, txtQuickInput, syncButtons);
        mainInputArea.getChildren().addAll(leftBox, rightBox);

        getChildren().addAll(topBar, mainInputArea);

        // Khởi tạo mặc định ví dụ mẫu
        loadExampleNice();
    }

    public void setSolveButtonDisable(boolean disable) {
        btnSolve.setDisable(disable);
    }

    @SuppressWarnings("unchecked")
    private void buildMatrixGrid(int n, int m) {
        matrixGrid.getChildren().clear();
        matrixGrid.setHgap(6);
        matrixGrid.setVgap(6);
        matrixGrid.setPadding(new Insets(8));

        objFields = new TextField[n];
        conFields = new TextField[m][n];
        relBoxes = new ComboBox[m];
        rhsFields = new TextField[m];

        // Hàng hàm mục tiêu
        Label lblObj = new Label("Hàm mục tiêu f(x) =");
        lblObj.setStyle("-fx-font-weight: bold;");
        matrixGrid.add(lblObj, 0, 0);

        for (int j = 0; j < n; j++) {
            HBox cell = new HBox(3);
            cell.setAlignment(Pos.CENTER_LEFT);
            TextField tf = new TextField("0");
            tf.setPrefWidth(58);
            tf.textProperty().addListener((obs, oldV, newV) -> {
                updateObjectivePreview();
                markGridEdited();
            });
            objFields[j] = tf;
            Label varLbl = new Label("x" + (j + 1) + (j < n - 1 ? " +" : ""));
            cell.getChildren().addAll(tf, varLbl);
            matrixGrid.add(cell, j + 1, 0);
        }

        // Các hàng ràng buộc
        for (int i = 0; i < m; i++) {
            Label lblCon = new Label("Ràng buộc " + (i + 1) + ":");
            matrixGrid.add(lblCon, 0, i + 1);

            for (int j = 0; j < n; j++) {
                HBox cell = new HBox(3);
                cell.setAlignment(Pos.CENTER_LEFT);
                TextField tf = new TextField("0");
                tf.setPrefWidth(58);
                tf.textProperty().addListener((obs, oldV, newV) -> markGridEdited());
                conFields[i][j] = tf;
                Label varLbl = new Label("x" + (j + 1) + (j < n - 1 ? " +" : ""));
                cell.getChildren().addAll(tf, varLbl);
                matrixGrid.add(cell, j + 1, i + 1);
            }

            ComboBox<String> relBox = new ComboBox<>();
            relBox.getItems().addAll("<=", "=", ">=");
            relBox.getSelectionModel().select("<=");
            relBox.setOnAction(e -> markGridEdited());
            relBoxes[i] = relBox;
            matrixGrid.add(relBox, n + 1, i + 1);

            TextField rhs = new TextField("0");
            rhs.setPrefWidth(65);
            rhs.textProperty().addListener((obs, oldV, newV) -> markGridEdited());
            rhsFields[i] = rhs;
            matrixGrid.add(rhs, n + 2, i + 1);
        }

        Label lblNonNeg = new Label("Mặc định điều kiện dấu: xj ≥ 0 (với mọi j = 1.." + n + ")");
        lblNonNeg.setStyle("-fx-font-style: italic; -fx-text-fill: #475569;");
        matrixGrid.add(lblNonNeg, 0, m + 1, n + 3, 1);

        updateObjectivePreview();
    }

    private void updateObjectivePreview() {
        if (objFields == null) return;
        StringBuilder sb = new StringBuilder("f(x) = ");
        for (int j = 0; j < objFields.length; j++) {
            String val = objFields[j].getText().trim();
            if (val.isEmpty()) val = "0";
            if (j > 0) sb.append(" + ");
            sb.append("(").append(val).append(")x").append(j + 1);
        }
        String arrow = comboObjective.getSelectionModel().getSelectedIndex() == 0 ? " → max" : " → min";
        sb.append(arrow);
        lblObjectivePreview.setText(sb.toString());
    }

    private void markGridEdited() {
        if (suppressDirty == 0) textIsSource = false;
    }

    private void populateFromLinearProgram(LinearProgram lp) {
        suppressDirty++;
        try {
            populateFromLinearProgramInternal(lp);
        } finally {
            suppressDirty--;
        }
        textIsSource = false;
    }

    private void populateFromLinearProgramInternal(LinearProgram lp) {
        int n = lp.numVars();
        int m = lp.numConstraints();
        spinnerVars.getValueFactory().setValue(n);
        spinnerCons.getValueFactory().setValue(m);
        comboObjective.getSelectionModel().select(lp.obj() == Objective.MAX ? 0 : 1);

        buildMatrixGrid(n, m);

        for (int j = 0; j < n; j++) {
            objFields[j].setText(lp.c()[j].toString());
        }
        for (int i = 0; i < m; i++) {
            Constraint c = lp.constraints().get(i);
            for (int j = 0; j < n; j++) {
                conFields[i][j].setText(c.a()[j].toString());
            }
            relBoxes[i].getSelectionModel().select(c.rel().symbol());
            rhsFields[i].setText(c.b().toString());
        }
        updateObjectivePreview();
        syncGridToText();
    }

    private void syncTextToGrid() {
        try {
            LinearProgram lp = TextLpParser.parse(txtQuickInput.getText());
            populateFromLinearProgram(lp);
        } catch (Exception ex) {
            showErrorAlert("Lỗi phân tích văn bản nhập nhanh", ex.getMessage());
        }
    }

    private void syncGridToText() {
        try {
            LinearProgram lp = readLinearProgramFromGrid();
            StringBuilder sb = new StringBuilder();
            sb.append("f(x) = ").append(formatPlainLinear(lp.c()));
            sb.append(" -> ").append(lp.obj().label()).append("\n");
            for (Constraint c : lp.constraints()) {
                sb.append(formatPlainLinear(c.a())).append(" ")
                        .append(c.rel().symbol()).append(" ")
                        .append(c.b()).append("\n");
            }
            suppressDirty++;
            try {
                txtQuickInput.setText(sb.toString().trim());
            } finally {
                suppressDirty--;
            }
        } catch (Exception ignored) {
            // Bỏ qua nếu người dùng đang gõ dở ký tự trong bảng
        }
    }

    private String formatPlainLinear(Fraction[] coeffs) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (int j = 0; j < coeffs.length; j++) {
            Fraction f = coeffs[j];
            if (f.isZero()) continue;
            if (first) {
                if (f.isMinusOne()) sb.append("-x").append(j + 1);
                else if (f.isOne()) sb.append("x").append(j + 1);
                else sb.append(f).append("x").append(j + 1);
                first = false;
            } else {
                if (f.isPositive()) {
                    sb.append(" + ").append(f.isOne() ? "" : f).append("x").append(j + 1);
                } else {
                    sb.append(" - ").append(f.abs().isOne() ? "" : f.abs()).append("x").append(j + 1);
                }
            }
        }
        return first ? "0x1" : sb.toString();
    }

    private LinearProgram readLinearProgramFromGrid() {
        int n = objFields.length;
        int m = conFields.length;

        Objective obj = (comboObjective.getSelectionModel().getSelectedIndex() == 0)
                ? Objective.MAX : Objective.MIN;

        Fraction[] c = new Fraction[n];
        for (int j = 0; j < n; j++) {
            try {
                c[j] = Fraction.parse(objFields[j].getText());
            } catch (Exception ex) {
                throw new IllegalArgumentException("Hệ số hàm mục tiêu c" + (j + 1) + " không hợp lệ: " + ex.getMessage());
            }
        }

        List<Constraint> constraints = new ArrayList<>();
        for (int i = 0; i < m; i++) {
            Fraction[] a = new Fraction[n];
            for (int j = 0; j < n; j++) {
                try {
                    a[j] = Fraction.parse(conFields[i][j].getText());
                } catch (Exception ex) {
                    throw new IllegalArgumentException(
                            "Hệ số a(" + (i + 1) + "," + (j + 1) + ") ở ràng buộc " + (i + 1) + " không hợp lệ: " + ex.getMessage()
                    );
                }
            }
            Relation rel = Relation.fromSymbol(relBoxes[i].getValue());
            Fraction b;
            try {
                b = Fraction.parse(rhsFields[i].getText());
            } catch (Exception ex) {
                throw new IllegalArgumentException("Vế phải b" + (i + 1) + " ở ràng buộc " + (i + 1) + " không hợp lệ: " + ex.getMessage());
            }
            constraints.add(new Constraint(a, rel, b));
        }

        return new LinearProgram(obj, c, constraints);
    }

    private void triggerSolve() {
        LinearProgram lp;
        if (textIsSource) {
            // Người dùng vừa sửa đề ở ô văn bản: phân tích văn bản, cập nhật lại bảng rồi mới giải.
            try {
                lp = TextLpParser.parse(txtQuickInput.getText());
            } catch (Exception ex) {
                showErrorAlert("Lỗi phân tích văn bản nhập nhanh", ex.getMessage());
                return;
            }
            populateFromLinearProgram(lp);
        } else {
            try {
                lp = readLinearProgramFromGrid();
            } catch (Exception ex) {
                showErrorAlert("Dữ liệu đầu vào không hợp lệ", ex.getMessage());
                return;
            }
            syncGridToText();
        }
        onSolveRequested.accept(lp);
    }

    private void clearAll() {
        int n = spinnerVars.getValue();
        int m = spinnerCons.getValue();
        buildMatrixGrid(n, m);
        suppressDirty++;
        try {
            txtQuickInput.clear();
        } finally {
            suppressDirty--;
        }
        textIsSource = false;
        onClearRequested.run();
    }

    /**
     * Nạp sẵn ví dụ mẫu: bài toán MIN có 3 ràng buộc (đồng bộ với ô nhập nhanh).
     * f(x) = -2x1 + 5x2 - 4x3 - x4 + 5x5 -> min
     * Ràng buộc:
     *   x1 + 6x2 - 2x4 - 9x5 = 32
     *   2x2 + x3 + x4 + 3x5 = 30
     *   3x2 + x5 + x6 = 36
     * Nghiệm tối ưu: x* = (92, 0, 0, 30, 0, 36), f_min = -214
     */
    public final void loadExampleNice() {
        LinearProgram lp = new LinearProgram(
                Objective.MIN,
                new Fraction[]{Fraction.of(-2), Fraction.of(5), Fraction.of(-4), Fraction.of(-1), Fraction.of(5), Fraction.ZERO},
                List.of(
                        new Constraint(new Fraction[]{Fraction.of(1), Fraction.of(6), Fraction.ZERO, Fraction.of(-2), Fraction.of(-9), Fraction.ZERO}, Relation.EQ, Fraction.of(32)),
                        new Constraint(new Fraction[]{Fraction.ZERO, Fraction.of(2), Fraction.of(1), Fraction.of(1), Fraction.of(3), Fraction.ZERO}, Relation.EQ, Fraction.of(30)),
                        new Constraint(new Fraction[]{Fraction.ZERO, Fraction.of(3), Fraction.ZERO, Fraction.ZERO, Fraction.of(1), Fraction.of(1)}, Relation.EQ, Fraction.of(36))
                )
        );
        populateFromLinearProgram(lp);
    }

    private void showErrorAlert(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi dữ liệu đầu vào");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
