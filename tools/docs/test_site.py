# SPDX-FileCopyrightText: 2026 SAP SE or an SAP affiliate company and contributors
# SPDX-License-Identifier: Apache-2.0
"""Check the published artifact, including project-prefix URLs and images."""

import json
import tempfile
import unittest
from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import unquote, urlsplit

from mkdocs.commands.build import build
from mkdocs.config import load_config


class ResourceParser(HTMLParser):
    def __init__(self):
        super().__init__()
        self.urls = []

    def handle_starttag(self, tag, attrs):
        for name, value in attrs:
            if name in ("src", "href") and value:
                self.urls.append(value)


class SiteTest(unittest.TestCase):
    def test_published_resources(self):
        root = Path(__file__).resolve().parents[2]
        with tempfile.TemporaryDirectory(prefix="commerce-docs-") as directory:
            site = Path(directory) / "site"
            config = load_config(str(root / "mkdocs.yml"), site_dir=str(site), strict=True)
            build(config)
            prefix = urlsplit(config.site_url).path
            for page in site.rglob("*.html"):
                parser = ResourceParser()
                parser.feed(page.read_text(encoding="utf-8"))
                for url in parser.urls:
                    with self.subTest(page=str(page.relative_to(site)), url=url):
                        parsed = urlsplit(url)
                        if parsed.scheme or parsed.netloc or not parsed.path:
                            continue
                        path = unquote(parsed.path)
                        if path.startswith("/"):
                            self.assertTrue(path.startswith(prefix), "URL escapes project prefix")
                            target = site / path[len(prefix):]
                        else:
                            target = page.parent / path
                        if target.is_dir():
                            target = target / "index.html"
                        self.assertTrue(target.is_file(), f"Missing published resource: {target}")

            for image in (root / "docs").rglob("*"):
                if image.suffix in (".gif", ".png"):
                    self.assertEqual(image.read_bytes(), (site / image.relative_to(root / "docs")).read_bytes())
            for excluded in ("docker-compose.yml", "start-sonar.sh", "dev/eclipse-code-style.xml"):
                self.assertFalse((site / excluded).exists())
            search = json.loads((site / "search/search_index.json").read_text(encoding="utf-8"))
            self.assertTrue(search["docs"], "Search index must contain documentation")


if __name__ == "__main__":
    unittest.main()
