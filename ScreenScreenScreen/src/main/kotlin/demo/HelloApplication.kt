package demo

import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage
import javafx.fxml.FXMLLoader
import javafx.scene.Parent


class ScreenScreen : Application() {
    override fun start(stage: Stage) {
        val fxmlLoader = FXMLLoader(ScreenScreen::class.java.getResource("hello-view.fxml"))
        val layout: Parent = fxmlLoader.load();
        fxmlLoader.getController<HelloController>().stage = stage
        val scene = Scene(layout)
        stage.title = "Cheese!"
        stage.scene = scene
        stage.show()
    }
}

fun main() {
    Application.launch(ScreenScreen::class.java)
}