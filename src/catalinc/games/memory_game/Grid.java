package catalinc.games.memory_game;

import java.io.Serializable;

public class Grid implements Serializable {

    private final int      rows;
    private final int      cols;
    private final Tile[][] data;

    private int selectedRow;
    private int selectedCol;

    private int tileHeight;
    private int tileWidth;

    public static final Grid EMPTY = new Grid(0, 0, 0, 0);

    public Grid(int rows, int cols, int canvasHeight, int canvasWidth) {
        this.rows = rows;
        this.cols = cols;
        this.data = new Tile[rows][cols];

        this.selectedRow = -1;
        this.selectedCol = -1;

        setCanvasHeight(canvasHeight);
        setCanvasWidth(canvasWidth);
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public void setCanvasHeight(int canvasHeight) {
        if (rows > 0) {
            tileHeight = canvasHeight / rows;
        } else {
            tileHeight = 0;
        }
    }

    public void setCanvasWidth(int canvasWidth) {
        if (cols > 0) {
            tileWidth = canvasWidth / cols;
        } else {
            tileWidth = 0;
        }
    }

    public int getTileHeight() {
        return tileHeight;
    }

    public int getTileWidth() {
        return tileWidth;
    }

    public Tile getTileAt(int row, int col) {
        if ((row >= 0 && row < rows) && (col >= 0 && col < cols)) {
            return data[row][col];
        }
        return null;
    }

    public void setTileAt(Tile tile, int row, int col) {
        if ((row >= 0 && row < rows) && (col >= 0 && col < cols)) {
            tile.setRow(row);
            tile.setCol(col);
            data[row][col] = tile;
        }
    }

    public Tile getTileAtPoint(float x, float y) {
        int row = (int) y / tileHeight;
        int col = (int) x / tileWidth;
        return getTileAt(row, col);
    }

    public void selectTile(Tile tile) {
        tile.setState(Tile.STATE_SELECTED);
        this.selectedRow = tile.getRow();
        this.selectedCol = tile.getCol();
    }

    public void clearSelectedTile() {
        Tile selected = getSelectedTile();
        if (selected != null) {
            selected.setState(Tile.STATE_HIDDEN);
        }
        this.selectedRow = -1;
        this.selectedCol = -1;
    }

    public Tile getSelectedTile() {
        return getTileAt(selectedRow, selectedCol);
    }
}
