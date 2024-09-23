package pyerter.squirrel;

import java.util.Map;

public class MatrixView <T> {

    protected Matrix<T> mat;
    protected Map<Integer, Integer> rowMap;
    protected Map<Integer, Integer> colMap;

    public MatrixView(Matrix<T> mat) {
        this.mat = mat;
        rowMap = null;
        colMap = null;
    }

    public T[] get(int row) {
        return rowMap != null ? mat.get(rowMap.get(row)) : mat.get(row);
    }

    public T get(int row, int col) {
        if (rowMap != null) row = rowMap.get(row);
        if (colMap != null) col = colMap.get(col);
        return mat.get(row, col);
    }

    public int getNumbRows() {
        return rowMap != null ? rowMap.size() : mat.getNumbRows();
    }

    public int getNumbCols() {
        return colMap != null ? colMap.size() : mat.getNumbCols();
    }

}
