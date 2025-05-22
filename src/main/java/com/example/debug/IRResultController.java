package com.example.debug;

import com.example.debug.Symbol.Quadruple;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;

import java.util.List;
import java.util.Map;

public class IRResultController {
    @FXML
    private TableView<Quadruple> irTable;

    public void setQuadruples(List<Quadruple> quadruples) {
        irTable.getItems().addAll(quadruples);
    }
}