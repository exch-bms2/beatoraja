# StringPropertyFactoryのID検索を索引化

2026-11-25。数値IDで文字列プロパティを引くとき、従来はStringPropertyPattern.values()の配列を生成し、11種類のID範囲を順番に探していた。

ID範囲の各プロパティを起動時にIntMapへ登録し、実行中はIDで直接検索する。範囲が重なった場合は列挙値の先勝ちを維持し、単独のStringTypeより範囲指定を優先する既存の順序も維持した。名前検索では列挙値配列を使い回す。プロパティの値は毎回取得する。

## 検証

StringPropertyLookupTestで-1から65536までの65538個のIDと整数の最小・最大値を従来の順次検索と比較し、同一のプロパティを返すことを確認した。名前、別名、範囲外、nullも確認した。
関連するPropertyNameCacheTest、MainStateInputLuaApiTest、SkinCompatibilityApiTest、SkipDrawWhenTimerOffTestも成功。

## JMH

JDK 17.0.19 arm64、JMH 1.37。各測定2 forks、warmup 2×500ms、measurement 3×500ms、ヒープ512 MiB、-prof gc。
変更前Aと変更後BをA1 -> B1 -> B2 -> A2の順で測定した。表は各構成2回のスコアの単純平均。変更前のStringPropertyFactoryクラスを保存し、同じ実行クラスパスの先頭で差し替えて比較した。
短いマイクロベンチマークであり、起動時の索引生成やフレーム全体の改善率を測ってはいない。

| ID | 変更前 ns/op | 変更後 ns/op | 時間の変化 | B/op |
| --- | ---: | ---: | ---: | ---: |
| 120（追加測定） | 4.97 | 1.04 | -79.0% | 64 -> approx. 0 |
| 119 | 4.30 | 1.03 | -76.0% | 64 -> approx. 0 |
| 10 | 5.78 | 2.18 | -62.2% | 64 -> approx. 0 |
| 65535 | 8.17 | 1.41 | -82.7% | 64 -> approx. 0 |

変更前の全ケースで64 B/opだった配列確保が、変更後は約0になった。ID 120はランキング名の範囲（変更前後各1測定を追加）、119はその直前の未登録ID、10は単独のStringType側、65535は未登録ID。名前検索の時間は未計測。

JFRで見えた列挙値配列の確保を直接取り除く変更だが、実スキンのフレーム時間への効果は未測定。索引と列挙値配列を常駐させるため、少量の固定メモリが増える。
測定ソース、基準と変更後ソースのSHA-256、JMH生データはstring-property-lookup-performance.jsonに保存した。
