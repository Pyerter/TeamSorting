package pyerter.squirrel;

public class TeamSorterInputReadingException extends Exception {

    protected Exception child;

    public TeamSorterInputReadingException(String message, Exception child) {
        super(message);
        this.child = child;
    }

    public boolean hasChildException() {
        return child != null;
    }

    public Exception getChildException() {
        return child;
    }
}
