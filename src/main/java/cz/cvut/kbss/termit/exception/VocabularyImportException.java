package cz.cvut.kbss.termit.exception;

/**
 * Indicates that an error occurred in the vocabulary import relationship.
 */
public class VocabularyImportException extends TermItException {

    private final String messageId;

    public VocabularyImportException(String message) {
        super(message);
        this.messageId = null;
    }

    public VocabularyImportException(String message, String messageId) {
        super(message);
        this.messageId = messageId;
    }

    public String getMessageId() {
        return messageId;
    }
}
