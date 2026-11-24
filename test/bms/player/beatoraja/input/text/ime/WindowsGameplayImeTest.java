package bms.player.beatoraja.input.text.ime;

import java.util.ArrayList;
import java.util.List;

/** Synthetic tests for keeping the IME detached outside text input (#404); runnable on every OS. */
public final class WindowsGameplayImeTest {
    public static void main(String[] args) {
        detachesOnInstallAndReattachesForTextInput();
        nestedTextInputsKeepImeUntilLastEnds();
        unbalancedEndIsIgnored();
        refusedCallsKeepStateConsistent();
        closeRestoresDefaultContext();
        staticEntryPointsAreNoOpsWithoutWindow();
        System.out.println("WindowsGameplayImeTest passed");
    }

    private static final class FakePlatform implements WindowsGameplayIme.Platform {
        final List<Boolean> calls = new ArrayList<>();
        boolean refuse;
        boolean closed;
        @Override public boolean associate(boolean enabled) { calls.add(enabled); return !refuse; }
        @Override public void close() { closed = true; }
    }

    private static void detachesOnInstallAndReattachesForTextInput() {
        var platform = new FakePlatform();
        var ime = new WindowsGameplayIme(platform);
        check(ime.isDetached() && platform.calls.equals(List.of(false)), "Gameplay starts with no IME context");
        ime.begin();
        check(!ime.isDetached() && platform.calls.equals(List.of(false, true)), "Text input restores the default context");
        ime.end();
        check(ime.isDetached() && platform.calls.equals(List.of(false, true, false)), "Closing text input detaches again");
    }

    private static void nestedTextInputsKeepImeUntilLastEnds() {
        var platform = new FakePlatform();
        var ime = new WindowsGameplayIme(platform);
        ime.begin();
        ime.begin();
        ime.end();
        check(!ime.isDetached() && platform.calls.size() == 2, "IME stays attached while another field is active");
        ime.end();
        check(ime.isDetached() && platform.calls.size() == 3, "Last field detaches the IME");
    }

    private static void unbalancedEndIsIgnored() {
        var platform = new FakePlatform();
        var ime = new WindowsGameplayIme(platform);
        ime.end();
        ime.begin();
        check(!ime.isDetached() && platform.calls.equals(List.of(false, true)), "Extra end must not break the next begin");
    }

    private static void refusedCallsKeepStateConsistent() {
        var platform = new FakePlatform();
        platform.refuse = true;
        var ime = new WindowsGameplayIme(platform);
        check(!ime.isDetached(), "A refused detach leaves the IME as it was");
        ime.begin();
        ime.end();
        platform.refuse = false;
        ime.begin();
        ime.end();
        check(ime.isDetached(), "A later successful detach is still applied");
    }

    private static void closeRestoresDefaultContext() {
        var platform = new FakePlatform();
        var ime = new WindowsGameplayIme(platform);
        ime.close();
        check(!ime.isDetached() && platform.calls.equals(List.of(false, true)) && platform.closed,
                "Shutdown restores the default context and frees native libraries");
    }

    private static void staticEntryPointsAreNoOpsWithoutWindow() {
        WindowsGameplayIme.install();
        WindowsGameplayIme.textInputBegin();
        WindowsGameplayIme.textInputEnd();
        WindowsGameplayIme.uninstall();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
