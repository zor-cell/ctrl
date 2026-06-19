package at.ac.tuwien.inso.ctrl.exception;

public class UnauthorizedException extends RuntimeException {
  public UnauthorizedException(String message) {
    super("Unauthorized to access resource " + message);
  }
}
