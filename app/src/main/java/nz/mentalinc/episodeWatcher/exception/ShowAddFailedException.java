package nz.mentalinc.episodeWatcher.exception;

/**
 * @author Dirk Vranckaert, maintained and updated by mentalinc
 *
 */
public class ShowAddFailedException extends Exception {
    private static final long serialVersionUID = 3656857588312135601L;

    public ShowAddFailedException(String message, Throwable e) {
        super(message, e);
    }

    public ShowAddFailedException(String message) {
        super(message);
    }
}
