package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;

/**
 * Created by exch on 2016/07/16.
 */
public interface TextResourceAccessor {

    public abstract String getValue(MainState state);


    public static TextResourceAccessor TITLE = new TextResourceAccessor() {
        @Override
        public String getValue(MainState state) {
            return state.getTitle();
        }
    };

    public static TextResourceAccessor ARTIST = new TextResourceAccessor() {
        @Override
        public String getValue(MainState state) {
            return state.getArtist();
        }
    };

    public static TextResourceAccessor GENRE = new TextResourceAccessor() {
        @Override
        public String getValue(MainState state) {
            return state.getGenre();
        }
    };

}
