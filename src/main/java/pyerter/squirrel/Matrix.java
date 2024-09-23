package pyerter.squirrel;

public class Matrix <T> {

    protected T[][] mat;
    protected int numbRows;
    protected int numbCols;
    protected boolean jagged = false;

    public Matrix(T[][] mat) {
        this.mat = mat;
        numbRows = mat.length;
        numbCols = mat.length > 0 ? mat[0].length : 0;
        for (int i = 0; i < mat.length; i++) {
            if (mat[i].length != numbCols) {
                jagged = true;
                return;
            }
        }
    }

    public T[][] mat() {
        return mat;
    }

    public T[] get(int row) {
        return mat[row];
    }

    public T get(int row, int col) {
        return mat[row][col];
    }

    public int getNumbRows() {
        return numbRows;
    }

    public int getNumbCols() {
        return numbCols;
    }

    public boolean isJagged() {
        return jagged;
    }

}
