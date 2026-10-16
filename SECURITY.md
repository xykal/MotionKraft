# Security Policy

## Reporting Vulnerabilities

If you discover a security vulnerability, do **not** open a public GitHub issue. Send details privately to the repository owner via GitHub's security advisory feature.

Do not disclose the issue publicly until it has been addressed.

## Scope
- APK signing keys are stored exclusively as repository secrets scoped to the `production` environment, gated by manual reviewer approval.
- Forks do not have access to release secrets.
- Release builds use R8 code shrinking, resource shrinking, and repackaged classes.
- Cleartext traffic is disabled via network security config.
- Release builds strip all `Log.v/d/i` output.

## Key Management
- The release keystore is held offline and injected to CI only at build time via encrypted secrets.
- The signing credentials are never committed to source control.
- Public key / signing certificate fingerprint will be published with the first public release so users can verify APK authenticity.
