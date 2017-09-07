package bms.player.beatoraja.select.bar;

/**
 * Created by exch on 2017/09/03.
 */
public class ContainerBar extends DirectoryBar {

	private String title;
    private Bar[] childbar;

    public ContainerBar(String title, Bar[] bar) {
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
