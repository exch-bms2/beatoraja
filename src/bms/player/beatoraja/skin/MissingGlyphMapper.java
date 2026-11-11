package bms.player.beatoraja.skin;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 元フォントにない文字の代替グリフ処理を SkinTextFont と SkinTextBitmap で共有する。
 * 解決順: 元フォント → スキン指定の代替フォント → 共通代替フォント(SkinFontSource・SystemFallbackGlyphs が自動で後ろに付ける) → 欠落記号。
 * 補助平面の文字は先に PUA 枠を取り、同じ順で元の文字を探して、その枠に登録する。
 * Resolution order: original font → skin-specified fallbacks → global fallbacks (appended automatically by SkinFontSource /
 * SystemFallbackGlyphs) → missing marker. A supplementary character first takes a PUA slot, is looked up in the same order
 * by its real code point, and is installed under that slot.
 * 補助平面文字の PUA 割り当て、成功・失敗の記録、PUA 枯渇時の欠落記号(□、なければ ?)、グリフ版をここで一度だけ持つ。
 * グリフの探し方と登録先は描画方式ごとに違うので Host に任せる。
 * Shared missing-glyph handling for SkinTextFont and SkinTextBitmap. Owns the PUA mapping of supplementary characters,
 * success/failure bookkeeping, the marker used when PUA slots run out (□, else ?), and the glyph revision.
 * Where glyphs come from and where they are installed differ per renderer, so the Host decides.
 */
final class MissingGlyphMapper {

    private static final char MARKER = '\u25A1';
    private static final char LAST_RESORT_MARKER = '?';

    /**
     * 描画方式ごとの差分。 / Renderer-specific parts.
     */
    interface Host {
        /** 元フォント自身が実グリフを持つか。 / Whether the original font itself holds a real glyph. */
        boolean hasNativeGlyph(int codePoint);

        /**
         * スキン指定の代替フォント、続いて共通代替フォントからグリフを探す。見つからなければ null。
         * Finds a glyph in the skin-specified fallbacks, then in the global fallbacks; null when none is found.
         *
         * @param mappedCodePoint 登録先の文字(補助平面なら PUA 枠) / key to install under (the PUA slot for supplementary characters)
         */
        BitmapFont.Glyph resolveFallbackGlyph(int codePoint, int mappedCodePoint, boolean supplementary) throws Exception;

        /** 描画方式のフォントから欠落記号を mappedCodePoint 用に作る。なければ null。 / Builds a missing marker for mappedCodePoint from the renderer's fonts; null when none. */
        BitmapFont.Glyph resolveMarkerGlyph(int mappedCodePoint) throws Exception;

        /**
         * 解決できなかった BMP 文字の表示用グリフを自分で登録する。登録したら true。
         * Installs the placeholder shown for an unresolved BMP character; true when one was installed.
         */
        boolean installUnresolved(int codePoint);

        /** いま描けるグリフがあるか(元・代替どちらでも)。 / Whether a drawable glyph exists now (original or fallback). */
        boolean hasDrawableGlyph(int codePoint);

        /** 代替グリフを描画用データへ登録する。 / Installs a fallback glyph into the draw data. */
        void installGlyph(int key, BitmapFont.Glyph glyph);

        /** 枠が予約・横取りされたので、その枠のグリフを外す。 / Removes the glyph of a slot that was reserved or taken over. */
        void removeGlyph(int slot, int evictedCodePoint);
    }

    private final Host host;
    private final String failureLabel;
    private final PuaSlotAllocator slots = new PuaSlotAllocator();
    private final Set<Integer> active = new HashSet<>();
    private final Set<Integer> failed = new HashSet<>();
    private final Set<Integer> reported = new HashSet<>();
    private long revision;
    /** この remap 呼び出し中に新しいグリフを登録したか。 / Whether this remap call installed a new glyph. */
    private boolean installed;

    MissingGlyphMapper(Host host, String failureLabel) {
        this.host = host;
        this.failureLabel = failureLabel;
    }

    PuaSlotAllocator slots() {
        return slots;
    }

    long revision() {
        return revision;
    }

    void bumpRevision() {
        revision++;
    }

    boolean isActive(int codePoint) {
        return active.contains(codePoint);
    }

    boolean isFailed(int codePoint) {
        return failed.contains(codePoint);
    }

    Set<Integer> failedCodePoints() {
        return failed;
    }

    /** 外から登録したグリフを成功として記録する。 / Records an externally installed glyph as resolved. */
    void markActive(int codePoint) {
        failed.remove(codePoint);
        active.add(codePoint);
    }

