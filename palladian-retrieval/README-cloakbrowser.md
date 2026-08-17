# CloakBrowser integration

[CloakBrowser](https://github.com/CloakHQ/cloakbrowser) is a stealth Chromium distribution
that bypasses most commodity bot-detection systems (Cloudflare, DataDome, PerimeterX, …)
without a paid cloud service. Palladian drives it over the Chrome DevTools Protocol (CDP),
so it slots into the existing `CascadingDocumentRetriever` as a **free local tier** that
runs before any paid cloud retriever (Bright Data, PhantomJS Cloud).

CloakBrowser is consumed through its **Docker image only** — no local process spawning,
no native binary. One container per host serves N parallel CDP sessions, so the pool
needs just one address.

## Architecture

```
DocumentRetriever (plain HTTP)
        │   fails / is blocked
        ▼
RenderingDocumentRetrieverPool
  └─► when cloakbrowser.debugger_address is set in webknox.properties,
       this is a CloakBrowserDocumentRetrieverPool (extends the rendering pool)
        │   fails / challenged
        ▼
JsEnabledDocumentRetriever cloud tier (BrightData, PhantomJsCloud, …)
```

```
slot 1 ─▶ ChromeDriver ─┐
slot 2 ─▶ ChromeDriver ─┼─CDP──▶  cloakbrowser container @ 127.0.0.1:9222
slot N ─▶ ChromeDriver ─┘
```

Attachment is via `chromeOptions.setExperimentalOption("debuggerAddress", ...)`, so all of
`RenderingDocumentRetriever`'s behaviour (navigation, waits, CDP stealth patches, cookies,
screenshots) is reused unchanged.

## Install

1. Install Docker (Docker Desktop on Windows, Docker Engine on Ubuntu).
2. Start the container once per host:

   ```bash
   docker run -d --name cloakbrowser -p 9222:9222 cloakhq/cloakbrowser \
       --remote-debugging-port=9222 --remote-debugging-address=0.0.0.0
   ```

3. Sanity check:

   ```bash
   curl -s http://127.0.0.1:9222/json/version
   ```

   A JSON blob with `webSocketDebuggerUrl` means Palladian can attach.

4. Enable in `WebKnox/config/webknox.properties`:

   ```ini
   cloakbrowser.debugger_address=127.0.0.1:9222
   # optional
   cloakbrowser.pool_size=2
   ```

## Configuration reference

All settings live in `config/webknox.properties` and are read via `Controller.getConfig()`.
The Palladian library itself accepts values only through constructors; it never reads
environment variables or configuration files.

| Key                             | Default | Meaning                                                                  |
|---------------------------------|---------|--------------------------------------------------------------------------|
| `cloakbrowser.debugger_address` | *unset* | CDP endpoint of the container (e.g. `127.0.0.1:9222`). Enables the pool. |
| `cloakbrowser.pool_size`        | `2`     | Number of parallel CDP sessions.                                         |

If `cloakbrowser.debugger_address` is unset, `RenderingDocumentRetrieverManager` silently
falls back to the plain Chrome pool — existing behaviour is preserved.

## Behaviour

- Broken / lost sessions are replaced by the existing supervisor in
  `RenderingDocumentRetrieverPool` (session invalid, page-load timeout, tab crashed, …).
  The cross-platform `ProcessHandle`-based kill path introduced alongside this integration
  means hard-kill fallbacks now work identically on Windows and Ubuntu.
- Interactive challenges (Turnstile click-wall, Vercel checkpoint, …) cause
  `CascadingDocumentRetriever` to mark the domain and route subsequent requests straight
  to the paid cloud tier for one hour.

## Testing

See `WebKnox/src/test/java/com/webknox/helper/RetrieverTest.java`
(`testCloakBrowserRetriever`). It is gated on `cloakbrowser.debugger_address` being set
in `webknox.properties`, so it will not run on developer machines without a local
container. When the container is running, the test fetches a page and asserts on the
`<title>` element.

```powershell
# Windows — start container, configure, run test
docker run -d --name cloakbrowser -p 9222:9222 cloakhq/cloakbrowser `
    --remote-debugging-port=9222 --remote-debugging-address=0.0.0.0
# add cloakbrowser.debugger_address=127.0.0.1:9222 to config/webknox.properties
mvn -pl WebKnox test -Dtest=RetrieverTest#testCloakBrowserRetriever
```

```bash
# Ubuntu
docker run -d --name cloakbrowser -p 9222:9222 cloakhq/cloakbrowser \
    --remote-debugging-port=9222 --remote-debugging-address=0.0.0.0
mvn -pl WebKnox test -Dtest=RetrieverTest#testCloakBrowserRetriever
```
