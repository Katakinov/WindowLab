package demo

import javafx.embed.swing.SwingFXUtils
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.geometry.Rectangle2D
import javafx.scene.SnapshotParameters
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.CheckBox
import javafx.scene.control.ColorPicker
import javafx.scene.control.ScrollPane
import javafx.scene.control.Slider
import javafx.scene.image.WritableImage
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Color
import javafx.scene.robot.Robot
import javafx.stage.FileChooser
import javafx.stage.Screen
import javafx.stage.Stage
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import javax.imageio.ImageIO

class HelloController {

    //задержка
    @FXML
    private lateinit var pingSlider: Slider

    //скрыть
    @FXML
    private lateinit var hideCheckbox: CheckBox

    //размер кисти
    @FXML
    private lateinit var brushSizeSlider: Slider

    //выбор цвета
    @FXML
    private lateinit var brushColorPicker: ColorPicker

    //окно
    @FXML
    private lateinit var scroll: ScrollPane;

    //canvas
    @FXML
    private lateinit var  drawCanvas: Canvas
    @FXML
    private lateinit var cutCanvas: Canvas
    @FXML
    private lateinit var imgCanvas: Canvas

    private lateinit var drawContext: GraphicsContext
    private lateinit var imgContext: GraphicsContext
    private lateinit var cutContext: GraphicsContext

    var stage: Stage? = null
    private var waiting: Long = 2
    private var brushSize = 5.0
    private var cutX = 0.0
    private var cutY = 0.0
    private var cutWidth = 0
    private var cutHeight = 0

    private var onMouseDrawEvent = EventHandler<MouseEvent> { event ->
        val x = event.x - brushSize / 2
        val y = event.y - brushSize / 2

        if (event.button == MouseButton.PRIMARY) {
            drawContext.fillRect(x, y, brushSize, brushSize)
        } else if (event.button == MouseButton.SECONDARY) {
            drawContext.clearRect(x, y, brushSize, brushSize)
        }
    }

    private var cropOnMousePressed = EventHandler<MouseEvent> { event ->
        cutX = event.x
        cutY = event.y
    }

    private var cropOnMouseDragged = EventHandler<MouseEvent> { event ->
        cutContext.fill = Color.BLACK
        cutContext.fillRect(0.0, 0.0, cutCanvas.width, cutCanvas.height)
        cutContext.fill = Color.WHITE
        cutContext.fillRect(cutX, cutY, event.x - cutX, event.y - cutY)
    }

    fun initialize() {
        drawContext = drawCanvas.graphicsContext2D
        drawCanvas.onMouseDragged = onMouseDrawEvent
        drawCanvas.onMouseClicked = onMouseDrawEvent

        cutCanvas.onMousePressed = cropOnMousePressed
        cutCanvas.onMouseDragged = cropOnMouseDragged
        cutCanvas.onMouseReleased = cropOnMouseReleased
        cutContext = cutCanvas.graphicsContext2D

        imgContext = imgCanvas.graphicsContext2D
        imgContext.isImageSmoothing = false

        pingSlider.setOnMouseReleased {
            waiting = pingSlider.value.toLong()
        }
        brushSizeSlider.setOnMouseReleased {
            brushSize = brushSizeSlider.value
        }
        brushColorPicker.setOnAction {
            drawContext.fill = brushColorPicker.value
        }

        if (!File(System.getenv("USERPROFILE") + "\\Documents\\Screenshoter\\" + "path.txt").exists()) {
            File(System.getenv("USERPROFILE") + "\\Documents\\Screenshoter").mkdir()
            File(System.getenv("USERPROFILE") + "\\Documents\\Screenshoter\\" + "path.txt").createNewFile()
        }
    }

    @FXML
    private fun screenshotButtonPressed() {
        takeAScreenshot()
    }

    private fun takeAScreenshot() {
        if (hideCheckbox.isSelected) {
            stage?.isIconified = true
        }

        Thread.sleep(300 + waiting * 1000)
        val screenSize = Screen.getPrimary().bounds
        val rectangle = Rectangle2D(0.0, 0.0, screenSize.width, screenSize.height)
        val image = Robot().getScreenCapture(null, rectangle)
        resize(image.width, image.height)
        drawContext.clearRect(0.0, 0.0, drawCanvas.width, drawCanvas.height)
        imgContext.drawImage(image, 0.0, 0.0)
        stage?.isIconified = false
    }

    private fun saveDirectory(path: String) {
        val i = path.lastIndexOf('\\')
        val finalPath = path.substring(0, i + 1);
        File(System.getenv("USERPROFILE") + "\\Documents\\Screenshoter\\" + "path.txt").writeText(finalPath)
    }

