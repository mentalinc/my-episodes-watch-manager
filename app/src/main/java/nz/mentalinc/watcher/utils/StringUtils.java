package nz.mentalinc.watcher.utils;

/**
 * @author Dirk Vranckaert, maintained and updated by mentalinc
 */
public class StringUtils {
    public static final String EMPTY = "";

    /**
     * Search what the start-index of a certain search value in a source string is.
     *
     * @param source The string to search in.
     * @param search The string to search for.
     * @return The index on which the search value has been found first. So for multiple ocurences only the result will
     * be the index of the first occurence in the source. Returns -1 if the search value hasn't been found!
     */
    public static int indexOf(String source, String search) {
        int result = -1;
        if (source.startsWith(search)) {
            result = 0;
        } else {
            String[] split = source.split(search);
            if (split.length > 0) {
                result = split[0].length();
            }
        }
        return result;
    }
}
