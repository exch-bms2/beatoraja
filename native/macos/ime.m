#import <AppKit/AppKit.h>
#include <jni.h>

// A per-session, non-drawing AppKit editor; never replaces GLFW classes or callbacks.
@class BRIMEState;
@interface BRIMEView : NSTextView
@property(nonatomic, weak) BRIMEState *ime;
@property(nonatomic) int mutation;
@property(nonatomic) BOOL keyStartedMarked;
@property(nonatomic, strong) NSPasteboard *clipboard;
@end

@interface BRIMEState : NSObject
@property(nonatomic, weak) NSWindow *window;
@property(nonatomic, strong) NSView *content;
@property(nonatomic, strong) NSResponder *previous;
@property(nonatomic, strong) BRIMEView *editor;
@property(nonatomic, strong) id monitor;
@property(nonatomic, strong) id resignObserver;
@property(nonatomic, strong) id selectionObserver;
@property(nonatomic, strong) id closeObserver;
@property(nonatomic, strong) NSMutableIndexSet *held;
@property(nonatomic, copy) NSString *committed;
@property(nonatomic) NSRange committedSelection;
@property(nonatomic) NSRect clientCaret;
@property(nonatomic) NSSize clientSize;
@property(nonatomic) NSRect lastScreenCaret;
@property(nonatomic) CGFloat lastBackingScale;
@property(nonatomic) BOOL hasCaret;
@property(nonatomic) BOOL active;
@property(nonatomic) JavaVM *vm;
@property(nonatomic) jobject callback;
@property(nonatomic) jmethodID method;
@property(nonatomic) jlong session;
- (void)emit;
- (void)send:(int)kind text:(NSString *)text anchor:(NSUInteger)a cursor:(NSUInteger)c consumed:(BOOL)used;
- (void)cancel;
- (void)stopMonitor;
- (NSRect)caretScreen;
- (NSEvent *)filter:(NSEvent *)event;
@end

static NSString *BRString(JNIEnv *env, jstring value) {
    const jchar *data = (*env)->GetStringChars(env, value, NULL);
    if (!data) return nil;
    NSString *result = [[NSString alloc] initWithCharacters:(const unichar *)data length:(*env)->GetStringLength(env, value)];
    (*env)->ReleaseStringChars(env, value, data);
    return result;
}

@implementation BRIMEView
- (NSView *)hitTest:(NSPoint)p { return nil; }
- (void)drawRect:(NSRect)rect { } // SkinTextInput draws text, preedit and caret.
- (BOOL)shouldDrawInsertionPoint { return NO; }
- (void)setMarkedText:(id)value selectedRange:(NSRange)selection replacementRange:(NSRange)replacement {
    self.mutation++;
    [super setMarkedText:value selectedRange:selection replacementRange:replacement];
    self.mutation--;
    if (!self.mutation) [self.ime emit];
}
- (void)insertText:(id)value replacementRange:(NSRange)replacement {
    NSString *text = [value isKindOfClass:[NSAttributedString class]] ? [value string] : value;
    text = [[text stringByReplacingOccurrencesOfString:@"\r" withString:@""] stringByReplacingOccurrencesOfString:@"\n" withString:@""];
    self.mutation++;
    [super insertText:text replacementRange:replacement];
    self.mutation--;
    if (!self.mutation) [self.ime emit];
}
- (void)unmarkText {
    self.mutation++;
    [super unmarkText];
    self.mutation--;
    if (!self.mutation) [self.ime emit];
}
- (void)didChangeText {
    [super didChangeText];
    if (!self.mutation) [self.ime emit];
}
- (BOOL)copySelection {
    NSRange range = self.selectedRange;
    if (!range.length || NSMaxRange(range) > self.string.length) return NO;
    NSString *text = [self.string substringWithRange:range];
    [self.clipboard clearContents];
    return [self.clipboard setString:text forType:NSPasteboardTypeString];
}
- (void)copy:(id)sender { [self copySelection]; }
- (void)cut:(id)sender {
    if (![self hasMarkedText] && [self copySelection]) [self insertText:@"" replacementRange:self.selectedRange];
}
- (void)paste:(id)sender {
    NSString *text = [self.clipboard stringForType:NSPasteboardTypeString];
    if (text) [self insertText:text replacementRange:NSMakeRange(NSNotFound, 0)];
}
- (NSRect)firstRectForCharacterRange:(NSRange)range actualRange:(NSRangePointer)actual {
    if (actual) *actual = [self hasMarkedText] ? [self markedRange] : [self selectedRange];
    return [self.ime caretScreen];
}
- (void)keyDown:(NSEvent *)event {
    self.keyStartedMarked = [self hasMarkedText];
    if ((event.modifierFlags & NSEventModifierFlagCommand) && !self.keyStartedMarked) {
        NSString *key = [event.charactersIgnoringModifiers lowercaseString];
        if ([key isEqualToString:@"a"]) { [self selectAll:nil]; return; }
        if ([key isEqualToString:@"c"]) { [self copy:nil]; return; }
        if ([key isEqualToString:@"x"]) { [self cut:nil]; return; }
        if ([key isEqualToString:@"v"]) { [self paste:nil]; return; }
        if ([key isEqualToString:@"z"]) {
            if (event.modifierFlags & NSEventModifierFlagShift) [self.undoManager redo]; else [self.undoManager undo];
            return;
        }
    }
    [super keyDown:event];
}
- (void)doCommandBySelector:(SEL)selector {
    if (selector == @selector(insertNewline:) || selector == @selector(insertNewlineIgnoringFieldEditor:)) {
        BOOL consumed = self.keyStartedMarked || [self hasMarkedText];
        if ([self hasMarkedText]) [self unmarkText];
        [self.ime send:3 text:nil anchor:0 cursor:0 consumed:consumed];
    } else if (selector == @selector(cancelOperation:)) {
        if ([self hasMarkedText]) [self.ime cancel];
        // Escape never leaves this editor or navigates the game.
    } else {
        NSString *name = NSStringFromSelector(selector);
        if ([name hasPrefix:@"move"] || [name hasPrefix:@"delete"] || [name hasPrefix:@"select"])
            [super doCommandBySelector:selector];
    }
}
@end

