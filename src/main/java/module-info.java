module com.example.debug {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires jdk.incubator.vector;
    requires ant;

    opens com.example.debug to javafx.fxml;
    exports com.example.debug;
    exports com.example.debug.Symbol;
    opens com.example.debug.Symbol to javafx.fxml;
}