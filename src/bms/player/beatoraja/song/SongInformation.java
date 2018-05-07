package bms.player.beatoraja.song;

import bms.model.*;
import bms.player.beatoraja.Validatable;

import java.util.logging.Logger;

/**
 * 楽曲詳細情報
 * 
 * @author exch
 */
public class SongInformation implements Validatable {
	/**
	 * 譜面のハッシュ値
	 */
	private String sha256;
	/**
	 * 通常ノート総数
	 */
	private int n;
	/**
	 * ロングノート総数
	 */
	private int ln;
	/**
	 * スクラッチノート総数
	 */
	private int s;
	/**
	 * ロングスクラッチノート総数
	 */
	private int ls;
	/**
	 * 
	 */
	private double density;
	private double peakdensity;
	private double enddensity;

	/**
	 * TOTAL
	 */
	private double total;
	/**
	 * 分布
	 */
	private String distribution;
	
	private int[][] distributionValues = new int[0][7];

	public SongInformation() {
		
	}
	
	public SongInformation(BMSModel model) {
		n = model.getTotalNotes(BMSModel.TOTALNOTES_KEY);
		ln = model.getTotalNotes(BMSModel.TOTALNOTES_LONG_KEY);
		s = model.getTotalNotes(BMSModel.TOTALNOTES_SCRATCH);
		ls = model.getTotalNotes(BMSModel.TOTALNOTES_LONG_SCRATCH);
		total = model.getTotal();
		
		int[][] data = new int[model.getLastTime() / 1000 + 2][7];
		int pos = 0;
		int border = (int) (model.getTotalNotes() * (1.0 - 100.0 / model.getTotal()));
		int borderpos = 0;
		for (TimeLine tl : model.getAllTimeLines()) {
			if (tl.getTime() / 1000 != pos) {
				pos = tl.getTime() / 1000;
			}
			for (int i = 0; i < model.getMode().key; i++) {
				Note n = tl.getNote(i);
				if (n != null) {
					if(n instanceof LongNote && !((LongNote)n).isEnd()) {
						for(int index = tl.getTime() / 1000;index <= ((LongNote)n).getPair().getTime() / 1000;index++) {
							data[index][model.getMode().isScratchKey(i) ? 1 : 4]++;
						}
					}

					if(!(model.getLntype() == BMSModel.LNTYPE_LONGNOTE && n instanceof LongNote
							&& ((LongNote) n).isEnd())) {
						if (n instanceof NormalNote) {
							data[tl.getTime() / 1000][model.getMode().isScratchKey(i) ? 2 : 5]++;
						}
						if (n instanceof LongNote) {
							data[tl.getTime() / 1000][model.getMode().isScratchKey(i) ? 0 : 3]++;
							data[tl.getTime() / 1000][model.getMode().isScratchKey(i) ? 1 : 4]--;
						}
						if (n instanceof MineNote) {
							data[tl.getTime() / 1000][6]++;
						}
						
						border--;
						if(border == 0) {
							borderpos = pos;
						}						
					}
				}
			}
		}

		final int bd = model.getTotalNotes() / data.length / 4;
		density = 0;
		peakdensity = 0;
		int count = 0;
		for(int i = 0;i < data.length;i++) {
			int notes = data[i][0] + data[i][1] + data[i][2] + data[i][3] + data[i][4] + data[i][5];
			peakdensity = Math.max(peakdensity, notes);
			if(notes >= bd) {
				density += notes;
				count++;
			}
		}
		density /= count;

		final int d = Math.min(5, data.length - borderpos - 1);
		enddensity = 0;
		for(int i = borderpos;i < data.length - d;i++) {
			int notes = 0;
			for(int j = 0;j < d;j++) {
				notes += data[i + j][0] + data[i + j][1] + data[i + j][2] + data[i + j][3] + data[i + j][4] + data[i + j][5];
			}
			enddensity = Math.max(enddensity, ((double)notes) / d);
		}
		setDistributionValues(data);
	}
	
	public String getDistribution() {
		return distribution;
	}

