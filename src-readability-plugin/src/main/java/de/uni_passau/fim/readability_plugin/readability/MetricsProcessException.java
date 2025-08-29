package de.uni_passau.fim.readability_plugin.readability;

/**
 * This exception is thrown if the readability model failed to extract metrics from a given code snippet
 */
public class MetricsProcessException extends Exception {


    private final ReadabilityResult resultContext;

    private final Exception causedBy;

    /**
     * To create a MetricsProcessException a corresponding readability result and an exception that caused the
     * MetricsProcessException th be thrown is required next to the error message.
     * @param message the error message
     * @param resultContext the readability result the metrics should have been added to
     * @param causedBy The exception that caused the MetricsProcessException
     */
    MetricsProcessException(String message, ReadabilityResult resultContext, Exception causedBy) {
        super(message);
        this.resultContext = resultContext;
        this.causedBy = causedBy;
    }

    /**
     * The readability result the metrics should have been added to
     * @return the result object
     */
    public ReadabilityResult getResultContext() {
        return resultContext;
    }

    /**
     * The exception that caused the MetricsProcessException
     * @return the causing exception object
     */
    public Exception getCausingException() {
        return causedBy;
    }
}
