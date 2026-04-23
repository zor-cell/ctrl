package at.ac.tuwien.inso.ctrl.exception;

public class EmailNotVerifiedException extends RuntimeException {
  public EmailNotVerifiedException(String message) {
    super(message);
  }
}