	public void setDistribution(String distribution) {
		this.distribution = distribution;
		int[] index = {0,2,3,5,6};
		if(distribution.startsWith("#")) {
			index = new int[]{0,1,2,3,4,5,6};
			distribution = distribution.substring(1);
		}
		final int count = distribution.length() % (index.length * 2) == 0 ? distribution.length() / (index.length * 2) : 0;
		if(count == 0) {
			Logger.getGlobal().warning("distributionのString超が不正です");
		}
		distributionValues = new int[count][7];
		for(int i = 0;i < count;i++) {
			for(int j = 0;j < index.length;j++) {
				try {
					distributionValues[i][index[j]] = parseInt36(distribution, i * (index.length * 2) + j * 2);
				} catch(NumberFormatException e) {
					Logger.getGlobal().warning("distribution解析中の例外:" + e.getMessage());
				}
			}
		}
		
	}
	
	public int[][] getDistributionValues() {
		return distributionValues;
	}

	public void setDistributionValues(int[][] values) {
		distributionValues = values;
		StringBuilder sb = new StringBuilder(values.length * 14 + 1);
		sb.append("#");
		for(int i = 0;i < values.length;i++) {
			for(int j = 0;j < 7;j++) {
				int value = Math.min(distributionValues[i][j], 36 * 36 - 1);
				int val1 = value / 36;
				sb.append((char) (val1 >= 10 ? val1 - 10 + 'a' : val1 + '0'));
				val1 = value % 36;
				sb.append((char) (val1 >= 10 ? val1 - 10 + 'a' : val1 + '0'));
			}
		}
		distribution = sb.toString();
	}

	private static int parseInt36(String s, int index) throws NumberFormatException {
		int result = 0;
		final char c1 = s.charAt(index);
		if (c1 >= '0' && c1 <= '9') {
			result = (c1 - '0') * 36;
		} else if (c1 >= 'a' && c1 <= 'z') {
			result = ((c1 - 'a') + 10) * 36;
		} else if (c1 >= 'A' && c1 <= 'Z') {
			result = ((c1 - 'A') + 10) * 36;
		} else {
			throw new NumberFormatException();
		}

		final char c2 = s.charAt(index + 1);
		if (c2 >= '0' && c2 <= '9') {
			result += (c2 - '0');
		} else if (c2 >= 'a' && c2 <= 'z') {
			result += (c2 - 'a') + 10;
		} else if (c2 >= 'A' && c2 <= 'Z') {
			result += (c2 - 'A') + 10;
		} else {
			throw new NumberFormatException();
		}

		return result;
	}

	public int getN() {
		return n;
	}

	public void setN(int n) {
		this.n = n;
	}

	public int getLn() {
		return ln;
	}

	public void setLn(int ln) {
		this.ln = ln;
	}

	public int getS() {
		return s;
	}

	public void setS(int s) {
		this.s = s;
	}

	public int getLs() {
		return ls;
	}

	public void setLs(int ls) {
		this.ls = ls;
	}

	public double getDensity() {
		return density;
	}

	public void setDensity(double density) {
		this.density = density;
	}

	public String getSha256() {
		return sha256;
	}

	public void setSha256(String sha256) {
		this.sha256 = sha256;
	}

	public double getTotal() {
		return total;
	}

	public void setTotal(double total) {
		this.total = total;
	}

	public double getPeakdensity() {
		return peakdensity;
	}

	public void setPeakdensity(double peakdensity) {
		this.peakdensity = peakdensity;
	}
	
	public double getEnddensity() {
		return enddensity;
	}

	public void setEnddensity(double enddensity) {
		this.enddensity = enddensity;
	}

	@Override
	public boolean validate() {
		if(sha256 == null || sha256.length() != 64) {
			return false;
		}
		if(n < 0 || ln < 0 || s < 0 || ls < 0) {
			return false;
		}
		if(density < 0 || peakdensity < 0 || enddensity < 0 || density > peakdensity || enddensity > peakdensity) {
			return false;
		}
		return true;
	}
}