    /** 元フォントが持つ PUA グリフの枠を予約する。 / Reserves PUA slots used by the original font. */
    void reserveNativeGlyphs(BitmapFont.BitmapFontData data) {
        slots.reserveNativeGlyphs(data);
    }

    /** 成功・失敗の記録と動的割り当てを消す。予約は残す。 / Clears bookkeeping and dynamic mappings; reservations stay. */
    void clearMappings() {
        active.clear();
        failed.clear();
        slots.clearMappings();
    }

    /** 予約も含めてすべて消す。 / Clears everything, including reservations. */
    void reset() {
        active.clear();
        failed.clear();
        slots.reset();
    }

    /** この文字列に代替が必要な文字があるか。 / Whether the text has a character that needs a fallback. */
    boolean needsFallback(String text) {
        for (int i = 0; i < text.length();) {
            int codePoint = text.codePointAt(i);
            if (codePoint != '\n' && codePoint != '\r' && !host.hasNativeGlyph(codePoint)
                    && !active.contains(codePoint) && !failed.contains(codePoint)) {
                return true;
            }
            i += Character.charCount(codePoint);
        }
        return false;
    }

    /**
     * 不足グリフを用意し、補助平面の文字を PUA 枠へ置き換えた文字列を返す。
     * Prepares missing glyphs and returns the text with supplementary characters replaced by their PUA slots.
     */
    String remap(String text) {
        slots.reserveLiteralPua(text, (slot, codePoint) -> {
            active.remove((int) slot);
            failed.remove((int) slot);
            host.removeGlyph(slot, codePoint);
            revision++;
        });

        Set<Integer> requiredSupplementary = new HashSet<>();
        for (int index = 0; index < text.length();) {
            int codePoint = text.codePointAt(index);
            if (codePoint > Character.MAX_VALUE) {
                requiredSupplementary.add(codePoint);
            }
            index += Character.charCount(codePoint);
        }

        installed = false;
        StringBuilder result = null;
        for (int index = 0; index < text.length();) {
            int codePoint = text.codePointAt(index);
            if (codePoint > Character.MAX_VALUE) {
                if (result == null) {
                    result = new StringBuilder(text.length());
                    result.append(text, 0, index);
                }
                Character mapped = mapSupplementary(codePoint, requiredSupplementary);
                result.append(mapped != null ? mapped.charValue() : marker());
            } else {
                ensureBmp(codePoint);
                if (result != null) {
                    result.append((char) codePoint);
                }
            }
            index += Character.charCount(codePoint);
        }
        if (installed) {
            revision++;
        }
        return result != null ? result.toString() : text;
    }

    /**
     * PUA 枠の枯渇や割り当て失敗でも文字を消さず、欠落記号(□、なければ ?)を出す。
     * Never drop a character on PUA exhaustion or failure; show a marker (□, else ?).
     */
    private char marker() {
        ensureBmp(MARKER);
        if (host.hasDrawableGlyph(MARKER) || active.contains((int) MARKER)) {
            return MARKER;
        }
        ensureBmp(LAST_RESORT_MARKER);
        return LAST_RESORT_MARKER;
    }

    /**
     * BMP 文字の代替グリフを用意する。 / Prepares a fallback glyph for a BMP character.
     */
    void ensureBmp(int codePoint) {
        if (codePoint < 0 || codePoint > Character.MAX_VALUE || codePoint == '\n' || codePoint == '\r'
                || active.contains(codePoint) || failed.contains(codePoint) || host.hasNativeGlyph(codePoint)) {
            return;
        }
        BitmapFont.Glyph glyph;
        try {
            glyph = resolve(codePoint, codePoint, false);
        } catch (Exception e) {
            failed.add(codePoint);
            reportFailure(codePoint, e);
            return;
        }
        if (glyph != null) {
            install(codePoint, glyph);
            active.add(codePoint);
            return;
        }
        // 解決できない文字は失敗として記録し、表示用のグリフがあればそれで描く。
        // Record unresolved characters as failed and draw them with the host's placeholder when it has one.
        failed.add(codePoint);
        if (host.installUnresolved(codePoint)) {
            installed = true;
        }
    }

