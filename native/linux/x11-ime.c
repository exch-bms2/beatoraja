/* Included only by the pinned, patched GLFW x11_window.c. Altered GLFW build.
 * One XIC and one event reader: GLFW retains ownership of its Display/window.
 */
#include <jni.h>
#include <wchar.h>
#include <stdint.h>

#define BA_LIMIT (1024 * 1024)
typedef struct {
    _GLFWwindow* window;
    XIC ic, previous;
    XIM im;
    JNIEnv* env;
    jobject receiver;
    jmethodID notify;
    jlong token;
    uint32_t* preedit;
    size_t length;
    int caret, composing, pending, inEvent, keycode, used, filtered, closing, lost;
    unsigned char guarded[256];
} BaIme;
static BaIme* baIme;
static jlong baNextToken;

static void baNotify(BaIme* b, int kind, const char* bytes, size_t count, int a, int c, int used)
{
    if (!b || b->closing || !b->receiver || count > BA_LIMIT) return;
    JNIEnv* env = b->env;
    if ((*env)->ExceptionCheck(env)) return;
    jbyteArray text = NULL;
    if (bytes) {
        text = (*env)->NewByteArray(env, (jsize) count);
        if (!text) return;
        (*env)->SetByteArrayRegion(env, text, 0, (jsize) count, (const jbyte*) bytes);
        if ((*env)->ExceptionCheck(env)) { (*env)->DeleteLocalRef(env,text); return; }
    }
    (*env)->CallVoidMethod(env, b->receiver, b->notify, kind, text, a, c, (jboolean) used);
    if (text) (*env)->DeleteLocalRef(env, text);
}
static void baFault(BaIme* b)
{
    if (!b->lost) { b->lost = 1; baNotify(b, 6, NULL, 0, 0, 0, 0); }
}
static void baFlush(BaIme* b)
{
    if (!b || !b->pending || b->lost) return;
    b->pending = 0;
    if (!b->composing) { baNotify(b, 2, NULL, 0, 0, 0, 0); return; }
    char* bytes = calloc(b->length * 4 + 1, 1);
    if (!bytes) { baFault(b); return; }
    size_t at = 0;
    for (size_t i = 0; i < b->length; i++) {
        uint32_t cp = b->preedit[i];
        if (cp < 0x80) bytes[at++] = (char) cp;
        else if (cp < 0x800) { bytes[at++] = 0xc0 | (cp >> 6); bytes[at++] = 0x80 | (cp & 63); }
        else if (cp < 0x10000) {
            bytes[at++] = 0xe0 | (cp >> 12); bytes[at++] = 0x80 | ((cp >> 6) & 63); bytes[at++] = 0x80 | (cp & 63);
        } else {
            bytes[at++] = 0xf0 | (cp >> 18); bytes[at++] = 0x80 | ((cp >> 12) & 63);
            bytes[at++] = 0x80 | ((cp >> 6) & 63); bytes[at++] = 0x80 | (cp & 63);
        }
    }
    baNotify(b, 1, bytes, at, b->caret, 0, 0);
    free(bytes);
}
static void baChanged(BaIme* b)
{
    b->pending = 1; b->used = 1;
    if (!b->inEvent) baFlush(b);
}
static int baStart(XIC ic, XPointer data, XPointer unused)
{
    BaIme* b = (BaIme*) data;
    (void) ic; (void) unused;
    b->composing = 1; b->length = 0; b->caret = 0; baChanged(b);
    return -1;
}
static void baDone(XIC ic, XPointer data, XPointer unused)
{
    BaIme* b = (BaIme*) data;
    (void) ic; (void) unused;
    b->composing = 0; b->length = 0; b->caret = 0; baChanged(b);
}
static void baDraw(XIC ic, XPointer data, XPointer call)
{
    BaIme* b = (BaIme*) data;
    XIMPreeditDrawCallbackStruct* draw = (XIMPreeditDrawCallbackStruct*) call;
    (void) ic;
    if (b->closing || b->lost || !draw) return;
    int first = draw->chg_first, removed = draw->chg_length;
    if (first < 0 || removed < 0 || (size_t) first > b->length || (size_t) removed > b->length - first) { baFault(b); return; }
    XIMText* text = draw->text;
    /* A NULL string with non-NULL XIMText changes feedback only, not characters. */
    int feedbackOnly = text && !text->string.multi_byte;
    size_t added = text && !feedbackOnly ? text->length : 0;
    size_t length = feedbackOnly ? b->length : b->length - removed + added;
    if (length > BA_LIMIT / 4) { baFault(b); return; }
    if (!feedbackOnly) {
        uint32_t* next = calloc(length + 1, sizeof(uint32_t));
        if (!next) { baFault(b); return; }
        if (first) memcpy(next, b->preedit, first * sizeof(uint32_t));
        if (b->length > (size_t) first + removed)
            memcpy(next + first + added, b->preedit + first + removed, (b->length - first - removed) * sizeof(uint32_t));
        mbstate_t state; memset(&state, 0, sizeof(state));
        const char* input = text && !text->encoding_is_wchar ? text->string.multi_byte : NULL;
        for (size_t i = 0; i < added; i++) {
            uint32_t cp;
            if (text->encoding_is_wchar) cp = (uint32_t) text->string.wide_char[i];
            else {
                wchar_t value;
                size_t count = mbrtowc(&value, input, MB_CUR_MAX, &state);
                if (count == (size_t) -1 || count == (size_t) -2 || count == 0) { free(next); baFault(b); return; }
                cp = (uint32_t) value; input += count;
            }
            if (cp > 0x10ffff || (cp >= 0xd800 && cp <= 0xdfff)) { free(next); baFault(b); return; }
            next[first + i] = cp;
        }
        free(b->preedit); b->preedit = next; b->length = length;
    }
    if (draw->caret < 0 || (size_t) draw->caret > b->length) { baFault(b); return; }
    b->caret = draw->caret; b->composing = 1; baChanged(b);
}
static void baCaret(XIC ic, XPointer data, XPointer call)
{
    BaIme* b = (BaIme*) data;
    XIMPreeditCaretCallbackStruct* caret = (XIMPreeditCaretCallbackStruct*) call;
    (void) ic;
    if (!caret || b->closing || b->lost) return;
    int next = b->caret;
    if (caret->direction == XIMAbsolutePosition) next = caret->position;
    else if (caret->direction == XIMForwardChar) next++;
    else if (caret->direction == XIMBackwardChar) next--;
    else if (caret->direction == XIMLineStart) next = 0;
    else if (caret->direction == XIMLineEnd) next = (int) b->length;
    next = next < 0 ? 0 : (size_t) next > b->length ? (int) b->length : next;
    caret->position = b->caret = next; baChanged(b);
}
static void baDestroyed(XIC ic, XPointer data, XPointer unused)
{
    BaIme* b = (BaIme*) data;
    (void) ic; (void) unused;
    if (b->closing) return;
    b->ic = b->previous = NULL;
    b->window->x11.ic = NULL;
    if (!b->closing) baFault(b);
}
static void baBefore(XEvent* event)
{
    BaIme* b = baIme;
    if (!b) return;
    b->inEvent = 1; b->keycode = 0; b->filtered = 0; b->used = 0;
    if ((event->type == KeyPress || event->type == KeyRelease) && event->xkey.window == b->window->x11.handle) {
        b->keycode = event->xkey.keycode;
        if (b->keycode > 0 && b->keycode < 256) b->used = b->composing || b->guarded[b->keycode];
    }
}
static void baFiltered(Bool filtered)
{
    BaIme* b = baIme;
    if (!b) return;
    b->filtered = filtered;
    if (!b->keycode) { b->inEvent = 0; baFlush(b); }
}
static int baCommit(_GLFWwindow* window, const char* bytes, int count)
{
    BaIme* b = baIme;
    if (!b || b->window != window) return 0;
    if (count < 0 || count > BA_LIMIT) { baFault(b); return 1; }
    /* Keys held before focus must be released before editing or submitting. */
    if (!b->lost && !(b->keycode > 0 && b->guarded[b->keycode] == 2))
        baNotify(b, 0, bytes, count, 0, 0, 0);
    return 1;
}
static void baKey(_GLFWwindow* window, int key, int mods, int release)
{
    BaIme* b = baIme;
    if (!b || b->window != window) return;
    int used = b->used || b->composing;
    if (b->keycode > 0 && b->keycode < 256) {
        if (release) b->guarded[b->keycode] = 0;
        else if (used && !b->guarded[b->keycode]) b->guarded[b->keycode] = 1;
    }
    b->inEvent = 0;
    /* XLookup may commit old text and start new preedit in one event. Commit first. */
    baFlush(b);
    /* XIM may filter now and forward the same unconsumed key later. Filtering
     * alone must not latch a non-composing Return forever or duplicate editing. */
    if (!release && b->filtered && !used) return;
    baNotify(b, release ? 4 : 3, NULL, 0, key, mods, used);
}
static void baFocusLost(_GLFWwindow* window)
{
    if (baIme && baIme->window == window) {
        memset(baIme->guarded, 0, sizeof(baIme->guarded));
        baNotify(baIme, 5, NULL, 0, 0, 0, 0);
    }
}
static void baFocusGained(_GLFWwindow* window)
{
    if (baIme && baIme->window == window) {
        char keys[32]; XQueryKeymap(_glfw.x11.display, keys);
        memset(baIme->guarded, 0, sizeof(baIme->guarded));
        for (int code = 1; code < 256; code++)
            if (keys[code / 8] & (1 << (code % 8))) baIme->guarded[code] = 2;
    }
}
static void baClose(BaIme* b)
{
    if (!b || b != baIme) return;
    b->closing = 1;
    int restore = b->window->x11.ic == b->ic;
    if (b->ic) {
        XUnsetICFocus(b->ic);
        char* discarded = Xutf8ResetIC(b->ic);
        if (discarded) XFree(discarded);
        XDestroyIC(b->ic); b->ic = NULL;
    }
    /* A restarted IM server may already have installed a fresh GLFW context. */
    if (restore) b->window->x11.ic = _glfw.x11.im == b->im ? b->previous : NULL;
    if (b->window->x11.ic && _glfwWindowFocusedX11(b->window)) XSetICFocus(b->window->x11.ic);
    baIme = NULL;
    (*b->env)->DeleteGlobalRef(b->env, b->receiver);
    free(b->preedit); free(b);
}
static void baWindowDestroyed(_GLFWwindow* window)
{
    if (baIme && baIme->window == window) baClose(baIme);
}

