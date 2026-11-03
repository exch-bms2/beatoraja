/* Real Xlib/JNI declarations, but fake server/window calls. No X server is contacted. */
#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <stdarg.h>
#include <stdint.h>
#include <locale.h>

typedef struct _GLFWwindow {
    struct _GLFWwindow* next;
    struct { XIC ic; Window handle; } x11;
} _GLFWwindow;
static struct { _GLFWwindow* windowListHead; struct { XIM im; Display* display; } x11; } _glfw;
static int _glfwWindowFocusedX11(_GLFWwindow* window) { (void) window; return 1; }
#include "../../../native/linux/x11-ime.c"

static _GLFWwindow fakeWindow;
static int destroyed, resetCount;
static int physicallyHeld;
static char resetChars[16];

char* XGetIMValues(XIM im, ...) {
    (void) im;
    va_list ap; va_start(ap,im); (void) va_arg(ap,char*);
    XIMStyles** out = va_arg(ap,XIMStyles**); va_end(ap);
    *out = calloc(1,sizeof(XIMStyles)+sizeof(XIMStyle));
    (*out)->count_styles = 1; (*out)->supported_styles = (XIMStyle*) (*out+1);
    (*out)->supported_styles[0] = XIMPreeditCallbacks | XIMStatusNothing;
    return NULL;
}
XVaNestedList XVaCreateNestedList(int unused, ...) { (void) unused; return malloc(1); }
XIC XCreateIC(XIM im, ...) { (void) im; return (XIC)(uintptr_t)2; }
char* XGetICValues(XIC ic, ...) {
    va_list ap; va_start(ap,ic); (void) va_arg(ap,char*);
    *va_arg(ap,unsigned long*) = 0; va_end(ap); return NULL;
}
char* XSetICValues(XIC ic, ...) { (void) ic; return NULL; }
Status XGetWindowAttributes(Display* display, Window window, XWindowAttributes* out) {
    (void) display; (void) window; memset(out,0,sizeof(*out)); return 1;
}
int XSelectInput(Display* display, Window window, long mask) { (void) display; (void) window; (void) mask; return 1; }
int XQueryKeymap(Display* display, char keys[32]) {
    (void) display; memset(keys,0,32);
    if (physicallyHeld) keys[36/8] |= 1 << (36%8);
    return 1;
}
void XSetICFocus(XIC ic) { (void) ic; }
void XUnsetICFocus(XIC ic) { (void) ic; }
char* Xutf8ResetIC(XIC ic) {
    (void) ic; resetCount++;
    if (baIme) baDone(baIme->ic,(XPointer)baIme,NULL);
    return strdup(resetChars);
}
void XDestroyIC(XIC ic) {
    if (ic == (XIC)(uintptr_t)2) {
        destroyed++;
        if (baIme) baDestroyed(ic,(XPointer)baIme,NULL);
    }
}
int XFree(void* pointer) { free(pointer); return 1; }

