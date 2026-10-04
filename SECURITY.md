# Credentials and release packaging

Public release distribution has resumed at the maintainer's request. The APK intentionally bundles the experimental accessory certificate and matching key described in docs/THIRD_PARTY_NOTICES.md. Anyone with the APK can extract them. Local compilation, Git history removal and obfuscation do not make a bundled shared key confidential or revoke previous copies.

The public Git tree and corresponding source archive exclude accessory keys and Android release-signing secrets. CI checks reject credential containers and private-key blocks in tracked files. Synthetic test identities are generated at runtime. Source builds have no automatic private-asset import; release packaging requires an explicit local directory and permits only the two expected runtime files.

The Android APK-signing key is separate, stays local and is never bundled in the APK. Current acceptance of the experimental accessory identity does not establish Apple certification or guarantee future compatibility.

Review diagnostic reports before posting. Never include credentials or pairing records in public issues. Use GitHub private vulnerability reporting for sensitive findings.
