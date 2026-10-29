package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;
import bms.player.beatoraja.SwingTextInputSupport;
import bms.player.beatoraja.skin.property.StringWriter;

import com.badlogic.gdx.Input;

/**
 * スキンのテキスト入力処理用クラス。
 *
 * 入力はSwingTextInputSupportのダイアログで受ける。ゲームウィンドウ内に
 * テキスト欄を置く方法は取れない:LWJGL2のLinuxKeyboard.openIM()が
 * XSetLocaleModifiers("@im=none")を無条件に呼び、自身のウィンドウに対して
 * 入力メソッドを無効化するため、IMEの変換文字が届かず日本語を入力できない
 * (ローマ字が素通りする)。ダイアログは別ウィンドウでIMEを持てる。
 */
class SkinTextInput {

	private static final String DIALOG_TITLE = "Input";

	/**
	 * ダイアログを多重に開かないためのフラグ
	 */
	private boolean open;

	void focus(MainState state, SkinText target) {
		final StringWriter writer = target.getWriter();
		if (writer == null || open) {
			return;
		}
		open = true;
		SwingTextInputSupport.showTextInput(DIALOG_TITLE, target.getCurrentText(), "",
				state.main.getInputProcessor().getKeyBoardInputProcesseor(), new Input.TextInputListener() {
			@Override
			public void input(String text) {
				writer.set(state, text);
				target.setText(text);
				open = false;
			}

			@Override
			public void canceled() {
				open = false;
			}
		});
	}
}
