import argparse
import json
from pathlib import Path
import re
import sys


ROOT = Path(__file__).resolve().parents[2]
SEMVER = re.compile(r"^[0-9]+\.[0-9]+\.[0-9]+$")


def property_value(path: Path, pattern: str) -> str | None:
    match = re.search(pattern, path.read_text(encoding="utf-8"), re.MULTILINE)
    return match.group(1) if match else None


def collect_versions(root: Path) -> dict[str, str | None]:
    package = json.loads((root / "frontend" / "package.json").read_text(encoding="utf-8"))
    package_lock = json.loads(
        (root / "frontend" / "package-lock.json").read_text(encoding="utf-8")
    )
    return {
        "release": (root / "VERSION").read_text(encoding="utf-8").strip(),
        "backend": property_value(
            root / "backend" / "build.gradle",
            r"^version\s*=\s*'([^']+)'$",
        ),
        "frontend": package.get("version"),
        "frontend_lock": package_lock.get("version"),
        "frontend_lock_root": package_lock.get("packages", {}).get("", {}).get("version"),
        "deployment": property_value(
            root / "deploy" / ".env.example",
            r"^RELEASE_VERSION=([^\s]+)$",
        ),
    }


def validate(root: Path, tag: str | None = None) -> list[str]:
    errors: list[str] = []
    versions = collect_versions(root)
    release_version = versions["release"]

    if release_version is None or not SEMVER.fullmatch(release_version):
        errors.append("VERSION must contain MAJOR.MINOR.PATCH")
    else:
        for component, version in versions.items():
            if version != release_version:
                errors.append(
                    f"{component} version must match VERSION: "
                    f"expected {release_version}, found {version}"
                )

        if tag is not None and tag != f"v{release_version}":
            errors.append(f"release tag must be v{release_version}")

        changelog = (root / "CHANGELOG.md").read_text(encoding="utf-8")
        if f"## {release_version} " not in changelog:
            errors.append("CHANGELOG must include the release version")

    required_artifacts = [
        "backend/Dockerfile",
        "frontend/Dockerfile",
        "deploy/compose.production.yml",
        "docs/deployment/database-backup-recovery.md",
        "docs/deployment/operational-observability.md",
    ]
    missing = [artifact for artifact in required_artifacts if not (root / artifact).is_file()]
    if missing:
        errors.append(f"required release artifacts are missing: {', '.join(missing)}")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--tag")
    arguments = parser.parse_args()

    errors = validate(ROOT, arguments.tag)
    if errors:
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1
    print(f"release {collect_versions(ROOT)['release']} is internally consistent")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
