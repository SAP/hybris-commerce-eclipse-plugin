# SPDX-FileCopyrightText: 2026 SAP SE or an SAP affiliate company and contributors
# SPDX-License-Identifier: Apache-2.0
"""Build documentation and publish the committed Eclipse p2 repository."""

import hashlib
import lzma
import os
import re
import shutil
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path

from mkdocs.exceptions import PluginError
from mkdocs.structure.files import File


def homepage_markdown(markdown, repo_url, source_ref="master"):
    """Rebase the README's inline links from the repository root to docs/."""
    # Keep the directory browser on GitHub; it is not a documentation page.
    markdown = markdown.replace("](docs/)", f"]({repo_url}/tree/{source_ref}/docs/)")
    # Rebase inline Markdown destinations without changing absolute URLs.
    markdown = markdown.replace("](docs/", "](")
    for filename in ("CODE_OF_CONDUCT.md", "CONTRIBUTING.md", "LICENSE"):
        markdown = markdown.replace(
            f"]({filename})", f"]({repo_url}/blob/{source_ref}/{filename})"
        )
    return markdown


def on_files(files, config):
    root = Path(os.environ.get("COMMERCE_DOCS_SOURCE_ROOT", Path(config.config_file_path).parent))
    source_ref = os.environ.get("COMMERCE_DOCS_SOURCE_REF", "master")
    markdown = homepage_markdown(
        (root / "README.md").read_text(encoding="utf-8"), config.repo_url.rstrip("/"), source_ref
    )
    homepage = File.generated(config, "index.md", content=markdown)
    homepage.edit_uri = f"{config.repo_url.rstrip('/')}/edit/master/README.md" if source_ref == "master" else None
    files.append(homepage)
    return files


def repository_metadata(source, name):
    """Read p2 descriptors and reject inconsistent compressed alternatives."""
    descriptors = []
    paths = []
    for suffix in (".xml.xz", ".xml", ".jar"):
        path = source / (name + suffix)
        if not path.is_file():
            continue
        if suffix == ".jar":
            with zipfile.ZipFile(path) as archive:
                content = archive.read(name + ".xml")
        elif suffix == ".xml.xz":
            content = lzma.decompress(path.read_bytes())
        else:
            content = path.read_bytes()
        descriptors.append(ET.fromstring(content))
        paths.append(path)
    if not descriptors:
        raise ValueError(f"Missing p2 {name} metadata in {source}")
    if any(ET.tostring(item) != ET.tostring(descriptors[0]) for item in descriptors[1:]):
        raise ValueError(f"Inconsistent p2 {name} metadata in {source}")
    return descriptors[0], paths


def publish_update_site(source, destination):
    """Validate and copy p2 bytes unchanged, without publishing source symlinks."""
    source = source.resolve()
    content, content_paths = repository_metadata(source, "content")
    artifacts, artifact_paths = repository_metadata(source, "artifacts")
    paths = [source / "p2.index", *content_paths, *artifact_paths]
    keys = set()
    directories = {"osgi.bundle": "plugins", "org.eclipse.update.feature": "features"}
    for artifact in artifacts.findall("./artifacts/artifact"):
        classifier, identifier, version = (artifact.get(key, "") for key in ("classifier", "id", "version"))
        if classifier not in directories or not all(re.fullmatch(r"[\w.-]+", value) for value in (identifier, version)):
            raise ValueError(f"Unsupported p2 artifact: {classifier}/{identifier}/{version}")
        path = source / directories[classifier] / f"{identifier}_{version}.jar"
        data = path.read_bytes()
        properties = {item.get("name"): item.get("value") for item in artifact.findall("./properties/property")}
        if "download.size" in properties and len(data) != int(properties["download.size"]):
            raise ValueError(f"Incorrect p2 artifact size: {path}")
        for algorithm in ("sha-256", "sha-512"):
            expected = properties.get("download.checksum." + algorithm)
            if expected and hashlib.new(algorithm.replace("-", ""), data).hexdigest() != expected:
                raise ValueError(f"Incorrect p2 artifact checksum: {path}")
        keys.add((classifier, identifier, version))
        paths.append(path)
    if not keys:
        raise ValueError(f"Empty p2 artifact repository: {source}")
    for artifact in content.findall("./units/unit/artifacts/artifact"):
        key = tuple(artifact.get(name) for name in ("classifier", "id", "version"))
        if key not in keys:
            raise ValueError(f"Missing p2 artifact referenced by content metadata: {key}")
    # Copy only metadata and referenced artifacts. Finder files and the `latest`
    # source symlink must not end up in the GitHub Pages artifact.
    for path in paths:
        target = destination / path.relative_to(source)
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(path, target)


def on_post_build(config):
    # Version snapshots contain docs only; the final Pages artifact adds one canonical p2 repository.
    if os.environ.get("COMMERCE_DOCS_VERSION_BUILD") == "1":
        return
    root = Path(config.config_file_path).parent
    try:
        publish_update_site(root / "updatesite/latest", Path(config.site_dir) / "updates/latest")
    except (OSError, ValueError, KeyError, ET.ParseError, zipfile.BadZipFile, lzma.LZMAError) as error:
        raise PluginError(f"Cannot publish Eclipse p2 update site: {error}") from error
