package com.dnastack.wes.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.web.savedrequest.SavedRequest;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Optional;

import static java.lang.String.format;
import static org.springframework.security.web.util.UrlUtils.buildFullRequestUrl;

public class XForwardUtil {

    private static final int HTTPS_DEFAULT_PORT = 443;
    private static final int HTTP_DEFAULT = 80;


    private interface Request {
        String getHeader(String headerName);
        URI getUri();
    }

    /**
     * Infers the existence of a reverse proxy from 'X-Forwarded-*' headers and generates a URL with the correct
     * protocol when necessary.
     *
     * @param request Never null. Headers in this request are used to infer reverse proxy setup.
     * @param path Never null.
     * @return The full URL with the externally visible hostname, port, and protocol, using the values from
     *          X-Forwarded-* headers if present, or else the literal values from the given request.
     */
    public static String getExternalPath(HttpRequest request, String path) {
        return getExternalPath(new HttpRequestAdapter(request), path);
    }

    /**
     * Infers the existence of a reverse proxy from 'X-Forwarded-*' headers and generates a URL with the correct
     * protocol when necessary.
     *
     * @param request Never null. Headers in this request are used to infer reverse proxy setup.
     * @param path Never null.
     * @return The full URL with the externally visible hostname, port, and protocol, using the values from
     *          X-Forwarded-* headers if present, or else the literal values from the given request.
     */
    public static String getExternalPath(HttpServletRequest request, String path) {
        return getExternalPath(new HttpServletRequestAdapter(request), path);
    }

    /**
     * Infers the existence of a reverse proxy from 'X-Forwarded-*' headers and generates a URL with the correct
     * protocol when necessary used by initiator of this request.
     *
     * @param req Never null. Headers in this request are used to infer reverse proxy setup.
     * @return The inferred URL used to initiate this request (from the other side of the reverse proxy, if one is present).
     */
    public static String getRedirectUrl(SavedRequest req) {
        final boolean forwardedFromHttps = req.getHeaderValues("x-forwarded-proto").stream().anyMatch("https"::equals);
        if (forwardedFromHttps) {
            final int port = getExternalPort(req);
            try {
                final URL rawUrl = new URL(req.getRedirectUrl());
                return buildFullRequestUrl("https", rawUrl.getHost(), port, rawUrl.getPath(), rawUrl.getQuery());
            } catch (MalformedURLException e) {
                throw new IllegalStateException(String.format("Unable to parse redirect url [%s] to modify protocol from x-forwarded-proto header.", req.getRedirectUrl()));
            }
        } else {
            return req.getRedirectUrl();
        }
    }

    private static int getExternalPort(SavedRequest req) {
        Optional<String> explicitForwardedPort = req.getHeaderValues("x-forwarded-port")
                                                    .stream()
                                                    .findFirst();
        return getNumericalPortOrDefault(explicitForwardedPort);
    }

    private static int getNumericalPortOrDefault(Optional<String> explicitForwardedPort) {
        return explicitForwardedPort
                .map(Integer::valueOf)
                .orElse(HTTPS_DEFAULT_PORT);
    }

    /**
     * @param request Must not be null.
     * @return True if the given request uses https directly, or is proxied from an https connection.
     */
    public static boolean hasExternalHttpsConnection(HttpServletRequest request) {
        return request.isSecure() || isForwardedFromHttps(request);
    }

    public static String getClientIp(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("x-forwarded-for")).orElseGet(request::getRemoteAddr);
    }

    private static boolean isForwardedFromHttps(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("x-forwarded-proto"))
                       .filter("https"::equals)
                       .isPresent();
    }

    /**
     * Infers the existence of a reverse proxy from 'X-Forwarded-*' headers and generates a URL with the correct
     * protocol when necessary.
     *
     * @param request Never null. Headers in this request are used to infer reverse proxy setup.
     * @param path Never null.
     * @return The full URL with the externally visible hostname, port, and protocol, using the values from
     *          X-Forwarded-* headers if present, or else the literal values from the given request.
     */
    public static String getExternalPath(ServerHttpRequest request, String path) {
        return getExternalPath(new SpringServerHttpRequestAdapter(request), path);
    }

    private static String getExternalPath(Request request, String path) {
        final int port = getExternalPort(request);
        final String protocol = getExternalProto(request);
        final String host = getExternalHost(request);
        return buildFullRequestUrl(protocol, host, port, path, null);
    }

    private static String getExternalHost(Request request) {
        return Optional.ofNullable(request.getHeader("x-forwarded-host"))
                       .orElseGet(() -> request.getUri().getHost());
    }

    private static String getExternalProto(Request request) {
        return Optional.ofNullable(request.getHeader("x-forwarded-proto"))
                       .orElseGet(() -> request.getUri().getScheme());
    }

    private static int getExternalPort(Request req) {
        final String xForwardedPort = req.getHeader("x-forwarded-port");
        if (xForwardedPort != null) {
            return Integer.valueOf(xForwardedPort);
        }

        final String xForwardedProto = req.getHeader("x-forwarded-proto");
        if ("https".equals(xForwardedProto)) {
            return HTTPS_DEFAULT_PORT;
        } else if ("http".equals(xForwardedProto)) {
            return HTTP_DEFAULT;
        }

        final URI uri = req.getUri();
        final int uriPort = uri.getPort();
        if (uriPort > 0) {
            return uriPort;
        } else if ("https".equals(uri.getScheme())) {
            return HTTPS_DEFAULT_PORT;
        } else if ("http".equals(uri.getScheme())) {
            return HTTP_DEFAULT;
        } else {
            throw new RuntimeException(format("Unable to infer port. uri=%s", uri));
        }
    }

    private static class HttpRequestAdapter implements Request {
        private final HttpRequest request;

        HttpRequestAdapter(HttpRequest request) {
            this.request = request;
        }

        @Override
        public String getHeader(String headerName) {
            return request.getHeaders().getFirst(headerName);
        }

        @Override
        public URI getUri() {
            return request.getURI();
        }
    }

    private static class SpringServerHttpRequestAdapter implements Request {
        private final ServerHttpRequest request;

        SpringServerHttpRequestAdapter(ServerHttpRequest request) {
            this.request = request;
        }

        @Override
        public String getHeader(String headerName) {
            return request.getHeaders().getFirst(headerName);
        }

        @Override
        public URI getUri() {
            return request.getURI();
        }
    }

    private static class HttpServletRequestAdapter implements Request {
        private final HttpServletRequest request;

        HttpServletRequestAdapter(HttpServletRequest request) {
            this.request = request;
        }

        @Override
        public String getHeader(String headerName) {
            return request.getHeader(headerName);
        }

        @Override
        public URI getUri() {
            return URI.create(request.getRequestURL().toString());
        }
    }
}
