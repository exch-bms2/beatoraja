# Lua skin main_state API

Lua skin can access selected runtime values through the global `main_state`
table. These APIs are intentionally narrower than `luajava`; file paths are
resolved inside the current skin directory where applicable.

## Properties

| API | Return | Description |
| --- | --- | --- |
| `main_state.option(id)` | boolean | Returns a boolean skin property by ID. |
| `main_state.number(id)` | number | Returns an integer skin property by ID. |
| `main_state.float_number(id)` | number | Returns a float/rate skin property by ID. |
| `main_state.text(id)` | string | Returns a string skin property by ID. |
| `main_state.offset(id)` | table | Returns `{x, y, w, h, r, a}` for a skin offset. |
| `main_state.event_index(id)` | number | Returns an image index property by ID. |
| `main_state.judge(id)` | number | Returns total judge count for the specified judge index. |

## Score and Gauge

| API | Return | Description |
| --- | --- | --- |
| `main_state.rate()` | number | Current score rate. |
| `main_state.exscore()` | number | Current EX score. |
| `main_state.rate_best()` | number | Best score rate. |
| `main_state.exscore_best()` | number | Best EX score. |
| `main_state.rate_rival()` | number | Rival score rate. |
| `main_state.exscore_rival()` | number | Rival EX score. |
| `main_state.gauge()` | number | Current gauge value in play state, otherwise `0`. |
| `main_state.gauge_type()` | number | Current gauge type in play state, otherwise `0`. |

## Timer and Events

Timer values are microseconds.

| API | Return | Description |
| --- | --- | --- |
| `main_state.timer(id)` | number | Returns the timer value. Off timers return `main_state.timer_off_value`. |
| `main_state.time()` | number | Returns the current main timer time. |
| `main_state.timer_off_value` | number | Timer off sentinel value. |
| `main_state.set_timer(id, value)` | boolean | Sets a skin-writable timer. Throws if the timer is not writable from skin. |
| `main_state.event_exec(id)` | boolean | Executes a skin-runnable event. |
| `main_state.event_exec(id, arg1)` | boolean | Executes a skin-runnable event with one argument. |
| `main_state.event_exec(id, arg1, arg2)` | boolean | Executes a skin-runnable event with two arguments. |

## Input

| API | Return | Description |
| --- | --- | --- |
| `main_state.key_pressed(key)` | boolean | Returns whether a libGDX key is pressed. `key` may be a keycode number or key name such as `"LEFT"`, `"RIGHT"`, `"UP"`, `"DOWN"`. |

## Volume

| API | Return | Description |
| --- | --- | --- |
| `main_state.volume_sys()` | number | System volume. |
| `main_state.volume_key()` | number | Key sound volume. |
| `main_state.volume_bg()` | number | Background volume. |
| `main_state.set_volume_sys(value)` | boolean | Sets system volume. |
| `main_state.set_volume_key(value)` | boolean | Sets key sound volume. |
| `main_state.set_volume_bg(value)` | boolean | Sets background volume. |

## File

File APIs resolve paths relative to the current skin directory and reject paths
outside that directory.

| API | Return | Description |
| --- | --- | --- |
| `main_state.file_exists(path)` | boolean | Returns whether the file exists. |
| `main_state.file_mkdir(path)` | boolean | Creates the directory and parent directories. |
| `main_state.file_list(path)` | string, number | Returns newline-separated file paths and count. |
| `main_state.file_list(path, pattern)` | string, number | Returns newline-separated regex matches and count. Lua `%` escapes are converted to Java regex escapes. |
| `main_state.file_read_lines(path)` | table | Reads UTF-8 lines into a 1-based Lua table. Returns an empty table on failure. |
| `main_state.file_write(path, text)` | boolean | Writes UTF-8 text, replacing existing content. |
| `main_state.file_append(path, text)` | boolean | Appends UTF-8 text. |
| `main_state.file_clear(path)` | boolean | Clears file content. |
| `main_state.file_count_lines(path)` | number | Returns UTF-8 line count, or `0` on failure. |
| `main_state.file_delete(path)` | boolean | Deletes one existing regular file. Directories, symbolic links, paths through symbolic links, and paths outside the skin directory are rejected. Disabled in Preview sandbox. |

The current directory remains the directory containing the skin entry file, not
automatically its parent. New facade file operations and `main_state.file_*`
reject symbolic-link components. This does not establish a race-free filesystem
sandbox against concurrent modification by another process, nor does it change
the separate legacy `io` / Lua module-loading restrictions.

## HTTP

Only `http` and `https` URLs are supported. All HTTP paths (synchronous,
asynchronous, and legacy URL facades) share a default response limit of 1 MiB
and a hard per-request limit of 8 MiB. Limits count body bytes read from the
stream, including UTF-8 multibyte characters and newline bytes. There is no
line-count limit. Oversized bodies fail rather than returning truncated data,
even without Content-Length or when the header understates the real size.
Timeout is clamped to 1-5000 ms.

| API | Return | Description |
| --- | --- | --- |
| `main_state.http_get_lines(url)` | table, true / nil, string | GETs UTF-8 text and returns lines, or `nil` and an error message. |
| `main_state.http_get_lines(url, timeout_ms)` | table, true / nil, string | Same as above with timeout. |
| `main_state.http_get_lines(url, timeout_ms, max_bytes)` | table, true / nil, string | Same as above with a per-request body limit. |
| `main_state.http_get(url)` | string, true / nil, string | GETs UTF-8 text as a single string, or `nil` and an error message. |
| `main_state.http_get(url, timeout_ms)` | string, true / nil, string | Same as above with timeout. |
| `main_state.http_get(url, timeout_ms, max_bytes)` | string, true / nil, string | Same as above with a per-request body limit. |