@implementation BRIMEState
- (void)send:(int)kind text:(NSString *)text anchor:(NSUInteger)a cursor:(NSUInteger)c consumed:(BOOL)used {
    if (!self.active || !self.callback) return;
    JNIEnv *env = NULL;
    if ((*self.vm)->GetEnv(self.vm, (void **)&env, JNI_VERSION_1_6) != JNI_OK || (*env)->ExceptionCheck(env)) return;
    jstring value = NULL;
    if (text) {
        NSUInteger length = text.length;
        unichar *buffer = malloc(MAX((NSUInteger)1, length) * sizeof(unichar));
        if (!buffer) return;
        [text getCharacters:buffer range:NSMakeRange(0, length)];
        value = (*env)->NewString(env, buffer, (jsize)length);
        free(buffer);
        if (!value) return;
    }
    (*env)->CallVoidMethod(env, self.callback, self.method, self.session, (jint)kind, value, (jint)a, (jint)c, (jboolean)used);
    if (value) (*env)->DeleteLocalRef(env, value);
}
- (void)emit {
    if (!self.active || self.editor.mutation) return;
    NSRange selection = self.editor.selectedRange;
    if (self.editor.hasMarkedText) {
        NSRange marked = self.editor.markedRange;
        if (NSMaxRange(marked) <= self.editor.string.length) {
            NSUInteger caret = selection.location >= marked.location ? selection.location - marked.location : 0;
            [self send:1 text:[self.editor.string substringWithRange:marked] anchor:0 cursor:MIN(caret, marked.length) consumed:NO];
        }
    } else {
        self.committed = self.editor.string;
        self.committedSelection = selection;
        [self send:0 text:self.committed anchor:selection.location cursor:NSMaxRange(selection) consumed:NO];
    }
}
- (void)cancel {
    self.editor.mutation++;
    [self.editor.inputContext discardMarkedText];
    [self.editor unmarkText];
    [self.editor setString:self.committed ?: @""];
    [self.editor setSelectedRange:self.committedSelection];
    self.editor.mutation--;
    [self send:2 text:nil anchor:0 cursor:0 consumed:NO];
}
- (NSRect)caretScreen {
    NSRect bounds = self.content.bounds;
    double sx = self.clientSize.width > 0 ? bounds.size.width / self.clientSize.width : 1;
    double sy = self.clientSize.height > 0 ? bounds.size.height / self.clientSize.height : 1;
    NSRect rect = self.clientCaret;
    rect.origin.x = bounds.origin.x + rect.origin.x * sx;
    rect.origin.y = bounds.origin.y + (self.content.isFlipped ? rect.origin.y * sy
            : bounds.size.height - (rect.origin.y + rect.size.height) * sy);
    rect.size.width *= sx; rect.size.height *= sy;
    return [self.window convertRectToScreen:[self.content convertRect:rect toView:nil]];
}
- (void)stopMonitor {
    __attribute__((objc_precise_lifetime)) BRIMEState *keepAlive = self;
    if (self.monitor) { [NSEvent removeMonitor:self.monitor]; self.monitor = nil; }
    if (self.resignObserver) { [NSNotificationCenter.defaultCenter removeObserver:self.resignObserver]; self.resignObserver = nil; }
    if (self.selectionObserver) { [NSNotificationCenter.defaultCenter removeObserver:self.selectionObserver]; self.selectionObserver = nil; }
    if (self.closeObserver) { [NSNotificationCenter.defaultCenter removeObserver:self.closeObserver]; self.closeObserver = nil; }
    (void)keepAlive;
}
- (NSEvent *)filter:(NSEvent *)event {
    if (event.window != self.window || !self.window.isKeyWindow) return event;
    if (self.active) {
        if (event.type == NSEventTypeKeyDown) {
            [self.held addIndex:event.keyCode];
            [self.editor keyDown:event];
        } else {
            [self.held removeIndex:event.keyCode];
            [self.content keyUp:event];
        }
        return nil;
    }
    if ([self.held containsIndex:event.keyCode]) {
        if (event.type == NSEventTypeKeyUp) {
            [self.held removeIndex:event.keyCode]; [self.content keyUp:event];
            if (!self.held.count) [self stopMonitor];
        }
        return nil; // Consume a submit key's autorepeat until its physical release.
    }
    return event;
}
@end

