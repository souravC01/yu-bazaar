if self.path.startswith("/actuator/health"):import os
from http.server import BaseHTTPRequestHandler, HTTPServer

TARGET = "https://yu-bazaar.vercel.app"


class RedirectHandler(BaseHTTPRequestHandler):

    def do_GET(self):
	if self.path.split("?", 1)[0] == "/actuator/health":
            self.send_response(200)
            self.send_header("Content-Type", "text/plain")
            self.end_headers()
            self.wfile.write(b"OK")
            return

        self.send_response(302)
        self.send_header("Location", TARGET + self.path)
        self.end_headers()

    def do_HEAD(self):
        if self.path.split("?", 1)[0] == "/actuator/health":
            self.send_response(200)
            self.end_headers()
            return

        self.send_response(302)
        self.send_header("Location", TARGET + self.path)
        self.end_headers()

    def log_message(self, format, *args):
        return


port = int(os.environ.get("PORT", "10000"))

server = HTTPServer(("0.0.0.0", port), RedirectHandler)

print(f"Legacy redirect server listening on port {port}")
server.serve_forever()
