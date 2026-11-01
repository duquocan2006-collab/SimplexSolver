package vn.edu.haui.it6095.simplex.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Panel hiển thị lời giải chi tiết bằng JavaFX WebView, hỗ trợ nút "Xuất HTML" và "Sao chép lời giải".
 */
public class SolutionPanel extends BorderPane {

    private final WebView webView = new WebView();
    private String currentHtml = "";

    public SolutionPanel() {
        HBox toolBar = new HBox(10);
        toolBar.setPadding(new Insets(8, 12, 8, 12));
        toolBar.setAlignment(Pos.CENTER_LEFT);
        toolBar.setStyle("-fx-background-color: #f1f5f9; -fx-border-color: #cbd5e1; -fx-border-width: 0 0 1 0;");

        Label title = new Label("LỜI GIẢI CHI TIẾT");
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #0f172a;");

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnCopy = new Button("Sao chép lời giải");
        btnCopy.setOnAction(e -> copySolutionToClipboard());

        Button btnExport = new Button("Xuất HTML");
        btnExport.setOnAction(e -> exportHtmlFile());

        toolBar.getChildren().addAll(title, spacer, btnCopy, btnExport);

        setTop(toolBar);
        setCenter(webView);
        showWelcomeMessage();
    }

    public void setHtmlContent(String html) {
        this.currentHtml = html;
        webView.getEngine().loadContent(html, "text/html");
    }

    public final void showWelcomeMessage() {
        String welcome = """
                <!DOCTYPE html>
                <html lang="vi"><body style="font-family:'Segoe UI',sans-serif; padding:24px; color:#475569;">
                <p style="font-size:16px; color:#64748b;">Nhập dữ liệu bài toán và nhấn <b>Giải bài toán</b> để xem lời giải chi tiết.</p>
                </body></html>
                """;
        setHtmlContent(welcome);
    }

    private void copySolutionToClipboard() {
        if (currentHtml == null || currentHtml.isBlank()) return;
        ClipboardContent content = new ClipboardContent();
        content.putHtml(currentHtml);
        // Trích xuất thêm bản văn bản thuần từ HTML để dán vào Notepad/Word dễ dàng
        String plainText = currentHtml
                .replaceAll("(?is)<style.*?</style>", "")
                .replaceAll("(?i)<li[^>]*>", "- ")
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>|</div>|</li>|</tr>|</h2>|</h3>", "\n")
                .replaceAll("(?i)</td>|</th>", "\t")
                .replaceAll("<[^>]+>", "")
                .replace("&le;", "<=")
                .replace("&ge;", ">=")
                .replace("&Delta;", "Δ")
                .replace("&theta;", "θ")
                .replace("&alpha;", "α")
                .replace("&eta;", "η")
                .replace("&rarr;", "->")
                .replace("&rArr;", "=>")
                .replace("&middot;", "*")
                .replace("&empty;", "∅")
                .replace("&approx;", "≈");
        content.putString(plainText.trim());
        Clipboard.getSystemClipboard().setContent(content);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Đã sao chép");
        alert.setHeaderText(null);
        alert.setContentText("Đã sao chép toàn bộ lời giải (định dạng HTML & văn bản) vào Clipboard!");
        alert.showAndWait();
    }

    private void exportHtmlFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Lưu lời giải ra file HTML");
        chooser.setInitialFileName("loi-giai-don-hinh.html");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("HTML Files (*.html)", "*.html"));
        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file != null) {
            try {
                Files.writeString(file.toPath(), currentHtml, StandardCharsets.UTF_8);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Xuất file thành công");
                alert.setHeaderText(null);
                alert.setContentText("Đã lưu lời giải tại:\n" + file.getAbsolutePath());
                alert.showAndWait();
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Lỗi xuất file");
                alert.setHeaderText("Không thể ghi file HTML");
                alert.setContentText(ex.getMessage());
                alert.showAndWait();
            }
        }
    }
}
