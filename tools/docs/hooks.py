# SPDX-FileCopyrightText: 2026 SAP SE or an SAP affiliate company and contributors
# SPDX-License-Identifier: Apache-2.0
"""Generate the homepage without maintaining a second copy of the README."""

from pathlib import Path

from mkdocs.structure.files import File


def homepage_markdown(markdown, repo_url):
    """Rebase the README's inline links from the repository root to docs/."""
    # Keep the directory browser on GitHub; it is not a documentation page.
    markdown = markdown.replace("](docs/)", f"]({repo_url}/tree/master/docs/)")
    # Rebase inline Markdown destinations without changing absolute URLs.
    markdown = markdown.replace("](docs/", "](")
    for filename in ("CODE_OF_CONDUCT.md", "CONTRIBUTING.md", "LICENSE"):
        markdown = markdown.replace(
            f"]({filename})", f"]({repo_url}/blob/master/{filename})"
        )
    return markdown


def on_files(files, config):
    root = Path(config.config_file_path).parent
    markdown = homepage_markdown(
        (root / "README.md").read_text(encoding="utf-8"), config.repo_url.rstrip("/")
    )
    homepage = File.generated(config, "index.md", content=markdown)
    homepage.edit_uri = f"{config.repo_url.rstrip('/')}/edit/master/README.md"
    files.append(homepage)
    return files