static BRIMEState *BRState(jlong handle) { return (__bridge BRIMEState *)(void *)(intptr_t)handle; }
static BOOL BRMain(JNIEnv *env) {
    if (NSThread.isMainThread) return YES;
    jclass error = (*env)->FindClass(env, "java/lang/IllegalStateException");
    (*env)->ThrowNew(env, error, "AppKit IME requires the main thread");
    (*env)->DeleteLocalRef(env, error);
    return NO;
}

JNIEXPORT jlong JNICALL Java_bms_player_beatoraja_input_MacOsImeBridge_open
  (JNIEnv *env, jobject callback, jlong windowHandle, jlong session, jstring text, jint anchor, jint cursor) {
    @autoreleasepool {
    if (!BRMain(env)) return 0;
    NSWindow *window = (__bridge NSWindow *)(void *)(intptr_t)windowHandle;
    if (!window || !window.contentView) return 0;
    NSString *initial = BRString(env, text);
    if (!initial) return 0;
    BRIMEState *state = [BRIMEState new];
    state.window = window; state.content = window.contentView; state.previous = window.firstResponder;
    state.held = [NSMutableIndexSet new];
    state.session = session; state.callback = (*env)->NewGlobalRef(env, callback);
    if (!state.callback) return 0;
    JavaVM *vm = NULL; (*env)->GetJavaVM(env, &vm); state.vm = vm;
    jclass clazz = (*env)->GetObjectClass(env, callback);
    if (!clazz) { (*env)->DeleteGlobalRef(env, state.callback); return 0; }
    state.method = (*env)->GetMethodID(env, clazz, "nativeEvent", "(JILjava/lang/String;IIZ)V");
    (*env)->DeleteLocalRef(env, clazz);
    if (!state.callback || !state.method) { if (state.callback) (*env)->DeleteGlobalRef(env, state.callback); return 0; }
    state.editor = [[BRIMEView alloc] initWithFrame:NSMakeRect(0, 0, 1, 1)];
    state.editor.ime = state;
    state.editor.clipboard = NSPasteboard.generalPasteboard;
    state.editor.drawsBackground = NO; state.editor.richText = NO; state.editor.importsGraphics = NO;
    state.editor.allowsUndo = YES; state.editor.horizontallyResizable = YES;
    state.editor.textContainer.widthTracksTextView = NO;
    state.editor.textContainer.containerSize = NSMakeSize(CGFLOAT_MAX, CGFLOAT_MAX);
    [state.editor setString:initial];
    [state.editor setSelectedRange:NSMakeRange(MIN(anchor, cursor), abs(cursor - anchor))];
    state.committed = state.editor.string; state.committedSelection = state.editor.selectedRange;
    [state.content addSubview:state.editor];
    if (![window makeFirstResponder:state.editor]) {
        [state.editor removeFromSuperview]; (*env)->DeleteGlobalRef(env, state.callback); return 0;
    }
    state.active = YES;
    state.monitor = [NSEvent addLocalMonitorForEventsMatchingMask:NSEventMaskKeyDown | NSEventMaskKeyUp handler:^NSEvent *(NSEvent *event) {
        return [state filter:event];
    }];
    state.resignObserver = [NSNotificationCenter.defaultCenter addObserverForName:NSWindowDidResignKeyNotification object:window queue:nil usingBlock:^(NSNotification *note) {
        if (state.active) { if (state.editor.hasMarkedText) [state cancel]; [state.held removeAllIndexes]; }
        else [state stopMonitor];
    }];
    __weak BRIMEState *weakState = state;
    state.selectionObserver = [NSNotificationCenter.defaultCenter addObserverForName:NSTextViewDidChangeSelectionNotification object:state.editor queue:nil usingBlock:^(NSNotification *note) {
        [weakState emit];
    }];
    state.closeObserver = [NSNotificationCenter.defaultCenter addObserverForName:NSWindowWillCloseNotification object:window queue:nil usingBlock:^(NSNotification *note) {
        [state stopMonitor];
    }];
    return (jlong)(intptr_t)CFBridgingRetain(state);
    }
}

