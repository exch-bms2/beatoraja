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

With Junrar 8.1.2 the RAR5 fixture is decoded rather than rejected. It contains
`FILE1.TXT` / `FILE2.TXT` with `file1\r\n` / `file2\r\n`. Additional RAR5 stored,
large-dictionary, REDIR and split-header fixtures are generated in Java, including
valid header CRCs; no external archive utility is required.

Additional unmodified fixtures from Junrar v8.1.2, commit
`b9b61cedd7c0b7fcbd2ab6b4cc6413e2bb9e481d`, under the same resource directory:

| Constant | Upstream path | SHA-256 |
| --- | --- | --- |
| RAR5_SOLID | `solid/rar5-solid.rar` | `d004997e805cb7bb47bd73904aae113f81ee1d82d80bed9372c5dd64d1684754` |
| RAR5_PASSWORD | `password/rar5-password-junrar.rar` | `3fd635f8e1b880d10c1fea72ab3b2487309d8699dabb3869c3a3b489ec36ee92` |

The solid fixture contains `file1.txt` through `file9.txt`, each `fileN\n`.
The password fixture is used only to verify rejection, not password-based extraction.
