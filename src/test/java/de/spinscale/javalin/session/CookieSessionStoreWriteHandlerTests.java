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
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.crypto.SecretKey;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CookieSessionStoreWriteHandlerTests {

    private final String cookieName = "my_cookie";
    private final String secret = "some_longer_secret_string_longer_than_256_bits";
    private final SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());

    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final Context context = new Context(request, response, Collections.emptyMap());

    @Test
    void testSimpleWrite() throws Exception {
        final CookieSessionStoreWriteHandler handler = new CookieSessionStoreWriteHandler(key, cookieName, s -> true);
        configureAttributes("foo", "foo_value", "bar", "bar_value");

        handler.handle(context);

        verifyCookieHasBeenWritten();
    }

    @Test
    void testWritingNumbers() throws Exception {
        final CookieSessionStoreWriteHandler handler = new CookieSessionStoreWriteHandler(key, cookieName, s -> true);
        configureAttributes("foo", 12, "bar", 12.123);

        handler.handle(context);

        verifyCookieHasBeenWritten();
    }

    @Test
    void testAttributeFilter() throws Exception {
        final Predicate<String> predicate = s -> s.startsWith("foo");
        final CookieSessionStoreWriteHandler handler = new CookieSessionStoreWriteHandler(key, cookieName, predicate);
        configureAttributes("foo", "in", "foo2", "in", "span", "out");

        handler.handle(context);

        Cookie cookie = verifyCookieHasBeenWritten();
        final Jws<Claims> claims = Jwts.parser().setSigningKey(key).parseClaimsJws(cookie.getValue());
        assertThat(claims.getBody()).containsKeys("foo", "foo2");
        assertThat(claims.getBody()).doesNotContainKey("span");
    }

    @Test
    void testNoAttributesConfigured() throws Exception {
        final CookieSessionStoreWriteHandler handler = new CookieSessionStoreWriteHandler(key, cookieName, s -> true);
        final Enumeration<String> enumeration = Collections.enumeration(Collections.emptyList());
        when(request.getAttributeNames()).thenReturn(enumeration);

        handler.handle(context);

        verifyCookieHasBeenDeleted();
    }

    @Test
    void testTooMuchData() throws Exception {
        final CookieSessionStoreWriteHandler handler = new CookieSessionStoreWriteHandler(key, cookieName, s -> true);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 3020; i++) { // tipping point is somewhere around here...
            sb.append('A');
        }
        configureAttributes("foo", sb.toString());

        handler.handle(context);

        verifyCookieHasBeenDeleted();
    }

    /**
     * Add request attributes to the mock request
     *
     * @param attributes A list of key value pairs to be added to the mocked request attributes
     */
    private void configureAttributes(Object ... attributes) {
        if (attributes.length % 2 != 0) {
            throw new RuntimeException("number of attributes must be even");
        }

        List<String> names = new ArrayList<>();
        for (int i = 0; i < attributes.length; i=i+2) {
            Object key = attributes[i];
            Object value = attributes[i+1];
            names.add(key.toString());
            when(request.getAttribute(eq(key.toString()))).thenReturn(value);
        }

        final Enumeration<String> enumeration = Collections.enumeration(names);
        when(request.getAttributeNames()).thenReturn(enumeration);
    }

    private void verifyCookieHasBeenDeleted() {
        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(cookieCaptor.capture());
        final Cookie capturedCookie = cookieCaptor.getValue();
        assertThat(capturedCookie.getName()).isEqualTo(cookieName);
        assertThat(capturedCookie.getValue()).isEqualTo("");
    }

    private Cookie verifyCookieHasBeenWritten() {
        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(cookieCaptor.capture());
        final Cookie capturedCookie = cookieCaptor.getValue();
        assertThat(capturedCookie.getName()).isEqualTo(cookieName);
        assertThat(capturedCookie.getValue()).isNotBlank();
        return capturedCookie;
    }
}