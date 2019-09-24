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
import io.javalin.core.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import javax.crypto.SecretKey;
import java.util.function.Predicate;

/**
 * A cookie based session plugin for java
 *
 * NOTE: This does not use the request session for reading and writing attributes and is not a drop-in replacement
 *
 * Instead you can write request attributes, and specify which one should be serialized. The plugin uses JWT to write its cookie.
 * JWT requires a preconfigured secret - which should not change between redeploys.
 */
public class CookieSessionStorePlugin implements Plugin {

    static final String COOKIE_NAME = "SESSION_COOKIE";

    private final SecretKey key;
    private final Predicate<String> attributeFilter;
    private final String cookieName;

    /**
     * @param key               The secret key representing the secret to encrypt the cookie
     * @param attributeFilter   A filter applied against the request attributes to decide if an attribute should be serialized into the cookie
     * @see #CookieSessionStorePlugin(String, SecretKey, Predicate)
     */
    public CookieSessionStorePlugin(final SecretKey key, Predicate<String> attributeFilter) {
        this(COOKIE_NAME, key, attributeFilter);
    }

    /**
     *
     * @param cookieName        The name of the cookie to be used
     * @param key               The secret key representing the secret to encrypt the cookie
     * @param attributeFilter   A filter applied against the request attributes to decide if an attribute should be serialized into the cookie
     */
    public CookieSessionStorePlugin(String cookieName, final SecretKey key,
                                    Predicate<String> attributeFilter) {
        this.cookieName = cookieName;
        this.key = key;
        this.attributeFilter = attributeFilter;
    }

    @Override
    public void apply(@NotNull Javalin app) {
        app.before(new CookieSessionStoreReadHandler(key, cookieName));
        app.after(new CookieSessionStoreWriteHandler(key, cookieName, attributeFilter));
    }
}
