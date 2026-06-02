from http.server import HTTPServer, BaseHTTPRequestHandler
import subprocess

class VRAMHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        # This command asks NVIDIA GPUs for their memory usage
        cmd = "nvidia-smi --query-gpu=memory.used,memory.total --format=csv,nounits,noheader"
        output = subprocess.check_output(cmd, shell=True).decode().strip()
        self.send_response(200)
        self.send_header('Content-type', 'text/plain')
        self.send_header('Access-Control-Allow-Origin', '*')
        self.end_headers()
        self.wfile.write(output.encode())

print("VRAM Bridge running on port 11435...")
HTTPServer(('0.0.0.0', 11435), VRAMHandler).serve_forever()
