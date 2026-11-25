# Luaへ返すタイマーOFF値の共有

2026-11-26。タイマーOFFのLong.MIN_VALUEはLuaNumber.valueOfで毎回LuaDoubleを確保していた。immutableなOFF値をLuaTimerValueで1個共有し、オンの値は従来どおり毎回変換する。

対象はmain_state.timer、timer_util.timer_function、timer_observe_boolean、new_passive_timerのtimer。監視関数の実行回数・順序・タイマーの更新条件は変更しない。Luaの数値の型、比較、long変換、文字列化も維持する。Javaコードから見たOFF値の参照同一性は共有に変わるが、Lua側の数値比較は従来どおり。

## 検証

LuaTimerValueTestでLong.MIN_VALUEの前後、整数キャッシュの境界、2^53付近、Long.MAX_VALUEを従来の変換と比較。実APIでのON/OFF、開始時刻の維持、最新値の取得、OFF定数との比較、受動タイマーの遷移を確認した。

LuaTimerValueTest、SkipDrawWhenTimerOffTest、PropertyNameCacheTest、MainStateInputLuaApiTest、SkinCompatibilityApiTest、PropertyIdCacheTestの6件が成功。

## JMH

JDK 17.0.19 arm64、LuaJはbeatorajaのlib/luaj-jse.jar（15bcbdd8…）、JMH 1.37、各測定2 forks、warmup 2×500ms、measurement 3×500ms、512 MiB、-prof gc。A1（旧）→B1（新）→B2（新）→A2（旧）の順。旧TimerUtilityとその内部クラスを保存し、同じクラスパスの先頭で差し替えた。表は各構成2回のスコアの単純平均。

監視関数はJavaの固定真偽値関数。Luaインタプリタの実行や画面全体は含まない。オン時刻は1234567890 µsで、LuaIntegerになる条件。2^31以上でLuaDoubleになるオン時刻の速度はこの測定に含まない。

| 対象 | 状態 | 旧 ns/op | 新 ns/op | 旧 B/op | 新 B/op |
| --- | --- | ---: | ---: | ---: | ---: |
| 監視タイマー | OFF | 1.66 | 0.84 | 24 | 0 |
| 監視タイマー | ON | 1.52 | 1.50 | 16 | 16 |
| 受動タイマー | OFF | 1.44 | 0.61 | 24 | 0 |
| 受動タイマー | ON | 1.37 | 1.36 | 16 | 16 |

OFF時は24 B/回の確保がなくなる。関数評価やタイマー判定そのものは残るため、前回の監視判定約69 µsを大きく削減できるとは評価しない。小さな確保量削減であり、実スキン全体の速度は未測定。

生データ、測定ソース、比較対象のハッシュはtimer-off-value-performance.jsonに保存。luaj-beatoraja側の単純Lua関数の高速化（別の未コミット作業）は本変更に含めない。
