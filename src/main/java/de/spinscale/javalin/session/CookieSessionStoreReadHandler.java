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
import io.javalin.http.Handler;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.jetbrains.annotations.NotNull;

import javax.crypto.SecretKey;
import java.util.Map;

/**
 * A handler that runs before others, which checks for a certain cookie
 * If that cookie exists, the cookie is decrypted and its arguments are stored in the request attributes
 */
class CookieSessionStoreReadHandler implements Handler {

    private final String cookieName;
    private final SecretKey key;

    public CookieSessionStoreReadHandler(SecretKey key, String cookieName) {
        this.cookieName = cookieName;
        this.key = key;
    }

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        final String data = ctx.cookie(cookieName);

        if (data == null || data.isEmpty()) {
            return;
        }

        try {
            final Jws<Claims> claims = Jwts.parser().setSigningKey(key).parseClaimsJws(data);
            for (Map.Entry<String, Object> entry : claims.getBody().entrySet()) {
                ctx.attribute(entry.getKey(), entry.getValue());
            }
        } catch (JwtException e) {
            // someone fiddled with the authentication
            ctx.removeCookie(cookieName);
            ctx.status(401);
        }
    }
}
