package bms.player.beatoraja.play;

import bms.player.beatoraja.BMSPlayerMode;
import bms.player.beatoraja.SkinConfig;
import bms.player.beatoraja.skin.SkinCustomization;
import bms.player.beatoraja.skin.SkinObject.SkinOffset;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

import static bms.player.beatoraja.skin.SkinProperty.OPTION_RANDOM_VALUE;

/** Render-thread-only editor for the currently loaded play skin. */
public final class SkinAdjustmentOverlay implements Disposable {
    private final Stage stage = new Stage(new ScreenViewport());
    private final BitmapFont font;
    private FreeTypeFontGenerator fontGenerator;
    private final Texture pixel;
    private final Session session;
    private ScrollPane choicesPane;
    private Cell<ScrollPane> choicesCell;
    private float preferredChoicesHeight;
    private Boolean saveRequested;
    private boolean previewRequested;
    private Label previewStatus;

    public SkinAdjustmentOverlay(PlaySkin skin, IntFunction<SkinOffset> liveOffsets, String fontPath) {
        this(skin, liveOffsets, fontPath, false);
    }

    public SkinAdjustmentOverlay(PlaySkin skin, IntFunction<SkinOffset> liveOffsets, String fontPath, boolean canPreview) {
        session = new Session(skin, liveOffsets);
        font = createFont(fontPath);
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        try {
            pixmap.setColor(Color.WHITE);
            pixmap.fill();
            pixel = new Texture(pixmap);
        } finally {
            pixmap.dispose();
        }
        var drawable = new TextureRegionDrawable(new com.badlogic.gdx.graphics.g2d.TextureRegion(pixel));
        var labelStyle = new Label.LabelStyle(font, Color.WHITE);
        var buttonStyle = new TextButton.TextButtonStyle(drawable.tint(Color.valueOf("37454f")),
                drawable.tint(Color.valueOf("17252e")), null, font);
        buttonStyle.over = drawable.tint(Color.valueOf("50636f"));
        var root = new Table();
        root.setFillParent(true);
        root.top().right().pad(12);
        var panel = new Table();
        panel.setBackground(drawable.tint(Color.valueOf("182126f5")));
        panel.pad(12);
        panel.defaults().space(6);
        addSettings(skin, panel, labelStyle, buttonStyle, drawable);
        if (canPreview) {
            panel.add(button("Apply & Preview", buttonStyle, () -> previewRequested = true))
                    .colspan(4).size(180, 28).row();
            previewStatus = new Label("", labelStyle);
            previewStatus.setColor(Color.ORANGE);
            panel.add(previewStatus).colspan(4).height(20).row();
            sizeChoices(Gdx.graphics.getHeight());
        }
        panel.add(button("Cancel", buttonStyle, () -> saveRequested = false)).colspan(2).size(88, 28);
        panel.add(button("Save", buttonStyle, () -> saveRequested = true)).colspan(2).size(88, 28);
        root.add(panel);
        stage.addActor(root);
        if (choicesPane != null) stage.setScrollFocus(choicesPane);
    }

    private BitmapFont createFont(String fontPath) {
        if (fontPath != null && !fontPath.isBlank()) {
            try {
                fontGenerator = new FreeTypeFontGenerator(Gdx.files.internal(fontPath));
                var parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
                parameter.size = 15;
                parameter.incremental = true;
                return fontGenerator.generateFont(parameter);
            } catch (RuntimeException e) {
                if (fontGenerator != null) fontGenerator.dispose();
                fontGenerator = null;
                java.util.logging.Logger.getGlobal().warning("Skin adjustment font unavailable: " + fontPath);
            }
        }
        return new BitmapFont();
    }

