# DiPlay GKUI compatibility research: Geely 2018–2020 model years

This adapted DiPlay 0.2.10 source tree studies compatibility with GKUI head units in 2018–2020 Geely Borui, Binyue, Binrui, Jiaji, and Xingyue vehicles. This is the target adaptation scope, not a claim that every listed model is compatible.

**Vehicle verification is currently limited to one Geely Borui H52 running Android 4.3/API 18.** Other listed models and other hardware or firmware versions have not been tested. That vehicle has shown USB and built-in-hotspot wireless CarPlay video, but the user still reports severe lag and poor touch response.

See [the H52 repair status report](docs/geely-android43/H52-REPAIR-STATUS-2026-10-06.md) for the confirmed fixes, local P12/P13/P14 candidate hashes, failed routes, remaining issues, and build/vehicle test limits. The 2018–2020 GKUI scope is research coverage; only the H52 Borui result is currently verified.

The public tree includes source and documentation only. It excludes local test APKs, authentication payloads, pairing records, device captures, and hotspot credentials. Authorized local authentication inputs are described in [the build guide](docs/geely-android43/BUILD.md). P14 ports one upstream PR 311 change; it is not the complete upstream 0.2.13 release, which targets API 28 and cannot be installed as-is on API 18.

Upstream project: https://github.com/shihabal3amri/DiPlay
