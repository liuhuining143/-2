package com.example.debug;

import com.example.debug.AST.JavaParser;
import com.example.debug.AST.ASTNode;
import com.example.debug.Symbol.Quadruple;
import com.example.debug.Symbol.SymbolType;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javafx.concurrent.Task;
import javafx.scene.control.TreeItem;



public class MainController {

    // 组件注入（必须与FXML中的fx:id完全匹配）
    @FXML private TreeView<String> projectTree;
    @FXML private TabPane codeTabs;
    @FXML private TableView<ErrorEntry> errorTable;
    @FXML private Label statusLabel;

    private File currentFile;
    private final Clipboard clipboard = Clipboard.getSystemClipboard();

    @FXML
    private void initialize() {
        // 初始化项目结构树
        setupProjectTree();

        // 初始化代码区域
        if (codeTabs.getTabs().isEmpty()) {
            createNewTab("Welcome.java", "// 欢迎使用编译器工具\n");
        }

        // 初始化错误表格
        setupErrorTable();
        errorTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
    }

    // region 文件操作
    @FXML
    private void handleNew() {
        createNewTab("Untitled.java", "");
        statusLabel.setText("已创建新文件");
    }

    @FXML
    private void handleOpen() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("打开文件");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Java Files", "*.java"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File file = fileChooser.showOpenDialog(getStage());
        if (file != null) {
            try {
                String content = Files.readString(file.toPath());
                createNewTab(file.getName(), content);
                currentFile = file;
                statusLabel.setText("已打开: " + file.getAbsolutePath());
            } catch (IOException e) {
                showErrorDialog("文件打开失败", e.getMessage());
            }
        }
    }



    @FXML
    private void handleSave() {
        if (currentFile != null) {
            saveToFile(currentFile);
        } else {
            handleSaveAs();
        }
    }

    @FXML
    private void handleSaveAs() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("另存为");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Java Files", "*.java")
        );

        File file = fileChooser.showSaveDialog(getStage());
        if (file != null) {
            saveToFile(file);
            currentFile = file;
        }
    }

    @FXML
    private void handleExit() {
        // 退出前确认保存
        if (confirmExit()) {
            Platform.exit();
        }
    }
    // endregion

    // region 编辑操作
    @FXML
    private void handleUndo() {
        getCurrentTextArea().undo();
    }

    @FXML
    private void handleCut() {
        TextArea textArea = getCurrentTextArea();
        copyToClipboard(textArea.getSelectedText());
        textArea.replaceSelection("");
    }

    @FXML
    private void handleCopy() {
        copyToClipboard(getCurrentTextArea().getSelectedText());
    }

    @FXML
    private void handlePaste() {
        String clipboardText = (String) clipboard.getContent(DataFormat.PLAIN_TEXT);
        getCurrentTextArea().replaceSelection(clipboardText != null ? clipboardText : "");
    }

    @FXML
    private void handleDelete() {
        getCurrentTextArea().replaceSelection("");
    }
    // endregion

    // region 编译流程
    @FXML

  private void handleLexicalAnalysis() {
        String code = getCurrentTextArea().getText();
        errorTable.getItems().clear(); // 清空旧错误

        // 创建一个 Task 来执行词法分析（同时返回 Tokens 和 Errors）
        Task<LexerResult> task = new Task<>() {
            @Override
            protected LexerResult call() throws Exception {
                Lexer lexer = new Lexer();
                try {
                    List<Token> tokens = lexer.analyze(code);
                    return new LexerResult(tokens, lexer.getErrors());
                } catch (LexerException e) {
                    // 即使有异常，也返回已解析的 Tokens 和错误
                    return new LexerResult(e.getTokens(), e.getErrors());
                }
            }
        };

        // 任务完成后的处理
        task.setOnSucceeded(event -> {
            LexerResult result = task.getValue();
            if (result != null) {
                // 1. 显示词法分析结果
                showLexResultWindow(result.tokens);

                // 2. 显示错误信息
                if (!result.errors.isEmpty()) {
                    errorTable.getItems().addAll(result.errors);
                    statusLabel.setText("词法分析完成，但发现 " + result.errors.size() + " 个错误");
                } else {
                    statusLabel.setText("词法分析完成");
                }
            }
        });

        // 任务失败处理（如系统级错误）
        task.setOnFailed(event -> {
            statusLabel.setText("词法分析失败");
            errorTable.getItems().add(new ErrorEntry(-1, "系统错误", "无法完成词法分析"));
        });

        // 启动任务
        new Thread(task).start();
    }

