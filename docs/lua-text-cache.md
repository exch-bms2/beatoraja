# main_state.textのUTF-8変換結果を再利用

2026-11-26。main_state.textは毎回プロパティのJava文字列をLuaStringへ変換する。同じテキストでも文字・バイト配列を確保し、UTF-8変換を繰り返していた。

TextFunctionごとのLuaTextCacheに、各StringPropertyの直近1件のJava文字列と変換済みLuaStringを保持する。プロパティのget(state)は毎回呼ぶ。内容が同じなら変換済み値を再利用し、変わったら上書きする。プロパティは参照同一性で識別し、equalsを独自実装したプロパティが干渉しないようにした。未知のプロパティは従来どおり空文字。nullも空文字へ正規化する。

保持するのは直近の値のみで履歴は増えないが、参照した各プロパティについてJava文字列・UTF-8表現・キャッシュ項目を保持するメモリが増える。テキストが毎回変わる場合もキャッシュ検索と比較の分が追加されるため、下記で測定した。

Luaから見える文字列の内容は同じ。Javaから取得したLuaStringの参照同一性は、同じ内容なら再利用される場合が増える。呼び出し元が保持する過去のLuaStringは変更しない。

## 検証

LuaTextCacheTestで同じ内容の別Java文字列、日本語・補助文字・NUL・不完全なサロゲートの既存変換との一致、値の変化、null、空文字、複数プロパティ、getterの実行回数を確認した。独自equalsを持つ別プロパティも区別する。

LuaTextCacheTest、LuaTimerValueTest、PropertyNameCacheTest、StringPropertyLookupTest、MainStateInputLuaApiTest、SkinCompatibilityApiTestの6件が成功。

## JMH

JDK 17.0.19 arm64、JMH 1.37、3 forks、warmup 3×500ms、measurement 3×500ms、512 MiB、-prof gc。旧変換と新キャッシュを同じハーネスで測定した。両方でgetterを毎回呼んでいる。LuaJはbeatoraja/lib/luaj-jse.jar（15bcbdd8…）。

短文は8文字のASCII、長文は日本語・補助文字を含む共通部分を64回反復した文字列。changingでは同じ共通部分で末尾だけ異なる2値を交互に返す。stableでは同じString参照を返す。Java文字列を毎回組み立てるコスト、名前・ID検索、Luaインタプリタと画面全体の速度は含まない。

| テキスト | 内容 | 旧 ns/op | 新 ns/op | 旧 B/op | 新 B/op |
| --- | --- | ---: | ---: | ---: | ---: |
| 短文 | 同じ内容 | 15.55 | 2.60 | 56 | 0 |
| 短文 | 毎回変更 | 15.32 | 18.67 | 56 | 56 |
| 長文 | 同じ内容 | 1408.46 | 2.65 | 4112 | 0 |
| 長文 | 毎回変更 | 1357.74 | 1427.15 | 4112 | 4112 |

同じ内容の連続取得でUTF-8変換とその一時配列を省ける。変換済みLuaStringの保持メモリと、変更時の追加処理が代償となる。画面全体への効果は未計測。

生データ、ベンチマークとヘルパーのソース、比較対象のハッシュはlua-text-cache-performance.jsonに保存。luaj-beatorajaの既存の未コミット作業は変更していない。

## 評価上の注意

毎回変更する短文は15.32→18.67 ns（約22%・3.35 ns増）。長文は1357.74→1427.15 ns（約5%・69 ns増）だが信頼区間が重なる。内容が安定したプロパティが主対象で、常に変わる値ではキャッシュ照合の追加コストがある。長文stableはJVM間の変動が大きく、数値は測定例である。同じ内容の別Java文字列でも機能的に再利用できることはテストしたが、その場合の内容比較コストはJMHでは測っていない。
