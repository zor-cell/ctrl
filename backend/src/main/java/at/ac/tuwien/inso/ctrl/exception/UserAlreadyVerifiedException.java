package at.ac.tuwien.inso.ctrl.exception;

public class UserAlreadyVerifiedException extends RuntimeException {
  public UserAlreadyVerifiedException(String message) {
    super(message);
  }
}
