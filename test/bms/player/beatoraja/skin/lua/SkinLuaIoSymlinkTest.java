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

    private static void assertDenied(SkinLuaAccessor lua, String path, String mode) {
        String quoted = "'" + path.replace("\\", "\\\\").replace("'", "\\'") + "'";
        lua.exec("local f,e=io.open(" + quoted + ",'" + mode + "'); "
                + "if f then f:close() end; assert(f==nil and e~=nil,'Expected IO access to be denied')");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