    private void addSettings(PlaySkin skin, Table panel, Label.LabelStyle labelStyle,
                                  TextButton.TextButtonStyle buttonStyle, TextureRegionDrawable drawable) {
        var rows = new Table();
        rows.top().left().defaults().space(4);
        if (session.properties.hasLegacyNoteAdjustment()) {
            var warning = new Label("Legacy note adjustment was not applied.\nOriginal values are kept in the config.", labelStyle);
            warning.setColor(Color.ORANGE);
            rows.add(warning).colspan(4).left().row();
        }
        String[] captions = {"X", "Y", "Width", "Height"};
        for (var edit : session.offsets) {
            if (!edit.allowed(0) && !edit.allowed(1) && !edit.allowed(2) && !edit.allowed(3)) continue;
            rows.add(new Label(edit.definition.name, labelStyle)).colspan(4).left().row();
            for (int axis = 0; axis < captions.length; axis++) {
                if (!edit.allowed(axis)) continue;
                final int index = axis;
                var value = new Label(Integer.toString(edit.values[index]), labelStyle);
                IntConsumer change = delta -> {
                    edit.change(index, delta);
                    value.setText(Integer.toString(edit.values[index]));
                };
                rows.add(new Label(captions[axis], labelStyle)).width(88).left();
                rows.add(button("-", buttonStyle, () -> change.accept(-1))).size(24, 24);
                rows.add(value).width(104);
                rows.add(button("+", buttonStyle, () -> change.accept(1))).size(24, 24).row();
            }
        }
        if (skin.header.getCustomOptions().length != 0 || skin.header.getCustomFiles().length != 0)
            rows.add(new Label("Skin settings (next launch)", labelStyle)).colspan(4).left().row();
        for (var option : skin.header.getCustomOptions()) {
            int length = Math.min(option.option.length, option.contents.length);
            if (length == 0) continue;
            var names = new ArrayList<String>(length + 1);
            int current = session.option(option.name, option.getDefaultOption()), selected = -1;
            for (int i = 0; i < length; i++) {
                names.add(option.contents[i]);
                if (current == option.option[i]) selected = i;
            }
            names.add("Random");
            if (current == OPTION_RANDOM_VALUE) selected = length;
            addChoice(rows, option.name, names, selected, labelStyle, buttonStyle,
                    index -> session.selectOption(option.name, index == length ? OPTION_RANDOM_VALUE : option.option[index]));
        }
        for (var file : skin.header.getCustomFiles()) {
            var choices = new ArrayList<>(SkinCustomization.fileChoices(file));
            if (choices.isEmpty()) continue;
            String current = session.file(file.name, SkinCustomization.defaultFile(file, choices));
            choices.add(SkinCustomization.RANDOM_FILE);
            addChoice(rows, file.name, choices, choices.indexOf(current), labelStyle, buttonStyle,
                    index -> session.selectFile(file.name, choices.get(index)));
        }
        if (!rows.hasChildren()) return;
        var track = drawable.tint(Color.valueOf("17252e"));
        var thumb = drawable.tint(Color.valueOf("50636f"));
        track.setMinWidth(6);
        thumb.setMinWidth(6);
        thumb.setMinHeight(12);
        choicesPane = new ScrollPane(rows, new ScrollPane.ScrollPaneStyle(null, null, null, track, thumb));
        choicesPane.setScrollingDisabled(true, false);
        choicesPane.setFadeScrollBars(false);
        preferredChoicesHeight = Math.min(380, rows.getPrefHeight() + 2);
        choicesCell = panel.add(choicesPane).colspan(4).width(270);
        panel.row();
        sizeChoices(Gdx.graphics.getHeight());
    }

    private static void addChoice(Table rows, String name, List<String> choices, int index,
                                  Label.LabelStyle style, TextButton.TextButtonStyle buttonStyle, IntConsumer onChange) {
        var title = new Label(name, style);
        title.setEllipsis(true);
        var value = new Label(index < 0 ? "-" : choices.get(index), style);
        value.setEllipsis(true);
        int[] selected = {index};
        IntConsumer step = direction -> {
            int next = selected[0] < 0 ? (direction > 0 ? 0 : choices.size() - 1)
                    : Math.floorMod(selected[0] + direction, choices.size());
            selected[0] = next;
            value.setText(choices.get(next));
            onChange.accept(next);
        };
        rows.add(title).width(88).left();
        rows.add(button("<", buttonStyle, () -> step.accept(-1))).size(24, 24);
        rows.add(value).width(104);
        rows.add(button(">", buttonStyle, () -> step.accept(1))).size(24, 24).row();
    }

