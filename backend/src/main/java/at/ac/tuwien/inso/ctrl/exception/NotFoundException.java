package at.ac.tuwien.inso.ctrl.exception;

public class NotFoundException extends RuntimeException {
  /**
   * Constructs a new {@code NotFoundException} with a standardized, formatted error message
   * indicating which resource was not found by which identifier.
   *
   * <p><b>Example usage:</b>
   *
   * <pre>
   * throw new NotFoundException("User", "john_doe");
   * </pre>
   */
  public NotFoundException(String resourceName, String identifier) {
    super(String.format("%s with identifier '%s' not found.", resourceName, identifier));
  }
}
