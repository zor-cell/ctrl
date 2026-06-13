package at.ac.tuwien.inso.ctrl.unitTests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import at.ac.tuwien.inso.ctrl.config.websocket.NoCompressionHandshakeHandler;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketExtension;

class NoCompressionHandshakeHandlerUnitTest {

  @Test
  void filterRequestedExtensions_returnsEmptyList() {
    ExposingHandler handler = new ExposingHandler();

    assertTrue(handler.filterExtensions(null, List.of(), List.of()).isEmpty());
  }

  private static final class ExposingHandler extends NoCompressionHandshakeHandler {
    List<WebSocketExtension> filterExtensions(
        ServerHttpRequest request,
        List<WebSocketExtension> requested,
        List<WebSocketExtension> supported) {
      return filterRequestedExtensions(request, requested, supported);
    }
  }
}
