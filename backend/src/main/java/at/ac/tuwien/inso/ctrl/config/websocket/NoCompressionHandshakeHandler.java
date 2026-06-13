package at.ac.tuwien.inso.ctrl.config.websocket;

import java.util.Collections;
import java.util.List;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

/**
 * Disables {@code permessage-deflate} negotiation. Some browsers (notably Safari/WebKit on macOS)
 * mis-parse compressed frames as invalid opcodes (e.g. opCode 7) when talking to Tomcat.
 *
 * <p>Filtering the requested extensions here is not sufficient on embedded Tomcat, which always
 * appends its built-in extensions during the handshake. The {@link
 * NoDeflateWebSocketUpgradeStrategy} additionally forces an empty negotiated-extension list, which
 * is what actually disables compression.
 */
public class NoCompressionHandshakeHandler extends DefaultHandshakeHandler {

  public NoCompressionHandshakeHandler() {
    super(new NoDeflateWebSocketUpgradeStrategy());
  }

  @Override
  protected List<WebSocketExtension> filterRequestedExtensions(
      ServerHttpRequest request,
      List<WebSocketExtension> requestedExtensions,
      List<WebSocketExtension> supportedExtensions) {
    return Collections.emptyList();
  }
}
