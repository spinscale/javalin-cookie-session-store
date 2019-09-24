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
import io.jsonwebtoken.security.Keys;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

class CookieSessionStorePluginTests {

    private final SecretKey key = Keys.hmacShaKeyFor("some_longer_secret_string_longer_than_256_bits".getBytes());

    @Test
    void testRegisterPlugin() throws Exception {
        final CookieSessionStorePlugin plugin = new CookieSessionStorePlugin(key, s -> true);
        final Javalin javalin = Javalin.create(config -> config.registerPlugin(plugin));

        final CookieSessionStorePlugin retrievedPlugin = javalin.config.getPlugin(CookieSessionStorePlugin.class);
        assertThat(retrievedPlugin).isSameAs(retrievedPlugin);
    }

    // start a javalin webserver and check if everything is working in an end to end test
    @Test
    void runFullBlownIntegrationTest() throws Exception {
        final Predicate<String> attributeFilter = s -> s.startsWith("session_");
        final Javalin javalin = Javalin.create(config -> config.registerPlugin(new CookieSessionStorePlugin(key, attributeFilter)));

        javalin.get("/", ctx -> {
            ctx.attribute("session_name", "Alexander");
            ctx.status(200);
        });

        javalin.get("/name", ctx -> {
            String name = ctx.attribute("session_name") == null ? "EMPTY" : ctx.attribute("session_name");
            ctx.result("{ \"name\": \"" + name + "\" }");
        });

        OkHttpClient httpClient = null;
        javalin.start(0);

        try {
            httpClient = new OkHttpClient();
            String host = "http://localhost:" + javalin.port();
            try (Response response = httpClient.newCall(new Request.Builder().url(host + "/name").build()).execute()) {
                assertThat(response.code()).isEqualTo(200);
                assertThat(response.body().string()).isEqualTo("{ \"name\": \"EMPTY\" }");
            }

            String cookieHeader;
            try (Response response = httpClient.newCall(new Request.Builder().url(host + "/").build()).execute()) {
                assertThat(response.code()).isEqualTo(200);
                cookieHeader = response.header("Set-Cookie");
                assertThat(cookieHeader).matches(s -> s.contains("JAVALIN_SESSION_COOKIE"));
            }

            final Request.Builder requestWithCookie = new Request.Builder().url(host + "/name");
            requestWithCookie.addHeader("Cookie", cookieHeader);
            try (Response response = httpClient.newCall(requestWithCookie.build()).execute()) {
                assertThat(response.code()).isEqualTo(200);
                assertThat(response.body().string()).isEqualTo("{ \"name\": \"Alexander\" }");
            }
        } finally {
            javalin.stop();
            if (httpClient != null) {
                httpClient.dispatcher().executorService().shutdown();
                httpClient.connectionPool().evictAll();
            }
        }
    }
}