    @FXML
    private fun onKeyPressed(event: KeyEvent) {
        if (event.isControlDown && event.code == KeyCode.S && event.isShiftDown) {
            saveImage(false)
        } else if (event.isControlDown && event.code == KeyCode.S) {
            saveImage(true)
        } else if (event.isControlDown && event.code == KeyCode.O) {
            takeAScreenshot()
        }
    }

    private fun resize(width: Double, height: Double) {
        imgCanvas.width = width
        imgCanvas.height = height
        cutCanvas.width = width
        cutCanvas.height = height
        scroll.prefWidth = width
        scroll.prefHeight = height + 100
        drawCanvas.width = width
        drawCanvas.height = height
    }

    private fun imposeCanvas(): WritableImage {
        val w = imgCanvas.width
        val h = imgCanvas.height
        val resultCanvas = Canvas(w, h)
        resultCanvas.graphicsContext2D.isImageSmoothing = false
        val params = SnapshotParameters()
        params.fill = Color.TRANSPARENT

        var imageBuffer = imgCanvas.snapshot(params, null)
        resultCanvas.graphicsContext2D.drawImage(imageBuffer, 0.0, 0.0)
        imageBuffer = drawCanvas.snapshot(params, null)
        resultCanvas.graphicsContext2D.drawImage(imageBuffer, 0.0, 0.0)
        imageBuffer = resultCanvas.snapshot(params, null)
        return imageBuffer
    }

    @FXML
    private fun cutButtonPressed() {
        cutCanvas.isDisable = false
        cutCanvas.opacity = 0.3
        cutContext.fillRect(0.0, 0.0, cutCanvas.width, cutCanvas.height)
    }

    private fun saveCroppedImage() {
        val image = imposeCanvas()
        val croppedImage = WritableImage(image.pixelReader, cutX.toInt(), cutY.toInt(), cutWidth, cutHeight)
        resize(cutWidth.toDouble(), cutHeight.toDouble())
        imgContext.drawImage(croppedImage, 0.0, 0.0)
    }

    private var cropOnMouseReleased = EventHandler<MouseEvent> { event ->
        cutWidth = event.x.toInt() - cutX.toInt()
        cutHeight = event.y.toInt() - cutY.toInt()

        if (cutWidth > drawCanvas.width - cutX) {
            cutWidth = drawCanvas.width.toInt() - cutX.toInt()
        }
        if (cutHeight > drawCanvas.height - cutY) {
            cutHeight = drawCanvas.height.toInt() - cutY.toInt()
        }

        cutCanvas.isDisable = true
        cutCanvas.opacity = 0.0
        saveCroppedImage()
    }

    @FXML
    private fun saveImage() {
        saveImage(false)
    }

    private fun saveImage(fastSaveKey: Boolean) {
        var directory = File(System.getenv("USERPROFILE") +
                "\\Documents\\Screenshoter\\" + "path.txt").readText()

        if (directory == "") {
            directory = ".\\"
        }

        var path = directory + LocalDateTime.now().toLocalDate() + LocalDateTime.now().second + ".png"
        // fileChooser передает сразу с .png
        var file: File? = null

        if (!fastSaveKey) {
            val fileChooser = FileChooser()
            fileChooser.initialDirectory = File(directory)
            fileChooser.extensionFilters.addAll(FileChooser.ExtensionFilter("Png image *.png", "*.png"))
            file = fileChooser.showSaveDialog(null)

            if (file != null) {
                saveDirectory(file.path)
                path = file.path
            }
        }

        if (file != null || fastSaveKey) {
            val imageBuffer = imposeCanvas()
            val imageToWrite = SwingFXUtils.fromFXImage(imageBuffer, null)
            try {
                ImageIO.write(imageToWrite, "png", File(path))
            } catch (ex: IOException) {
                print(ex)
            }
        }
    }

    @FXML
    private fun openImage() {
        val directory = File(System.getenv("USERPROFILE") + "\\Documents\\Screenshoter\\" + "path.txt").readText()
        val fileChooser = FileChooser()
        fileChooser.initialDirectory = File(directory)
        fileChooser.extensionFilters.addAll(
            FileChooser.ExtensionFilter("Png image *.png", "*.png"),
            FileChooser.ExtensionFilter("Jpg image *.jpg", "*.jpg"),
            FileChooser.ExtensionFilter("Jpeg image *.jpeg", "*.jpeg")
        )
        val file = fileChooser.showSaveDialog(null)

        if (file != null) {
            val image = SwingFXUtils.toFXImage(ImageIO.read(file), null)
            resize(image.width, image.height)
            imgContext.drawImage(image, 0.0, 0.0)
        }
    }

    @FXML
    private fun closeApp() {
        stage?.close();
    }
}