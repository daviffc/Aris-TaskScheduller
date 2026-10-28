
# Security Policy

## Project Status

Aris is an educational and portfolio-oriented backend project.

It is designed primarily for trusted local development and testing.

It has not been hardened or audited for public internet deployment.

## Current Security Controls

- Environment-based database credentials
- Localhost-only HTTP binding by default
- Localhost-only Docker port publishing in staging
- HTTP actions disabled by default
- Explicit HTTPS host allowlisting for optional HTTP actions
- Disabled automatic HTTP redirects
- Bounded asynchronous execution queue
- Git history and source-code secret scanning

## Known Limitations

The REST API currently does not implement authentication or authorization.

The application is not intended to accept untrusted workflow definitions.

HTTP host allowlisting is not a replacement for network-level SSRF protection.

The scheduler is designed for single-instance execution and does not implement distributed coordination.

## Reporting Vulnerabilities

Please use GitHub's private vulnerability reporting feature when available.

Do not disclose working exploits or credentials in public issues.

## Production Deployment

Do not expose Aris directly to the public internet.

Any production-oriented deployment would require additional authentication, authorization, network isolation, rate limiting, execution limits, monitoring, and security testing.
