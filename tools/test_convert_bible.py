import os
import sqlite3
import tempfile
import unittest
import zipfile

from convert_bible import main as convert_main


class ConvertBibleTest(unittest.TestCase):
    def test_epub_to_sqlite_conversion(self):
        with tempfile.TemporaryDirectory() as tmp:
            epub_path = os.path.join(tmp, "sample_nlt.epub")
            sqlite_path = os.path.join(tmp, "nlt.sqlite")

            container_xml = """<?xml version="1.0"?>
            <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
              <rootfiles>
                <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
              </rootfiles>
            </container>"""

            opf_xml = """<?xml version="1.0"?>
            <package xmlns="http://www.idpf.org/2007/opf" version="2.0">
              <manifest>
                <item id="gen" href="genesis.xhtml" media-type="application/xhtml+xml"/>
                <item id="jhn" href="john.xhtml" media-type="application/xhtml+xml"/>
              </manifest>
              <spine>
                <itemref idref="gen"/>
                <itemref idref="jhn"/>
              </spine>
            </package>"""

            gen_html = """<html><body>
              <h1>Genesis</h1>
              <h2>Chapter 1</h2>
              <p><sup>1</sup>In the beginning God created the heavens and the earth.
              <sup>2</sup>The earth was formless and empty, and darkness covered the deep waters.</p>
            </body></html>"""

            john_html = """<html><body>
              <h1>John</h1>
              <h2>Chapter 3</h2>
              <p><span class="verse">16</span>For this is how God loved the world: He gave his one and only Son.</p>
            </body></html>"""

            with zipfile.ZipFile(epub_path, "w") as zf:
                zf.writestr("META-INF/container.xml", container_xml)
                zf.writestr("OEBPS/content.opf", opf_xml)
                zf.writestr("OEBPS/genesis.xhtml", gen_html)
                zf.writestr("OEBPS/john.xhtml", john_html)

            rc = convert_main([
                "--input", epub_path,
                "--output", sqlite_path,
                "--code", "NLT",
                "--name", "New Living Translation",
            ])
            self.assertEqual(rc, 0)
            self.assertTrue(os.path.exists(sqlite_path))

            conn = sqlite3.connect(sqlite_path)
            meta = dict(conn.execute("SELECT key, value FROM meta").fetchall())
            rows = conn.execute("SELECT book_id, chapter, verse, text FROM verses ORDER BY book_id, chapter, verse").fetchall()
            conn.close()

            self.assertEqual(meta["code"], "NLT")
            self.assertEqual(len(rows), 3)
            self.assertEqual(rows[0][:3], (1, 1, 1))
            self.assertIn("beginning", rows[0][3])
            self.assertEqual(rows[1][:3], (1, 1, 2))
            self.assertEqual(rows[2][:3], (43, 3, 16))
            self.assertIn("God loved the world", rows[2][3])


if __name__ == "__main__":
    unittest.main()
