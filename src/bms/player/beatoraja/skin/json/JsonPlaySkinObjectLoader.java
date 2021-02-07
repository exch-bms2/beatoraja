package bms.player.beatoraja.skin.json;

import static bms.player.beatoraja.skin.SkinProperty.OFFSET_HIDDEN_COVER;
import static bms.player.beatoraja.skin.SkinProperty.OFFSET_LIFT;

import java.nio.file.Path;
import java.util.Arrays;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

import bms.player.beatoraja.Resolution;
import bms.player.beatoraja.play.*;
import bms.player.beatoraja.skin.*;
import bms.player.beatoraja.skin.SkinObject.SkinOffset;

public class JsonPlaySkinObjectLoader extends JsonSkinObjectLoader<PlaySkin> {

	public JsonPlaySkinObjectLoader(JSONSkinLoader loader) {
		super(loader);
	}

	@Override
	public PlaySkin getSkin(Resolution src, Resolution dst) {
		return new PlaySkin(src, dst);
	}

	@Override
	public SkinObject loadSkinObject(PlaySkin skin, JsonSkin.Skin sk, JsonSkin.Destination dst, Path p) {
		SkinObject obj =super.loadSkinObject(skin, sk, dst, p);
		if(obj != null) {
			return obj;
		}
		
		// note (playskin only)
		if(sk.note != null && dst.id.equals(sk.note.id)) {
			
			// TODO プレイスキン固有の値設定。note未定義時は通らないので場所を変えたほうがいいかも
			skin.setClose(sk.close);
			skin.setLoadend(sk.loadend);
			skin.setPlaystart(sk.playstart);
			skin.setJudgetimer(sk.judgetimer);
			skin.setFinishMargin(sk.finishmargin);
			
			SkinSource[] notes = getNoteTexture(sk.note.note, p);
			SkinSource[][] lns = new SkinSource[10][];
			lns[0] = getNoteTexture(sk.note.lnend, p);
			lns[1] = getNoteTexture(sk.note.lnstart, p);
			if(sk.note.lnbodyActive != null && sk.note.lnbodyActive.length > 0) {
				lns[2] = getNoteTexture(sk.note.lnbodyActive, p);
				lns[3] = getNoteTexture(sk.note.lnbody, p);
			} else {
				lns[2] = getNoteTexture(sk.note.lnbody, p);
				lns[3] = getNoteTexture(sk.note.lnactive, p);
			}
			lns[4] = getNoteTexture(sk.note.hcnend, p);
			lns[5] = getNoteTexture(sk.note.hcnstart, p);
			if(sk.note.hcnbodyActive != null && sk.note.hcnbodyActive.length > 0) {
				lns[6] = getNoteTexture(sk.note.hcnbodyActive, p);
				lns[7] = getNoteTexture(sk.note.hcnbody, p);
				lns[8] = getNoteTexture(sk.note.hcnbodyReactive, p);
				lns[9] = getNoteTexture(sk.note.hcnbodyMiss, p);
			} else {
				lns[6] = getNoteTexture(sk.note.hcnbody, p);
				lns[7] = getNoteTexture(sk.note.hcnactive, p);
				lns[8] = getNoteTexture(sk.note.hcndamage, p);
				lns[9] = getNoteTexture(sk.note.hcnreactive, p);
			}
			final SkinSource[][] lnss = new SkinSource[lns[0].length][10];
			for(int i = 0;i < 10;i++) {
				for(int j = 0;j < lns[0].length;j++) {
					lnss[j][i] = lns[i][j];
				}
			}

			SkinSource[] mines = getNoteTexture(sk.note.mine, p);

			Rectangle[] region = new Rectangle[sk.note.dst.length];
			float[] scale = new float[region.length];
			int[] dstnote2 = new int[region.length];
			Arrays.fill(dstnote2,  Integer.MIN_VALUE);
			float dx = (float)loader.dstr.width / sk.w;
			float dy = (float)loader.dstr.height / sk.h;
			for (int i = 0; i < region.length; i++) {
				JsonSkin.Animation dest = sk.note.dst[i];
				region[i] = new Rectangle(dest.x * dx, dest.y * dy, dest.w * dx, dest.h * dy);
				if(i < sk.note.size.length) {
					scale[i] = sk.note.size[i] * dy;
				} else {
					scale[i] = ((SkinSourceImage)notes[i]).getImages()[0][0].getRegionHeight() * dy;
				}
			}
			Rectangle[] gregion = new Rectangle[sk.note.group.length];
			SkinImage[] lines = new SkinImage[gregion.length];
			for (int i = 0; i < gregion.length; i++) {
				JsonSkin.Destination dest = sk.note.group[i];
				gregion[i] = new Rectangle(dest.dst[0].x * dx, dest.dst[0].y * dy, dest.dst[0].w * dx,
						dest.dst[0].h * dy);

				for (JsonSkin.Image img : sk.image) {
					if (dest.id.equals(img.id)) {
						Texture tex = getTexture(img.src, p);
						lines[i] = new SkinImage(
								getSourceImage(tex, img.x, img.y, img.w, img.h, img.divx, img.divy),
								img.timer, img.cycle);
						setDestination(skin, lines[i], dest);
						break;
					}
				}

			}
			((PlaySkin) skin).setLine(lines);

			if(sk.note.bpm != null) {
				SkinImage[] bpm = new SkinImage[gregion.length];
				for (int i = 0; i < gregion.length && i < sk.note.bpm.length; i++) {
					JsonSkin.Destination dest = sk.note.bpm[i];

					for (JsonSkin.Image img : sk.image) {
						if (dest.id.equals(img.id)) {
							Texture tex = getTexture(img.src, p);
							bpm[i] = new SkinImage(
									getSourceImage(tex, img.x, img.y, img.w, img.h, img.divx, img.divy),
									img.timer, img.cycle);
							setDestination(skin, bpm[i], dest);
							break;
						}
					}
				}
				((PlaySkin) skin).setBPMLine(bpm);
			}

			if(sk.note.stop != null) {
				SkinImage[] stop = new SkinImage[gregion.length];
				for (int i = 0; i < gregion.length && i < sk.note.stop.length; i++) {
					JsonSkin.Destination dest = sk.note.stop[i];

					for (JsonSkin.Image img : sk.image) {
						if (dest.id.equals(img.id)) {
							Texture tex = getTexture(img.src, p);
							stop[i] = new SkinImage(
									getSourceImage(tex, img.x, img.y, img.w, img.h, img.divx, img.divy),
									img.timer, img.cycle);
							setDestination(skin, stop[i], dest);
							break;
						}
					}
				}
				((PlaySkin) skin).setStopLine(stop);
			}

			if(sk.note.time != null) {
				SkinImage[] time = new SkinImage[gregion.length];
				for (int i = 0; i < gregion.length && i < sk.note.time.length; i++) {
					JsonSkin.Destination dest = sk.note.time[i];

					for (JsonSkin.Image img : sk.image) {
						if (dest.id.equals(img.id)) {
							Texture tex = getTexture(img.src, p);
							time[i] = new SkinImage(
									getSourceImage(tex, img.x, img.y, img.w, img.h, img.divx, img.divy),
									img.timer, img.cycle);
							setDestination(skin, time[i], dest);
							break;
						}
					}
				}
				((PlaySkin) skin).setTimeLine(time);
			}

			if(sk.note.dst2 != Integer.MIN_VALUE) {
				Arrays.fill(dstnote2, (int) Math.round(sk.note.dst2 * dy));
			}
			SkinNote sn = new SkinNote(notes, lnss, mines);
			sn.setLaneRegion(region, scale, dstnote2, skin);
			((PlaySkin) skin).setLaneRegion(region);
			((PlaySkin) skin).setLaneGroupRegion(gregion);
			((PlaySkin) skin).setNoteExpansionRate(sk.note.expansionrate);
			obj = sn;
		}
		// hidden cover (playskin only)
		for (JsonSkin.HiddenCover img : sk.hiddenCover) {
			if (dst.id.equals(img.id)) {
				Texture tex = getTexture(img.src, p);
				if(tex != null) {
					obj = new SkinHidden(getSourceImage(tex, img.x, img.y, img.w, img.h, img.divx, img.divy), img.timer, img.cycle);
					((SkinHidden) obj).setDisapearLine((float) (img.disapearLine * skin.getScaleY()));
					((SkinHidden) obj).setDisapearLineLinkLift(img.isDisapearLineLinkLift);
					int[] offsets = new int[dst.offsets.length + 2];
					for(int i = 0; i < dst.offsets.length; i++) {
						offsets[i] = dst.offsets[i];
					}
					offsets[dst.offsets.length] = OFFSET_LIFT;
					offsets[dst.offsets.length + 1] = OFFSET_HIDDEN_COVER;
					dst.offsets = offsets;								
				}
				break;
			}
		}
		// lift cover (playskin only)
		for (JsonSkin.LiftCover img : sk.liftCover) {
			if (dst.id.equals(img.id)) {
				Texture tex = getTexture(img.src, p);
				if(tex != null) {
					obj = new SkinHidden(getSourceImage(tex, img.x, img.y, img.w, img.h, img.divx, img.divy), img.timer, img.cycle);
					((SkinHidden) obj).setDisapearLine((float) (img.disapearLine * skin.getScaleY()));
					((SkinHidden) obj).setDisapearLineLinkLift(img.isDisapearLineLinkLift);
					int[] offsets = new int[dst.offsets.length + 2];
					for(int i = 0; i < dst.offsets.length; i++) {
						offsets[i] = dst.offsets[i];
					}
					offsets[dst.offsets.length] = OFFSET_LIFT;
					dst.offsets = offsets;								
				}

				break;
			}
		}
		// bga (playskin only)
		if (sk.bga != null && dst.id.equals(sk.bga.id)) {
			obj = new SkinBGA(loader.bgaExpand);
		}
		// judge (playskin only)
		for (JsonSkin.Judge judge : sk.judge) {
			if (dst.id.equals(judge.id)) {
				SkinImage[] images = new SkinImage[judge.images.length];
				SkinNumber[] numbers = new SkinNumber[judge.images.length];
				for (int i = 0; i < judge.images.length; i++) {
					for (JsonSkin.Image img : sk.image) {
						if (judge.images[i].id.equals(img.id)) {
							Texture tex = getTexture(img.src, p);
							if(tex != null) {
								images[i] = new SkinImage(
										getSourceImage(tex, img.x, img.y, img.w, img.h, img.divx, img.divy),
										img.timer, img.cycle);
								setDestination(skin, images[i], judge.images[i]);											
							}
							break;
						}
					}

					for (JsonSkin.Value value : sk.value) {
						if (judge.numbers[i].id.equals(value.id)) {
							Texture tex = getTexture(value.src, p);
							if(tex != null) {
								TextureRegion[] numimages = getSourceImage(tex, value.x, value.y, value.w,
										value.h, value.divx, value.divy);
								int d = numimages.length % 10 == 0 ? 10 : 11;

								TextureRegion[][] nimages = new TextureRegion[value.divx * value.divy / d][d];
								for (int j = 0; j < d; j++) {
									for (int k = 0; k < value.divx * value.divy / d; k++) {
										nimages[k][j] = numimages[k * d + j];
									}
								}
								numbers[i] = new SkinNumber(nimages, value.timer, value.cycle, value.digit,
										d > 10 ? 2 : 0, value.space, value.ref);
								numbers[i].setAlign(2);
								numbers[i].setRelative(true);
								if(value.offset != null) {
									SkinOffset[] offsets = new SkinOffset[value.offset.length];
									for(int j = 0;j < offsets.length;j++) {
										offsets[j] = new SkinOffset();
										offsets[j].x = value.offset[j].x;
										offsets[j].y = value.offset[j].y;
										offsets[j].w = value.offset[j].w;
										offsets[j].h = value.offset[j].h;
									}
									numbers[i].setOffsets(offsets);
								}

								for(JsonSkin.Animation ani : judge.numbers[i].dst) {
									ani.x -= ani.w * value.digit / 2;
								}
								setDestination(skin, numbers[i], judge.numbers[i]);											
							}
							break;
						}
					}

				}
				obj = new SkinJudge(images, numbers, judge.index, judge.shift);

				int region = ((PlaySkin) skin).getJudgeregion();
				if (judge.index >= region) {
					((PlaySkin) skin).setJudgeregion(judge.index + 1);
				}
				break;
			}
		}

		return obj;
	}
}
