# Lua offset テーブルの初期容量

`MainStatePropertyLuaApiExporter.OffsetFunction` の `new LuaTable()` を
`new LuaTable(0, 6)` に変更。既知の6項目 x/y/w/h/r/a に必要なハッシュ容量を
先に確保し、追加時の拡張・再配置と途中の配列割り当てを省く。

呼び出しごとの新規テーブル、最新値の取得、キーと数値変換は維持する。
Lua側が返却テーブルを変更しても次の返却結果には伝わらない。
テーブル列挙順には依存しないこと（Lua APIとして順序は保証しない）。

## 計測

JDK 17.0.19 arm64 / JMH 1.37、3 forks、warmup/measurement 各3回500ms、
heap 512 MiB、`-prof gc`。beatoraja既定の `lib/luaj-jse.jar` を使用。
6個の float を同じ順に set し、テーブル全体を返して比較。
ゼロ値と小数値の2条件を測定した。

| 値 | 変更前 ns/op | 変更後 ns/op | 割り当て B/op 前→後 |
| --- | ---: | ---: | ---: |
| 全項目ゼロ | 66.44 ± 3.55 | 35.37 ± 0.76 | 496 → 416 |
| 小数値 | 63.16 ± 2.96 | 28.67 ± 0.21 | 496 → 416 |

誤差はJMHの99.9%信頼区間。割り当ては約16%減。
ここでの時間はテーブル生成単体で、Lua呼び出しや状態取得の費用は含まない。
ゲーム全体の速度改善率は未測定。`main_state.offset()` を呼ぶ箇所だけに効く。

生データ、ベンチマーク・実行コード、VMのSHA-256は
`lua-offset-capacity-performance.json` に保存した。

## 確認

既存の Gradle 回帰テスト MainStateInputLuaApiTest / SkinCompatibilityApiTest /
LuaTimerValueTest / LuaStringResultCacheTest が成功。
LuaStringResultCacheTestの expected test failure 警告は例外回復確認の想定ログ。
