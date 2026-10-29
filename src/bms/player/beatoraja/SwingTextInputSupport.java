package bms.player.beatoraja;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.im.InputContext;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

import bms.player.beatoraja.input.KeyBoardInputProcesseor;

/**
 * テキスト入力ダイアログ用の処理。
 *
 * ゲームウィンドウ内のテキスト欄では日本語を入力できない。LWJGL2の
 * LinuxKeyboard.openIM()がXSetLocaleModifiers("@im=none")を無条件に呼び、
 * 自身のウィンドウに対して入力メソッドを無効化するため、IMEの変換文字が届かず
 * ローマ字が素通りする。設定はプロセス全体に効き、Javaからは元に戻せない。
 * 入力メソッドを持てるのは別ウィンドウであるダイアログだけなので、検索ワードの
 * 入力はここを通す。
 *
 * libGDXのGdx.input.getTextInput()を使わず自前で持つのは、フルスクリーンの
 * ゲームウィンドウに隠されないようにするためと、クリックが通らないボタンを
 * 置かずキー操作だけで完結させるためである。
 */
public class SwingTextInputSupport {

	/**
	 * フォントが日本語を描画できるかの判定に使う文字列
	 */
	private static final String JAPANESE_SAMPLE = "日本語ひらがなカタカナ";

	/**
	 * ボタンを置かない代わりに表示するキー操作の案内
	 */
	private static final String KEY_GUIDE = "Enter : OK    Esc : Cancel";

	/**
	 * 差し替えに使う同梱フォント。Configのsystemfontpathの既定値と同じものだが、
	 * この初期化はJavaFX(ランチャー)の起動前に済ませる必要があり、その時点では
	 * まだConfigを読み込めないため直接持つ
	 */
	private static final String FALLBACK_FONT_PATH = "font/VL-Gothic-Regular.ttf";

	/**
	 * フォントを差し替えるUIManagerのキー。ダイアログを構成するコンポーネントと
	 * JOptionPane本体の分
	 */
	private static final String[] FONT_KEYS = { "TextField.font", "Label.font", "Button.font", "Panel.font",
			"OptionPane.font", "OptionPane.messageFont", "OptionPane.buttonFont" };

	private SwingTextInputSupport() {
	}

	/**
	 * テキスト入力ダイアログの初期化を行う。
	 *
	 * main()から、LwjglApplicationの生成より前かつJavaFX(ランチャー)の起動より前に
	 * 呼ぶ必要がある。前者はopenInputMethod()、後者はinstallFontFallback()の
	 * 制約で、どちらもそれぞれのコメントに記載している。
	 */
	public static void initialize() {
		openInputMethod();
		installFontFallback();
	}

	/**
	 * テキスト入力ダイアログを表示する。
	 *
	 * 描画を止めないため表示はSwingのEDTに投げるが、listenerは描画スレッドで呼ぶので
	 * 呼び出し元でスレッドを気にする必要はない。
	 *
	 * @param title    ウィンドウタイトル
	 * @param text     入力欄の初期値
	 * @param hint     入力欄の上に表示する説明。nullまたは空なら表示しない
	 * @param keyboard 表示中のキー入力を抑止するためのキーボード入力処理
	 * @param listener 入力結果の受け取り先
	 */
	public static void showTextInput(String title, String text, String hint, KeyBoardInputProcesseor keyboard,
			Input.TextInputListener listener) {
		// ダイアログを閉じたEnterやEscapeがゲーム側にも渡らないようにする
		keyboard.setTextInputMode(true);
		SwingUtilities.invokeLater(() -> {
			final JTextField field = new JTextField(text != null ? text : "", 20);

			final JPanel panel = new JPanel(new BorderLayout(0, 4));
			if (hint != null && !hint.isEmpty()) {
				final JLabel label = new JLabel(hint);
				label.setForeground(Color.GRAY);
				panel.add(label, BorderLayout.NORTH);
			}
			panel.add(field, BorderLayout.CENTER);
			final JLabel keys = new JLabel(KEY_GUIDE);
			keys.setForeground(Color.GRAY);
			panel.add(keys, BorderLayout.SOUTH);

			// optionsに空配列を渡すとボタンが一つも作られない。フルスクリーン中は
			// LWJGL2のLinuxDisplay.updatePointerGrab()が無条件にgrabPointer()を呼び、
			// ポインタのイベントがゲームウィンドウに吸われてボタンを押せないため、
			// 押せないボタンは置かず操作はキーだけで完結させる。キーボードのグラブは
			// FULLSCREEN_LEGACYのときだけなので、キー入力は届く
			final JOptionPane pane = new JOptionPane(panel, JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION,
					null, new Object[0], null);
			pane.setInitialValue(null);

			final JDialog dialog = pane.createDialog(title);
			pane.selectInitialValue();
			// フルスクリーンのゲームウィンドウの下に回り込ませない
			dialog.setAlwaysOnTop(true);
			// 入力欄にフォーカスを移すのはここでなければならない。ゲームウィンドウと
			// 競合する状況ではwindowOpened/windowActivatedでは間に合わず、
			// フォーカスがダイアログ既定のコンポーネントに残って何も打てなくなる
			dialog.addWindowFocusListener(new WindowAdapter() {
				@Override
				public void windowGainedFocus(WindowEvent e) {
					field.requestFocusInWindow();
				}
			});

			// ボタンが無いので確定とキャンセルをキーに割り当てる。JOptionPaneは
			// valueが変わるとダイアログを閉じる
			field.addActionListener(e -> pane.setValue(JOptionPane.OK_OPTION));
			final JRootPane root = dialog.getRootPane();
			root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
					.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "beatoraja-cancel");
			root.getActionMap().put("beatoraja-cancel", new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					pane.setValue(JOptionPane.CANCEL_OPTION);
				}
			});

