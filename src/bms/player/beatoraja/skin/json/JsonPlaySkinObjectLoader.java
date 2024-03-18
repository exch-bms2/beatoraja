package bms.player.beatoraja.skin.json;

import static bms.player.beatoraja.skin.SkinProperty.OFFSET_HIDDEN_COVER;
import static bms.player.beatoraja.skin.SkinProperty.OFFSET_LIFT;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

import bms.player.beatoraja.play.*;
import bms.player.beatoraja.skin.*;
import bms.player.beatoraja.skin.SkinObject.SkinOffset;

public class JsonPlaySkinObjectLoader extends JsonSkinObjectLoader<PlaySkin> {

	public JsonPlaySkinObjectLoader(JSONSkinLoader loader) {
		super(loader);
	}

	@Override
	public PlaySkin getSkin(SkinHeader header) {
		return new PlaySkin(header);
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
					scale[i] = ((SkinSourceImage)notes[i]).getImages()[0].getRegionHeight() * dy;
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
			skin.setLine(lines);

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
				skin.setBPMLine(bpm);
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
				skin.setStopLine(stop);
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
				skin.setTimeLine(time);
			}

			if(sk.note.dst2 != Integer.MIN_VALUE) {
				Arrays.fill(dstnote2, Math.round(sk.note.dst2 * dy));
			}
			SkinNote sn = new SkinNote(notes, lnss, mines);
			sn.setLaneRegion(region, scale, dstnote2, skin);
			skin.setLaneRegion(region);
			skin.setLaneGroupRegion(gregion);
			skin.setNoteExpansionRate(sk.note.expansionrate);
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
                    System.arraycopy(dst.offsets, 0, offsets, 0, dst.offsets.length);
					offsets[dst.offsets.length] = OFFSET_LIFT;
					offsets[dst.offsets.length + 1] = OFFSET_HIDDEN_COVER;
					dst.offsets = offsets;								
				}
				return obj;
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
                    System.arraycopy(dst.offsets, 0, offsets, 0, dst.offsets.length);
					offsets[dst.offsets.length] = OFFSET_LIFT;
					dst.offsets = offsets;
					return obj;
				}
			}
		}
		// bga (playskin only)
		if (sk.bga != null && dst.id.equals(sk.bga.id)) {
			return new SkinBGA(loader.bgaExpand);
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
										d > 10 ? 2 : 0, value.space, value.ref, 2);
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

				int region = skin.getJudgeregion();
				if (judge.index >= region) {
					skin.setJudgeregion(judge.index + 1);
				}
				return obj;
			}
		}

		//POMYU chara
		for (JsonSkin.PMchara chara : sk.pmchara) {
			if (dst.id.equals(chara.id)) {
				//type 0:プレイ 1:キャラ背景 2:名前画像 3:ハリアイ画像(上半身のみ) 4:ハリアイ画像(全体) 5:キャラアイコン 6:NEUTRAL 7:FEVER 8:GREAT 9:GOOD 10:BAD 11:FEVERWIN 12:WIN 13:LOSE 14:OJAMA 15:DANCE
				File imagefile = getSrcIdPath(chara.src, p);
				if(dst.dst.length > 0 && imagefile != null) {
					int color = chara.color == 2 ? 2 : 1;
					int side = chara.side == 2 ? 2 : 1;
					int[] option = new int[3];
					for(int i = 0; i < option.length; i++) {
						if(i < dst.op.length) option[i] = dst.op[i];
						else option[i] = 0;
					}
					if(chara.type == 0) {
						obj = new PomyuCharaLoader(skin).load(loader.usecim, imagefile, chara.type, color,
								dst.dst[0].x, dst.dst[0].y, dst.dst[0].w, dst.dst[0].h,
								side, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, dst.offset);
					} else if(chara.type >= 1 && chara.type <= 5) {
						obj = new PomyuCharaLoader(skin).load(loader.usecim, imagefile, chara.type, color,
								Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE,
								Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
					} else if(chara.type >= 6 && chara.type <= 15) {
						obj = new PomyuCharaLoader(skin).load(loader.usecim, imagefile, chara.type, color,
								dst.dst[0].x, dst.dst[0].y, dst.dst[0].w, dst.dst[0].h,
								Integer.MIN_VALUE, dst.timer, option[0], option[1], option[2], dst.offset);
					}
					return obj;
				}
			}
		}

		return obj;
	}
}
