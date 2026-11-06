# Public release review — 1.0.14

Reviewed the complete initial Git tree, documentation graphics and production APK before publishing to Elaine-Lynn/tvscreenguard. The repository starts with a new reviewed commit; no development history or private diagnostics are imported.

## Checks

- Gitleaks directory scan with redacted reporting and archive traversal: no findings in the public source snapshot or extracted production APK.
- Additional signature checks: no private-key files, release signing configuration, known device IP, local home-directory paths or known GitHub-token signatures in the public Git tree / APK.
- Documentation PNGs: no EXIF, author, comment or XMP metadata. Settings and question screenshots were generated on a disposable emulator, not taken from the user's TV or video account.
- Build output, device captures, backups, local SDK configuration, signing files, ADB keys and private logs are excluded from Git. Distribution archives are created from an explicit public file list.
- Release signature verified against the approved GenTV production certificate. The keystore remains external; only the public certificate fingerprint is documented.
- Commit attribution uses the owner's GitHub no-reply address rather than the workstation's configured personal email.
- Tests contain fixed synthetic PINs and fixture identifiers for disposable emulators. These are test inputs, not real user credentials. Runtime ADB key generation and references to signing property names are implementation code, not embedded credentials.
- Public licence/copyright notices and certificate fingerprints are intentionally retained.

## Verification and ongoing checks

`tools/check_publication.py` checks tracked-file boundaries and common private-information signatures. CI also runs project invariants, JVM tests, lint and a Debug build. Release signing keys are not uploaded to GitHub or CI. Gitleaks was run locally against the exact source and distribution contents before upload.

The signed APK SHA-256 remains `f30e62b447d93b29f268bb11017a2187496f77c547cd441601099992c942a250`. Release removes the 30-second QA option. Device compatibility and new-package migration limitations remain documented; this review does not claim a full new-package B3P acceptance test or prove the absence of every possible vulnerability.
