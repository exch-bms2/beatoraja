// Test-only JNI entry points: this translation unit is NEVER packaged.
#include "../../../native/macos/ime.m"

@interface BRIMEFailingPasteboard : NSObject
@end
@implementation BRIMEFailingPasteboard
- (NSInteger)clearContents { return 0; }
- (BOOL)setString:(NSString *)text forType:(NSPasteboardType)type { return NO; }
- (NSString *)stringForType:(NSPasteboardType)type { return nil; }
- (void)releaseGlobally { }
@end

JNIEXPORT jlong JNICALL Java_bms_player_beatoraja_input_MacOsImeNativeTest_retainNative
  (JNIEnv *env, jclass self, jlong pointer) {
    return (jlong)(intptr_t)CFBridgingRetain(BRState(pointer));
}
JNIEXPORT void JNICALL Java_bms_player_beatoraja_input_MacOsImeNativeTest_releaseNative
  (JNIEnv *env, jclass self, jlong pointer) {
    BRIMEState *state = CFBridgingRelease((void *)(intptr_t)pointer);
    [state stopMonitor];
    if (state.editor.clipboard != NSPasteboard.generalPasteboard) [state.editor.clipboard releaseGlobally];
}
JNIEXPORT jboolean JNICALL Java_bms_player_beatoraja_input_MacOsImeNativeTest_injectNative
  (JNIEnv *env, jclass self, jlong pointer, jint op, jstring value) {
    if (!BRMain(env)) return JNI_FALSE;
    BRIMEState *state = BRState(pointer);
    NSString *text = value ? BRString(env, value) : @"";
    switch (op) {
        case 0: [state.editor setMarkedText:text selectedRange:NSMakeRange(text.length, 0) replacementRange:NSMakeRange(NSNotFound, 0)]; break;
        case 1: [state.editor insertText:text replacementRange:NSMakeRange(NSNotFound, 0)]; break;
        case 2: [state.editor doCommandBySelector:@selector(cancelOperation:)]; break;
        case 3:
            state.editor.keyStartedMarked = YES;
            [state.editor unmarkText];
            [state.editor doCommandBySelector:@selector(insertNewline:)]; break;
        case 4:
            state.editor.keyStartedMarked = NO;
            [state.editor doCommandBySelector:@selector(insertNewline:)]; break;
        case 5: case 6: case 7: {
            [state.window makeKeyAndOrderFront:nil];
            NSEvent *event = [NSEvent keyEventWithType:op == 7 ? NSEventTypeKeyUp : NSEventTypeKeyDown
                    location:NSZeroPoint modifierFlags:0 timestamp:0 windowNumber:state.window.windowNumber
                    context:nil characters:@"\r" charactersIgnoringModifiers:@"\r" isARepeat:op == 6 keyCode:36];
            return [state filter:event] == nil;
        }
        case 8: [state.editor doCommandBySelector:@selector(deleteBackward:)]; break;
        case 9: [state.editor selectAll:nil]; break;
        case 10: [NSNotificationCenter.defaultCenter postNotificationName:NSWindowDidResignKeyNotification object:state.window]; break;
        case 11:
            state.editor.clipboard = [NSPasteboard pasteboardWithUniqueName];
            [state.editor.clipboard setString:text forType:NSPasteboardTypeString];
            [state.editor paste:nil]; break;
        case 12: [state.editor cut:nil]; break;
        case 13:
            if (state.editor.clipboard != NSPasteboard.generalPasteboard) [state.editor.clipboard releaseGlobally];
            state.editor.clipboard = (NSPasteboard *)[BRIMEFailingPasteboard new];
            [state.editor cut:nil]; break;
        case 14: [state.editor paste:nil]; break;
    }
    return JNI_TRUE;
}
JNIEXPORT jdoubleArray JNICALL Java_bms_player_beatoraja_input_MacOsImeNativeTest_geometryNative
  (JNIEnv *env, jclass self, jlong pointer) {
    BRIMEState *state = BRState(pointer);
    NSRect actual = [state.editor firstRectForCharacterRange:NSMakeRange(0, 0) actualRange:NULL];
    NSRect bounds = state.content.bounds;
    NSRect origin = [state.window convertRectToScreen:[state.content convertRect:NSMakeRect(0, 0, 0, 0) toView:nil]];
    jdouble values[] = {actual.origin.x, actual.origin.y, actual.size.width, actual.size.height,
        origin.origin.x, origin.origin.y, bounds.size.width, bounds.size.height, state.window.backingScaleFactor,
        state.callback != NULL, state.monitor != nil, state.window.firstResponder == state.editor};
    jdoubleArray result = (*env)->NewDoubleArray(env, 12);
    (*env)->SetDoubleArrayRegion(env, result, 0, 12, values);
    return result;
}
JNIEXPORT void JNICALL Java_bms_player_beatoraja_input_MacOsImeNativeTest_positionNative
  (JNIEnv *env, jclass self, jlong pointer, jdouble x, jdouble y, jdouble w, jdouble h, jdouble cw, jdouble ch) {
    Java_bms_player_beatoraja_input_MacOsImeBridge_caretNative(env, NULL, pointer, x, y, w, h, cw, ch);
}