    private static TextButton button(String text, TextButton.TextButtonStyle style, Runnable action) {
        var button = new TextButton(text, style);
        button.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                action.run();
            }
        });
        return button;
    }

    public Stage stage() { return stage; }
    public Boolean saveRequested() { return saveRequested; }

    public boolean takePreviewRequest() {
        boolean requested = previewRequested;
        previewRequested = false;
        return requested;
    }

    public SkinConfig.Property previewProperties() { return session.previewProperties(); }

    public void previewFailed() {
        if (previewStatus != null) previewStatus.setText("Preview failed. Previous skin kept.");
    }

    public void draw() {
        int width = Gdx.graphics.getWidth(), height = Gdx.graphics.getHeight();
        var viewport = stage.getViewport();
        if (width <= 0 || height <= 0) return;
        if (viewport.getScreenWidth() != width || viewport.getScreenHeight() != height) {
            viewport.update(width, height, true);
            sizeChoices(height);
        } else viewport.apply();
        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();
    }

    private void sizeChoices(int height) {
        if (choicesCell == null) return;
        choicesCell.height(Math.min(preferredChoicesHeight, Math.max(30, height - (previewStatus == null ? 80 : 140))));
        ((Table) choicesPane.getParent()).invalidateHierarchy();
    }

    public void finish(boolean save) { session.finish(save); }

    @Override public void dispose() {
        stage.dispose();
        font.dispose();
        if (fontGenerator != null) fontGenerator.dispose();
        pixel.dispose();
    }

    static boolean allows(BMSPlayerMode.Mode mode) {
        return mode == BMSPlayerMode.Mode.PRACTICE || mode == BMSPlayerMode.Mode.AUTOPLAY;
    }

    static final class Session {
        private final PlaySkin skin;
        private final SkinConfig.Property properties;
        final List<SkinOffsetSettings.Edit> offsets;
        private final LinkedHashMap<String, SkinConfig.Option> options = new LinkedHashMap<>();
        private final LinkedHashMap<String, SkinConfig.FilePath> files = new LinkedHashMap<>();

        Session(PlaySkin skin, IntFunction<SkinOffset> liveOffsets) {
            this.skin = skin;
            properties = skin.getEditableNoteProperties();
            offsets = SkinOffsetSettings.edits(skin, liveOffsets);
            var pending = properties.getPendingSelection();
            if (pending != null) {
                for (var option : pending.options()) options.put(option.name, option);
                for (var file : pending.files()) files.put(file.name, file);
            }
        }

        int option(String name, int fallback) {
            var pending = options.get(name);
            if (pending != null) return pending.value;
            for (var option : properties.getOption()) if (name.equals(option.name)) return option.value;
            return fallback;
        }

        String file(String name, String fallback) {
            var pending = files.get(name);
            if (pending != null) return pending.path;
            for (var file : properties.getFile()) if (name.equals(file.name)) return file.path;
            return fallback;
        }

        void selectOption(String name, int id) {
            var choice = new SkinConfig.Option();
            choice.name = name;
            choice.value = id;
            options.put(name, choice);
        }

        void selectFile(String name, String filename) {
            var choice = new SkinConfig.FilePath();
            choice.name = name;
            choice.path = filename;
            files.put(name, choice);
        }

        void finish(boolean save) {
            for (var offset : offsets) offset.finish(skin, properties, save);
            if (save) {
                properties.setPendingSelection(selection());
            }
        }

        SkinConfig.Property previewProperties() {
            var draft = properties.candidateFor(selection());
            for (var offset : offsets) offset.copyTo(draft);
            return draft;
        }

        private SkinConfig.Selection selection() {
            return new SkinConfig.Selection(options.values().toArray(SkinConfig.Option[]::new),
                    files.values().toArray(SkinConfig.FilePath[]::new));
        }
    }
}