Pass `nil` for timeout or capacity to retain its default. Explicit `max_bytes`
must be an integer in 1-8388608; out-of-range values return nil/error rather than
silently clamping. Existing synchronous text-return newline normalization is
unchanged; byte limits are checked before UTF-8 decoding.

### Asynchronous HTTP

| API | Return | Description |
| --- | --- | --- |
| `main_state.http_get_async(url, timeout_ms=1000, max_bytes=1048576)` | request ID / nil, error | Starts a bounded GET job with a per-request byte limit. Timeout is clamped to 1-5000 ms and covers queueing and processing. |
| `main_state.http_poll(id)` | table | Returns `{status, status_code, lines, error}`. Status is `pending`, `completed`, or `failed`. A terminal result is consumed once. |
| `main_state.http_cancel(id)` | boolean | Cancels and removes a request/result; returns false for an unknown ID. |

```lua
local id, err = main_state.http_get_async("https://example.com/data", 5000, 4 * 1024 * 1024)
-- In a subsequent normal skin update, without a blocking loop:
if id then
    local result = main_state.http_poll(id)
    if result.status ~= "pending" then
        if result.status == "completed" then
            -- Consume result.lines (a 1-based UTF-8 line table).
        end
        id = nil
    end
end
```

There are at most 16 retained requests/results per skin, two shared workers,
and 32 queued jobs. A 16 MiB per-skin budget covers outstanding raw response
bodies and reservations for queued/running requests. Submission reserves the
requested maximum; completion shrinks that reservation to the actual body size.
Polling a terminal result, cancellation, failure, or skin disposal releases its
capacity. A cancelled running request keeps its reservation until its worker
actually exits. Requests exceeding the remaining budget return nil/error.
Uncollected results are stored as raw bytes, not decoded line lists. This budget
does not include transient buffer copies, decoded strings/tables, or data already
returned to and retained by Lua; it is not a cap on total JVM/Lua heap usage.
Redirects are limited to three; their URLs are validated again, and HTTPS
downgrades are rejected. Non-2xx responses fail and retain their HTTP status.
Cancellation, failure, timeout, or skin disposal does not invoke Lua callbacks.
Workers manipulate Java data only. Skin disposal cancels jobs and clears results.
These new APIs are disabled in Preview sandbox.

The async transport rejects non-public DNS results (including loopback, private,
link-local and multicast addresses), disables automatic redirects and bypasses
configured HTTP proxies. Destination validation uses DNS resolution before the
JDK connection, not a pinned socket address: it is not a complete defense against
DNS rebinding. Existing synchronous HTTP and legacy URL facades are unchanged
and do not inherit the async destination rules, queue limits, or total timeout.
They do share the byte limits and have no line-count limit.

### Legacy URL Capacity

```lua
local j = require("luajava")
local connection = j.newInstance("java.net.URL", url):openConnection()
connection:setResponseLimitBytes(4 * 1024 * 1024) -- optional; default 1 MiB
local reader = j.newInstance("java.io.BufferedReader",
    j.newInstance("java.io.InputStreamReader", connection:getInputStream(), "utf8"))
-- Read with reader:readLine(); then release its buffered body:
reader:close()
```

The legacy setter accepts the same 1-8388608 range (invalid values raise a Lua
error). Reading to EOF or calling close releases the reader's reference to its
buffered response. The 16 MiB uncollected-result budget applies to async jobs;
synchronous/legacy data already delivered to Lua remains Lua-owned.

## Browser

`main_state.open_url(url)` returns a boolean. Only absolute HTTP/HTTPS URLs
without credentials, up to 2048 characters, are accepted. It requires a current
mouse/touch press or an Enter/Space key press and is limited to one launch per
second per skin. Call it from a user action, not from initialization or an
automatic timer. Disabled in Preview sandbox and after skin disposal. Input
checking is an accidental-launch guard, not proof of consent against malicious
skin code reacting to an unrelated input.

## Legacy Compatibility Facades

`luajava.bindClass("com.badlogic.gdx.Gdx").input` exposes read-only
`isKeyPressed`, `isKeyJustPressed`, `isButtonPressed`, `getX`, `getY`,
`getDeltaX`, and `getDeltaY`. Input-less contexts return false/zero.
`luajava.bindClass("com.badlogic.gdx.Input").Buttons` provides LEFT, RIGHT,
MIDDLE, BACK and FORWARD. No input injection or processor replacement is exposed.

Both `luajava.new(FileClass, path)` and
`luajava.newInstance("java.io.File", path)` return limited Lua File tables:
`isFile`, `isDirectory`, `getName`, `getPath`, `mkdir`, and `listFiles`.
`listFiles` returns a 1-based table of File facades, excluding symbolic links;
it returns nil on failure or more than 4096 entries. `tostring(file)` returns
its normalized path (for existing skins that explicitly convert entries to text).
`main_state.file_list` keeps its existing string/count return convention.
No real Java File instances, arbitrary classes, Threads or Runnable proxies
are exposed. Code expecting a raw string directly from legacy `listFiles`
must use `tostring(entry)` or `entry:getPath()`.

## Audio

Audio paths are resolved relative to the current skin directory.

| API | Return | Description |
| --- | --- | --- |
| `main_state.audio_play(path, volume)` | boolean | Plays a sound once. Volume is clamped to `0.0`-`2.0` and multiplied by system volume. |
| `main_state.audio_loop(path, volume)` | boolean | Plays a sound in a loop. |
| `main_state.audio_preload(path)` | boolean | Loads a sound through the audio processor with volume `0`. |
| `main_state.audio_stop(path)` | boolean | Stops the sound. |
| `main_state.audio_dispose(path)` | boolean | Disposes the sound resource. |

