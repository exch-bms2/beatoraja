# Lua文字列プロパティの復号結果を再利用

2026-11-26。SkinLuaAccessorの文字列プロパティは、Lua関数が同じLuaStringを返しても毎回UTF-8をJava Stringへ復号していた。前回のmain_state.text側のエンコードキャッシュと組み合わせ、同じ文字列オブジェクトを繰り返し返す場合の復号を省く。

Lua関数は毎回呼ぶ。結果が直近と同じLuaString参照の場合だけ復号済みJava文字列を返す。別のLuaStringなら同じ内容でも復号し直す。LuaString以外のLuaValueは、状態を持つtojstring()の可能性があるためキャッシュせず、毎回変換する。例外時の空文字フォールバックと診断計測も維持する。

値と復号結果はfinalフィールドを持つ不変のペアにして、並行呼び出しで取り違えないようにした。変更時はペアを1個確保する。同一参照でのヒットは確保なし。各プロパティにつき直近のLuaStringとJava文字列を保持するため、保持メモリは増える。LuaStringのbyte配列はLuaJのAPI契約どおり変更しない前提。

## 検証

LuaStringResultCacheTestで毎回の関数呼び出し、値の変更、日本語・補助文字・不正バイト列の従来との一致、Lua文字列以外の動的変換、例外と回復、4スレッドからの同時復号を確認。

LuaStringResultCacheTest、LuaTextCacheTest、LuaTimerValueTest、PropertyNameCacheTest、MainStateInputLuaApiTest、SkinCompatibilityApiTestの6件が成功した。

## JMH

JDK 17.0.19 arm64、LuaJはbeatoraja/lib/luaj-jse.jar（15bcbdd8…）、JMH 1.37、3 forks、warmup 3×500ms、measurement 3×500ms、512 MiB、-prof gc。旧復号と新キャッシュを同じハーネスで測定した。両方でJavaのZeroArgFunctionを毎回呼ぶ。Lua VM、プロパティ全体のgetや画面全体の速度は測定していない。

短文は8文字ASCII。長文は日本語・補助文字を含む共通部分を64回反復する。stableは同じLuaString、changingは末尾だけ異なる2つのLuaStringを交互に返す。新しいLuaStringを作るコストは含まない。

| 文字列 | 戻り値 | 旧 ns/op | 新 ns/op | 旧 B/op | 新 B/op |
| --- | --- | ---: | ---: | ---: | ---: |
| 短文 | 同一参照 | 11.45 | 1.72 | 80 | 0 |
| 短文 | 毎回変更 | 11.01 | 12.98 | 80 | 104 |
| 長文 | 同一参照 | 1391.20 | 1.68 | 4896 | 0 |
| 長文 | 毎回変更 | 1395.04 | 1389.54 | 4896 | 4920 |

同じLuaStringが続く場合は復号を省ける。毎回戻り値が変わる場合にはキャッシュ照合と不変ペアの追加処理があり、確保量も増える。この負担と同一参照時の改善の両方を見て評価する。実スキンのフレーム時間への効果は未測定。

生データ、測定ソース、ヘルパー、ソースとVMのハッシュはlua-string-result-cache-performance.jsonに保存。luaj-beatorajaの既存の未コミット高速化には触れていない。
