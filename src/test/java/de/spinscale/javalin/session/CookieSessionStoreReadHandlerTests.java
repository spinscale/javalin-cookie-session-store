/*
 * Copyright [2019] [Alexander Reelsen]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package de.spinscale.javalin.session;

import io.javalin.http.Context;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.crypto.SecretKey;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class CookieSessionStoreReadHandlerTests {

    private final String cookieName = "my_cookie";
    private final String secret = "some_longer_secret_string_longer_than_256_bits";
    private final SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
    private final CookieSessionStoreReadHandler handler = new CookieSessionStoreReadHandler(key, cookieName);

    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final Context context = new Context(request, response, Collections.emptyMap());

    @Test
    void testValidKey() throws Exception {
        final String data = Jwts.builder().signWith(key).claim("foo", "bar").compact();
        Cookie[] cookies = new Cookie[]{new Cookie(cookieName, data)};
        when(request.getCookies()).thenReturn(cookies);

        handler.handle(context);

        verify(request).setAttribute(eq("foo"), eq("bar"));
    }

    @Test
    void testInvalidKey() throws Exception {
        final SecretKey anotherKey = Keys.hmacShaKeyFor("some_other_secret_string_longer_than_256_bits".getBytes());
        final String data = Jwts.builder().signWith(anotherKey).claim("foo", "bar").compact();
        Cookie[] cookies = new Cookie[]{new Cookie(cookieName, data)};
        when(request.getCookies()).thenReturn(cookies);

        handler.handle(context);

        verify(response).setStatus(eq(401));
        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(cookieCaptor.capture());
        final Cookie capturedCookie = cookieCaptor.getValue();
        assertThat(capturedCookie.getName()).isEqualTo(cookieName);
        assertThat(capturedCookie.getValue()).isEqualTo("");
    }

    @Test
    void testNonExistingCookie() throws Exception {
        Cookie[] cookies = new Cookie[]{new Cookie("foo", "bar")};
        when(request.getCookies()).thenReturn(cookies);

        handler.handle(context);

        verifyZeroInteractions(response);
    }

    @Test
    void testEmptyCookie() throws Exception {
        Cookie[] cookies = new Cookie[]{new Cookie(cookieName, "")};
        when(request.getCookies()).thenReturn(cookies);

        handler.handle(context);

        verifyZeroInteractions(response);
    }
}