/*  private void handleLexicalAnalysis() {
        String code = getCurrentTextArea().getText();
        errorTable.getItems().clear();

        Task<JavaCCLexerAdapter.LexerResult> task = new Task<>() {
            @Override
            protected JavaCCLexerAdapter.LexerResult call() {
                JavaCCLexerAdapter lexer = new JavaCCLexerAdapter();
                return lexer.analyze(code);
            }
        };

        task.setOnSucceeded(event -> {
            JavaCCLexerAdapter.LexerResult result = task.getValue();
            showLexResultWindow(result.tokens);

            if (!result.errors.isEmpty()) {
                errorTable.getItems().addAll(result.errors);
                statusLabel.setText("发现 " + result.errors.size() + " 个词法错误");
            } else {
                statusLabel.setText("词法分析完成");
            }
        });

        task.setOnFailed(event -> {
            statusLabel.setText("分析失败: " + event.getSource().getException().getMessage());
            errorTable.getItems().add(new ErrorEntry(-1, "SYSTEM", "分析过程异常"));
        });

        new Thread(task).start();
    }*/




    //--- 辅助类：封装词法分析结果和错误 ---//
    private static class LexerResult {
        final List<Token> tokens;
        final List<ErrorEntry> errors;

        LexerResult(List<Token> tokens, List<ErrorEntry> errors) {
            this.tokens = tokens;
            this.errors = errors;
        }
    }

    @FXML
    private void generateAST() {
        String code = getCurrentTextArea().getText();
        errorTable.getItems().clear();
        statusLabel.setText("正在执行语法分析...");

        Task<ParseResult> task = new Task<>() {
            @Override
            protected ParseResult call() throws Exception {
                try {
                    // 词法分析
                    Lexer lexer = new Lexer();
                    List<Token> tokens = lexer.analyze(code);

                    // 语法分析
                    JavaParser parser = new JavaParser(tokens);
                    ASTNode root = parser.parse();

                    return new ParseResult(root, parser.getErrors());
                } catch (Exception e) {
                    List<ErrorEntry> errors = new ArrayList<>();
                    errors.add(new ErrorEntry(-1, "系统错误", "语法分析失败: " + e.getMessage()));
                    return new ParseResult(null, errors);
                }
            }
        };

        task.setOnSucceeded(event -> {
            ParseResult result = task.getValue();
            if (!result.errors.isEmpty()) {
                errorTable.getItems().addAll(result.errors);
                statusLabel.setText("语法分析完成，但发现 " + result.errors.size() + " 个错误");
            } else {
                statusLabel.setText("语法分析完成");
            }
            if (result.astRoot != null) {
                showASTWindow(result.astRoot);
            }
        });

        task.setOnFailed(event -> {
            Throwable ex = event.getSource().getException();
            statusLabel.setText("语法分析失败: " + ex.getMessage());
            errorTable.getItems().add(new ErrorEntry(-1, "系统错误", ex.getMessage()));
        });

        new Thread(task).start();
    }

    private void showASTWindow(ASTNode root) {
        // 创建根树节点
        TreeItem<String> rootItem = createTreeItem(root, 0);
        // 创建 TreeView 并设置根节点
        TreeView<String> treeView = new TreeView<>(rootItem);
        // 设置 TreeView 展开根节点
        rootItem.setExpanded(true);

        // 创建场景
        Scene scene = new Scene(treeView, 400, 400);
        // 创建舞台
        Stage stage = new Stage();
        // 设置舞台标题
        stage.setTitle("抽象语法树 (AST)");
        // 设置舞台场景
        stage.setScene(scene);
        // 显示舞台
        stage.show();
    }

    private TreeItem<String> createTreeItem(ASTNode node, int indent) {
        // 根据缩进生成空格字符串
        String indentStr = " ".repeat(indent);
        // 创建树节点，使用节点的 toString 方法生成显示文本
        TreeItem<String> item = new TreeItem<>(indentStr + node.toString(indent));
        // 遍历节点的子节点
        for (int i = 0; i < node.getChildCount(); i++) {
            // 递归创建子节点的树节点
            TreeItem<String> childItem = createTreeItem(node.getChild(i), indent + 2);
            // 将子节点的树节点添加到当前节点的树节点中
            item.getChildren().add(childItem);
        }
        return item;
    }

    @FXML
    private void showSymbolTable() {
        String code = getCurrentTextArea().getText();
        errorTable.getItems().clear();
        statusLabel.setText("正在获取符号表...");

        javafx.concurrent.Task<Map<String, SymbolType>> task = new javafx.concurrent.Task<>() {
            @Override
            protected Map<String, SymbolType> call() throws Exception {
                try {
                    // 词法分析
                    Lexer lexer = new Lexer();
                    java.util.List<Token> tokens = lexer.analyze(code);

                    // 语法分析
                    JavaParser parser = new JavaParser(tokens);
                    parser.parse();

                    return parser.getSymbols();
                } catch (Exception e) {
                    List<ErrorEntry> errors = new ArrayList<>();
                    errors.add(new ErrorEntry(-1, "系统错误", "获取符号表失败: " + e.getMessage()));
                    errorTable.getItems().addAll(errors);
                    return null;
                }
            }
        };

        task.setOnSucceeded(event -> {
            Map<String, SymbolType> symbols = task.getValue();
            if (symbols != null) {
                showSymbolTableWindow(symbols);
                statusLabel.setText("符号表已展示");
            } else {
                statusLabel.setText("获取符号表失败");
            }
        });

        task.setOnFailed(event -> {
            Throwable ex = event.getSource().getException();
            statusLabel.setText("获取符号表失败: " + ex.getMessage());
            errorTable.getItems().add(new ErrorEntry(-1, "系统错误", ex.getMessage()));
        });

        new Thread(task).start();
    }

    private void showSymbolTableWindow(Map<String, SymbolType> symbols) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("symbol-table.fxml"));
            Parent root = loader.load();
            SymbolTableController controller = loader.getController();
            controller.setSymbols(symbols);

            Stage stage = new Stage();
            stage.setTitle("符号表");
            stage.setScene(new Scene(root, 600, 400));
            stage.show();
        } catch (IOException e) {
            showErrorDialog("窗口打开失败", e.getMessage());
        }
    }

    @FXML
    private void generateIR() {
        String code = getCurrentTextArea().getText();
        errorTable.getItems().clear();
        statusLabel.setText("正在执行中间代码生成...");

        javafx.concurrent.Task<List<Quadruple>>task = new javafx.concurrent.Task<>() {
            @Override
            protected List<Quadruple> call() throws Exception {
                try {
                    // 词法分析
                    Lexer lexer = new Lexer();
                    java.util.List<Token> tokens = lexer.analyze(code);

                    // 语法分析
                    JavaParser parser = new JavaParser(tokens);
                    parser.parse();

                    return parser.getQuadruples();
                } catch (Exception e) {
                    List<ErrorEntry> errors = new ArrayList<>();
                    errors.add(new ErrorEntry(-1, "系统错误", "中间代码生成失败: " + e.getMessage()));
                    errorTable.getItems().addAll(errors);
                    return null;
                }
            }
        };

        task.setOnSucceeded(event -> {
            List<Quadruple> quadruples = task.getValue();
            if (quadruples != null) {
                // 打印四元式到控制台
                int index = 0;
                for (Quadruple quad : quadruples) {
                    System.out.printf("%d: (%s, %s, %s, %s)%n",
                            index++,
                            quad.getOp(),
                            quad.getArg1(),
                            quad.getArg2(),
                            quad.getResult());
                }

                showIRResultWindow(quadruples);
                statusLabel.setText("中间代码生成完成");
            } else {
                statusLabel.setText("中间代码生成失败");
            }
        });

        task.setOnFailed(event -> {
            Throwable ex = event.getSource().getException();
            statusLabel.setText("中间代码生成失败: " + ex.getMessage());
            errorTable.getItems().add(new ErrorEntry(-1, "系统错误", ex.getMessage()));
        });

        new Thread(task).start();
    }

    private void showIRResultWindow(List<Quadruple> quadruples) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ir-result.fxml"));
            Parent root = loader.load();
            IRResultController controller = loader.getController();
            controller.setQuadruples(quadruples); // 现在传入的是 List<Quadruple>

            Stage stage = new Stage();
            stage.setTitle("中间代码（四元式）");
            stage.setScene(new Scene(root, 600, 400));
            stage.show();
        } catch (IOException e) {
            showErrorDialog("窗口打开失败", e.getMessage());
        }
    }


    @FXML
    private void generateAssembly() {
        analyzeCode("目标代码生成");
    }
    // endregion

    // region 帮助系统
    @FXML
    private void showUserManual() {
        HostServices hostServices = HostServicesProvider.getHostServices();
        if (hostServices != null) {
            hostServices.showDocument("https://example.com/manual");
        }
    }

    @FXML
    private void checkUpdates() {
        statusLabel.setText("正在检查更新...");
        // 实际更新检查逻辑
    }

    @FXML
    private void showAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("关于");
        alert.setHeaderText("JavaFX 编译器工具");
        alert.setContentText("版本 1.0\n作者: Your Name");
        alert.showAndWait();
    }
    // endregion

    // region 私有辅助方法
    private void createNewTab(String title, String content) {
        Tab newTab = new Tab(title);
        TextArea textArea = new TextArea(content);
        textArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 12pt;");

        // 添加关闭确认
        newTab.setOnCloseRequest(event -> {
            if (!confirmCloseTab(newTab)) {
                event.consume(); // 取消关闭
            }
        });

        newTab.setContent(textArea);
        codeTabs.getTabs().add(newTab);
        codeTabs.getSelectionModel().select(newTab);
    }

    private void saveToFile(File file) {
        try {
            String content = getCurrentTextArea().getText();
            Files.writeString(file.toPath(), content);
            statusLabel.setText("已保存到: " + file.getAbsolutePath());
            // 更新标签标题
            Tab currentTab = codeTabs.getSelectionModel().getSelectedItem();
            currentTab.setText(file.getName());
        } catch (IOException e) {
            showErrorDialog("保存失败", e.getMessage());
        }
    }

    private void copyToClipboard(String text) {
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);
    }

    private void analyzeCode(String analysisType) {
        String code = getCurrentTextArea().getText();
        statusLabel.setText("正在执行 " + analysisType);

        // 模拟错误显示
        errorTable.getItems().add(new ErrorEntry(1, "错误", analysisType + " 示例错误"));
    }

    private TextArea getCurrentTextArea() {
        Tab selectedTab = codeTabs.getSelectionModel().getSelectedItem();
        if (selectedTab != null && selectedTab.getContent() instanceof TextArea) {
            return (TextArea) selectedTab.getContent();
        }
        return new TextArea(); // 返回空文本域防止NPE
    }

    private Stage getStage() {
        return (Stage) codeTabs.getScene().getWindow();
    }

    private void showErrorDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private boolean confirmExit() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("退出确认");
        alert.setHeaderText("是否保存当前更改？");
        alert.setContentText("未保存的修改将会丢失");

        ButtonType saveButton = new ButtonType("保存并退出");
        ButtonType exitButton = new ButtonType("直接退出");
        ButtonType cancelButton = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(saveButton, exitButton, cancelButton);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == saveButton) {
                handleSave();
                return true;
            } else if (result.get() == exitButton) {
                return true;
            }
        }
        return false;
    }

    private boolean confirmCloseTab(Tab tab) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("关闭确认");
        alert.setHeaderText("是否保存对 " + tab.getText() + " 的更改？");

        ButtonType saveButton = new ButtonType("保存");
        ButtonType discardButton = new ButtonType("不保存");
        ButtonType cancelButton = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(saveButton, discardButton, cancelButton);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == saveButton) {
                handleSave();
                return true;
            } else if (result.get() == discardButton) {
                return true;
            }
        }
        return false;
    }

    private void setupProjectTree() {
        TreeItem<String> root = new TreeItem<>("项目根目录");
        root.getChildren().addAll(
                new TreeItem<>("src"),
                new TreeItem<>("libs"),
                new TreeItem<>("docs")
        );
        root.setExpanded(true);
        projectTree.setRoot(root);
        projectTree.setShowRoot(true);
    }

    private void setupErrorTable() {
        // 配置列与数据模型的绑定
        TableColumn<ErrorEntry, Number> lineColumn = new TableColumn<>("行号");
        lineColumn.setCellValueFactory(cellData -> cellData.getValue().lineNumberProperty());

        TableColumn<ErrorEntry, String> typeColumn = new TableColumn<>("类型");
        typeColumn.setCellValueFactory(cellData -> cellData.getValue().errorTypeProperty());

        TableColumn<ErrorEntry, String> descColumn = new TableColumn<>("描述");
        descColumn.setCellValueFactory(cellData -> cellData.getValue().descriptionProperty());

        // 添加列到表格
        errorTable.getColumns().setAll(lineColumn, typeColumn, descColumn);

        // 行高亮逻辑
        errorTable.setRowFactory(tv -> new TableRow<ErrorEntry>() {
            @Override
            protected void updateItem(ErrorEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle(""); // 清空样式
                } else {
                    // 根据错误类型设置背景色
                    if ("错误".equals(item.getErrorType())) {
                        setStyle("-fx-background-color: #ffcccc;"); // 红色背景
                    } else {
                        setStyle(""); // 默认样式
                    }
                }
            }
        });
    }

    private void showLexResultWindow(List<Token> tokens) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("lex-result.fxml"));
            Parent root = loader.load();
            LexResultController controller = loader.getController();
            controller.setTokens(tokens);

            Stage stage = new Stage();
            stage.setTitle("词法分析结果");
            stage.setScene(new Scene(root, 400, 300));
            stage.show();
        } catch (IOException e) {
            showErrorDialog("窗口打开失败", e.getMessage());
        }
    }
    // endregion
}
