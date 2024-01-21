package bms.player.beatoraja.select.bar;

/**
 * 指定のバーを子に持つバー
 *
 * @author exch
 */
public class ContainerBar extends DirectoryBar {

	private final String title;
    private final Bar[] childbar;

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
    public Bar[] getChildren() {
        return childbar;
    }
}
