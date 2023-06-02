## Running with Self Signed Certificates and mTLS

The Cromwell WES Server can be run in a way which does not require OAuth and instead uses mTLS for authentication. This 
approach may be desirable when there is not an OAuthServer available, or you want to use mTLS in addition to OAuth for 
added security.

You can accomplish this with many reverse proxies, but the example here uses [nginx](https://www.nginx.com/) which is
a fast, simple and popular reverse proxy.

## Requirements

- `nginx` locally installed
- `crowmell-wes-server` running locally on port 8090. You will also need to run the wes service
  with `SECURITY_ENABLED=false` environment variable set if you do not plan on using OAuth As well as mTLS

## Generating new certs

Before starting the NGINX service, you will need to generate certificates to use for performing mTLS or Https. The
following will provide certificates for hostname `127.0.0.1`.

```bash
cd cert

# Generate server cert to be signed
openssl req -new -nodes -x509 -days 365 -keyout server.key -out server.crt -config server.conf

# Generate client cert to be signed
openssl req -new -nodes -x509 -days 365 -keyout client.key -out client.crt -config client.conf
```

## Running

All the necessary config is provided in the [nginx.conf](nginx.conf). Running with that configuration will start nginx
using mTLS and the self-signed certificate you generated in the `certs` folder using the code from [above](#generating-new-certs).

```
# Start nginx in the foreground using the defined config
nginx -c nginx.conf
```

## Requests with curl

```bash
curl --cacert certs/server.crt \
  --key certs/client.key \
  --cert certs/client.crt \
  https://127.0.0.1:8443/ga4gh/wes/v1/runs
```