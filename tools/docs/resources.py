# SPDX-FileCopyrightText: 2026 SAP SE or an SAP affiliate company and contributors
# SPDX-License-Identifier: Apache-2.0
"""Validate local links and assets in the final Pages artifact, including redirects."""

from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import unquote, urlsplit


class ResourceParser(HTMLParser):
    def __init__(self):
        super().__init__()
        self.urls = []

    def handle_starttag(self, tag, attrs):
        for name, value in attrs:
            if name in ("src", "href") and value:
                self.urls.append(value)


def validate_site_resources(site, site_url):
    site = Path(site).resolve()
    prefix = urlsplit(site_url).path
    for page in site.rglob("*.html"):
        parser = ResourceParser()
        parser.feed(page.read_text(encoding="utf-8"))
        for url in parser.urls:
            parsed = urlsplit(url)
            if parsed.scheme or parsed.netloc or not parsed.path:
                continue
            path = unquote(parsed.path)
            if path.startswith("/"):
                if not path.startswith(prefix):
                    raise ValueError(f"URL escapes project prefix in {page.relative_to(site)}: {url}")
                target = site / path[len(prefix):]
            else:
                target = page.parent / path
            if target.is_dir():
                target /= "index.html"
            if not target.resolve().is_relative_to(site) or not target.is_file():
                raise ValueError(f"Missing published resource in {page.relative_to(site)}: {url}")
