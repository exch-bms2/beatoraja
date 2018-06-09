package bms.player.beatoraja.input;

public class mouseData {
	public static int mousex;
	public static int mousey;
	public static int mousebutton;
	public static boolean mousepressed;
	public static boolean mousedragged;
	public static boolean mouseMoved;

	
	public static int getMouseX() {
		return mousex;
	}

	public static int getMouseY() {
		return mousey;
	}

	public static int getMouseButton() {
		return mousebutton;
	}

	public static boolean isMousePressed() {
		return mousepressed;
	}

	public static void setMousePressed() {
		mousepressed = false;
	}

	public static boolean isMouseDragged() {
		return mousedragged;
	}

	public static void setMouseDragged() {
		mousedragged = false;
	}

	public static boolean isMouseMoved() {
		return mouseMoved;
	}

	public static void setMouseMoved(boolean mouseMoved) {
		mouseMoved = mouseMoved;
	}
}