JNIEXPORT jint JNICALL Java_bms_player_beatoraja_input_X11ImeNative_versionNative(JNIEnv* env, jclass type)
{ (void) env; (void) type; return 1; }
JNIEXPORT jlong JNICALL Java_bms_player_beatoraja_input_X11ImeNative_openNative(JNIEnv* env, jobject receiver, jlong pointer)
{
    if (baIme || !_glfw.x11.im) return 0;
    _GLFWwindow* window = _glfw.windowListHead;
    while (window && window != (_GLFWwindow*) (intptr_t) pointer) window = window->next;
    if (!window || !window->x11.ic) return 0;
    XIMStyles* styles = NULL; int supported = 0;
    if (XGetIMValues(_glfw.x11.im, XNQueryInputStyle, &styles, NULL)) return 0;
    if (styles) {
        for (unsigned int i = 0; i < styles->count_styles; i++)
            if (styles->supported_styles[i] == (XIMPreeditCallbacks | XIMStatusNothing)) supported = 1;
        XFree(styles);
    }
    if (!supported) return 0;
    BaIme* b = calloc(1, sizeof(BaIme));
    if (!b) return 0;
    b->window = window; b->env = env; b->previous = window->x11.ic; b->im = _glfw.x11.im; b->token = ++baNextToken;
    b->receiver = (*env)->NewGlobalRef(env, receiver);
    if (!b->receiver) { free(b); return 0; }
    jclass type = (*env)->GetObjectClass(env, receiver);
    if (!type) { (*env)->DeleteGlobalRef(env,b->receiver); free(b); return 0; }
    b->notify = (*env)->GetMethodID(env, type, "nativeEvent", "(I[BIIZ)V");
    (*env)->DeleteLocalRef(env, type);
    if (!b->receiver || !b->notify) { if (b->receiver) (*env)->DeleteGlobalRef(env,b->receiver); free(b); return 0; }
    XIMCallback start = {(XPointer) b, (XIMProc) baStart}, done = {(XPointer) b, (XIMProc) baDone},
        draw = {(XPointer) b, (XIMProc) baDraw}, caret = {(XPointer) b, (XIMProc) baCaret}, destroyed = {(XPointer) b, (XIMProc) baDestroyed};
    XVaNestedList preedit = XVaCreateNestedList(0, XNPreeditStartCallback, &start, XNPreeditDoneCallback, &done,
            XNPreeditDrawCallback, &draw, XNPreeditCaretCallback, &caret, NULL);
    if (!preedit) { (*env)->DeleteGlobalRef(env,b->receiver); free(b); return 0; }
    b->ic = XCreateIC(_glfw.x11.im, XNInputStyle, XIMPreeditCallbacks | XIMStatusNothing,
            XNClientWindow, window->x11.handle, XNFocusWindow, window->x11.handle,
            XNPreeditAttributes, preedit, XNDestroyCallback, &destroyed, NULL);
    if (preedit) XFree(preedit);
    if (!b->ic) { (*env)->DeleteGlobalRef(env,b->receiver); free(b); return 0; }
    XWindowAttributes attributes;
    if (XGetWindowAttributes(_glfw.x11.display, window->x11.handle, &attributes)) {
        unsigned long filter = 0;
        if (!XGetICValues(b->ic, XNFilterEvents, &filter, NULL))
            XSelectInput(_glfw.x11.display, window->x11.handle, attributes.your_event_mask | filter);
    }
    char keys[32]; XQueryKeymap(_glfw.x11.display, keys);
    for (int code = 1; code < 256; code++) if (keys[code / 8] & (1 << (code % 8))) b->guarded[code] = 2;
    XUnsetICFocus(b->previous);
    char* discarded = Xutf8ResetIC(b->previous);
    if (discarded) XFree(discarded);
    window->x11.ic = b->ic; baIme = b;
    if (_glfwWindowFocusedX11(window)) XSetICFocus(b->ic);
    return b->token;
}
JNIEXPORT void JNICALL Java_bms_player_beatoraja_input_X11ImeNative_resetNative(JNIEnv* env, jclass type, jlong handle)
{
    (void) env; (void) type;
    BaIme* b = baIme;
    if (!b || b->token != handle || !b->ic) return;
    char* discarded = Xutf8ResetIC(b->ic);
    if (discarded) XFree(discarded);
    b->composing = b->pending = b->caret = 0; b->length = 0;
}
JNIEXPORT jboolean JNICALL Java_bms_player_beatoraja_input_X11ImeNative_caretNative(JNIEnv* env, jclass type, jlong handle, jint x, jint y, jint right, jint bottom)
{
    (void) env; (void) type; (void) right;
    BaIme* b = baIme;
    if (!b || b->token != handle || !b->ic) return JNI_FALSE;
    /* Best effort only: XIM standard does not promise spot positioning for callbacks. */
    XPoint spot = {(short) x, (short) bottom};
    XRectangle area = {0, 0, (unsigned short) (right > 0 ? right : 1), (unsigned short) (bottom > y ? bottom-y : 1)};
    XVaNestedList values = XVaCreateNestedList(0, XNSpotLocation, &spot, XNArea, &area, NULL);
    if (!values) return JNI_FALSE;
    char* error = XSetICValues(b->ic, XNPreeditAttributes, values, NULL);
    XFree(values);
    return error == NULL;
}
JNIEXPORT void JNICALL Java_bms_player_beatoraja_input_X11ImeNative_closeNative(JNIEnv* env, jclass type, jlong handle)
{
    (void) env; (void) type;
    if (baIme && baIme->token == handle) baClose(baIme);
}
