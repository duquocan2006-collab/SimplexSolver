package vn.edu.haui.it6095.simplex.ui;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.control.Alert;
import javafx.scene.layout.BorderPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import vn.edu.haui.it6095.simplex.core.SimplexSolver;
import vn.edu.haui.it6095.simplex.model.LinearProgram;
import vn.edu.haui.it6095.simplex.model.SolutionOutput;
import vn.edu.haui.it6095.simplex.render.HtmlSolutionRenderer;

/**
 * Cửa sổ chính của phần mềm giải bài toán Quy hoạch tuyến tính bằng phương pháp đơn hình (Java 25 + JavaFX 25).
 */
public class MainApp extends Application {

    private final SimplexSolver solver = new SimplexSolver();
    private final HtmlSolutionRenderer renderer = new HtmlSolutionRenderer();

    private InputPanel inputPanel;
    private SolutionPanel solutionPanel;

    @Override
    public void start(Stage primaryStage) {
        solutionPanel = new SolutionPanel();
        inputPanel = new InputPanel(this::solveInBackground, () -> solutionPanel.showWelcomeMessage());

        BorderPane root = new BorderPane();
        root.setTop(inputPanel);
        root.setCenter(solutionPanel);

        // Kích thước cửa sổ theo màn hình: gọn hơn trước và luôn nằm gọn trong vùng nhìn thấy
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        double width = Math.min(1000, screen.getWidth() * 0.80);
        double height = Math.min(680, screen.getHeight() * 0.80);

        Scene scene = new Scene(root, width, height);
        primaryStage.setTitle("Phần mềm Giải Quy hoạch Tuyến tính bằng Phương pháp Đơn hình (JDK 25)");
        primaryStage.setScene(scene);
        try (var iconStream = MainApp.class.getResourceAsStream("/icon.png")) {
            if (iconStream != null) {
                primaryStage.getIcons().add(new Image(iconStream));
            }
        } catch (Exception ignored) {
            // Không có icon vẫn chạy bình thường
        }
        primaryStage.setMinWidth(Math.min(720, screen.getWidth() * 0.9));
        primaryStage.setMinHeight(Math.min(500, screen.getHeight() * 0.9));
        primaryStage.show();

        // Căn giữa màn hình sau khi biết kích thước thật của cửa sổ (gồm cả thanh tiêu đề)
        primaryStage.setX(screen.getMinX() + (screen.getWidth() - primaryStage.getWidth()) / 2);
        primaryStage.setY(screen.getMinY() + (screen.getHeight() - primaryStage.getHeight()) / 2);

        // Giải ngay ví dụ mẫu khi vừa khởi động để người dùng quan sát
        inputPanel.loadExampleNice();
    }

    private void solveInBackground(LinearProgram lp) {
        inputPanel.setSolveButtonDisable(true);

        Task<String> solveTask = new Task<>() {
            @Override
            protected String call() {
                SolutionOutput output = solver.solve(lp);
                return renderer.render(output);
            }
        };

        solveTask.setOnSucceeded(e -> {
            inputPanel.setSolveButtonDisable(false);
            solutionPanel.setHtmlContent(solveTask.getValue());
        });

        solveTask.setOnFailed(e -> {
            inputPanel.setSolveButtonDisable(false);
            Throwable ex = solveTask.getException();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi trong quá trình giải");
            alert.setHeaderText("Không thể hoàn tất giải thuật đơn hình");
            alert.setContentText(ex != null ? ex.getMessage() : "Lỗi không xác định");
            alert.showAndWait();
        });

        Thread worker = new Thread(solveTask, "simplex-solver-thread");
        worker.setDaemon(true);
        worker.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
