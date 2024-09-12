package org.apache.coyote.http11;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import com.techcourse.db.InMemoryUserRepository;
import com.techcourse.model.User;

public class ResponseGenerator {

    public static HttpResponse generate(final HttpRequest httpRequest) throws IOException {

        final String uri = httpRequest.getUri();
        final String version = httpRequest.getVersion();
        final String acceptLine = httpRequest.getAcceptLine();
        final String httpMethod = httpRequest.getMethod();
        final String requestBody = httpRequest.getBody();

        Cookie cookie = httpRequest.getCookie();
        HttpResponse httpResponse = null;

        if (uri.equals("/login")) {
            if (httpMethod.equalsIgnoreCase(HttpMethod.GET.name())) {
                if (isLoggedIn(cookie)) {
                    httpResponse = HttpResponse.of(version, HttpStatus.FOUND.getHttpStatusMessage(), ContentType.HTML.getContentType());
                    httpResponse.addHeader(HttpHeader.LOCATION.getHeaderName(), "/index.html");
                } else {
                    String body = StaticResourceReader.read("/login.html");
                    httpResponse = HttpResponse.of(version, HttpStatus.OK.getHttpStatusMessage(), ContentType.HTML.getContentType(), body);
                }
            }

            if (httpMethod.equalsIgnoreCase(HttpMethod.POST.name())) {
                final String[] loginRequest = requestBody.split("&");
                final String account = loginRequest[0].split("=")[1];
                final String password = loginRequest[1].split("=")[1];

                final Optional<User> user = InMemoryUserRepository.findByAccount(account);
                if (isValidUser(user, password)) {
                    final UUID uuid = UUID.randomUUID();
                    final Session session = new Session(uuid.toString());
                    session.setAttribute("user", user);
                    final SessionManager sessionManager = SessionManager.getInstance();
                    sessionManager.add(session);

                    cookie = new Cookie("JSESSIONID=" + session.getId());
                    httpResponse = HttpResponse.of(version, HttpStatus.FOUND.getHttpStatusMessage(), ContentType.HTML.getContentType());
                    httpResponse.addHeader("Set-Cookie", cookie.getValue());
                    httpResponse.addHeader(HttpHeader.LOCATION.getHeaderName(), "/index.html");
                } else {
                    httpResponse = HttpResponse.of(version, HttpStatus.FOUND.getHttpStatusMessage(), ContentType.HTML.getContentType());
                    httpResponse.addHeader(HttpHeader.LOCATION.getHeaderName(), "/401.html");
                }
            }

        } else if (uri.equals("/register")) {
            if (httpMethod.equalsIgnoreCase(HttpMethod.GET.name())) {
                final String body = StaticResourceReader.read("/register.html");
                httpResponse = HttpResponse.of(version, HttpStatus.OK.getHttpStatusMessage(), ContentType.HTML.getContentType(), body);
            }

            if (httpMethod.equalsIgnoreCase(HttpMethod.POST.name())) {
                final String[] loginRequest = requestBody.split("&");
                final String account = loginRequest[0].split("=")[1];
                final String email = loginRequest[1].split("=")[1];
                final String password = loginRequest[2].split("=")[1];

                final User user = new User(account, email, password);
                InMemoryUserRepository.save(user);
                httpResponse = HttpResponse.of(version, HttpStatus.FOUND.getHttpStatusMessage(), ContentType.HTML.getContentType());
                httpResponse.addHeader(HttpHeader.LOCATION.getHeaderName(), "/index.html");
            }

        } else if (ContentType.isValidType(uri)) {
            String contentType = ContentTypeResolver.resolve(uri, acceptLine);
            String body = StaticResourceReader.read(uri);
            httpResponse = HttpResponse.of(version, HttpStatus.OK.getHttpStatusMessage(), contentType, body);

        } else {
            String body = StaticResourceReader.read("/404.html");
            httpResponse = HttpResponse.of(version, HttpStatus.NOT_FOUND.getHttpStatusMessage(), ContentType.HTML.getContentType(), body);
        }
        return httpResponse;
    }

    private static boolean isValidUser(Optional<User> user, String password) {
        if (user.isEmpty()) {
            System.out.println("일치하는 유저가 없습니다.");
            return false;
        }
        final boolean isMatchedPassword = user.get().checkPassword(password);
        if (isMatchedPassword) {
            return true;
        }
        System.out.println("비밀번호가 틀렸습니다.");
        return false;
    }

    private static boolean isLoggedIn(final Cookie cookie) {
        if (cookie == null) {
            return false;
        }
        if (cookie.isJSessionCookie()) {
            return SessionManager.getInstance().hasSession(cookie.getJSessionId());
        }
        return false;
    }
}
