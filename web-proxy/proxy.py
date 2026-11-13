#!/usr/bin/env python3
"""Serve the web build of the app and forward /cc/* to www.classcharts.com.

Browsers can't talk to ClassCharts directly: it sends no CORS headers, its login cookies are
HttpOnly, and fetch() can't see redirects. This proxy fixes all three. Cookies go back and forth
in X-CC-Cookie / X-CC-Set-Cookie headers, and redirects come back as 200 with X-CC-Status, so the
app can manage the session itself. The proxy stores nothing.

Usage:
    ./gradlew :shared:wasmJsBrowserDistribution
    python3 web-proxy/proxy.py            # then open http://localhost:8787

Only standard library, Python 3.8+. It listens on localhost by default; if you host it for other
people, put it behind HTTPS, because codes, dates of birth and session cookies pass through it.
"""
import argparse
import functools
import http.server
import os
import urllib.error
import urllib.request

UPSTREAM = "https://www.classcharts.com"
USER_AGENT = ("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
              "(KHTML, like Gecko) Chrome/130.0 Safari/537.36")
FORWARDED_HEADERS = ("Authorization", "Content-Type", "X-Requested-With", "Accept")
DEFAULT_DIST = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..",
                            "shared", "build", "dist", "wasmJs", "productionExecutable")


class _NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *args, **kwargs):
        return None  # surface 3xx as HTTPError so we can pass it on


_opener = urllib.request.build_opener(_NoRedirect)


class Handler(http.server.SimpleHTTPRequestHandler):
    extensions_map = {
        **http.server.SimpleHTTPRequestHandler.extensions_map,
        ".wasm": "application/wasm",
        ".js": "text/javascript",
        ".mjs": "text/javascript",
    }

    def end_headers(self):
        self.send_header("Access-Control-Allow-Origin", self.headers.get("Origin") or "*")
        self.send_header("Access-Control-Allow-Headers",
                         "Authorization, Content-Type, X-Requested-With, Accept, X-CC-Cookie")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Expose-Headers", "X-CC-Status, X-CC-Location, X-CC-Set-Cookie")
        super().end_headers()

    def do_OPTIONS(self):
        self.send_response(204)
        self.end_headers()

    def do_GET(self):
        if self.path.startswith("/cc/"):
            self._proxy()
        else:
            super().do_GET()

    def do_POST(self):
        if self.path.startswith("/cc/"):
            self._proxy()
        else:
            self.send_error(404)

    def _proxy(self):
        length = int(self.headers.get("Content-Length") or 0)
        body = self.rfile.read(length) if length else None
        headers = {"User-Agent": USER_AGENT, "Referer": UPSTREAM + "/mobile/student"}
        for name in FORWARDED_HEADERS:
            if self.headers.get(name):
                headers[name] = self.headers[name]
        if self.headers.get("X-CC-Cookie"):
            headers["Cookie"] = self.headers["X-CC-Cookie"]

        request = urllib.request.Request(UPSTREAM + self.path[len("/cc"):], data=body,
                                         headers=headers, method=self.command)
        try:
            response = _opener.open(request, timeout=30)
            status, response_headers, data = response.status, response.headers, response.read()
        except urllib.error.HTTPError as e:
            status, response_headers, data = e.code, e.headers, e.read()
        except Exception as e:  # network failure upstream
            self.send_error(502, f"Couldn't reach ClassCharts: {e}")
            return

        cookies = [c.split(";", 1)[0].strip() for c in response_headers.get_all("Set-Cookie") or []]
        self.send_response(200 if 300 <= status < 400 else status)
        self.send_header("Content-Type", response_headers.get("Content-Type", "application/json"))
        self.send_header("Content-Length", str(len(data)))
        self.send_header("Cache-Control", "no-store")
        self.send_header("X-CC-Status", str(status))
        if response_headers.get("Location"):
            self.send_header("X-CC-Location", response_headers["Location"])
        if cookies:
            self.send_header("X-CC-Set-Cookie", "; ".join(cookies))
        self.end_headers()
        self.wfile.write(data)

    def log_message(self, fmt, *args):
        # Don't log query strings or form bodies; they can contain personal details.
        print(f"{self.command} {self.path.split('?', 1)[0]} -> {args[1] if len(args) > 1 else ''}")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=8787)
    parser.add_argument("--dist", default=DEFAULT_DIST, help="folder containing the built web app")
    args = parser.parse_args()
    dist = os.path.abspath(args.dist)
    if not os.path.isdir(dist):
        print(f"Warning: {dist} doesn't exist yet. Run ./gradlew :shared:wasmJsBrowserDistribution first.")
    server = http.server.ThreadingHTTPServer((args.host, args.port), functools.partial(Handler, directory=dist))
    print(f"ClassCharts web app on http://{args.host}:{args.port}  (serving {dist})")
    server.serve_forever()


if __name__ == "__main__":
    main()
