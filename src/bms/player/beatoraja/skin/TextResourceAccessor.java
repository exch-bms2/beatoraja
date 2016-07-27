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

    public static TextResourceAccessor SUBTITLE = new TextResourceAccessor() {
        @Override
        public String getValue(MainState state) {
            return state.getSubtitle();
        }
    };

    public static TextResourceAccessor FULLTITLE = new TextResourceAccessor() {
        @Override
        public String getValue(MainState state) {
            return state.getTitle() + " " + state.getSubtitle();
        }
    };

    public static TextResourceAccessor ARTIST = new TextResourceAccessor() {
        @Override
        public String getValue(MainState state) {
            return state.getArtist();
        }
    };

    public static TextResourceAccessor SUBARTIST = new TextResourceAccessor() {
        @Override
        public String getValue(MainState state) {
            return state.getSubartist();
        }
    };

    public static TextResourceAccessor FULLARTIST = new TextResourceAccessor() {
        @Override
        public String getValue(MainState state) {
            return state.getArtist() + " " + state.getSubartist();
        }
    };

    public static TextResourceAccessor GENRE = new TextResourceAccessor() {
        @Override
        public String getValue(MainState state) {
            return state.getGenre();
        }
    };

    public static TextResourceAccessor DIRECTORY = new TextResourceAccessor() {
        @Override
        public String getValue(MainState state) {
            return state.getDirectory();
        }
    };

}
