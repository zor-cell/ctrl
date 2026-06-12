package at.ac.tuwien.inso.ctrl.exception;

public class SessionLimitExceededException extends RuntimeException {
  public SessionLimitExceededException(String message) {
    super(message);
  }
}
