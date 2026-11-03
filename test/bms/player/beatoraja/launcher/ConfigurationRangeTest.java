package bms.player.beatoraja.launcher;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Element;

import bms.model.Mode;
import bms.player.beatoraja.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.SpinnerValueFactory;

/** Tests FXML constant resolution and validation boundaries without starting a GUI. */
public final class ConfigurationRangeTest {
    public static void main(String[] args) throws Exception {
        check("Audio", "audiobuffer", AudioConfig.class, "deviceBufferSize", "DEVICE_BUFFER_SIZE");
        check("Audio", "audiosim", AudioConfig.class, "deviceSimultaneousSources", "DEVICE_SIMULTANEOUS_SOURCES");
        check("Video", "maxFps", Config.class, "maxFramePerSecond", "MAX_FPS");
        check("Video", "missLayerTime", PlayerConfig.class, "misslayerDuration", "MISS_LAYER_DURATION");
        check("Play", "hispeed", PlayConfig.class, "hispeed", "HISPEED");
        check("Play", "gvalue", PlayConfig.class, "duration", "DURATION");
        check("Play", "hispeedmargin", PlayConfig.class, "hispeedmargin", "HISPEEDMARGIN");
        check("Play", "hranthresholdbpm", PlayerConfig.class, "hranThresholdBPM", "HRAN_THRESHOLD_BPM");
        check("Play", "extranotedepth", PlayerConfig.class, "extranoteDepth", "EXTRA_NOTE_DEPTH");
        check("Play", "exitpressduration", PlayerConfig.class, "exitPressDuration", "EXIT_PRESS_DURATION");
        check("Input", "inputduration", PlayModeConfig.class, "keyboard.duration", "INPUT_DURATION");
        check("Input", "mouseScratchTimeThreshold", PlayModeConfig.class,
                "keyboard.mouseScratchConfig.mouseScratchTimeThreshold", "MOUSE_SCRATCH_TIME_THRESHOLD");
        check("Input", "mouseScratchDistance", PlayModeConfig.class,
                "keyboard.mouseScratchConfig.mouseScratchDistance", "MOUSE_SCRATCH_DISTANCE");
        check("MusicSelect", "scrolldurationlow", Config.class, "scrolldurationlow", "SCROLL_DURATION_LOW");
        check("MusicSelect", "analogTicksPerScroll", Config.class, "analogTicksPerScroll", "ANALOG_TICKS_PER_SCROLL");
        check("Stream", "maxRequestCount", PlayerConfig.class, "maxRequestCount", "MAX_REQUEST_COUNT");
        System.out.println("ConfigurationRangeTest passed (16 ranges)");
    }

    private static void check(String view, String id, Class<?> owner, String property, String constant) throws Exception {
        Path path = Path.of("src/bms/player/beatoraja/launcher/" + view + "ConfigurationView.fxml");
        var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(path.toFile());
        Element factory = null;
        var elements = document.getElementsByTagName("NumericSpinner");
        for (int i = 0; i < elements.getLength(); i++) {
            Element spinner = (Element) elements.item(i);
            if (id.equals(spinner.getAttribute("fx:id"))) {
                var children = spinner.getElementsByTagName("valueFactory").item(0).getChildNodes();
                for (int j = 0; j < children.getLength(); j++) {
                    if (children.item(j) instanceof Element element) factory = element;
                }
            }
        }
        if (factory == null) throw new AssertionError("Missing spinner: " + id);
        double min = ((Number) owner.getField(constant + "_MIN").get(null)).doubleValue();
        double max = ((Number) owner.getField(constant + "_MAX").get(null)).doubleValue();
        for (String bound : new String[] {"min", "max"}) {
            Element value = (Element) factory.getElementsByTagName(owner.getSimpleName())
                    .item(bound.equals("min") ? 0 : 1);
            if (value == null || !value.getAttribute("fx:constant").equals(constant + "_" + bound.toUpperCase(java.util.Locale.ROOT))) {
                throw new AssertionError("Missing shared bound: " + id + "." + bound);
            }
        }
        factory.setAttribute("xmlns:fx", "http://javafx.com/fxml/1");
        StringWriter xml = new StringWriter();
        var transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.transform(new DOMSource(factory), new StreamResult(xml));
        String fxml = "<?import javafx.scene.control." + factory.getTagName() + "?>\n<?import " + owner.getName() + "?>\n" + xml;
        SpinnerValueFactory<?> loaded = new FXMLLoader().load(new ByteArrayInputStream(fxml.getBytes(StandardCharsets.UTF_8)));
        equal(Double.parseDouble(factory.getAttribute("initialValue")), ((Number) loaded.getValue()).doubleValue());
        if (loaded instanceof SpinnerValueFactory.IntegerSpinnerValueFactory integers) {
            equal(min, integers.getMin());
            equal(max, integers.getMax());
            integers.setValue((int) max);
            equal(max, integers.getValue());
            integers.setValue((int) min);
            equal(min, integers.getValue());
        } else if (loaded instanceof SpinnerValueFactory.DoubleSpinnerValueFactory doubles) {
            equal(min, doubles.getMin());
            equal(max, doubles.getMax());
            doubles.setValue(max);
            equal(max, doubles.getValue());
            doubles.setValue(min);
            equal(min, doubles.getValue());
        } else throw new AssertionError("Unexpected value factory: " + id);

        for (double input : new double[] {min - 1, min, max, max + 1}) {
            Object config = owner.getConstructor().newInstance();
            Object target = config;
            String[] fields = property.split("\\.");
            for (int i = 0; i < fields.length - 1; i++) {
                Field field = target.getClass().getDeclaredField(fields[i]);
                field.setAccessible(true);
                target = field.get(target);
            }
            Field field = target.getClass().getDeclaredField(fields[fields.length - 1]);
            field.setAccessible(true);
            if (field.getType() == float.class) field.setFloat(target, (float) input);
            else field.setInt(target, (int) input);
            if (config instanceof PlayModeConfig mode) mode.validate(Mode.BEAT_7K);
            else owner.getMethod("validate").invoke(config);
            equal(Math.max(min, Math.min(max, input)), ((Number) field.get(target)).doubleValue());
        }
    }

    private static void equal(double expected, double actual) {
        if (Math.abs(expected - actual) > 0.000001) {
            throw new AssertionError("Expected " + expected + " but was " + actual);
        }
    }
}
