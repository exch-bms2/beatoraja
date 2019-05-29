package bms.player.beatoraja.skin.json;

import bms.player.beatoraja.skin.SkinText;
import bms.player.beatoraja.skin.property.*;

public class JsonSkin {
	public static class Skin {

		public int type = -1;
		public String name;
		public int w = 1280;
		public int h = 720;
		public int fadeout;
		public int input;
		public int scene;
		public int close;
		public int loadend;
		public int playstart;
		public int judgetimer = 1;
		public int finishmargin = 0;

		public Property[] property = new Property[0];
		public Filepath[] filepath = new Filepath[0];
		public Offset[] offset = new Offset[0];
		public Source[] source = new Source[0];
		public Font[] font = new Font[0];
		public Image[] image = new Image[0];
		public ImageSet[] imageset = new ImageSet[0];
		public Value[] value = new Value[0];
		public Text[] text = new Text[0];
		public Slider[] slider = new Slider[0];
		public Graph[] graph = new Graph[0];
		public GaugeGraph[] gaugegraph = new GaugeGraph[0];
		public JudgeGraph[] judgegraph = new JudgeGraph[0];
		public BPMGraph[] bpmgraph = new BPMGraph[0];
		public TimingVisualizer[] timingvisualizer = new TimingVisualizer[0];
		public TimingDistributionGraph[] timingdistributiongraph = new TimingDistributionGraph[0];
		public NoteSet note;
		public Gauge gauge;
		public HiddenCover[] hiddenCover = new HiddenCover[0];
		public LiftCover[] liftCover = new LiftCover[0];
		public BGA bga;
		public Judge[] judge = new Judge[0];
		public SongList songlist;
		public PMchara[] pmchara = new PMchara[0];
		public SkinConfigurationProperty skinSelect;
		public CustomEvent[] customEvents = new CustomEvent[0];
		public CustomTimer[] customTimers = new CustomTimer[0];

		public Destination[] destination;
	}

	public static class Property {
		public String name;
		public PropertyItem[] item = new PropertyItem[0];
		public String def;
	}

	public static class PropertyItem {
		public String name;
		public int op;
	}

	public static class Filepath {
		public String name;
		public String path;
		public String def;
	}

	public static class Offset {
		public String name;
		public int id;
		public boolean x;
		public boolean y;
		public boolean w;
		public boolean h;
		public boolean r;
		public boolean a;
	}

	public static class Source {
		public String id;
		public String path;
	}

	public static class Font {
		public String id;
		public String path;
		public int type;
	}

	public static class Image {
		public String id;
		public String src;
		public int x;
		public int y;
		public int w;
		public int h;
		public int divx = 1;
		public int divy = 1;
		public TimerProperty timer;
		public int cycle;
		public int len;
		public int ref;
		public Event act;
		public int click = 0;
	}

	public static class ImageSet {
		public String id;
		public int ref;
		public IntegerProperty value;
		public String[] images = new String[0];
		public Event act;
		public int click = 0;
	}

	public static class Value {
		public String id;
		public String src;
		public int x;
		public int y;
		public int w;
		public int h;
		public int divx = 1;
		public int divy = 1;
		public TimerProperty timer;
		public int cycle;
		public int align;
		public int digit;
		public int padding;
		public int zeropadding;
		public int space;
		public int ref;
		public IntegerProperty value;
		public Value[] offset;
	}

	public static class Text {
		public String id;
		public String font;
		public int size;
		public int align;
		public int ref;
		public StringProperty value;
		public boolean wrapping = false;
		public int overflow = SkinText.OVERFLOW_OVERFLOW;
		public String outlineColor = "ffffff00";
		public float outlineWidth = 0;
		public String shadowColor = "ffffff00";
		public float shadowOffsetX = 0;
		public float shadowOffsetY = 0;
		public float shadowSmoothness = 0;
	}

	public static class Slider {
		public String id;
		public String src;
		public int x;
		public int y;
		public int w;
		public int h;
		public int divx = 1;
		public int divy = 1;
		public TimerProperty timer;
		public int cycle;
		public int angle;
		public int range;
		public int type;
		public FloatProperty value;
		public FloatWriter event;
		public boolean isRefNum = false;
		public int min = 0;
		public int max = 0;
	}

	public static class Graph {
		public String id;
		public String src;
		public int x;
		public int y;
		public int w;
		public int h;
		public int divx = 1;
		public int divy = 1;
		public TimerProperty timer;
		public int cycle;
		public int angle = 1;
		public int type;
		public FloatProperty value;
		public boolean isRefNum = false;
		public int min = 0;
		public int max = 0;
	}

	public static class GaugeGraph {
		public String id;
	}

	public static class JudgeGraph {
		public String id;
		public int type;
		public int backTexOff = 0;
		public int delay = 500;
		public int orderReverse = 0;
		public int noGap = 0;
	}

	public static class BPMGraph {
		public String id;
		public int delay = 0;
		public int lineWidth = 2;
		public String mainBPMColor = "00ff00";
		public String minBPMColor = "0000ff";
		public String maxBPMColor = "ff0000";
		public String otherBPMColor = "ffff00";
		public String stopLineColor = "ff00ff";
		public String transitionLineColor = "7f7f7f";
	}

