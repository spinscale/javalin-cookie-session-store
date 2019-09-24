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

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import org.jetbrains.annotations.NotNull;

import javax.crypto.SecretKey;
import java.util.Map;
import java.util.function.Predicate;

/**
 * A handler that runs after others, which adds a cookie.
 * If request attributes exists, their value is encrypted and stored in the respective cookie.
 *
 * If the size of the cookie exceeds 4kb a warning is logged and the cookie is removed.
 * If no attributes are found, the cookie is removed
 */
public class CookieSessionStoreWriteHandler implements Handler {

    private final String cookieName;
    private final SecretKey key;
    private final Predicate<String> attributeFilter;

    CookieSessionStoreWriteHandler(SecretKey key, String cookieName, Predicate<String> attributeFilter) {
        this.cookieName = cookieName;
        this.key = key;
        this.attributeFilter = attributeFilter;
    }

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        final Map<String, Object> attributes = ctx.attributeMap();
        final boolean hasCookieAttributes = !attributes.isEmpty() && attributes.keySet().stream().anyMatch(attributeFilter);
        if (hasCookieAttributes) {
            final JwtBuilder builder = Jwts.builder().signWith(key);

            attributes.forEach((key, value) -> {
                if (attributeFilter.test(key)) {
                    builder.claim(key, value);
                }
            });

            String jws = builder.compact();
            if (jws.length() > 4096) {
                Javalin.log.warn("Cannot store session in cookie, too big...");
                ctx.removeCookie(cookieName);
            } else {
                ctx.cookie(cookieName, jws);
            }
        } else {
                ctx.removeCookie(cookieName);
        }
    }
}
