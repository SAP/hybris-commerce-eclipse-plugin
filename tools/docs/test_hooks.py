# SPDX-FileCopyrightText: 2026 SAP SE or an SAP affiliate company and contributors
# SPDX-License-Identifier: Apache-2.0
"""Regression tests for the generated documentation homepage."""

import unittest
from pathlib import Path

from mkdocs.config import load_config
from mkdocs.structure.files import Files

from hooks import homepage_markdown


class HomepageTest(unittest.TestCase):
    def test_document_links_preserve_fragments(self):
        self.assertEqual(
            "[Import](importer/importer.md#features)",
            homepage_markdown("[Import](docs/importer/importer.md#features)", "https://example.com/repo"),
        )

    def test_repository_links(self):
        for filename in ("LICENSE", "CODE_OF_CONDUCT.md", "CONTRIBUTING.md"):
            with self.subTest(filename=filename):
                self.assertEqual(
                    f"[Link](https://example.com/repo/blob/master/{filename})",
                    homepage_markdown(f"[Link]({filename})", "https://example.com/repo"),
                )

    def test_docs_directory_links_go_to_repository(self):
        self.assertEqual(
            "[Sources](https://example.com/repo/tree/master/docs/)",
            homepage_markdown("[Sources](docs/)", "https://example.com/repo"),
        )

    def test_absolute_links_and_prose_are_unchanged(self):
        markdown = "Use docs/importer. [External](https://example.com/docs/importer.md) ![Image](image.gif)"
        self.assertEqual(markdown, homepage_markdown(markdown, "https://example.com/repo"))

    def test_homepage_is_generated_from_readme(self):
        root = Path(__file__).resolve().parents[2]
        config = load_config(str(root / "mkdocs.yml"))
        files = config.plugins.on_files(Files([]), config=config)
        homepage = files.get_file_from_path("index.md")
        self.assertEqual(
            homepage_markdown((root / "README.md").read_text(encoding="utf-8"), config.repo_url),
            homepage.content_string,
        )
        self.assertEqual(f"{config.repo_url}/edit/master/README.md", homepage.edit_uri)
        self.assertFalse((root / "docs" / "index.md").exists())


if __name__ == "__main__":
    unittest.main()
