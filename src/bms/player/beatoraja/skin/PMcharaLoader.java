package bms.player.beatoraja.skin;

import static bms.player.beatoraja.skin.SkinProperty.OPTION_1P_100;
import static bms.player.beatoraja.skin.SkinProperty.OPTION_1P_BORDER_OR_MORE;
import static bms.player.beatoraja.skin.SkinProperty.TIMER_MUSIC_END;
import static bms.player.beatoraja.skin.SkinProperty.TIMER_PM_CHARA_1P_BAD;
import static bms.player.beatoraja.skin.SkinProperty.TIMER_PM_CHARA_1P_FEVER;
import static bms.player.beatoraja.skin.SkinProperty.TIMER_PM_CHARA_1P_GOOD;
import static bms.player.beatoraja.skin.SkinProperty.TIMER_PM_CHARA_1P_GREAT;
import static bms.player.beatoraja.skin.SkinProperty.TIMER_PM_CHARA_1P_NEUTRAL;
import static bms.player.beatoraja.skin.SkinProperty.TIMER_PM_CHARA_2P_BAD;
import static bms.player.beatoraja.skin.SkinProperty.TIMER_PM_CHARA_2P_GREAT;
import static bms.player.beatoraja.skin.SkinProperty.TIMER_PM_CHARA_2P_NEUTRAL;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class PMcharaLoader {
	private final Skin skin;
	
	public static final int PLAY = 0;
	public static final int BACKGROUND = 1;
	public static final int NAME = 2;
	public static final int FACE_UPPER = 3;
	public static final int FACE_ALL = 4;
	public static final int SELECT_CG = 5;
	private final int NO_VALUE = -1;
	private final int CharBMPIndex = 0;
	private final int CharTexIndex = 2;
	private final int CharFaceIndex = 4;
	private final int SelectCGIndex = 6;
	private final PMparseMapping parseMapping = new PMparseMapping(CharBMPIndex, CharTexIndex, CharFaceIndex, SelectCGIndex);
	int[] charFaceUpperXywh = {0, 0, 256, 256};
	int[] charFaceAllXywh = {320, 0, 320, 480};
	
	private int loop[];
	private int[][] Position;
	private int anime = 100;
	
	private int setColor = 1;
	private int setMotion = Integer.MIN_VALUE;
	private int frame[];
	private int size[] = {0, 0};
	private Texture[] CharBMP ;
	List<List<String>> patternData = new ArrayList<List<String>>();
	
	public PMcharaLoader(Skin skin) {
		this.skin = skin;
	}
	
	private int PMcharaTime[] = {1,1,1,1,1,1,1,1};

	public int getPMcharaTime(int index) {
		if(index < 0 || index >= PMcharaTime.length) return 1;
		return PMcharaTime[index];
	}
	
	public void setPMcharaTime(int index, int value) {
		if(index >= 0 && index < PMcharaTime.length && value >= 1) {
			this.PMcharaTime[index] = value;
		}
	}

	public SkinImage Load(boolean usecim, File imagefile, int type, int color, SkinDestinationSize dstSize, int side, int dsttimer, SkinOption skinOption) {
		//type 0:占쎈쐻占쎈윞�눧琉룸쐻占쎈윞占쎈빣占쎈쐻占쎈윞占쎈�� 1:占쎈쐻占쎈윞占쎈��占쎈쐻占쎈윞�맱�뎽�쐻占쎈윞�뜝�뜴�쐻占쎈윞占쎈뤇占쎈쐻占쎈윪占쎈�� 2:占쎈쐻占쎈윥占쎈돗占쎈쐻占쎈윥占쎌졋占쎈쐻占쎈윥占쎈짍占쎈쐻占쎈윞占쎈뤍 3:占쎈쐻占쎈윞占쎈뤍占쎈쐻占쎈윞占쎈빞占쎈쐻占쎈윞占쎈�섓옙�쐻占쎈윞占쎈�욑옙�쐻占쎈윥占쎈짍占쎈쐻占쎈윞占쎈뤍(佯몃돆鍮뽬린�뫜�삕占쎈쭎�솾占썸�ⓑ븍춪�뇦猿뗰옙占쏙옙�굲�뇦猿볦삕) 4:占쎈쐻占쎈윞占쎈뤍占쎈쐻占쎈윞占쎈빞占쎈쐻占쎈윞占쎈�섓옙�쐻占쎈윞占쎈�욑옙�쐻占쎈윥占쎈짍占쎈쐻占쎈윞占쎈뤍(占쎈쐻占쎈윥�뜝�럡��占쎄샴占쎈쐻�뜝占�) 5:占쎈쐻占쎈윞占쎈��占쎈쐻占쎈윞�맱�뎽�쐻占쎈윞�뜝�뜴�쐻占쎈윞占쎈�섓옙�쐻占쎈윞占쎈�욑옙�쐻占쎈윞占쎈�뤄옙�쐻占쎈윞占쎈뼀 6:NEUTRAL 7:FEVER 8:GREAT 9:GOOD 10:BAD 11:FEVERWIN 12:WIN 13:LOSE 14:OJAMA 15:DANCE
		
		if(type < 0 || type > 15) return null;
		File chpdir = null;
		int fileState = 0;
		boolean isPathFile = !imagefile.exists() && imagefile.getPath().substring(imagefile.getPath().length()-4,imagefile.getPath().length()).equalsIgnoreCase(".chp");
		boolean isChpFile = imagefile.exists() && imagefile.getPath().substring(imagefile.getPath().length()-4,imagefile.getPath().length()).equalsIgnoreCase(".chp");
		boolean isEqualsFile = imagefile.getPath().charAt(imagefile.getPath().length()-1) != '/' && imagefile.getPath().charAt(imagefile.getPath().length()-1) != '\\';
		if(isPathFile) {
			fileState = 1;
		}
		else if(isChpFile) {
			fileState = 2;
		}
		else if(isEqualsFile) {
			fileState = 3;
		}
		else {
			fileState = 4;
		}
		File chp = SkinFileLoad(imagefile , chpdir, fileState);
		if(chp == null) return null;

		//占쎈쐻占쎈윥占쎈짍占쎈쐻占쎈윞占쎈뤍占쎈쐻占쎈윞占쎈룺占쎈쐻占쎈윞占쎈뼎占쎈쐻占쎈윞占쎈마 0:#CharBMP 1:#CharBMP2P 2:#CharTex 3:#CharTex2P 4:#CharFace 5:#CharFace2P 6:#SelectCG 7:#SelectCG2P
		CharBMP = new Texture[8];
		Arrays.fill(CharBMP, null);
		//占쎈쐻占쎈윥占쎈뉵占쎈쐻占쎈윞占쎈뭶占쎈쐻占쎈윞�뜝�뜴�쐻占쎈윞�맱袁��쐻占쎈윞占쎈뼎占쎈쐻占쎈윞占쎈마
		Position = new int[1296][4];
		for(int[] i: Position){
			Arrays.fill(i, 0);
		}
		frame = new int[20];
		Arrays.fill(frame, Integer.MIN_VALUE);
		loop = new int[20];
		Arrays.fill(loop, -1);

		//占쎈쐻占쎈윞�눧袁��쐻占쎈윞占쎈빣占쎈쐻占쎈윞占쎈뼎占쎈쐻占쎈윞�눧�뼲�삕占쎌맚�뜝�럥痢놅옙�젂占쎈염占쎌굲�뇦猿뗰옙占쏙옙�굲�뜝�럩諭쇔슖�떜媛�占쎈렱�뇦猿뗰옙占쏙옙�굲�뜝�럥嫄�占쎈쐻占쎈윥占쎈젻 60FPS占쎈쐻占쎈윞�뜎占�17ms
		//占쎈쐻占쎈짗占쎌굲�솾�뜄�늾占쎈눆�뜝�럥猷깍옙�쐻占쎈윞�떋�뇯�쐻占쎈윥�뙴占�

		//#Pattern,#Texture,#Layer占쎈쐻占쎈윞�뜎怨⑸쐻占쎈윞占쎈룺占쎈쐻占쎈윞占쎈뼎占쎈쐻占쎈윞占쎈마
		for(int i = 0; i < 3; i++) patternData.add(new ArrayList<String>());
		
		//file Read and Setting 
		FileReadSetting(usecim, chp);
		
		//#CharBMP占쎈쐻占쎈윞�꽴琉룸쐻占쎈윞占쎈쭢占쎈쐻占쎈윞�굜�꽒�쐻占쎈윪占쎄괍占쎈쐻占쎈윞�뜎援뛢turn
		if(CharBMP[CharBMPIndex] == null) return null;
		//#CharBMP2P占쎈쐻占쎈윞�꽴瑜낆삕占쎌뵂�몭�굩�삕筌륁렱�쐻占쎈윞�꽴�뇯�쐻占쎈윞占쎄턀濾곌쒀�삕占쎈쐻占쎈윞�떋占�#Texture占쎈릅占쎈샍占쎈펻�뜝�럥爾�占쎈쐻占쎈윞�꽴琉룸쐻占쎈윞�굜�엺�쐻占쎈윞占쎈쑓占쎈쐻占쎈윞�떋諛㏓쐻占쎈윞�꽴臾덈쐻占쎈윞�뜎占�#CharTex2P占쎈쐻占쎈윞�꽴瑜낆삕占쎌뵂�몭�굩�삕筌륁렱�쐻占쎈윞�뤃�맕�쐻占쎈윞占쎈쑓占쎈쐻占쎈윞�떋�뇯�쐻占쎈윞占쎈쑆2P占쎈쐻占쎈윞占쎈�わ옙�쐻占쎈윞�뜝�뜴�쐻占쎈윞占쎈뼎占쎈쐻占쎈윞�떋諛㏓쐻占쎈윞�뤃�맕�쐻占쎈윞占쎈쑓
		if(color == 2 && CharBMP[CharBMPIndex+1] != null
				&& (patternData.get(1).size() == 0 || (patternData.get(1).size() > 0 && CharBMP[CharTexIndex+1] != null))
				) setColor = 2;
		//#Texture占쎈릅占쎈샍占쎈펻�뜝�럥爾�占쎈쐻占쎈윞�꽴琉룸쐻占쎈윞�굜�엺�쐻占쎈윞占쎈쑓占쎈쐻占쎈윞�뜎怨⑸쐻占쎈윞�떋占�#CharTex占쎈쐻占쎈윞�꽴琉룸쐻占쎈윞占쎈쭢占쎈쐻占쎈윞�굜�꽒�쐻占쎈윪占쎄괍占쎈쐻占쎈윞�뜎援뛢turn
		if(setColor == 1 && patternData.get(1).size() > 0 && CharBMP[CharTexIndex] == null) return null;


		//占쎈쐻占쎈윥占쎈�묉ㅀ袁놁슦�굲�뜝�럥�뱥占쎈쐻占쎈윥占쎈탫 占쎈쐻占쎈윥鸚룔룤�렧占쎈퉾�굢節낆물�뜝占�1pixel占쎈쐻占쎈윞�꽴琉룸쐻占쎈윥占쎈�묉ㅀ袁놁슦�굲�뜝�럥竊� 占쎈쐻占쎈윞�댆�겭�쐻占쎈윥獒뺣쉘�쐻占쎈윥占쎈짍占쎈쐻占쎈윪�얘쐼�쐻占쎈윞占쎈�섓옙�쐻占쎈윞占쎈�욑옙�쐻占쎈윞占쎈�뤄옙�쐻占쎈윞占쎈뼀占쎈쐻占쎈윞�뜎洹쏅쐻占쎈윥占쎈�묉ㅀ袁놁슦�굲濾곌쒀�삕占쎈쐻占쎈윞�떋�뇯�쐻占쎈윞�굜占�
		for(int i = 0; i < SelectCGIndex; i++) {
			if(CharBMP[i] != null) {
				Pixmap pixmap = new Pixmap( CharBMP[i].getWidth(), CharBMP[i].getHeight(), Format.RGBA8888 );
				int transparentColor = CharBMP[i].getTextureData().consumePixmap().getPixel(CharBMP[i].getWidth() - 1, CharBMP[i].getHeight() - 1);
				for(int x = 0; x < CharBMP[i].getWidth(); x++) {
					for(int y = 0; y < CharBMP[i].getHeight(); y++) {
						if(transparentColor != CharBMP[i].getTextureData().consumePixmap().getPixel(x, y)) {
							pixmap.drawPixel(x, y, CharBMP[i].getTextureData().consumePixmap().getPixel(x, y));
						}
					}
				}
				CharBMP[i].dispose();
				CharBMP[i] = new Texture( pixmap );
				pixmap.dispose();
			}
		}
		CurrentType currentType = new CurrentType();
		if(currentType.getType(type) != 0) {
			if(setMotion == Integer.MIN_VALUE)
				setMotion = currentType.getType(type);
		}
		else if(type == PLAY) {
			PlayCharacter(skinOption,dsttimer,side, dstSize, patternData);
		}
		else {
			addCharaFactory CharaFactory = addCharaFactory.instance();
			SkinImage skinImage = CharaFactory.getAddChara(skin, color, CharBMP, Position, type);
			return skinImage;
		}
		return null;
	}
	
	private void PlayCharacter(SkinOption skinOption, int dsttimer, int side, SkinDestinationSize dstSize,List<List<String>>  patternData) {
		int increaseRateThreshold = 17;
		for(int i = 0; i < frame.length; i++) {
			if(frame[i] == Integer.MIN_VALUE) frame[i] = anime;
			if(frame[i] < 1) frame[i] = 100;
		}//////////////////////////////
		
		//占쎈쐻占쎈짗占쎌굲占쎈쐻占쎈윞�눧�뜴�쐻占쎈윞占쎈뼎占쎈쐻占쎈윥占쎈윛
		Pixmap pixmap = new Pixmap( 1, 1, Format.RGBA8888 );
		Texture transparent = new Texture( pixmap );
		SkinImage ImagePart = null;
		//#Pattern,#Texture,#Layer占쎈쐻占쎈윞�뜎怨⑸쐻占쎈윪占쎌쓥占쎈쐻占쎈윞�떋�뜴�쐻占쎈윥筌욊퇊�쐻占쎈윥占쎈짍鵝�紐껊룏占쎌굲占쎈릅占쎈샍占쎈펳占쎈쨨�뜝�룞�삕繹먯���삕占쎈뮉濾곌쒀�삕
		int[] setBMPIndex = {CharBMPIndex,CharTexIndex,CharBMPIndex};
		for(int patternIndex = 0; patternIndex < 3; patternIndex++) {
			Texture setBMP = CharBMP[setBMPIndex[patternIndex] + setColor-1];
			for(int patternDataIndex = 0; patternDataIndex < patternData.get(patternIndex).size(); patternDataIndex++) {
				String[] str = patternData.get(patternIndex).get(patternDataIndex).split("\t", -1);
				if (str.length > 1) {
					int motion = Integer.MIN_VALUE;
					String dst[] = new String[4];
					Arrays.fill(dst, "");
					List<String> data = PMparseStr(str);
					if(data.size() > 1) motion = PMparseInt(data.get(1));
					for (int i = 0; i < dst.length; i++) {
						if(data.size() > i + 2) dst[i] = data.get(i + 2).replaceAll("[^0-9a-zA-Z-]", "");
					}
					int timer = Integer.MIN_VALUE;
					SkinTimer skinTimer = new SkinTimer(timer, setMotion);
					skinTimer.TimerSetting(motion, skinOption, side ,dsttimer);
					if(timer != Integer.MIN_VALUE
							&& (dst[0].length() > 0 && dst[0].length() % 2 == 0)
							&& (dst[1].length() == 0 || (dst[1].length() > 0 && dst[1].length() == dst[0].length()))
							&& (dst[2].length() == 0 || (dst[2].length() > 0 && dst[2].length() == dst[0].length()))
							&& (dst[3].length() == 0 || (dst[3].length() > 0 && dst[3].length() == dst[0].length()))
							) {
						if(loop[motion] >= dst[0].length() / 2 - 1) loop[motion] = dst[0].length() / 2 - 2;
						else if(loop[motion] < -1) loop[motion] = -1;
						int cycle = frame[motion] * dst[0].length() / 2;
						int loopTime = frame[motion] * (loop[motion]+1);
						if(setMotion == Integer.MIN_VALUE && timer >= TIMER_PM_CHARA_1P_NEUTRAL && timer < TIMER_MUSIC_END) {
							setPMcharaTime(timer - TIMER_PM_CHARA_1P_NEUTRAL, cycle);
						}
						boolean hyphenFlag = false;
						for(int i = 1; i < dst.length; i++) {
							if(dst[i].indexOf("-") != -1) {
								hyphenFlag = true;
								break;
							}
						}
						//占쎈쐻占쎈윞占쎈뤍占쎈쐻占쎈윞占쎈�욑옙�쐻占쎈윞�눧袁��쐻占쎈윞占쎈뼀占쎈쐻占쎈윞�꽴琉룸쐻占쎈윞�굜�엺�쐻占쎈윞占쎈쑓占쎈쐻占쎈윪占쎄괍占쎈쐻占쎈윞�뜎洹쏅쐻占쎈윞�눧袁��쐻占쎈윞占쎈빣占쎈쐻占쎈윞占쎈뼎占쎈쐻占쎈윞�눧�뼲�삕占쎌맚�뜝�럥痢놅옙�젂占쎈염占쎌굲占쎈쨨�뜝�룞�삕繹먯���삕占쎈뮉濾곌쒀�삕 60FPS占쎈쐻占쎈윞�뜎占�17ms占쎈쐻占쎈윞�꽴琉룸쐻占쎈윪占쎈굴�슖�떜肄덌옙�굲
						int increaseRate = 1;
						if(hyphenFlag && frame[motion] >= increaseRateThreshold) {
							for(int i = 1; i <= frame[motion]; i++) {
								if(frame[motion] / i < increaseRateThreshold && frame[motion] % i == 0) {
									increaseRate = i;
									break;
								}
							}
							for(int i = 1; i < dst.length; i++) {
								int charsIndex = 0;
								char[] chars = new char[dst[i].length() * increaseRate];
								for(int j = 0; j < dst[i].length(); j+=2) {
									for(int k = 0; k < increaseRate; k++) {
										chars[charsIndex] = dst[i].charAt(j);
										charsIndex++;
										chars[charsIndex] = dst[i].charAt(j+1);
										charsIndex++;
									}
								}
								dst[i] = String.valueOf(chars);
							}
						}
						//DST�썒占썹뙼蹂��굲占쎈쐻占쎈윞�뙼�닂已드뜝�룞�삕占쎈눨�뇦猿볦삕
						double frameTime = frame[motion]/increaseRate;
						int loopFrame = loop[motion]*increaseRate;
						int dstPosition[][] = new int[dst[1].length() > 0 ? dst[1].length()/2 : dst[0].length()/2][4];
						for(int i = 0; i < dstPosition.length;i++){
							dstPosition[i][0] = 0;
							dstPosition[i][1] = 0;
							dstPosition[i][2] = size[0];
							dstPosition[i][3] = size[1];
						}
						int startPosition[] = {0,0,size[0],size[1]};
						int endPosition[] = {0,0,size[0],size[1]};
						int count = 0;
						for(int i = 0; i < dst[1].length(); i+=2) {
							if(dst[1].length() >= i+2) {
								if(dst[1].substring(i, i+2).equals("--")) {
									count = 0;
									for(int j = i; j < dst[1].length() && dst[1].substring(j, j+2).equals("--"); j+=2) count++;
									if(PMparseInt(dst[1].substring(i+count*2, i+count*2+2), 36) >= 0 && PMparseInt(dst[1].substring(i+count*2, i+count*2+2), 36) < Position.length) endPosition = Position[PMparseInt(dst[1].substring(i+count*2, i+count*2+2), 36)];
									for(int j = i; j < dst[1].length() && dst[1].substring(j, j+2).equals("--"); j+=2) {
										int[] value = new int[dstPosition[0].length];
										for(int k = 0; k < dstPosition[0].length; k++) {
											value[k] = startPosition[k] + (endPosition[k] - startPosition[k]) * ((j - i) / 2 + 1) / (count + 1);
										}
										System.arraycopy(value,0,dstPosition[j/2],0,value.length);
									}
									i += (count - 1) * 2;
								} else if(PMparseInt(dst[1].substring(i, i+2), 36) >= 0 && PMparseInt(dst[1].substring(i, i+2), 36) < Position.length) {
									startPosition = Position[PMparseInt(dst[1].substring(i, i+2), 36)];
									System.arraycopy(startPosition,0,dstPosition[i/2],0,startPosition.length);
								}
							}
						}
						//alpha占쎈쐻占쎈윞�떋�뎺ngle占쎈쐻占쎈윞�뜎怨�踰㎫뙼蹂��굲占쎈쐻占쎈윞�뙼�닂已드뜝�룞�삕占쎈눨�뇦猿볦삕
						int alphaAngle[][] = new int[dstPosition.length][2];
						LoaderSetting setAngle = new LoaderSetting(alphaAngle);
						setAngle.settingAngle(alphaAngle, count, dst, this);
						//占쎈쐻占쎈윞占쎈빟占쎈쐻占쎈윞占쎈뼎占쎈쐻占쎈윞�눧琉룸쐻占쎈윥�뜝�룞�삕占쎄쉽�뜝�럥�뿏濚밸㉡�삕占쎈쐻占쎈윞占쎈빣占쎈쐻占쎈윞占쎈뼎占쎈쐻占쎈윞�눧�뼃�쐻占쎈윞�뙼�꼪�쐻占쎈윞�떋占�
						if((loopFrame+increaseRate) != 0) {
							TextureRegion[] images = new TextureRegion[(loop[motion]+1)];
							for(int i = 0; i < (loop[motion]+1) * 2; i+=2) {
								int index = PMparseInt(dst[0].substring(i, i+2), 36);
								if(index >= 0 && index < Position.length && Position[index][2] > 0 && Position[index][3] > 0) images[i/2] = new TextureRegion(setBMP, Position[index][0], Position[index][1], Position[index][2], Position[index][3]);
								else images[i/2] = new TextureRegion(transparent, 0, 0, 1, 1);
							}
							ImagePart = new SkinImage(images, timer, loopTime);
							skin.add(ImagePart);
							for(int i = 0; i < (loopFrame+increaseRate); i++) {
								dstSize = new SkinDestinationSize(dstSize.getDstx()+dstPosition[i][0]*dstSize.getDstw()/size[0], dstSize.getDsty()+dstSize.getDsth()-(dstPosition[i][1]+dstPosition[i][3])*dstSize.getDsth()/size[1], dstPosition[i][2]*dstSize.getDstw()/size[0], dstPosition[i][3]*dstSize.getDsth()/size[1]);
								ImagePart.setDestination((int)(frameTime*i),dstSize,3,alphaAngle[i][0],255,255,255,1,0,alphaAngle[i][1],0,-1,timer,skinOption.getDstOpt1(),skinOption.getDstOpt2(),skinOption.getDstOpt3(),0);
							}
							dstSize = new SkinDestinationSize(dstSize.getDstx()+dstPosition[(loopFrame+increaseRate)-1][0]*dstSize.getDstw()/size[0], dstSize.getDsty()+dstSize.getDsth()-(dstPosition[(loopFrame+increaseRate)-1][1]+dstPosition[(loopFrame+increaseRate)-1][3])*dstSize.getDsth()/size[1], dstPosition[(loopFrame+increaseRate)-1][2]*dstSize.getDstw()/size[0], dstPosition[(loopFrame+increaseRate)-1][3]*dstSize.getDsth()/size[1]);
							ImagePart.setDestination(loopTime-1,dstSize ,3,alphaAngle[(loopFrame+increaseRate)-1][0],255,255,255,1,0,alphaAngle[(loopFrame+increaseRate)-1][1],0,-1,timer,skinOption.getDstOpt1(),skinOption.getDstOpt2(),skinOption.getDstOpt3(),skinOption.getDstOffset());
						}
						//占쎈쐻占쎈윞占쎈빟占쎈쐻占쎈윞占쎈뼎占쎈쐻占쎈윞�눧琉룸쐻占쎈윥�뜝�룞�삕占쎄쉽�뜝�럥�뿏濚밸㉡�삕占쎈쐻占쎈윞占쎈빣占쎈쐻占쎈윞占쎈뼎占쎈쐻占쎈윞�눧�뼃�쐻占쎈윞�꽴�씢�쐻占쎈윞占쎈쑆
						TextureRegion[] images = new TextureRegion[dst[0].length() / 2 - (loop[motion]+1)];
						for(int i = (loop[motion]+1)  * 2; i < dst[0].length(); i+=2) {
							int index = PMparseInt(dst[0].substring(i, i+2), 36);
							if(index >= 0 && index < Position.length && Position[index][2] > 0 && Position[index][3] > 0) images[i/2-(loop[motion]+1)] = new TextureRegion(setBMP, Position[index][0], Position[index][1], Position[index][2], Position[index][3]);
							else images[i/2-(loop[motion]+1)] = new TextureRegion(transparent, 0, 0, 1, 1);
						}
						ImagePart = new SkinImage(images, timer, cycle - loopTime);
						skin.add(ImagePart);
						for(int i = (loopFrame+increaseRate); i < dstPosition.length; i++) {
							dstSize = new SkinDestinationSize(dstSize.getDstx()+dstPosition[i][0]*dstSize.getDstw()/size[0], dstSize.getDsty()+dstSize.getDsth()-(dstPosition[i][1]+dstPosition[i][3])*dstSize.getDsth()/size[1], dstPosition[i][2]*dstSize.getDstw()/size[0], dstPosition[i][3]*dstSize.getDsth()/size[1]);
							ImagePart.setDestination((int)(frameTime*i),dstSize,3,alphaAngle[i][0],255,255,255,1,0,alphaAngle[i][1],0,loopTime,timer,skinOption.getDstOpt1(),skinOption.getDstOpt2(),skinOption.getDstOpt3(),0);
						}
						dstSize = new SkinDestinationSize(dstSize.getDstx()+dstPosition[(loopFrame+increaseRate)-1][0]*dstSize.getDstw()/size[0], dstSize.getDsty()+dstSize.getDsth()-(dstPosition[(loopFrame+increaseRate)-1][1]+dstPosition[(loopFrame+increaseRate)-1][3])*dstSize.getDsth()/size[1], dstPosition[(loopFrame+increaseRate)-1][2]*dstSize.getDstw()/size[0], dstPosition[(loopFrame+increaseRate)-1][3]*dstSize.getDsth()/size[1]);
						ImagePart.setDestination(loopTime-1,dstSize ,3,alphaAngle[(loopFrame+increaseRate)-1][0],255,255,255,1,0,alphaAngle[(loopFrame+increaseRate)-1][1],0,-1,timer,skinOption.getDstOpt1(),skinOption.getDstOpt2(),skinOption.getDstOpt3(),skinOption.getDstOffset());
					}
				}
			}
		}
	}
	
	
	private int PMparseInt(String s) {
		return Integer.parseInt(s.replaceAll("[^0-9-]", ""));
	}
	public int PMparseInt(String s, int radix) {
		if(radix == 36) {
			int result = 0;
			final char c1 = s.charAt(0);
			if (c1 >= '0' && c1 <= '9') {
				result = (c1 - '0') * 36;
			} else if (c1 >= 'a' && c1 <= 'z') {
				result = ((c1 - 'a') + 10) * 36;
			} else if (c1 >= 'A' && c1 <= 'Z') {
				result = ((c1 - 'A') + 10) * 36;
			}
			final char c2 = s.charAt(1);
			if (c2 >= '0' && c2 <= '9') {
				result += (c2 - '0');
			} else if (c2 >= 'a' && c2 <= 'z') {
				result += (c2 - 'a') + 10;
			} else if (c2 >= 'A' && c2 <= 'Z') {
				result += (c2 - 'A') + 10;
			}
			return result;
		}
		return Integer.parseInt(s.replaceAll("[^0-9a-fA-F-]", ""), radix);
	}
	
	
	
	private List<String> PMparseStr(String[] s) {
		List<String> list = new ArrayList<String>();
		for (int i = 0; i < s.length; i++) {
			if(s[i].length() > 0) {
				if(s[i].startsWith("/")) {
					break;
				} else if(s[i].indexOf("//") != -1) {
					list.add(s[i].substring(0, s[i].indexOf("//")));
					break;
				} else {
					list.add(s[i]);
				}
			}
		}
		return list;
	}
	
	boolean checkChar(String[] str) {
		if(parseMapping.getIndex(str[0]) == 0) {
			return false;
		}
		return true;
	}
	void CharSkinLoader(List<String> data, String[] str, boolean usecim, File chp  ) {
		if(data.size() > 1) CharBMP[parseMapping.getIndex(str[0])] = SkinLoader.getTexture(chp.getPath().substring(0, Math.max(chp.getPath().lastIndexOf('\\'), chp.getPath().lastIndexOf('/')) + 1) + data.get(1).replace("\\", "/"), usecim);
	}
	
	public File SkinFileLoad(File chpdir, File imagefile, int FileState) {
		File chp = null;
		chpFileFactory ChpFileFactory = chpFileFactory.instance();
		chp = ChpFileFactory.getFile(imagefile, chpdir, FileState);
		return chp;

	}
	
	public void FileReadSetting(boolean usecim, File chp) {
		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(new FileInputStream(chp), "MS932"));) {
				String line;
				PatternDataAdd patternDataAdd = new PatternDataAdd();
				while ((line = br.readLine()) != null) {
					if (line.startsWith("#") ) {
						String[] str = line.split("\t", -1);
						if (str.length > 1) {
							List<String> data = PMparseStr(str);
							if(checkChar(str)) {
								CharSkinLoader(data, str, usecim, chp);
							} 
							else if(patternDataAdd.getType(str[0])) {
								patternData.get(patternDataAdd.addType(str[0])).add(line);
							} else if(str[0].equalsIgnoreCase("#Flame") || str[0].equalsIgnoreCase("#Frame")) {
								if(data.size() > 2) {
									if(PMparseInt(data.get(1)) >= 0 && PMparseInt(data.get(1)) < frame.length) frame[PMparseInt(data.get(1))] = PMparseInt(data.get(2));
								}
							} else if(str[0].equalsIgnoreCase("#Anime")) {
								if(data.size() > 1) anime = PMparseInt(data.get(1));
							} else if(str[0].equalsIgnoreCase("#Size")) {
								if(data.size() > 2) {
									size[0] = PMparseInt(data.get(1));
									size[1] = PMparseInt(data.get(2));
								}
							} else if(str[0].length() == 3 && PMparseInt(str[0].substring(1,3), 36) >= 0 && PMparseInt(str[0].substring(1,3), 36) < Position.length) {
								//岳묒빢�삕�뜝�럥�젟占썩돦猷욑옙���뜝�럥堉뗥뜝�럥爾�
								if(data.size() > Position[0].length) {
									for(int i = 0; i < Position[0].length; i++) {
										Position[PMparseInt(str[0].substring(1,3), 36)][i] = PMparseInt(data.get(i+1));
									}
								}
							} else if(str[0].equalsIgnoreCase("#CharFaceUpperSize")) {
								//占쎈쐻占쎈윞占쎈뤍占쎈쐻占쎈윞占쎈빞占쎈쐻占쎈윞占쎈�섓옙�쐻占쎈윞占쎈��(佯몃돆鍮뽬린�뫜�삕占쎈쭎�솾占썸�ⓑ븍춪�뇦猿뗰옙占쏙옙�굲�뇦猿볦삕) 岳묒빢�삕�뜝�럥�젟占썩댙�삕&占쎈쐻占쎈윞占쎈툞占쎈쐻占쎈윞占쎈�욑옙�쐻占쎈윞占쎈륵
								if(data.size() > charFaceUpperXywh.length) {
									for(int i = 0; i < charFaceUpperXywh.length; i++) {
										charFaceUpperXywh[i] = PMparseInt(data.get(i+1));
									}
								}
							} else if(str[0].equalsIgnoreCase("#CharFaceAllSize")) {
								//占쎈쐻占쎈윞占쎈뤍占쎈쐻占쎈윞占쎈빞占쎈쐻占쎈윞占쎈�섓옙�쐻占쎈윞占쎈��(占쎈쐻占쎈윥�뜝�럡��占쎄샴占쎈쐻�뜝占�) 岳묒빢�삕�뜝�럥�젟占썩댙�삕&占쎈쐻占쎈윞占쎈툞占쎈쐻占쎈윞占쎈�욑옙�쐻占쎈윞占쎈륵
								if(data.size() > charFaceAllXywh.length) {
									for(int i = 0; i < charFaceAllXywh.length; i++) {
										charFaceAllXywh[i] = PMparseInt(data.get(i+1));
									}
								}
							} else if(str[0].equalsIgnoreCase("#Loop")) {
								//占쎈쐻占쎈윞占쎈빟占쎈쐻占쎈윞占쎈뼎占쎈쐻占쎈윞�눧琉띤벃占쎈퉾占쎈데�뜝�럥猷�
								if(data.size() > 2) {
									if(PMparseInt(data.get(1)) >= 0 && PMparseInt(data.get(1)) < loop.length) loop[PMparseInt(data.get(1))] = PMparseInt(data.get(2));
								}
							}
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
	}
	
	
}
