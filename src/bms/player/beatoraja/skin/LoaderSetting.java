package bms.player.beatoraja.skin;

public class LoaderSetting {
	public LoaderSetting(int alphaAngle[][]) {
		for (int i = 0; i < alphaAngle.length; i++) {
			alphaAngle[i][0] = 255;
			alphaAngle[i][1] = 0;
		}
	}

	public void settingAngle(int alphaAngle[][], int count, String dst[], PMcharaLoader pmCharaLoader) {
		for (int index = 2; index < dst.length; index++) {
			int startValue = 0;
			int endValue = 0;
			for (int i = 0; i < dst[index].length(); i += 2) {
				if (dst[index].length() >= i + 2) {
					if (dst[index].substring(i, i + 2).equals("--")) {
						count = 0;
						for (int j = i; j < dst[index].length() && dst[index].substring(j, j + 2).equals("--"); j += 2)
							count++;
						if (pmCharaLoader.PMparseInt(dst[index].substring(i + count * 2, i + count * 2 + 2), 16) >= 0
								&& pmCharaLoader.PMparseInt(dst[index].substring(i + count * 2, i + count * 2 + 2),
										16) <= 255) {
							endValue = pmCharaLoader.PMparseInt(dst[index].substring(i + count * 2, i + count * 2 + 2),
									16);
							if (index == 3)
								endValue = Math.round(endValue * 360f / 256f);
						}
						for (int j = i; j < dst[index].length()
								&& dst[index].substring(j, j + 2).equals("--"); j += 2) {
							alphaAngle[j / 2][index - 2] = startValue
									+ (endValue - startValue) * ((j - i) / 2 + 1) / (count + 1);
						}
						i += (count - 1) * 2;
					} else if (pmCharaLoader.PMparseInt(dst[index].substring(i, i + 2), 16) >= 0
							&& pmCharaLoader.PMparseInt(dst[index].substring(i, i + 2), 16) <= 255) {
						startValue = pmCharaLoader.PMparseInt(dst[index].substring(i, i + 2), 16);
						if (index == 3)
							startValue = Math.round(startValue * 360f / 256f);
						;
						alphaAngle[i / 2][index - 2] = startValue;
					}
				}
			}
		}
	}
}
