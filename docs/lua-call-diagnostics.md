# Lua呼び出し計測（luaj高速化の実測用）

luaj-beatorajaの最適化を判断するための計測です。対象は、実スキンでのJava→Lua呼び出しの回数・時間・確保量です。
既定では無効です。無効時は呼び出し箇所の `site` がnullになり、計測処理を一切通りません。

## 何を測るか

- `SkinLuaAccessor` の boolean/integer/float/string/timer プロパティ経由のLua呼び出し。
  毎フレーム評価されるスキン値のほぼすべてが、この経路を通ります。
  イベントとWriter（クリックやスライダー操作など、低頻度の経路）は対象外です。
- 状態（SELECT/PLAY/RESULTなど）の訪問ごとに、ウォームアップ後のフレームだけを集計します。
  これでスキンのロードとJITのウォームアップを除外します。
- 時間は包括的に測ります。Luaから呼ばれる `main_state.*` などのJava関数と、戻り値の `toint()` 等の変換も含みます。
- 描画スレッド以外からの呼び出しは、回数だけを `off-render-thread calls` に数えます。

| モード | `-Dbeatoraja.luaDiagnostics=` | 内容 | 計測自体の負荷 |
| --- | --- | --- | --- |
| 回数 | `count` | 呼び出し回数・呼び出し箇所ごとの回数 | 最小 |
| 時間 | `time`（または `true`） | 上記＋呼び出しごとの `nanoTime` | 1回あたり数十ns |
| 確保量 | `alloc` | 上記＋呼び出し内のスレッド確保バイト数 | 大きめ |

計測のオーバーヘッドは、起動時にキャリブレーションした値をレポートに表示します。
回数・時間・確保量は、モードを分けて別々に取るのが基本です。

## 実行

```sh
export JAVA_HOME=/Library/Java/JavaVirtualMachines/liberica-jdk-17-full.jdk/Contents/Home
./gradlew run -Dorg.gradle.java.installations.paths=$JAVA_HOME -PluaDiagnostics=time
```

| Gradleプロパティ | システムプロパティ | 既定値 |
| --- | --- | --- |
| `-PluaDiagnostics` | `beatoraja.luaDiagnostics` | 無効 |
| `-PluaDiagnosticsWarmupMillis` | `beatoraja.luaDiagnostics.warmupMillis` | 5000 |
| `-PluaDiagnosticsOutput` | `beatoraja.luaDiagnostics.output` | `luadiag` |
| `-PluajJar=<path>` | — | `lib/luaj-jse.jar` |

`-PluajJar` を使うと、`lib/` を書き換えずにLua VMのjarを差し替えられます。
レポートの `vm` 行に、実際に読み込まれたjar名が出ます。

出力:
- `luadiag/lua-calls-<run>.txt`：訪問ごとのレポート。ログにも同じ内容が出ます。
- `luadiag/lua-sites-<run>.csv`：呼び出し箇所ごとの値（state, visit, kind, calls, total_ns, max_ns, bytes, label）。

レポートの主な項目は次のとおりです。

- frame interval p50/p95/p99
- 1フレームあたりのLua呼び出し回数
- 1フレームあたりのLua時間（mean/p50/p95/p99/max）と、フレーム間隔に対するLuaの割合
- 描画スレッドの1フレームあたりの確保量と、そのうちLua内の割合（allocモード）
- GC回数と時間
- 上位40の呼び出し箇所

## JFR（どのメソッドで時間とメモリを使っているか）

計測したい画面を表示し、ウォームアップが済んでから実行します。

```sh
scripts/lua-profile-jfr.sh 60
python3 scripts/jfr-hot.py luadiag/profile-<time>.jfr
```

ランチャーではなくゲーム本体のプロセス（`beatoraja.gameProcess=true`）に `jcmd JFR.start` で接続します。
`jfr-hot.py` は、最も忙しいスレッド（通常は描画スレッド）について次を集計します。
- CPUサンプルと確保量の、`org.luaj` / `skin.lua` / beatoraja / gdx 別の割合
- 葉メソッドの上位
- luaj内の包括的な上位メソッド
- 確保量の多いクラスと確保箇所

JFR測定のときは `-PluaDiagnostics` を外します。計測コード自体がプロファイルに混ざるのを避けるためです。

## 測定マトリクス（推奨）

条件をそろえます。同じスキン・同じ楽曲・同じ描画設定・同じウィンドウサイズにし、FPS上限・VSyncは固定、他のアプリは閉じます。
各構成で、選曲画面60秒、プレイ1曲、リザルト30秒を2回以上測ります。

| 構成 | `-PluajJar` |
| --- | --- |
| legacy（現行） | 指定なし（`lib/luaj-jse.jar`、sha256 `15bcbdd8…`） |
| upstream（プールなしの上流） | `../luaj-beatoraja/build/baseline/upstream.jar` |
| fork（Codex最適化版） | `../luaj-beatoraja/target/luaj-beatoraja-0.1.0-SNAPSHOT.jar` |

1. `count` で、1フレームあたりの呼び出し回数と上位の呼び出し箇所を把握する（3構成で同じになるはず）。
2. `time` で、Lua時間/フレームとフレーム間隔に占める割合を比べる。
3. `alloc` で、描画スレッドの確保量に占めるLuaの割合と、MB/sを比べる。
4. 計測を無効にしてJFRを取り、`org.luaj` の包括CPU割合と確保量の上位を比べる。

判断の目安：
- Lua時間がフレーム間隔の数%未満で、Luaの確保量がGCを誘発していない場合、VMの改造は体感に効きません。
  その場合はbeatoraja側の呼び出し経路（定数引数の事前解決など）を優先します。
- 上位の呼び出し箇所が `main_state.*` の名前引きや、文字列返却に集中している場合も、beatoraja側の改善が先です。
- `org.luaj.vm2.LuaClosure.execute` 自体やテーブル探索が上位なら、VM側（インタプリタ・LuaJC）を検討します。