			// モーダルなので閉じられるまで戻らない
			dialog.setVisible(true);
			dialog.dispose();

			final boolean accepted = pane.getValue() instanceof Integer option && option == JOptionPane.OK_OPTION;
			final String value = field.getText();
			// ここはEDTなので、ゲーム側に触る処理は描画スレッドに戻してから行う
			Gdx.app.postRunnable(() -> {
				keyboard.setTextInputMode(false);
				if (accepted) {
					listener.input(value);
				} else {
					listener.canceled();
				}
			});
		});
	}

	/**
	 * LWJGL2がX11のロケール修飾子を書き換える前に、AWTのホスト入力メソッドを生成する。
	 *
	 * AWTはXIMを遅延して開くため、何もしないとダイアログを出す時点では既に
	 * "@im=none"に固定されており、IMEが繋がらない。InputContextに変換状態を
	 * 問い合わせるとホスト入力メソッドが生成され、その時点でXOpenIMが走る。
	 */
	private static void openInputMethod() {
		// XSetLocaleModifiersはX11のみ。他のOSでは前倒しする意味がない
		if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("linux")) {
			return;
		}
		try {
			// 入力メソッドを生成させるのが目的で、変換状態そのものは問わない
			InputContext.getInstance().setCompositionEnabled(true);
		} catch (UnsupportedOperationException e) {
			// 変換状態を持たない入力メソッドでも、生成はされているので問題ない
		} catch (Throwable e) {
			Logger.getGlobal().warning("AWT入力メソッドの初期化に失敗:" + e.getMessage());
		}
	}

	/**
	 * Swingの論理フォントが日本語を描画できない場合に、同梱フォントへ差し替える。
	 *
	 * 論理フォント(Dialog/SansSerif/Serif)の実体はfontconfig経由で解決され、
	 * ロケールが日本語でない環境では日本語を含まない実体になることがある。その場合
	 * 入力した検索ワードが豆腐(□)で表示されて読めない。既定のフォントで描画できる
	 * 環境では何もしない。
	 *
	 * UIManagerに初めて触れた時点でSwingのLook&Feelが初期化され、Linuxでは
	 * GTKLookAndFeelがGTKを呼ぶ。これをJavaFXのアプリケーションスレッドから行うと
	 * JavaFX側のGTKと衝突してデッドロックするため、JavaFX(ランチャー)の起動前に
	 * メインスレッドから呼ばなければならない。
	 */
	private static void installFontFallback() {
		final Font current = UIManager.getFont("TextField.font");
		if (current != null && current.canDisplayUpTo(JAPANESE_SAMPLE) == -1) {
			return;
		}

		final File file = new File(FALLBACK_FONT_PATH);
		if (!file.exists()) {
			Logger.getGlobal().warning("ダイアログ用フォントが見つからない:" + FALLBACK_FONT_PATH);
			return;
		}

		final Font font;
		try {
			font = Font.createFont(Font.TRUETYPE_FONT, file);
		} catch (FontFormatException | IOException e) {
			Logger.getGlobal().warning("ダイアログ用フォント読み込み失敗:" + FALLBACK_FONT_PATH);
			return;
		}
		if (font.canDisplayUpTo(JAPANESE_SAMPLE) != -1) {
			return;
		}

		final FontUIResource resource = new FontUIResource(
				font.deriveFont(current != null ? current.getSize2D() : 12f));
		for (String key : FONT_KEYS) {
			UIManager.put(key, resource);
		}
	}
}
