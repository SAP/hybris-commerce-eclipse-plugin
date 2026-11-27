# SPDX-FileCopyrightText: 2026 SAP SE or an SAP affiliate company and contributors
# SPDX-License-Identifier: Apache-2.0
"""Check the published artifact, including project-prefix URLs and images."""

import json
import tempfile
import unittest
from pathlib import Path

from mkdocs.commands.build import build
from mkdocs.config import load_config
from resources import validate_site_resources


class SiteTest(unittest.TestCase):
    def test_published_resources(self):
        root = Path(__file__).resolve().parents[2]
        with tempfile.TemporaryDirectory(prefix="commerce-docs-") as directory:
            site = Path(directory) / "site"
            config = load_config(str(root / "mkdocs.yml"), site_dir=str(site), strict=True)
            build(config)
            validate_site_resources(site, config.site_url)

            for image in (root / "docs").rglob("*"):
                if image.suffix in (".gif", ".png"):
                    self.assertEqual(image.read_bytes(), (site / image.relative_to(root / "docs")).read_bytes())
            for excluded in ("docker-compose.yml", "start-sonar.sh", "dev/eclipse-code-style.xml"):
                self.assertFalse((site / excluded).exists())
            search = json.loads((site / "search/search_index.json").read_text(encoding="utf-8"))
            self.assertTrue(search["docs"], "Search index must contain documentation")
            repository = site / "updates/latest"
            self.assertTrue((repository / "p2.index").is_file())
            self.assertTrue((repository / "content.xml.xz").is_file())
            self.assertTrue((repository / "artifacts.xml.xz").is_file())
            self.assertFalse((repository / ".DS_Store").exists())
            for published in repository.rglob("*"):
                self.assertFalse(published.is_symlink())
                if published.is_file():
                    source = root / "updatesite/latest" / published.relative_to(repository)
                    self.assertEqual(source.read_bytes(), published.read_bytes())


if __name__ == "__main__":
    unittest.main()
