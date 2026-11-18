package bms.player.beatoraja.skin.lua;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

/** Normal skin IO must reject links for both existing and newly created files. */
public final class SkinLuaIoSymlinkTest {

    public static void main(String[] args) throws Exception {
        Path temp = Files.createTempDirectory("skin-lua-io-links");
        try {
            Path root = Files.createDirectory(temp.resolve("skin"));
            Path outside = Files.createDirectory(temp.resolve("outside"));
            Files.writeString(root.resolve("normal.txt"), "normal");
            Files.writeString(outside.resolve("keep.txt"), "keep");
            var lua = new SkinLuaAccessor(false);
            try {
                lua.setDirectory(root);
                lua.exec("local f=assert(io.open('normal.txt','r')); assert(f:read('*a')=='normal'); f:close(); "
                        + "local w=assert(io.open('nested/new.txt','w')); w:write('new'); w:close(); "
                        + "local a=assert(io.open('nested/new.txt','a')); a:write('!'); a:close(); "
                        + "local t=assert(io.tmpfile()); t:write('temp'); t:close()");
                check(Files.readString(root.resolve("nested/new.txt")).equals("new!"), "Regular writes must work");
                verifyBufferedIo(root, lua);
                assertDenied(lua, "../outside/keep.txt", "r");
                assertDenied(lua, outside.resolve("keep.txt").toString(), "r");
                try {
                    Files.createSymbolicLink(root.resolve("file-link"), outside.resolve("keep.txt"));
                    Files.createSymbolicLink(root.resolve("dir-link"), outside);
                    Files.createSymbolicLink(root.resolve("dangling"), outside.resolve("missing.txt"));
                    Files.createSymbolicLink(root.resolve("internal-link"), root.resolve("normal.txt"));
                    Files.createSymbolicLink(root.resolve("cycle"), root);
                    Files.createSymbolicLink(temp.resolve("root-link"), root);
                } catch (IOException | UnsupportedOperationException | SecurityException e) {
                    System.out.println("Symlink-specific tests skipped: " + e);
                    return;
                }
                for (String mode : List.of("r", "w", "a", "r+", "w+", "a+")) {
                    for (String path : List.of("file-link", "dir-link/keep.txt", "dir-link/new/deep.txt",
                            "dangling", "internal-link", "cycle/normal.txt")) {
                        assertDenied(lua, path, mode);
                        assertDenied(lua, root.resolve(path).toString(), mode);
                    }
                }
                lua.exec("assert(not pcall(io.input,'file-link')); assert(not pcall(io.output,'file-link')); "
                        + "assert(not pcall(io.lines,'file-link'))");
                check(Files.readString(outside.resolve("keep.txt")).equals("keep"), "Outside file must not change");
                check(!Files.exists(outside.resolve("new")), "Do not create directories through links");
                check(!Files.exists(outside.resolve("missing.txt")), "Do not follow dangling links");
                check(Files.readString(root.resolve("normal.txt")).equals("normal"), "Reject links inside the skin too");

                lua.setDirectory(temp.resolve("root-link"));
                assertDenied(lua, "normal.txt", "r");
                lua.exec("local ok,f=pcall(io.tmpfile); assert(not ok or f==nil)");
                lua.setDirectory(root);
                lua.exec("local f=assert(io.open('normal.txt','r')); f:close()");
            } finally {
                lua.dispose();
            }
            System.out.println("SkinLuaIoSymlinkTest passed");
        } finally {
            // Files.walk does not follow directory links.
            try (var paths = Files.walk(temp)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }

    private static void verifyBufferedIo(Path root, SkinLuaAccessor lua) throws IOException {
        // 1. Multi-line file crossing multiple 8KB (8192-byte) buffer boundaries.
        StringBuilder linesContent = new StringBuilder();
        for (int i = 1; i <= 1000; i++) {
            linesContent.append(String.format("line-%04d-payload-0123456789\n", i));
        }
        check(linesContent.length() == 29000, "Test payload must span multiple 8KB buffers");
        Files.writeString(root.resolve("multiline.txt"), linesContent.toString());

        lua.exec("""
                local count = 0
                for line in io.lines('multiline.txt') do
                    count = count + 1
                    local expected = string.format('line-%04d-payload-0123456789', count)
                    assert(line == expected, 'Mismatch at line ' .. count .. ': ' .. tostring(line))
                end
                assert(count == 1000, 'Expected 1000 lines, got ' .. count)

                -- Partial buffer consumption (*l) followed by multi-buffer read(n) and read(*a).
                local f = assert(io.open('multiline.txt', 'r'))
                assert(f:read(0) == '', 'read(0) before EOF must return empty string')
                local first = assert(f:read('*l'))
                assert(first == 'line-0001-payload-0123456789')
                local mid = assert(f:read(10000))
                assert(#mid == 10000, 'read(10000) after buffered read(*l) must not truncate at 8KB boundary: ' .. #mid)
                local rest = assert(f:read('*a'))
                assert(f:read(0) == nil, 'read(0) at EOF must return nil')
                assert(f:read('*l') == nil, 'read(*l) at EOF must return nil')
                assert(f:read(1) == nil, 'read(1) at EOF must return nil')
                assert(f:read('*a') == '', 'read(*a) at EOF must return empty string')
                f:close()
                local g = assert(io.open('multiline.txt', 'r'))
                local all = assert(g:read('*a'))
                g:close()
                local joined = first .. '\\n' .. mid .. rest
                assert(#all == 29000 and #joined == 29000, 'Expected 29000 bytes, got ' .. #joined .. ' and ' .. #all)
                assert(joined == all, 'Concatenated partial reads must match full file content')
                """);

        // 2. Unsigned byte values (0x00..0xFF) across 8KB boundary without sign extension or false EOF on 0xFF.
        byte[] binary = new byte[9216];
        for (int i = 0; i < binary.length; i++) {
            binary[i] = (byte) ((i * 37 + 0x80) & 0xFF);
        }
        binary[8191] = (byte) 0xFF;
        binary[8192] = (byte) 0x80;
        Files.write(root.resolve("binary.bin"), binary);

        lua.exec("""
                local f = assert(io.open('binary.bin', 'rb'))
                local all = assert(f:read('*a'))
                assert(#all == 9216, 'Expected 9216 binary bytes, got ' .. #all)
                assert(string.byte(all, 8192) == 0xFF, 'Byte at 8191 (1-based 8192) must be 0xFF')
                assert(string.byte(all, 8193) == 0x80, 'Byte at 8192 (1-based 8193) must be 0x80')
                assert(f:seek('set', 8190) == 8190)
                local slice = assert(f:read(4))
                assert(#slice == 4)
                assert(string.byte(slice, 2) == 0xFF and string.byte(slice, 3) == 0x80)
                f:close()
                """);

        // 3. Number parsing (*n) using peek()/read() across 8KB boundary and right up to EOF.
        String numContent = " ".repeat(8188) + "-12345.5 6789";
        Files.writeString(root.resolve("numbers.txt"), numContent);
        lua.exec("""
                local f = assert(io.open('numbers.txt', 'r'))
                local n1 = f:read('*n')
                local n2 = f:read('*n')
                local n3, err = f:read('*n')
                f:close()
                assert(n1 == -12345.5, 'Expected -12345.5 across 8KB boundary, got ' .. tostring(n1))
                assert(n2 == 6789, 'Expected 6789 at EOF without trailing newline, got ' .. tostring(n2))
                assert(n3 == nil and err == nil, 'Expected nil at EOF without error, got ' .. tostring(n3) .. ', ' .. tostring(err))
                """);

        // 4. seek('set'/'cur'/'end') and r+ read/write interleaving with active read buffer.
        Files.writeString(root.resolve("rw.txt"), "0123456789abcdefghij");
        lua.exec("""
                local f = assert(io.open('rw.txt', 'r+'))
                assert(f:read(4) == '0123')
                assert(f:seek() == 4, 'Logical position after reading 4 bytes must be 4')
                assert(f:read(2) == '45')
                assert(f:seek('cur', -2) == 4, 'Relative seek backwards from logical position')
                assert(f:read(2) == '45')
                assert(f:write('ABCD'))
                assert(f:seek() == 10, 'Position after write must be 10')
                assert(f:read('*a') == 'abcdefghij', 'Read after write must continue from new position')
                assert(f:seek('end', -4) == 16)
                assert(f:read('*a') == 'ghij')
                f:close()
                """);
        check(Files.readString(root.resolve("rw.txt")).equals("012345ABCDabcdefghij"),
                "r+ write after buffered read must write at logical file position");
    }

    private static void assertDenied(SkinLuaAccessor lua, String path, String mode) {
        String quoted = "'" + path.replace("\\", "\\\\").replace("'", "\\'") + "'";
        lua.exec("local f,e=io.open(" + quoted + ",'" + mode + "'); "
                + "if f then f:close() end; assert(f==nil and e~=nil,'Expected IO access to be denied')");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
