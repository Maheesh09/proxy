import http.server
import json
import os

LOG_FILE = "d:/ProxyMaze/scratch/webhooks.log"

class WebhookHandler(http.server.BaseHTTPRequestHandler):
    def do_POST(self):
        content_length = int(self.headers['Content-Length'])
        post_data = self.rfile.read(content_length)
        
        with open(LOG_FILE, "a") as f:
            f.write(f"\n--- RECEIVED WEBHOOK ---\n")
            f.write(f"Path: {self.path}\n")
            try:
                payload = json.loads(post_data.decode('utf-8'))
                f.write(json.dumps(payload, indent=2) + "\n")
            except:
                f.write(post_data.decode('utf-8') + "\n")
        
        self.send_response(200)
        self.end_headers()

if __name__ == "__main__":
    if os.path.exists(LOG_FILE): os.remove(LOG_FILE)
    print("Mock Webhook Server starting on port 9000...")
    http.server.HTTPServer(('localhost', 9000), WebhookHandler).serve_forever()
