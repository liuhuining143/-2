package com.example.debug;

import com.example.debug.Symbol.SymbolType;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;

import java.util.Map;

public class SymbolTableController {
    @FXML
    private TableView<SymbolType> symbolTableView;

    public void setSymbols(Map<String, SymbolType> symbols) {
        ObservableList<SymbolType> symbolList = FXCollections.observableArrayList(symbols.values());
        symbolTableView.setItems(symbolList);
    }
}