	public static class TimingVisualizer {
		public String id;
		public int width = 301;
		public int judgeWidthMillis = 150;
		public int lineWidth = 1;
		public String lineColor = "00FF00FF";
		public String centerColor = "FFFFFFFF";
		public String PGColor = "000088FF";
		public String GRColor = "008800FF";
		public String GDColor = "888800FF";
		public String BDColor = "880000FF";
		public String PRColor = "000000FF";
		public int transparent = 0;
		public int drawDecay = 1;
	}

	public static class TimingDistributionGraph {
		public String id;
		public int width = 301;
		public int lineWidth = 1;
		public String graphColor = "00FF00FF";
		public String averageColor = "FFFFFFFF";
		public String devColor = "FFFFFFFF";
		public String PGColor = "000088FF";
		public String GRColor = "008800FF";
		public String GDColor = "888800FF";
		public String BDColor = "880000FF";
		public String PRColor = "000000FF";
		public int drawAverage = 1;
		public int drawDev = 1;
	}

	public static class NoteSet {
		public String id;
		public String[] note = new String[0];
		public String[] lnstart = new String[0];
		public String[] lnend = new String[0];
		public String[] lnbody = new String[0];
		public String[] lnactive = new String[0];
		public String[] hcnstart = new String[0];
		public String[] hcnend = new String[0];
		public String[] hcnbody = new String[0];
		public String[] hcnactive = new String[0];
		public String[] hcndamage = new String[0];
		public String[] hcnreactive = new String[0];
		public String[] mine = new String[0];
		public String[] hidden = new String[0];
		public String[] processed = new String[0];
		public Animation[] dst = new Animation[0];
		public int dst2 = Integer.MIN_VALUE;
		public int[] expansionrate = {100,100};
		public float[] size = new float[0];
		public Destination[] group = new Destination[0];
		public Destination[] bpm = new Destination[0];
		public Destination[] stop = new Destination[0];
		public Destination[] time = new Destination[0];
	}

	public static class Gauge {
		public String id;
		public String[] nodes;
		public int parts = 50;
		public int type;
		public int range = 3;
		public int cycle = 33;
		public int starttime = 0;
		public int endtime = 500;
	}

	public static class HiddenCover {
		public String id;
		public String src;
		public int x;
		public int y;
		public int w;
		public int h;
		public int divx = 1;
		public int divy = 1;
		public TimerProperty timer;
		public int cycle;
		public int disapearLine = -1;
		public boolean isDisapearLineLinkLift = true;
	}

	public static class LiftCover {
		public String id;
		public String src;
		public int x;
		public int y;
		public int w;
		public int h;
		public int divx = 1;
		public int divy = 1;
		public TimerProperty timer;
		public int cycle;
		public int disapearLine = -1;
		public boolean isDisapearLineLinkLift = false;
	}

	public static class BGA {
		public String id;
	}

	public static class Judge {
		public String id;
		public int index;
		public Destination[] images = new Destination[0];
		public Destination[] numbers = new Destination[0];
		public boolean shift;
	}

	public static class SongList {
		public String id;
		public int center;
		public int[] clickable = new int[0];
		public Destination[] listoff = new Destination[0];
		public Destination[] liston = new Destination[0];
		public Destination[] text = new Destination[0];
		public Destination[] level = new Destination[0];
		public Destination[] lamp = new Destination[0];
		public Destination[] playerlamp = new Destination[0];
		public Destination[] rivallamp = new Destination[0];
		public Destination[] trophy = new Destination[0];
		public Destination[] label = new Destination[0];
		public Destination graph;
	}

	public static class Destination {
		public String id;
		public int blend;
		public int filter;
		public TimerProperty timer;
		public int loop;
		public int center;
		public int offset;
		public int[] offsets = new int[0];
		public int stretch = -1;
		public int[] op = new int[0];
		public BooleanProperty draw;
		public Animation[] dst = new Animation[0];
		public Rect mouseRect;
	}

	public static class Rect {
		public int x;
		public int y;
		public int w;
		public int h;
	}

	public static class Animation {
		public int time = Integer.MIN_VALUE;

		public int x = Integer.MIN_VALUE;
		public int y = Integer.MIN_VALUE;
		public int w = Integer.MIN_VALUE;
		public int h = Integer.MIN_VALUE;

		public int acc = Integer.MIN_VALUE;

		public int a = Integer.MIN_VALUE;
		public int r = Integer.MIN_VALUE;
		public int g = Integer.MIN_VALUE;
		public int b = Integer.MIN_VALUE;

		public int angle = Integer.MIN_VALUE;

	}

	public static class PMchara {
		public String id;
		public String src;
		public int color = 1;
		public int type = Integer.MIN_VALUE;
		public int side = 1;
	}

	public static class SkinConfigurationProperty {
		public String[] customBMS;
		public int defaultCategory = 0;
		public int customPropertyCount = -1;
		public int customOffsetStyle = 0;
	}

	public static class CustomEvent {
		public int id;
		public Event action;
		public BooleanProperty condition;
		public int minInterval;
	}

	public static class CustomTimer {
		public int id;
		public TimerProperty timer;
	}
}
