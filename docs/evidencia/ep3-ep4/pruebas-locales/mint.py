# Emisor de JWT de PRUEBA local (RS256). Uso: python3 mint.py <email> [ROL,ROL] [scopes]
import base64, json, subprocess, sys, time, os
d = os.path.dirname(os.path.abspath(__file__))
b64 = lambda b: base64.urlsafe_b64encode(b).rstrip(b'=').decode()
def jwks():
    mod = subprocess.run(['openssl','rsa','-in',f'{d}/key.pem','-noout','-modulus'],capture_output=True,text=True).stdout.strip().split('=')[1]
    return {"keys":[{"kty":"RSA","kid":"local-test","use":"sig","alg":"RS256","n":b64(bytes.fromhex(mod)),"e":b64((65537).to_bytes(3,'big'))}]}
if sys.argv[1] == '--jwks':
    json.dump(jwks(), open(f'{d}/jwks.json','w')); sys.exit()
email = sys.argv[1]; roles = [r for r in (sys.argv[2] if len(sys.argv)>2 else '').split(',') if r]
scp = sys.argv[3] if len(sys.argv)>3 else 'reportes.read reportes.write'
ttl = int(os.environ.get('TTL', '3600')); now = int(time.time())
h = b64(json.dumps({"alg":"RS256","typ":"JWT","kid":"local-test"}).encode())
p = b64(json.dumps({"iss":"https://issuer.local.test/v2.0","aud":"ecopunto-local","sub":email,"preferred_username":email,"email":email,"roles":roles,"scp":scp,"iat":now-10,"nbf":now-10,"exp":now+ttl}).encode())
sig = subprocess.run(['openssl','dgst','-sha256','-sign',f'{d}/key.pem'],input=f'{h}.{p}'.encode(),capture_output=True).stdout
print(f'{h}.{p}.{b64(sig)}')
