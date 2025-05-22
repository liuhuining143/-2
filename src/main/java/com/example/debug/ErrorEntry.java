package com.example.debug;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ErrorEntry {
    private final IntegerProperty lineNumber = new SimpleIntegerProperty();
    private final StringProperty errorType = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();

    public ErrorEntry(int lineNumber, String errorType, String description) {
        setLineNumber(lineNumber);
        setErrorType(errorType);
        setDescription(description);
    }

    // Property访问器
    public IntegerProperty lineNumberProperty() { return lineNumber; }
    public StringProperty errorTypeProperty() { return errorType; }
    public StringProperty descriptionProperty() { return description; }

    // 常规getter/setter
    public int getLineNumber() { return lineNumber.get(); }
    public void setLineNumber(int value) { lineNumber.set(value); }

    public String getErrorType() { return errorType.get(); }
    public void setErrorType(String value) { errorType.set(value); }

    public String getDescription() { return description.get(); }
    public void setDescription(String value) { description.set(value); }
}
