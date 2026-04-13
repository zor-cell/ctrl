package at.ac.tuwien.inso.ctrl.exception;

public class ResourceAlreadyExistsException extends RuntimeException {
    /**
     * Constructs a new {@code ResourceAlreadyExistsException} with a standardized,
     * formatted error message indicating exactly which field caused the conflict.
     * <p>
     * <b>Example usage:</b>
     * <pre>
     * throw new ResourceAlreadyExistsException("User", "username", "john_doe");
     * </pre>
     */
    public ResourceAlreadyExistsException(String resourceName, String fieldName, String fieldValue) {
        super(String.format("%s with %s '%s' already exists.", resourceName, fieldName, fieldValue));
    }
}
