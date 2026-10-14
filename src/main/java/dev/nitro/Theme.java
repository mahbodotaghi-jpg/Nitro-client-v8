package dev.nitro;

import dev.nitro.setting.TextSetting;

public final class Theme {
	public static final int[] ACCENTS = {0xFF7FE3FF, 0xFF4DFF88, 0xFFFF7AC8, 0xFFFFB347};
	public static int accent = ACCENTS[0];

	/** Enabled-modules list (top right). */
	public static boolean hud = true;
	/** Name text (top left). */
	public static boolean watermark = true;
	public static final TextSetting watermarkText = new TextSetting("Name", "Nitro Client");

	private Theme() {}
}