    /**
     * 補助平面の文字に PUA 枠を割り当て、代替グリフをその枠に登録する。失敗時は null。
     * Maps a supplementary character to a PUA slot and installs its fallback glyph there; null on failure.
     */
    private Character mapSupplementary(int codePoint, Set<Integer> requiredCodePoints) {
        if (failed.contains(codePoint)) {
            return null;
        }
        Character existing = slots.getMappedSlot(codePoint);
        if (existing != null) {
            return existing;
        }
        Character mapped = slots.allocateSlot(codePoint, requiredCodePoints, (slot, evictedCodePoint) -> {
            host.removeGlyph(slot, evictedCodePoint);
            revision++;
        });
        if (mapped == null) {
            return null;
        }
        BitmapFont.Glyph glyph;
        try {
            glyph = resolve(codePoint, mapped.charValue(), true);
        } catch (Exception e) {
            failed.add(codePoint);
            slots.releaseSlot(codePoint);
            reportFailure(codePoint, e);
            return null;
        }
        if (glyph == null) {
            failed.add(codePoint);
            slots.releaseSlot(codePoint);
            return null;
        }
        install(mapped.charValue(), glyph);
        return mapped;
    }

    /** 代替フォント(スキン指定 → 共通)、なければ欠落記号。 / Fallback fonts (skin-specified, then global), else the missing marker. */
    private BitmapFont.Glyph resolve(int codePoint, int mappedCodePoint, boolean supplementary) throws Exception {
        BitmapFont.Glyph glyph = host.resolveFallbackGlyph(codePoint, mappedCodePoint, supplementary);
        return glyph != null ? glyph : host.resolveMarkerGlyph(mappedCodePoint);
    }

    private void install(int key, BitmapFont.Glyph glyph) {
        host.installGlyph(key, glyph);
        installed = true;
    }

    /** 失敗は同じ文字につき1回だけ記録する。 / Each failure is logged only once per character. */
    void reportFailure(int codePoint, Exception cause) {
        if (reported.add(codePoint)) {
            Logger.getGlobal().warning(String.format("%s: U+%04X (%s)", failureLabel, codePoint, cause.getClass().getSimpleName()));
        }
    }

    /**
     * 補助平面文字(U+10000 以上)を BMP の私用領域(PUA: U+E000..U+F8FF、6,400 枠)へ一時的に割り当てる。
     * 元フォントが持つ PUA アイコンや、文字列にそのまま含まれる PUA 文字の枠は予約して上書きしない。
     * Temporarily maps supplementary-plane characters (U+10000+) to BMP Private Use Area slots (U+E000..U+F8FF, 6,400 slots).
     * Slots used by PUA icons in the original font or by literal PUA characters in the text are reserved and never overwritten.
     */
    public static final class PuaSlotAllocator {

        public static final char PUA_START = 0xe000;
        public static final char PUA_END = 0xf8ff;
        public static final int TOTAL_SLOTS = PUA_END - PUA_START + 1; // 6400

        /**
         * 割り当て済みの枠が解放・横取りされたときに呼ばれる。 / Called when a mapped slot is released or taken over.
         */
        @FunctionalInterface
        public interface EvictionListener {
            void onEvicted(char slot, int evictedCodePoint);
        }

        private final BitSet reservedSlots = new BitSet(TOTAL_SLOTS);
        private final Map<Integer, Character> codePointToSlot = new HashMap<>();
        /** アクセス順(LRU)を保つ逆引き表。 / Reverse map kept in access (LRU) order. */
        private final LinkedHashMap<Character, Integer> slotToCodePoint = new LinkedHashMap<>(16, 0.75f, true);

        public PuaSlotAllocator() {
        }

        /**
         * 枠を予約する。既に動的に割り当て済みなら、その割り当てを外してから予約する。
         * Reserves a slot, dropping any dynamic mapping it currently holds.
         */
        private void reserveSlot(char slot, EvictionListener listener) {
            if (slot >= PUA_START && slot <= PUA_END) {
                reservedSlots.set(slot - PUA_START);
                Integer previous = slotToCodePoint.remove(slot);
                if (previous != null) {
                    codePointToSlot.remove(previous);
                    if (listener != null) {
                        listener.onEvicted(slot, previous);
                    }
                }
            }
        }

        /**
         * 枠が元フォントまたは文字列中の PUA 文字のために予約されているか。
         * Whether a slot is reserved for the original font or for a literal PUA character.
         */
        boolean isReserved(char slot) {
            if (slot >= PUA_START && slot <= PUA_END) {
                return reservedSlots.get(slot - PUA_START);
            }
            return false;
        }

        /**
         * 元の BitmapFontData が持つ PUA グリフ(リズムゲーム用アイコンなど)をすべて予約する。
         * Reserves every PUA glyph already present in the original BitmapFontData (e.g. rhythm-game icons).
         */
        void reserveNativeGlyphs(BitmapFont.BitmapFontData data) {
            if (data == null) {
                return;
            }
            for (int c = PUA_START; c <= PUA_END; c++) {
                if (data.getGlyph((char) c) != null) {
                    reserveSlot((char) c, null);
                }
            }
        }

