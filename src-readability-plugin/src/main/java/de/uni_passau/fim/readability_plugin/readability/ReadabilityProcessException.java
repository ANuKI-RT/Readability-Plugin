package de.uni_passau.fim.readability_plugin.readability;

/**
 * This exception is thrown if a readability model failed to generate a readability
 * rating for a given code snippet
 */
public class ReadabilityProcessException extends Exception {

    private final Exception causedBy;
    private final String filePath;

    private final boolean processedRecursive;

    /**
     * Create a new exception object
     * @param message the error message to add
     * @param causedBy the exception that caused the ReadabilityProcessException
     * @param filePath the file path of the java file that included the code that could not be rated
     */
    ReadabilityProcessException(String message, Exception causedBy,String filePath) {
        this(message,causedBy,filePath,false);
    }

    /**
     * !!! THIS CONSTRUCTOR IS OUT-DATED !!!
     *
     * In older version of the plugin the exception took a "processedRecursive" parameter.
     * This value however is not required at all.
     *
     * TODO: remove the processedRecursive parameter
     * @param message
     * @param causedBy
     * @param filePath
     * @param processedRecursive
     */
    ReadabilityProcessException(String message, Exception causedBy,String filePath,boolean processedRecursive) {
        super(message);
        this.causedBy = causedBy;
        this.filePath = filePath;
        this.processedRecursive = processedRecursive;
    }

    /**
     * Get the exception that caused the ReadabilityProcessException
     * @return the causing exception
     */
    public Exception getCausingException() {
        return causedBy;
    }

    /**
     * Get the file path of the java file that included the code that could not be rated.
     * @return the file path
     */
    public String getProcessedPath() {
        return filePath;
    }

    /**
     * !!! THIS METHOD IS OUT-DATED !!!
     *
     *
     * In older version of the plugin the exception took a "processedRecursive" parameter.
     * This value however is not required at all.
     *
     * TODO: remove the processedRecursive parameter
     *
     * @return the value for "processedRecursive"
     */
    public boolean processedRecursive() {
        return processedRecursive;
    }

}
