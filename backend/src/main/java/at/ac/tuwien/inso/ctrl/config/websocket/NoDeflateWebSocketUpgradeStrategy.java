package at.ac.tuwien.inso.ctrl.config.websocket;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.websocket.Endpoint;
import jakarta.websocket.Extension;
import java.net.InetSocketAddress;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.adapter.standard.StandardWebSocketHandlerAdapter;
import org.springframework.web.socket.adapter.standard.StandardWebSocketSession;
import org.springframework.web.socket.adapter.standard.WebSocketToStandardExtensionAdapter;
import org.springframework.web.socket.server.HandshakeFailureException;
import org.springframework.web.socket.server.standard.ServerEndpointRegistration;
import org.springframework.web.socket.server.standard.StandardWebSocketUpgradeStrategy;

/**
 * Upgrade strategy that disables WebSocket protocol extensions (notably {@code
 * permessage-deflate}).
 *
 * <p>Embedded Tomcat always appends its built-in {@code permessage-deflate} extension during the
 * handshake and negotiates it whenever the browser requests it, regardless of the extensions Spring
 * selects. Some browsers (notably Safari/WebKit on macOS) then produce corrupted compressed frames,
 * which Tomcat rejects as an unrecognised opCode (e.g. opCode 7), forcing the client onto the
 * SockJS fallback. Returning an empty negotiated-extension list from the configurator prevents
 * compression from being enabled at all.
 */
public class NoDeflateWebSocketUpgradeStrategy extends StandardWebSocketUpgradeStrategy {

  @Override
  public void upgrade(
      ServerHttpRequest request,
      ServerHttpResponse response,
      @Nullable String selectedProtocol,
      List<WebSocketExtension> selectedExtensions,
      @Nullable Principal user,
      WebSocketHandler wsHandler,
      Map<String, Object> attributes)
      throws HandshakeFailureException {

    HttpHeaders headers = request.getHeaders();
    InetSocketAddress localAddress = request.getLocalAddress();
    InetSocketAddress remoteAddress = request.getRemoteAddress();

    StandardWebSocketSession session =
        new StandardWebSocketSession(headers, attributes, localAddress, remoteAddress, user);
    StandardWebSocketHandlerAdapter endpoint =
        new StandardWebSocketHandlerAdapter(wsHandler, session);

    List<Extension> extensions = new ArrayList<>();
    for (WebSocketExtension extension : selectedExtensions) {
      extensions.add(new WebSocketToStandardExtensionAdapter(extension));
    }

    HttpServletRequest servletRequest = getHttpServletRequest(request);
    HttpServletResponse servletResponse = getHttpServletResponse(response);

    ServerEndpointRegistration endpointConfig =
        new NoDeflateServerEndpointRegistration(servletRequest.getRequestURI(), endpoint);
    endpointConfig.setSubprotocols(Collections.singletonList(selectedProtocol));
    endpointConfig.setExtensions(extensions);

    try {
      getContainer(servletRequest)
          .upgradeHttpToWebSocket(
              servletRequest, servletResponse, endpointConfig, Collections.emptyMap());
    } catch (Exception ex) {
      throw new HandshakeFailureException(
          "Servlet request failed to upgrade to WebSocket: " + servletRequest.getRequestURL(), ex);
    }
  }

  /** Registration whose configurator refuses to negotiate any protocol extension. */
  private static final class NoDeflateServerEndpointRegistration
      extends ServerEndpointRegistration {

    NoDeflateServerEndpointRegistration(String path, Endpoint endpoint) {
      super(path, endpoint);
    }

    @Override
    public List<Extension> getNegotiatedExtensions(
        List<Extension> installed, List<Extension> requested) {
      return Collections.emptyList();
    }
  }
}
