# SPDX-FileCopyrightText: 2026 SAP SE or an SAP affiliate company and contributors
# SPDX-License-Identifier: Apache-2.0
"""Exercise actual Mike builds in an isolated Git repository, never the user's branches."""

import hashlib
import json
import shutil
import tempfile
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path
from urllib.parse import unquote, urlsplit

import yaml

from resources import ResourceParser, validate_site_resources
from versioned_site import HISTORY_BRANCH, build_site, deployment_config, git, history_versions


class VersioningTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="commerce-versioning-")
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        git(self.root, "init", "-b", "master")
        git(self.root, "config", "user.name", "Documentation test")
        git(self.root, "config", "user.email", "docs-test@example.invalid")
        (self.root / "docs").mkdir()
        (self.root / "tools/docs").mkdir(parents=True)
        original = Path(__file__).resolve().parents[2]
        shutil.copyfile(original / "tools/docs/hooks.py", self.root / "tools/docs/hooks.py")
        (self.root / ".gitignore").write_text("site/\ndocs-history.bundle\n", encoding="utf-8")
        self.config = {
            "site_name": "Versioning test", "site_url": "https://example.com/project/",
            "repo_url": "https://github.com/example/repository", "docs_dir": "docs", "site_dir": "site",
            "theme": {"name": "material", "font": False, "features": ["content.action.edit"]},
            "hooks": ["tools/docs/hooks.py"], "plugins": ["search"],
            "watch": ["README.md", "tools/docs", "updatesite"],
            "nav": [{"Home": "index.md"}, {"Install": "installation.md"}],
            "validation": {"nav": {"omitted_files": "warn"}, "links": {"not_found": "warn"}},
        }
        (self.root / "mkdocs.yml").write_text(yaml.safe_dump(self.config), encoding="utf-8")
        self.write_source("Released documentation", "1.2.3.RELEASE")
        self.write_repository(b"release artifact")
        self.commit()
        git(self.root, "tag", "v1.2.3")
        self.release_sha = git(self.root, "rev-parse", "HEAD").stdout.decode().strip()
        self.write_source("Development documentation", "1.2.4-SNAPSHOT")
        self.write_repository(b"current master artifact")
        self.commit()

    def write_source(self, title, version):
        (self.root / "README.md").write_text(f"# {title}\n[Install](docs/installation.md)\n[License](LICENSE)\n", encoding="utf-8")
        (self.root / "docs/installation.md").write_text(f"# Installation\n{title}\n", encoding="utf-8")
        (self.root / "pom.xml").write_text(
            f'<project xmlns="http://maven.apache.org/POM/4.0.0"><version>{version}</version></project>', encoding="utf-8"
        )

    def write_repository(self, artifact):
        source = self.root / "updatesite/latest"
        (source / "plugins").mkdir(parents=True, exist_ok=True)
        (source / "plugins/example.bundle_1.2.3.jar").write_bytes(artifact)
        content = ET.fromstring('<repository><units><unit><artifacts><artifact classifier="osgi.bundle" id="example.bundle" version="1.2.3"/></artifacts></unit></units></repository>')
        artifacts = ET.fromstring('<repository><artifacts><artifact classifier="osgi.bundle" id="example.bundle" version="1.2.3"><properties/></artifact></artifacts></repository>')
        props = artifacts.find("./artifacts/artifact/properties")
        ET.SubElement(props, "property", name="download.size", value=str(len(artifact)))
        ET.SubElement(props, "property", name="download.checksum.sha-256", value=hashlib.sha256(artifact).hexdigest())
        (source / "content.xml").write_bytes(ET.tostring(content))
        (source / "artifacts.xml").write_bytes(ET.tostring(artifacts))
        (source / "p2.index").write_text("version=1\n", encoding="utf-8")

    def commit(self):
        git(self.root, "add", ".")
        git(self.root, "commit", "-m", "Documentation fixture")

    def test_version_archives_selector_redirects_p2_and_history_round_trip(self):
        source_head = git(self.root, "rev-parse", "HEAD").stdout
        output = build_site(self.root, "v1.2.3", export_history=True)
        versions = json.loads((output / "versions.json").read_text())
        release = next(item for item in versions if item["version"] == "1.2.3")
        development = next(item for item in versions if item["version"] == "development")
        self.assertEqual(self.release_sha, release["properties"]["source_commit"])
        self.assertEqual(["latest"], development["aliases"])
        released_html = (output / "1.2.3/index.html").read_bytes()
        self.assertIn(b"Released documentation", released_html)
        self.assertIn(b"/blob/v1.2.3/LICENSE", released_html)
        self.assertIn(b"Development documentation", (output / "development/index.html").read_bytes())
        self.assertIn(b'"version"', (output / "development/index.html").read_bytes())
        self.assertTrue((output / "latest/installation/index.html").exists())
        self.assertIn("latest", (output / "index.html").read_text())
        self.assertIn("latest/installation/index.html", (output / "installation/index.html").read_text())
        self.assertEqual(b"current master artifact", (output / "updates/latest/plugins/example.bundle_1.2.3.jar").read_bytes())
        self.assertFalse((output / "1.2.3/updates").exists())
        self.assertFalse((output / "development/updates").exists())
        self.assertFalse(any(path.is_symlink() for path in output.rglob("*")))
        for page in output.rglob("*.html"):
            parser = ResourceParser()
            parser.feed(page.read_text(encoding="utf-8"))
            for url in parser.urls:
                parsed = urlsplit(url)
                if parsed.scheme or parsed.netloc or not parsed.path:
                    continue
                path = unquote(parsed.path)
                if path.startswith("/"):
                    self.assertTrue(path.startswith("/project/"), (page, url))
                    target = output / path[len("/project/"):]
                else:
                    target = page.parent / path
                if target.is_dir():
                    target /= "index.html"
                self.assertTrue(target.is_file(), (page, url))
        self.assertEqual(source_head, git(self.root, "rev-parse", "HEAD").stdout)
        self.assertEqual("master", git(self.root, "branch", "--show-current").stdout.decode().strip())
        self.assertTrue((self.root / "docs-history.bundle").is_file())
        restored = self.root / "restored"
        restored.mkdir()
        git(restored, "init")
        git(restored, "fetch", str(self.root / "docs-history.bundle"), f"{HISTORY_BRANCH}:refs/heads/{HISTORY_BRANCH}")
        self.assertEqual(history_versions(self.root), history_versions(restored))
        shutil.rmtree(restored)
        self.write_source("New development documentation", "1.2.4-SNAPSHOT")
        self.commit()
        build_site(self.root, "v1.2.3")
        self.assertEqual(released_html, (output / "1.2.3/index.html").read_bytes())
        self.assertIn(b"New development documentation", (output / "development/index.html").read_bytes())

    def test_invalid_release_and_dirty_export_do_not_modify_history(self):
        with self.assertRaisesRegex(ValueError, "vX.Y.Z"):
            build_site(self.root, "../master")
        git(self.root, "tag", "v1.2.4")
        with self.assertRaisesRegex(ValueError, "does not identify"):
            build_site(self.root, "v1.2.4")
        self.assertNotEqual(0, git(self.root, "rev-parse", "--verify", HISTORY_BRANCH, check=False).returncode)
        (self.root / "docs/uncommitted.md").write_text("Unreviewed content")
        with self.assertRaisesRegex(ValueError, "Commit documentation"):
            build_site(self.root, export_history=True)

    def test_archived_release_cannot_be_overwritten_by_a_moved_tag(self):
        build_site(self.root, "v1.2.3")
        before = git(self.root, "rev-parse", HISTORY_BRANCH).stdout
        self.write_source("Moved release source", "1.2.3.RELEASE")
        self.commit()
        git(self.root, "tag", "-f", "v1.2.3")
        with self.assertRaisesRegex(ValueError, "Immutable"):
            build_site(self.root, "v1.2.3")
        self.assertEqual(before, git(self.root, "rev-parse", HISTORY_BRANCH).stdout)

    def test_artifact_validation_rejects_missing_and_prefix_escaping_resources(self):
        site = self.root / "site"
        site.mkdir()
        page = site / "index.html"
        page.write_text('<img src="/outside/image.png">', encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "escapes project prefix"):
            validate_site_resources(site, self.config["site_url"])
        page.write_text('<img src="missing.png">', encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "Missing published resource"):
            validate_site_resources(site, self.config["site_url"])

    def test_snapshot_config_rejects_outside_sources_and_untrusted_hooks(self):
        source = dict(self.config, docs_dir="../outside")
        with self.assertRaisesRegex(ValueError, "inside"):
            deployment_config(self.config, source, self.root, self.root / "tools/docs/hooks.py", self.root / "build", "v1.2.3")
        source = dict(self.config, hooks=["unsafe.py"], plugins=["unsafe"])
        config = deployment_config(self.config, source, self.root, self.root / "tools/docs/hooks.py", self.root / "build", "v1.2.3")
        self.assertEqual([str(self.root / "tools/docs/hooks.py")], config["hooks"])
        self.assertEqual("search", config["plugins"][0])
        self.assertNotIn("unsafe", str(config["plugins"]))
        self.assertEqual("", config["edit_uri"])

    def test_workflow_only_pushes_history_from_the_trusted_deployment_job(self):
        root = Path(__file__).resolve().parents[2]
        workflow = yaml.load((root / ".github/workflows/docs.yml").read_text(), Loader=yaml.BaseLoader)
        self.assertEqual("read", workflow["jobs"]["build"]["permissions"]["contents"])
        self.assertEqual("write", workflow["jobs"]["deploy"]["permissions"]["contents"])
        self.assertIn("github.ref == 'refs/heads/master'", workflow["jobs"]["deploy"]["if"])
        self.assertIn("github.event_name == 'release'", workflow["jobs"]["deploy"]["if"])
        build_commands = "\n".join(step.get("run", "") for step in workflow["jobs"]["build"]["steps"])
        self.assertNotIn("git push", build_commands)
        deploy_commands = "\n".join(step.get("run", "") for step in workflow["jobs"]["deploy"]["steps"])
        self.assertIn("refs/heads/docs-versions:refs/heads/docs-versions", deploy_commands)
        self.assertNotIn("--force", deploy_commands)


if __name__ == "__main__":
    unittest.main()