JNIEXPORT void JNICALL Java_bms_player_beatoraja_input_MacOsImeBridge_updateNative
  (JNIEnv *env, jobject self, jlong handle, jstring text, jint anchor, jint cursor) {
    @autoreleasepool {
    if (!BRMain(env)) return;
    BRIMEState *state = BRState(handle);
    if (!state.editor.hasMarkedText && [state.editor.string isEqualToString:BRString(env, text)]) {
        state.editor.mutation++;
        [state.editor setSelectedRange:NSMakeRange(MIN(anchor, cursor), abs(cursor - anchor))];
        state.committedSelection = state.editor.selectedRange;
        state.editor.mutation--;
    }
    }
}
JNIEXPORT void JNICALL Java_bms_player_beatoraja_input_MacOsImeBridge_caretNative
  (JNIEnv *env, jobject self, jlong handle, jdouble x, jdouble y, jdouble w, jdouble h, jdouble cw, jdouble ch) {
    @autoreleasepool {
    if (!BRMain(env)) return;
    BRIMEState *state = BRState(handle);
    state.clientCaret = NSMakeRect(x, y, w, h); state.clientSize = NSMakeSize(cw, ch);
    NSRect screen = [state caretScreen];
    CGFloat scale = state.window.backingScaleFactor;
    if (!state.hasCaret || !NSEqualRects(screen, state.lastScreenCaret) || scale != state.lastBackingScale) {
        state.hasCaret = YES; state.lastScreenCaret = screen; state.lastBackingScale = scale;
        [state.editor.inputContext invalidateCharacterCoordinates];
    }
    if (state.window.isKeyWindow && state.window.firstResponder == state.content) [state.window makeFirstResponder:state.editor];
    }
}
JNIEXPORT void JNICALL Java_bms_player_beatoraja_input_MacOsImeBridge_confirmNative
  (JNIEnv *env, jobject self, jlong handle) {
    @autoreleasepool {
    if (!BRMain(env)) return;
    BRIMEState *state = BRState(handle);
    [state.editor unmarkText]; [state.editor.inputContext discardMarkedText];
    }
}
JNIEXPORT void JNICALL Java_bms_player_beatoraja_input_MacOsImeBridge_cancelNative
  (JNIEnv *env, jobject self, jlong handle) { @autoreleasepool { if (BRMain(env)) [BRState(handle) cancel]; } }
JNIEXPORT void JNICALL Java_bms_player_beatoraja_input_MacOsImeBridge_closeNative
  (JNIEnv *env, jobject self, jlong handle) {
    @autoreleasepool {
    if (!BRMain(env)) return;
    BRIMEState *state = CFBridgingRelease((void *)(intptr_t)handle);
    state.active = NO;
    [state.editor.inputContext discardMarkedText];
    if (state.window.firstResponder == state.editor) [state.window makeFirstResponder:state.previous ?: state.content];
    [state.editor removeFromSuperview];
    (*env)->DeleteGlobalRef(env, state.callback); state.callback = NULL;
    if (!state.held.count) [state stopMonitor];
    }
}
