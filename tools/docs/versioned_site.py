# SPDX-FileCopyrightText: 2026 SAP SE or an SAP affiliate company and contributors
# SPDX-License-Identifier: Apache-2.0
"""Build Mike documentation history plus one canonical, byte-preserved p2 repository.

Only the generated docs-versions branch is changed. This tool never pushes Git
branches; CI exports a bundle and pushes it from the trusted deployment job.
"""

import argparse
import copy
import html
import io
import json
import os
import posixpath
import re
import shutil
import subprocess
import sys
import tarfile
import tempfile
import xml.etree.ElementTree as ET
from pathlib import Path

import yaml

from hooks import publish_update_site
from resources import validate_site_resources


HISTORY_BRANCH = "docs-versions"
DEVELOPMENT_VERSION = "development"
LATEST_ALIAS = "latest"
RELEASE_TAG = re.compile(r"v(\d+\.\d+\.\d+)\Z")


def git(root, *args, check=True):
    return subprocess.run(
        ["git", *args], cwd=root, check=check, stdout=subprocess.PIPE, stderr=subprocess.PIPE
    )


def history_versions(root):
    for ref in (HISTORY_BRANCH, f"origin/{HISTORY_BRANCH}"):
        if git(root, "rev-parse", "--verify", ref, check=False).returncode:
            continue
        result = git(root, "show", f"{ref}:versions.json", check=False)
        if result.returncode:
            raise ValueError(f"{ref} exists but is not a Mike documentation-history branch")
        versions = json.loads(result.stdout)
        if not isinstance(versions, list):
            raise ValueError("Invalid documentation version metadata")
        return versions
    return []


def source_links(value, source_ref):
    if isinstance(value, str):
        return value.replace("/blob/master/", f"/blob/{source_ref}/").replace(
            "/tree/master/", f"/tree/{source_ref}/"
        )
    if isinstance(value, list):
        return [source_links(item, source_ref) for item in value]
    if isinstance(value, dict):
        return {key: source_links(item, source_ref) for key, item in value.items()}
    return value


def deployment_config(base, source, source_root, trusted_hook, build_directory, source_ref):
    config = copy.deepcopy(source)
    docs = Path(config.get("docs_dir", "docs"))
    if docs.is_absolute() or not (source_root / docs).resolve().is_relative_to(source_root.resolve()):
        raise ValueError("Documentation sources must stay inside the source snapshot")
    if not (source_root / docs).is_dir():
        raise ValueError("Source snapshot has no documentation directory")
    config["docs_dir"] = str((source_root / docs).resolve())
    config["site_dir"] = str(build_directory)
    # Snapshot configurations live in a temporary directory, not the live-preview project root.
    config["watch"] = []
    config["site_url"] = base["site_url"]
    config["repo_url"] = base["repo_url"]
    config["theme"] = copy.deepcopy(base["theme"])
    config["plugins"] = ["search", {"mike": {"alias_type": "redirect"}}]
    config["hooks"] = [str(trusted_hook)]
    config["markdown_extensions"] = copy.deepcopy(base.get("markdown_extensions", []))
    config["validation"] = copy.deepcopy(base.get("validation", {}))
    config["strict"] = True
    config["remote_branch"] = HISTORY_BRANCH
    config.setdefault("extra", {})["version"] = {"provider": "mike"}
    if source_ref != "master":
        config["edit_uri"] = ""
        config["theme"]["features"] = [
            item for item in config["theme"].get("features", []) if item != "content.action.edit"
        ]
        config = source_links(config, source_ref)
    return config


def mike(root, config_path, source_root, source_ref, *args):
    env = os.environ.copy()
    # Mike launches the mkdocs console script; keep both commands in this Python environment.
    env["PATH"] = str(Path(sys.executable).parent) + os.pathsep + env.get("PATH", "")
    env.update(
        COMMERCE_DOCS_VERSION_BUILD="1",
        COMMERCE_DOCS_SOURCE_ROOT=str(source_root),
        COMMERCE_DOCS_SOURCE_REF=source_ref,
    )
    subprocess.run(
        [sys.executable, "-c", "from mike.driver import main\nmain()", *args,
         "--branch", HISTORY_BRANCH, "--config-file", str(config_path)],
        cwd=root, env=env, check=True,
    )


def release_source(root, tag, destination):
    match = RELEASE_TAG.fullmatch(tag)
    if not match:
        raise ValueError("Release documentation requires a tag named vX.Y.Z")
    sha = git(root, "rev-parse", "--verify", f"refs/tags/{tag}^{{commit}}").stdout.decode().strip()
    archive = git(root, "archive", "--format=tar", tag, "README.md", "docs", "mkdocs.yml", "pom.xml")
    with tarfile.open(fileobj=io.BytesIO(archive.stdout)) as files:
        files.extractall(destination, filter="data")
    version = match.group(1)
    pom = ET.parse(destination / "pom.xml").getroot()
    actual = pom.findtext("{http://maven.apache.org/POM/4.0.0}version")
    if actual != f"{version}.RELEASE":
        raise ValueError(f"Tag {tag} does not identify plugin version {version}.RELEASE")
    return version, sha


