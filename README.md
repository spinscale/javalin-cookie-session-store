# A cookie based session for Javalin

TLDR; This package contains a plugin to act as a cookie based session
mechanism for a Javalin app.

The cookie is JWT based and requires a configured secret as part of the plugin setup.

The data which is stored in the cookie can be set as request attributes.
Those can be filtered on specified criteria, so that not all attributes will be
stored in the cookie.

Note that cookie based sessions have limitations, as the maximum cookie size
is at 4kb.

## Usage

Add the dependency to your gradle file. 

```
compile 'de.spinscale.javalin:javalin-cookie-session-store:0.1.0'
```

If you are using maven

```
<dependency>
    <groupId>de.spinscale.javalin</groupId>
    <artifactId>javalin-cookie-session-store</artifactId>
    <version>0.1.0</version>
</dependency>

```

Register the plugin on javalin startup

```java
SecretKey key = Keys.hmacShaKeyFor("some_secret_with_more_than_32_chars".getBytes());
Predicate<String> attributeFilter = s -> s.startsWith("session_");

Javalin javalin = Javalin.create();
javalin.config.registerPlugin(new CookieSessionStorePlugin(key, attribute));
```

The secret should be retrieved from an external configuration and should not
be changed during restarts, as otherwise sessions would be lost.

Set an attribute works like this

```java
javalin.get("/", ctx -> {
  // this attribute will be stored
  ctx.attribute("session_name", "Alexander");
  // this will not be stored in the cookie, does not match the attributeFilter
  ctx.attribute("foo", "bar");
});
```

Retrieving an attribute 

```
javalin.get("/name", ctx -> {
  String name = ctx.attribute("session_name");
  ctx.json(Collections.singletonMap("name", name);
});

```

## Development

Ensure code coverage, run `./gradlew clean test jacocoTestReport`

When opening pull requests, please ensure, you added a test.

## TODO

* Check if one can use the session and ensure that the session cookie does not get written, but only this one
* Expose more cookie configuration options
