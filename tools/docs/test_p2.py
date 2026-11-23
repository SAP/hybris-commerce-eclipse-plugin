# SPDX-FileCopyrightText: 2026 SAP SE or an SAP affiliate company and contributors
# SPDX-License-Identifier: Apache-2.0
"""Regression tests for byte-preserving p2 publication and validation."""

import lzma
import shutil
import tempfile
import unittest
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path

from hooks import publish_update_site, repository_metadata


class UpdateSiteTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="commerce-p2-")
        self.addCleanup(self.temporary.cleanup)
        root = Path(__file__).resolve().parents[2]
        self.source = Path(self.temporary.name) / "source"
        self.destination = Path(self.temporary.name) / "published"
        shutil.copytree(root / "updatesite/latest", self.source)

    def test_publishes_regular_files_without_changing_bytes(self):
        (self.source / ".DS_Store").write_bytes(b"Finder metadata")
        publish_update_site(self.source, self.destination)
        self.assertFalse((self.destination / ".DS_Store").exists())
        for published in self.destination.rglob("*"):
            self.assertFalse(published.is_symlink())
            if published.is_file():
                self.assertEqual(
                    (self.source / published.relative_to(self.destination)).read_bytes(),
                    published.read_bytes(),
                )

    def test_missing_artifact_fails(self):
        next((self.source / "plugins").glob("*.jar")).unlink()
        with self.assertRaises(FileNotFoundError):
            publish_update_site(self.source, self.destination)

    def test_corrupted_artifact_checksum_fails(self):
        path = next((self.source / "plugins").glob("*.jar"))
        data = path.read_bytes()
        path.write_bytes(bytes([data[0] ^ 1]) + data[1:])
        with self.assertRaisesRegex(ValueError, "checksum"):
            publish_update_site(self.source, self.destination)

    def test_truncated_artifact_size_fails(self):
        path = next((self.source / "plugins").glob("*.jar"))
        path.write_bytes(b"truncated")
        with self.assertRaisesRegex(ValueError, "size"):
            publish_update_site(self.source, self.destination)

    def test_inconsistent_metadata_fails(self):
        path = self.source / "content.xml.xz"
        metadata = ET.fromstring(lzma.decompress(path.read_bytes()))
        metadata.set("name", "inconsistent")
        path.write_bytes(lzma.compress(ET.tostring(metadata)))
        with self.assertRaisesRegex(ValueError, "Inconsistent"):
            publish_update_site(self.source, self.destination)

    def test_missing_metadata_fails(self):
        (self.source / "content.xml.xz").unlink()
        (self.source / "content.jar").unlink()
        with self.assertRaisesRegex(ValueError, "Missing p2 content metadata"):
            publish_update_site(self.source, self.destination)

    def test_missing_content_artifact_reference_fails(self):
        metadata, unused_paths = repository_metadata(self.source, "content")
        artifact = metadata.find("./units/unit/artifacts/artifact")
        self.assertIsNotNone(artifact)
        artifact.set("id", "missing.bundle")
        content = ET.tostring(metadata)
        (self.source / "content.xml.xz").write_bytes(lzma.compress(content))
        with zipfile.ZipFile(self.source / "content.jar", "w") as archive:
            archive.writestr("content.xml", content)
        with self.assertRaisesRegex(ValueError, "referenced by content"):
            publish_update_site(self.source, self.destination)


if __name__ == "__main__":
    unittest.main()
