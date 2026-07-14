#!python3

from http.server import BaseHTTPRequestHandler, HTTPServer


hostName = "localhost"
serverPort = 9999


class MyServer(BaseHTTPRequestHandler):
    def _set_headers(self):
        self.send_response(200)
        self.send_header('Content-type', 'text/html')
        self.end_headers()

    def do_GET(self):
        self._set_headers()
        if self.path.endswith("/agent"):
            self.wfile.write(bytes(":7015", "utf-8"))
        elif self.path.endswith("/distributor"):
            self.wfile.write(bytes(":7011", "utf-8"))
        else:
            self.wfile.write(bytes("WRONG request. Use /agent or /distributor endpoints", "utf-8"))


if __name__ == "__main__":
    webServer = HTTPServer((hostName, serverPort), MyServer)
    print("Server started http://%s:%s" % (hostName, serverPort))

    try:
        webServer.serve_forever()
    except KeyboardInterrupt:
        pass

    webServer.server_close()
    print("Server stopped.")