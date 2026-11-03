# Junrar Test Fixtures

The Base64 constants in `../RarSongArchiveTest.java` are unmodified binaries from
Junrar v7.6.0, commit `cb4b7fdb84269e95741fa22120e08ceb15a06152`:

| Constant | Upstream path under `src/test/resources/com/github/junrar/` | SHA-256 |
| --- | --- | --- |
| COMPRESSED | `test.rar` | `65f8921cab788afaa177f7c080c29a364a77eea91ee57c4390f5d4d14527da60` |
| SOLID | `solid/rar4-solid.rar` | `b158d4b0e3506f04fcaff18db9b369b573fd54f313062c7b8c622cb33b9c5da4` |
| RAR5 | `rar5.rar` | `aaf1a7d974f302f181720280151053f15ef1119b13d2f7bf9fddf2163bbf1701` |

Source: https://github.com/junrar/junrar/tree/cb4b7fdb84269e95741fa22120e08ceb15a06152

Junrar's upstream license is retained in `JUNRAR-LICENSE.txt`. Test data is embedded
in test code only and is not shipped in the application distribution. No downloads
are needed to run the tests.

Expected compressed content (`foo/bar.txt`: `baz\n`) was independently checked
using macOS libarchive/bsdtar. Solid content (`file1.txt` through `file9.txt`, each
`fileN\n`) matches Junrar's upstream `ArchiveTest`. macOS bsdtar cannot extract
this solid fixture, so it was not used as an independent solid-content check.
