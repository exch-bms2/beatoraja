# Luaプロパティ名キャッシュ

2026-11-25。main_state.option / number / float_number / text の名前指定を対象にした。

従来は毎回Lua文字列をJava文字列に変換し、containsKeyとgetを実行していた。PropertyNameCacheはLuaStringをキーに解決済みプロパティを保持し、ヒット時の変換を省く。Optional.emptyで未登録の名前もキャッシュする。数値（数値文字列を含む）のID解決は従来どおり先に行う。

キャッシュは各API exporterに属する。値そのものは保存せず、プロパティのget(state)は毎回実行する。キーを登録するときはバイト列をコピーし、大きなsubstringバッファの保持を避ける。

## 計測

JDK 17.0.19 arm64、JMH 1.37、3 forks、warmup 3×1秒、measurement 3×1秒、-Xms512m -Xmx512m、-prof gc。キャッシュが暖まった同じLuaStringで比較。キャッシュミス、毎回異なる名前、数値ID、画面全体の速度は測定していない。

| 名前 | 旧処理 ns/op | 新処理 ns/op | 旧 B/op | 新 B/op |
| --- | ---: | ---: | ---: | ---: |
| 登録済み | 25.81 | 3.03 | 120 | ≈0 |
| 未登録（解決結果はnull） | 28.00 | 3.08 | 128 | ≈0 |

ModernChicとWMIIは数値ID指定が多いため、この検索の改善率からフレーム時間の改善率は推定しない。

## 検証

PropertyNameCacheTest、MainStateInputLuaApiTest、SkinCompatibilityApiTest、SkipDrawWhenTimerOffTestが成功。名前の内容一致、異なるオフセットの文字列、負のキャッシュ、プロパティ値の更新、4種類のAPIの未知名フォールバックを確認した。

生データと測定ソースは property-name-cache-performance.json に保存。実装はbeatoraja側のみで、luaj jarの変更はない。
