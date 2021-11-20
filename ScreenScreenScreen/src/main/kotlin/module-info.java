module demo {
    requires javafx.controls;
    requires javafx.fxml;
    requires kotlin.stdlib;
    requires java.desktop;
    requires javafx.swing;


    opens demo to javafx.fxml;
    exports demo;
}