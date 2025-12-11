package bms.player.beatoraja.skin.json;

import java.nio.file.Path;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import bms.player.beatoraja.select.MusicSelectSkin;
import bms.player.beatoraja.select.SkinBar;
import bms.player.beatoraja.select.SkinDistributionGraph;
import bms.player.beatoraja.skin.*;
import bms.player.beatoraja.skin.property.TimerProperty;

/**
 * JSONセレクトスキンオブジェクトローダー
 * 
 * @author exch
 */
public class JsonSelectSkinObjectLoader extends JsonSkinObjectLoader<MusicSelectSkin> {

	public JsonSelectSkinObjectLoader(JSONSkinLoader loader) {
		super(loader);
	}

	@Override
	public MusicSelectSkin getSkin(SkinHeader header) {
		return new MusicSelectSkin(header);
	}

	@Override
	public SkinObject loadSkinObject(MusicSelectSkin skin, JsonSkin.Skin sk, JsonSkin.Destination dst, Path p) {
		SkinObject obj =super.loadSkinObject(skin, sk, dst, p);
		if(obj != null) {
			return obj;
		}
		
		if (sk.songlist != null && dst.id.equals(sk.songlist.id)) {
			SkinBar barobj = new SkinBar(0);

			SkinImage[] onimage = new SkinImage[sk.songlist.liston.length];
			SkinImage[] offimage = new SkinImage[sk.songlist.listoff.length];

			// 選曲バー本体(選曲時/非選曲時)
			for (int i = 0; i < sk.songlist.liston.length; i++) {
				for (JsonSkin.ImageSet imgs : sk.imageset) {
					if (sk.songlist.liston[i].id.equals(imgs.id)) {
						TextureRegion[][] tr = new TextureRegion[imgs.images.length][];
						TimerProperty timer = null;
						int cycle = -1;
						for (int j = 0; j < imgs.images.length; j++) {
							for (JsonSkin.Image img : sk.image) {
								if (img.id.equals(imgs.images[j])) {
									Texture tex = getTexture(img.src, p);
									if(tex != null) {
										tr[j] = getSourceImage(tex, img.x, img.y, img.w, img.h, img.divx,
												img.divy);
										if (timer == null) {
											timer = img.timer;
										}
										if (cycle == -1) {
											cycle = img.cycle;
										}													
									}
									break;
								}
							}
						}

						onimage[i] = new SkinImage(tr, timer, cycle, null);
						offimage[i] = new SkinImage(tr, timer, cycle, null);
						setDestination(skin, onimage[i], sk.songlist.liston[i]);
						setDestination(skin, offimage[i], sk.songlist.listoff[i]);
						break;
					}
				}
			}
			barobj.setBarImage(onimage, offimage);

			((MusicSelectSkin) skin).setCenterBar(sk.songlist.center);
			((MusicSelectSkin) skin).setClickableBar(sk.songlist.clickable);

			// 選曲バーランプ
			for (int i = 0; i < sk.songlist.lamp.length; i++) {
				for (JsonSkin.Image img : sk.image) {
					if (sk.songlist.lamp[i].id.equals(img.id)) {
						Texture tex = getTexture(img.src, p);
						if(tex != null) {
							SkinImage lamp = new SkinImage(
									getSourceImage(tex, img.x, img.y, img.w, img.h, img.divx, img.divy),
									img.timer, img.cycle);
							setDestination(skin, lamp, sk.songlist.lamp[i]);
							barobj.setLamp(i, lamp);										
						}
						break;
					}
				}
			}
			
			// 選曲バープレイヤーランプ
			for (int i = 0; i < sk.songlist.playerlamp.length; i++) {
				for (JsonSkin.Image img : sk.image) {
					if (sk.songlist.playerlamp[i].id.equals(img.id)) {
						Texture tex = getTexture(img.src, p);
						if(tex != null) {
							SkinImage playerlamp = new SkinImage(
									getSourceImage(tex, img.x, img.y, img.w, img.h, img.divx, img.divy),
									img.timer, img.cycle);
							setDestination(skin, playerlamp, sk.songlist.playerlamp[i]);
							barobj.setPlayerLamp(i, playerlamp);										
						}
						break;
					}
				}
			}

			// 選曲バーライバルランプ
			for (int i = 0; i < sk.songlist.rivallamp.length; i++) {
				for (JsonSkin.Image img : sk.image) {
					if (sk.songlist.rivallamp[i].id.equals(img.id)) {
						Texture tex = getTexture(img.src, p);
						SkinImage rivallamp = new SkinImage(
								getSourceImage(tex, img.x, img.y, img.w, img.h, img.divx, img.divy),
								img.timer, img.cycle);
						setDestination(skin, rivallamp, sk.songlist.rivallamp[i]);
						barobj.setRivalLamp(i, rivallamp);
						break;
					}
				}
			}

			// 選曲バートロフィー
			for (int i = 0; i < sk.songlist.trophy.length; i++) {
				for (JsonSkin.Image img : sk.image) {
					if (sk.songlist.trophy[i].id.equals(img.id)) {
						Texture tex = getTexture(img.src, p);
						if(tex != null) {
							SkinImage trophy = new SkinImage(
									getSourceImage(tex, img.x, img.y, img.w, img.h, img.divx, img.divy),
									img.timer, img.cycle);
							setDestination(skin, trophy, sk.songlist.trophy[i]);
							barobj.setTrophy(i, trophy);										
						}

						break;
					}
				}
			}

			for (int i = 0; i < sk.songlist.label.length; i++) {
				for (JsonSkin.Image img : sk.image) {
					if (sk.songlist.label[i].id.equals(img.id)) {
						Texture tex = getTexture(img.src, p);
						if(tex != null) {
							SkinImage label = new SkinImage(
									getSourceImage(tex, img.x, img.y, img.w, img.h, img.divx, img.divy),
									img.timer, img.cycle);
							setDestination(skin, label, sk.songlist.label[i]);
							barobj.setLabel(i, label);										
						}
						break;
					}
				}
			}

			// 選曲バーテキスト
			for (int i = 0; i < sk.songlist.text.length; i++) {
				for (JsonSkin.Text text : sk.text) {
					if (sk.songlist.text[i].id.equals(text.id)) {
						SkinText skinTexts = createText(text, p);
						if (skinTexts != null) {
							setDestination(skin, skinTexts, sk.songlist.text[i]);
							barobj.setText(i, skinTexts);
						}
						break;
					}
				}
			}

			// 選曲バーレベル表示
			for (int i = 0; i < sk.songlist.level.length; i++) {
				for (JsonSkin.Value value : sk.value) {
					if (sk.songlist.level[i].id.equals(value.id)) {
						Texture tex = getTexture(value.src, p);
						if(tex != null) {
							TextureRegion[] numimages = getSourceImage(tex, value.x, value.y, value.w, value.h,
									value.divx, value.divy);
							int d = numimages.length % 10 == 0 ? 10 : 11;

							TextureRegion[][] nimages = new TextureRegion[value.divx * value.divy / d][d];
							for (int j = 0; j < d; j++) {
								for (int k = 0; k < value.divx * value.divy / d; k++) {
									nimages[k][j] = numimages[k * d + j];
								}
							}
							SkinNumber numbers = new SkinNumber(nimages, value.timer, value.cycle, value.digit,
									d > 10 ? 2 : 0, value.space, value.ref, value.align);
							setDestination(skin, numbers, sk.songlist.level[i]);
							barobj.setBarlevel(i, numbers);										
						}
						break;
					}
				}
			}

			// 選曲バー分布グラフバー
			for (JsonSkin.Graph img : sk.graph) {
				if (sk.songlist.graph != null && sk.songlist.graph.id.equals(img.id)) {
					if (img.type < 0) {
						Texture tex = getTexture(img.src, p);
						if(tex != null) {
							TextureRegion[][] imgs = null;
							if(tex != null) {
								TextureRegion[] images = getSourceImage(tex, img.x, img.y, img.w, img.h,
										img.divx, img.divy);
								final int len = img.type == -1 ? 11 : 28;
								imgs = new TextureRegion[len][images.length / len];
								for(int j = 0 ;j < len;j++) {
									for(int i = 0 ;i < imgs[j].length;i++) {
										imgs[j][i] = images[i * len + j];
									}
								}
							}

							final int graphtype = img.type == -1 ? 0 : 1;

							SkinDistributionGraph bargraph = null;
							if(imgs != null) {
								bargraph = new SkinDistributionGraph(graphtype,  imgs, img.timer, img.cycle);
							} else {
								bargraph = new SkinDistributionGraph(graphtype);
							}

							setDestination(skin, bargraph, sk.songlist.graph);
							barobj.setGraph(bargraph);										
						}
					}
				}
			}
			return barobj;
		}
		
		return null;
	}
}
