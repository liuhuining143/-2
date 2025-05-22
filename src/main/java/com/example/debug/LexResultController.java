package com.example.debug;

import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import java.util.List;

public class LexResultController {
    @FXML private TableView<Token> tokenTable;
    @FXML private TableColumn<Token, Integer> codeColumn;
    @FXML private TableColumn<Token, String> typeColumn;
    @FXML private TableColumn<Token, String> valueColumn;
    @FXML private TableColumn<Token, String> lineColumn;

    public void initialize() {
        // 绑定列数据
        codeColumn.setCellValueFactory(new PropertyValueFactory<>("code"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        valueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
        lineColumn.setCellValueFactory(new PropertyValueFactory<>("line"));
    }

    public void setTokens(List<Token> tokens) {
        tokenTable.getItems().setAll(tokens);
    }
}