package bms.player.beatoraja.config;

import bms.player.beatoraja.Resolution;

import bms.player.beatoraja.skin.*;

public class SkinConfigurationSkin extends Skin {

	private String[] sampleBMS = {};
	private int defaultSkinType = 0;
	private int customPropertyCount = -1;
	private int customOffsetStyle = 0;

	public SkinConfigurationSkin(Resolution src, Resolution dst) {
		super(src, dst);
	}

	public void setSampleBMS(String[] sampleBMS) {
		this.sampleBMS = sampleBMS;
	}

	public String[] getSampleBMS() {
		return sampleBMS;
	}

	public void setDefaultSkinType(int defaultSkinType) {
		this.defaultSkinType = defaultSkinType;
	}

	public int getDefaultSkinType() {
		return defaultSkinType;
	}

	public void setCustomOffsetStyle(int customOffsetStyle) {
		this.customOffsetStyle = customOffsetStyle;
	}

	public int getCustomOffsetStyle() {
		return customOffsetStyle;
	}

	public void setCustomPropertyCount(int count) {
		this.customPropertyCount = count;
	}

	public int getCustomPropertyCount() {
		return customPropertyCount;
	}
}