JNIEXPORT jlong JNICALL Java_bms_player_beatoraja_input_X11ImeNativeTest_window(JNIEnv* env, jclass type) {
    (void) env; (void) type;
    memset(&fakeWindow,0,sizeof(fakeWindow)); fakeWindow.x11.ic = (XIC)(uintptr_t)1; fakeWindow.x11.handle = 99;
    _glfw.windowListHead = &fakeWindow; _glfw.x11.im = (XIM)(uintptr_t)1;
    strcpy(resetChars,"unconfirmed"); destroyed = resetCount = physicallyHeld = 0;
    return (jlong)(intptr_t)&fakeWindow;
}
JNIEXPORT void JNICALL Java_bms_player_beatoraja_input_X11ImeNativeTest_draw(JNIEnv* env, jclass type, jint first, jint removed, jstring text, jint caret, jboolean wide) {
    (void) type;
    if (!baIme) return;
    XIMText value; memset(&value,0,sizeof(value));
    wchar_t* chars = NULL; char* bytes = NULL;
    if (text) {
        jsize count = (*env)->GetStringLength(env,text);
        const jchar* source = (*env)->GetStringChars(env,text,NULL);
        chars = calloc(count+1,sizeof(wchar_t));
        for (int i=0; i<count; i++) {
            uint32_t cp = source[i];
            if (cp>=0xd800 && cp<=0xdbff && i+1<count) cp = 0x10000+((cp-0xd800)<<10)+(source[++i]-0xdc00);
            chars[value.length++] = (wchar_t)cp;
        }
        (*env)->ReleaseStringChars(env,text,source);
        value.encoding_is_wchar = wide;
        if (wide) value.string.wide_char = chars;
        else {
            mbstate_t state; memset(&state,0,sizeof(state)); bytes = calloc(value.length*MB_CUR_MAX+1,1); size_t at=0;
            for (int i=0; i<value.length; i++) { size_t n=wcrtomb(bytes+at,chars[i],&state); if (n==(size_t)-1) { free(bytes); free(chars); return; } at+=n; }
            value.string.multi_byte = bytes;
        }
    }
    XIMPreeditDrawCallbackStruct draw = {caret,first,removed,text ? &value : NULL};
    baDraw(baIme->ic,(XPointer)baIme,(XPointer)&draw);
    free(bytes); free(chars);
}
JNIEXPORT void JNICALL Java_bms_player_beatoraja_input_X11ImeNativeTest_key(JNIEnv* env, jclass type, jbyteArray commit) {
    (void) type;
    if (!baIme) return;
    XEvent event; memset(&event,0,sizeof(event)); event.type=KeyPress; event.xkey.window=99; event.xkey.keycode=36;
    baBefore(&event); baDone(baIme->ic,(XPointer)baIme,NULL); baFiltered(0);
    jsize count=(*env)->GetArrayLength(env,commit); jbyte* bytes=(*env)->GetByteArrayElements(env,commit,NULL);
    baCommit(&fakeWindow,(const char*)bytes,count); (*env)->ReleaseByteArrayElements(env,commit,bytes,JNI_ABORT);
    baKey(&fakeWindow,257,0,0);
    baKey(&fakeWindow,257,0,1);
}
JNIEXPORT jint JNICALL Java_bms_player_beatoraja_input_X11ImeNativeTest_state(JNIEnv* env, jclass type, jint what) {
    (void) env; (void) type;
    if (what==0) return destroyed;
    if (what==1) return fakeWindow.x11.ic==(XIC)(uintptr_t)1;
    if (what==2) return resetCount;
    return fakeWindow.x11.ic==(XIC)(uintptr_t)3;
}
JNIEXPORT void JNICALL Java_bms_player_beatoraja_input_X11ImeNativeTest_lost(JNIEnv* env, jclass type) {
    (void) env; (void) type;
    if (baIme) { baDestroyed(baIme->ic,(XPointer)baIme,NULL); fakeWindow.x11.ic=(XIC)(uintptr_t)3; }
}
JNIEXPORT void JNICALL Java_bms_player_beatoraja_input_X11ImeNativeTest_flow(JNIEnv* env, jclass type, jint action) {
    (void) env; (void) type;
    if (!baIme) return;
    if (action==0) { baFocusLost(&fakeWindow); physicallyHeld=1; baFocusGained(&fakeWindow); return; }
    if (action==1) { baWindowDestroyed(&fakeWindow); return; }
    XEvent event; memset(&event,0,sizeof(event)); event.type=KeyPress; event.xkey.window=99; event.xkey.keycode=36;
    baBefore(&event); baFiltered(action==2); baKey(&fakeWindow,257,0,0);
    if (action==4) baKey(&fakeWindow,257,0,1);
}
JNIEXPORT jboolean JNICALL Java_bms_player_beatoraja_input_X11ImeNativeTest_locale(JNIEnv* env, jclass type) {
    (void) env; (void) type;
    return setlocale(LC_CTYPE,"en_US.UTF-8") != NULL || setlocale(LC_CTYPE,"C.UTF-8") != NULL;
}
