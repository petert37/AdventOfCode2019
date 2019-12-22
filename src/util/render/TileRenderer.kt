package util.render

import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.WindowConstants
import kotlin.math.min
import kotlin.math.roundToInt

abstract class TileRenderer<T>(protected val backgroundColor: Color = Color.BLACK) : JPanel() {

    protected var tiles: Map<Pair<Long, Long>, T> = emptyMap()
    protected var tileSize = 0.0

    override fun paint(g: Graphics?) {
        super.paint(g)
        if (graphics == null)
            return
        g as Graphics2D
        renderGame(g)
    }

    fun render(tiles: Map<Pair<Long, Long>, T>) {
        this.tiles = tiles
        repaint()
    }

    protected open fun renderGame(g: Graphics2D) {
        g.color = backgroundColor
        g.fillRect(0, 0, width, height)

        val minX = tiles.keys.minBy { it.first }?.first ?: 0
        val maxX = tiles.keys.maxBy { it.first }?.first ?: 0
        val minY = tiles.keys.minBy { it.second }?.second ?: 0
        val maxY = tiles.keys.maxBy { it.second }?.second ?: 0
        val xTiles = maxX - minX + 1
        val yTiles = maxY - minY + 1
        val xSize = width / if (xTiles == 0L) 1.0 else xTiles.toDouble()
        val ySize = height / if (yTiles == 0L) 1.0 else yTiles.toDouble()
        tileSize = min(xSize, ySize)

        for (y in minY..maxY) {
            for (x in minX..maxX) {
                renderTile(g, tiles[x to y], x, y, tileSize)
            }
        }

    }

    protected abstract fun renderTile(graphics: Graphics2D, tile: T?, x: Long, y: Long, tileSize: Double)

    protected fun drawRect(graphics: Graphics2D, color: Color, x: Long, y: Long, tileSize: Double? = null) {
        graphics.color = color
        val size = tileSize ?: this.tileSize
        graphics.fillRect((x * size).roundToInt(), (y * size).roundToInt(), size.roundToInt(), size.roundToInt())
    }

    protected fun drawCircle(graphics: Graphics2D, color: Color, x: Long, y: Long, tileSize: Double? = null) {
        graphics.color = color
        val size = tileSize ?: this.tileSize
        graphics.fillOval((x * size).roundToInt(), (y * size).roundToInt(), size.roundToInt(), size.roundToInt())
    }

}

fun <C : JPanel> openTileRendererWindow(title: String, canvas: C, width: Int = 800, height: Int = 600): C {
    val frame = JFrame(title)
    frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    frame.add(canvas)
    frame.size = Dimension(width, height)
    frame.setLocationRelativeTo(null)
    frame.isVisible = true
    return canvas
}