def write_legacy_redirects(output):
    """Keep previous unversioned page URLs working without touching archived files."""
    for page in (output / DEVELOPMENT_VERSION).rglob("*.html"):
        relative = page.relative_to(output / DEVELOPMENT_VERSION)
        destination = output / relative
        if destination.exists():
            continue
        target = posixpath.relpath(f"{LATEST_ALIAS}/{relative.as_posix()}", relative.parent.as_posix())
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_text(
            '<!doctype html><meta charset="utf-8">'
            f'<meta http-equiv="refresh" content="0; url={html.escape(target, quote=True)}">'
            f'<a href="{html.escape(target, quote=True)}">Latest documentation</a>'
            '<script>const target=new URL('
            + json.dumps(target)
            + ',location.href);target.search=location.search;target.hash=location.hash;'
              'location.replace(target.href);</script>',
            encoding="utf-8",
        )


def build_site(root, release_tag="", export_history=False):
    root = Path(root).resolve()
    if export_history:
        changed = git(root, "status", "--porcelain", "--", "README.md", "docs", "mkdocs.yml",
                      "tools/docs", "updatesite").stdout
        if changed:
            raise ValueError("Commit documentation and tooling changes before exporting publication history")
    base = yaml.safe_load((root / "mkdocs.yml").read_text(encoding="utf-8"))
    hook = (root / "tools/docs/hooks.py").resolve()
    versions = history_versions(root)
    with tempfile.TemporaryDirectory(prefix="commerce-versioned-docs-") as temporary:
        directory = Path(temporary)
        release = None
        # Validate release input before modifying documentation history.
        if release_tag:
            source_root = directory / "release"
            source_root.mkdir()
            version, sha = release_source(root, release_tag, source_root)
            existing = next((item for item in versions if item["version"] == version), None)
            if existing and existing.get("properties", {}).get("source_commit") != sha:
                raise ValueError(f"Immutable documentation snapshot {version} has different source provenance")
            release = (source_root, version, sha, existing)
        development = deployment_config(base, base, root, hook, directory / "development-build", "master")
        development_config = directory / "development.yml"
        development_config.write_text(yaml.safe_dump(development, sort_keys=False), encoding="utf-8")
        sha = git(root, "rev-parse", "HEAD").stdout.decode().strip()
        mike(root, development_config, root, "master", "deploy", "--update-aliases", "--alias-type", "redirect",
             "--title", "Development", "--prop-set-string", f"source_commit={sha}",
             "--prop-set-string", "source_ref=master", DEVELOPMENT_VERSION, LATEST_ALIAS)
        if release and not release[3]:
            source_root, version, sha, unused_existing = release
            source = yaml.safe_load((source_root / "mkdocs.yml").read_text(encoding="utf-8"))
            config = deployment_config(base, source, source_root, hook, directory / "release-build", release_tag)
            config_path = directory / "release.yml"
            config_path.write_text(yaml.safe_dump(config, sort_keys=False), encoding="utf-8")
            mike(root, config_path, source_root, release_tag, "deploy", "--alias-type", "redirect",
                 "--prop-set-string", f"source_commit={sha}", "--prop-set-string", f"source_ref={release_tag}", version)
        mike(root, development_config, root, "master", "set-default", LATEST_ALIAS)
        output = root / "site"
        if output.exists():
            shutil.rmtree(output)
        output.mkdir()
        archive = git(root, "archive", "--format=tar", HISTORY_BRANCH)
        with tarfile.open(fileobj=io.BytesIO(archive.stdout)) as files:
            files.extractall(output, filter="data")
        if any(path.is_symlink() for path in output.rglob("*")):
            raise ValueError("Documentation history must use redirect aliases, not symlinks")
        write_legacy_redirects(output)
        # Always use the checked-out master repository, never a release snapshot's p2 bytes.
        publish_update_site(root / "updatesite/latest", output / "updates/latest")
        validate_site_resources(output, base["site_url"])
        if export_history:
            bundle = root / "docs-history.bundle"
            if bundle.exists():
                bundle.unlink()
            git(root, "bundle", "create", str(bundle), HISTORY_BRANCH)
    return output


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--release-tag", default="", help="Optional immutable vX.Y.Z release snapshot")
    parser.add_argument("--export-history", action="store_true", help="Write docs-history.bundle for trusted CI publication")
    args = parser.parse_args()
    build_site(Path(__file__).resolve().parents[2], args.release_tag, args.export_history)


if __name__ == "__main__":
    main()
