package bms.player.beatoraja.select.bar;

/**
 * 指定のバーを子に持つバー
 *
 * @author exch
 */
public class ContainerBar extends DirectoryBar {

	private String title;
    private Bar[] childbar;

    public ContainerBar(String title, Bar[] bar) {
    	super(null);
        this.title = title;
        childbar = bar;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getArtist() {
        return null;
    }

    @Override
    public Bar[] getChildren() {
        return childbar;
    }
}
