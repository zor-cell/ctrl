package at.ac.tuwien.inso.ctrl.exception;

public class InvalidEmailVerificationCodeException extends RuntimeException {
  public InvalidEmailVerificationCodeException(String message) {
    super(message);
  }
}
