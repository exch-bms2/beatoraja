package bms.player.beatoraja.skin;

import java.util.Arrays;
/**
 * 小数点数の配列表現フォーマッター
 *
 * @author keh
 */
public final class FloatFormatter {

    private final int iketa;
    private final int fketa;
    private final int sign;
    private final int length;
    private final int zeropadding;
    private final int[] digits;
    private int base;

    private static final int KETAMAX = 8;

    private static final int SIGNSYMBOL = 12;
    private static final int DECIMALPOINT = 11;
    private static final int REVERSEZERO = 10;

    public int getIketa() {
        return iketa;
    }

    public int getFketa() {
        return fketa;
    }

    public int getSign() {
        return sign;
    }

    public int getZeropadding() {
        return zeropadding;
    }

    public int[] getDigits() {
        return digits;
    }

    public int getketaLength() {
        return length;
    }
    /**
     * 整数桁+少数桁≧8 超える場合は少数桁優先で8となる
     * @param iketa 整数桁数
     * @param fketa 少数桁数
     * @param sign 符号を表示するか
     * @param zeropadding ゼロパディング方式
     */
    public FloatFormatter(int iketa, int fketa, boolean sign, int zeropadding) {
        int tempiketa = (iketa >= 0) ? iketa : 0;
        int tempfketa = (fketa >= 0) ? fketa : 0;
        this.sign = sign ? 1 : 0;
        this.zeropadding = (zeropadding >= 2) ? 2 : zeropadding >= 1 ? 1 : 0; // 2 or 1 or 0
        if (tempiketa >= KETAMAX || tempfketa >= KETAMAX || tempiketa + tempfketa >= KETAMAX) {
            this.fketa = (tempfketa < KETAMAX) ? tempfketa : KETAMAX;
            this.iketa = KETAMAX - this.fketa;
        } else {
            this.iketa = tempiketa;
            this.fketa = tempfketa;
        }
        this.length = this.sign + this.iketa + this.fketa + ((this.fketa != 0) ? 1 : 0);
        this.digits = new int[this.length + 1];
        this.base = this.sign + this.iketa;
        Arrays.fill(this.digits, -1);
    }

    public int[] calcuateAndGetDigits(double value) {
        if (this.digits.length == 1) {
            return this.digits; // 空定義用
        }
        Arrays.fill(this.digits, -1);
        if (this.iketa == 0 && this.fketa == 0 && this.sign == 1) {
            this.digits[1] = SIGNSYMBOL;
            return this.digits; // 符号のみ用
        }

        boolean isSign;
        isSign = (this.sign == 1) && (value < Math.pow(10, this.iketa));
        if (this.zeropadding == 0) { // 整数桁数算出
            int ival = (int) value;
            this.base = Math.min(this.iketa, (int) Math.log10((ival != 0) ? ival : 1) + 1) + this.sign;
        }
        long fval = (long) (value * Math.pow(10, this.fketa));
        int nowketa;
        if (this.iketa == 0) {
            nowketa = this.fketa + this.sign + 1;
        } else {
            nowketa = this.base + this.fketa + ((this.fketa != 0) ? 1 : 0);
        }
        int fcnt = this.fketa;
        while (nowketa > this.sign) {
            if (fcnt > -1) {
                digits[nowketa] = (int) (fval % 10);
            } else {
                if (fval == 0 && this.zeropadding == 2) {
                    digits[nowketa] = REVERSEZERO;
                } else {
                    digits[nowketa] = (int) (fval % 10);
                }
            }
            fcnt--;
            if (fcnt == 0) {
                nowketa--;
                digits[nowketa] = DECIMALPOINT;
            }
            fval /= 10;
            nowketa--;
        }
        if (nowketa == 1) {
            if (isSign) {
                digits[1] = SIGNSYMBOL;
            } else {
                digits[1] = (int) (fval % 10);
            }
        }

        if (this.iketa == 0 && this.sign == 1) {
            digits[1] = SIGNSYMBOL;
        }

        return this.digits;
    }

}
