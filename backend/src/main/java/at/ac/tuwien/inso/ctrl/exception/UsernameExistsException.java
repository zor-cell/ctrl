package at.ac.tuwien.inso.ctrl.exception;

public class UsernameExistsException extends RuntimeException {
    public UsernameExistsException(String message) {
        super(message);
    }
}
