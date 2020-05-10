package bms.player.beatoraja.launcher;

import bms.player.beatoraja.PlayModeConfig.ControllerConfig;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * ControllerConfig の ViewModel
 */
public class ControllerConfigViewModel {
    private StringProperty nameProperty = new SimpleStringProperty();
    private BooleanProperty isAnalogScratchProperty = new SimpleBooleanProperty();
    private ObjectProperty<Integer> analogScratchThresholdProperty = new SimpleIntegerProperty().asObject();
    private ObjectProperty<Integer> analogScratchModeProperty = new SimpleIntegerProperty().asObject();
    
    private ControllerConfig config;
    
    public ControllerConfigViewModel(ControllerConfig config) {
        this.config = config;
        
        this.nameProperty.set(config.getName());
        this.isAnalogScratchProperty.set(config.isAnalogScratch());
        this.analogScratchThresholdProperty.set(config.getAnalogScratchThreshold());
        this.analogScratchModeProperty.set(config.getAnalogScratchMode());
        
    }
    
    public String getName() {
        return this.nameProperty.get();
    }
    public StringProperty getNameProperty() {
        return nameProperty;
    }

    public boolean getIsAnalogScratch() {
        return isAnalogScratchProperty.get();
    }
    public void setIsAnalogScratch(boolean isAnalogScratch) {
        this.isAnalogScratchProperty.set(isAnalogScratch);
    }
    public BooleanProperty getIsAnalogScratchProperty() {
        return isAnalogScratchProperty;
    }

    public int getAnalogScratchThreshold() {
        return analogScratchThresholdProperty.get();
    }
    public void setAnalogScratchThreshold(Integer analogScratchThreshold) {
        this.analogScratchThresholdProperty.set(analogScratchThreshold);
    }
    public ObjectProperty<Integer> getAnalogScratchThresholdProperty() {
        return analogScratchThresholdProperty;
    }
    
    public int getAnalogScratchMode() {
        return this.analogScratchModeProperty.get();
    }
    public void setAnalogScratchMode(int analogScratchMode) {
        this.analogScratchModeProperty.set(analogScratchMode);
    }
    public ObjectProperty<Integer> getAnalogScratchModeProperty() {
        return analogScratchModeProperty;
    }
    
    public ControllerConfig getConfig() {
        return this.config;
    }
}