        /**
         * 文字列にそのまま含まれる BMP の PUA 文字を予約し、動的割り当てで上書きされないようにする。
         * Reserves literal BMP PUA characters in the text so dynamic mappings never overwrite them.
         */
        void reserveLiteralPua(String text, EvictionListener listener) {
            if (text == null) {
                return;
            }
            for (int i = 0; i < text.length(); ) {
                int cp = text.codePointAt(i);
                if (cp >= PUA_START && cp <= PUA_END) {
                    reserveSlot((char) cp, listener);
                }
                i += Character.charCount(cp);
            }
        }

        /**
         * 割り当て済みの枠を返し、LRU 順を更新する。未割り当てなら null。
         * Returns the mapped slot and refreshes its LRU position; null when unmapped.
         */
        Character getMappedSlot(int codePoint) {
            Character slot = codePointToSlot.get(codePoint);
            if (slot != null) {
                slotToCodePoint.get(slot); // LRU 順の更新 / refresh LRU order
                return slot;
            }
            return null;
        }

        /**
         * 補助平面文字に予約外の枠を割り当てる。空きがなければ requiredCodePoints に含まれない最も古い枠を横取りする。
         * Allocates an unreserved slot for a supplementary code point. When none is free, the least recently used
         * slot whose code point is not in requiredCodePoints is taken over.
         *
         * @param supplementaryCodePoint 割り当てる補助平面文字 / supplementary code point to map
         * @param requiredCodePoints 現在の描画に必要で横取りしてはいけない文字 / code points needed now that must not be evicted
         * @param listener 横取り時に呼ばれる(null 可) / called on eviction (nullable)
         * @return 割り当てた枠。割り当てできなければ null / the slot, or null when nothing can be allocated
         */
        Character allocateSlot(int supplementaryCodePoint, Set<Integer> requiredCodePoints, EvictionListener listener) {
            if (supplementaryCodePoint <= Character.MAX_VALUE) {
                return null;
            }
            Character existing = codePointToSlot.get(supplementaryCodePoint);
            if (existing != null) {
                slotToCodePoint.get(existing); // LRU 順の更新 / refresh LRU order
                return existing;
            }

            // 1. 予約されておらず未使用の枠を先頭から探す / Linear scan for a free, unreserved slot
            for (int c = PUA_START; c <= PUA_END; c++) {
                char candidate = (char) c;
                if (isReserved(candidate)) {
                    continue;
                }
                if (!slotToCodePoint.containsKey(candidate)) {
                    codePointToSlot.put(supplementaryCodePoint, candidate);
                    slotToCodePoint.put(candidate, supplementaryCodePoint);
                    return candidate;
                }
            }

            // 2. 空きがなければ、必要でない最も古い枠を横取りする / Otherwise evict the least recently used unneeded slot
            for (Map.Entry<Character, Integer> entry : slotToCodePoint.entrySet()) {
                char candidate = entry.getKey();
                if (isReserved(candidate)) {
                    continue;
                }
                int mappedCp = entry.getValue();
                if (requiredCodePoints == null || !requiredCodePoints.contains(mappedCp)) {
                    if (listener != null) {
                        listener.onEvicted(candidate, mappedCp);
                    }
                    codePointToSlot.remove(mappedCp);
                    slotToCodePoint.remove(candidate);
                    codePointToSlot.put(supplementaryCodePoint, candidate);
                    slotToCodePoint.put(candidate, supplementaryCodePoint);
                    return candidate;
                }
            }

            return null; // すべての枠が予約済みか使用中 / every slot is reserved or in use
        }

        /**
         * 補助平面文字に割り当てた枠を解放する(グリフ生成に失敗したときの巻き戻し)。
         * Releases the slot mapped to a supplementary code point (rollback after glyph generation failure).
         */
        void releaseSlot(int supplementaryCodePoint) {
            Character slot = codePointToSlot.remove(supplementaryCodePoint);
            if (slot != null) {
                slotToCodePoint.remove(slot);
            }
        }

        /**
         * 動的な割り当てだけを消す。予約は残す。 / Clears dynamic mappings only; reservations are kept.
         */
        void clearMappings() {
            codePointToSlot.clear();
            slotToCodePoint.clear();
        }

        /**
         * 動的な割り当てと予約の両方を消す。 / Clears both dynamic mappings and reservations.
         */
        public void reset() {
            clearMappings();
            reservedSlots.clear();
        }
    }
}
