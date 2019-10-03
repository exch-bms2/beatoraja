package bms.player.beatoraja.launcher;

import java.awt.Desktop;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.logging.Logger;

import bms.player.beatoraja.PlayerConfig;
import bms.player.beatoraja.PlayerConfig.IRConfig;
import bms.player.beatoraja.ir.IRConnectionManager;
import bms.player.beatoraja.launcher.PlayConfigurationView.OptionListCell;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class IRConfigurationView implements Initializable {
	
	@FXML
	private Button primarybutton;
	@FXML
	private ComboBox<String> irname;
	@FXML
	private Hyperlink irhome;
	@FXML
	private TextField iruserid;
	@FXML
	private PasswordField irpassword;
	@FXML
	private ComboBox<Integer> irsend;
	@FXML
	private CheckBox importrival;
	@FXML
	private CheckBox importscore;
	
	private Map<String, IRConfig> irmap = new HashMap<String, IRConfig>();
	
	private String primary;
	
	private IRConfig currentir;
	
	private PlayerConfig player;

	private void initComboBox(ComboBox<Integer> combo, final String[] values) {
		combo.setCellFactory((param) -> new OptionListCell(values));
		combo.setButtonCell(new OptionListCell(values));
		for (int i = 0; i < values.length; i++) {
			combo.getItems().add(i);
		}
	}
	public void initialize(URL arg0, ResourceBundle arg1) {
		initComboBox(irsend, new String[] { arg1.getString("IR_SEND_ALWAYS"), arg1.getString("IR_SEND_FINISH"), arg1.getString("IR_SEND_UPDATE")});
		irname.getItems().setAll(IRConnectionManager.getAllAvailableIRConnectionName());
	}
	
    public void update(PlayerConfig player) {
    	this.player = player;
    	
    	for(IRConfig ir : player.getIrconfig()) {
    		irmap.put(ir.getIrname(), ir);
    	}
    	    	
		primary = player.getIrconfig().length > 0 ? player.getIrconfig()[0].getIrname() : null;
		if(!irname.getItems().contains(primary)) {
			if (irname.getItems().size() == 0) {
				primary = null;
			} else {
				primary = irname.getItems().get(0);
			}
		}
		irname.setValue(primary);
		updateIRConnection();

    }

    public void commit() {
		updateIRConnection();
		
		List<IRConfig> irlist = new ArrayList<IRConfig>();
		
		for(String s : irname.getItems()) {
			IRConfig ir = irmap.get(s);
			if(ir != null && ir.getUserid().length() > 0) {
				if(s.equals(primary) ) {
					irlist.add(0, ir);
				} else {
					irlist.add(ir);
				}
			}
		}
		
		player.setIrconfig(irlist.toArray(new IRConfig[irlist.size()]));
    }
    
	@FXML
	public void setPrimary() {
		primary = irname.getValue();
		updateIRConnection();
	}

	@FXML
	public void updateIRConnection() {
		if(currentir != null) {
			currentir.setUserid(iruserid.getText());
			currentir.setPassword(irpassword.getText());
			currentir.setIrsend(irsend.getValue());
			currentir.setImportscore(importscore.isSelected());
			currentir.setImportrival(importrival.isSelected());
		}
		
    	String homeurl = IRConnectionManager.getHomeURL(irname.getValue());
		irhome.setText(homeurl);
		irhome.setOnAction((event) -> {
            Desktop desktop = Desktop.getDesktop();
            URI uri;
            try {
                uri = new URI(homeurl);
                desktop.browse(uri);
            } catch (Exception e) {
                Logger.getGlobal().warning("最新版URLアクセス時例外:" + e.getMessage());
            }
        });
		
		if(!irmap.containsKey(irname.getValue())) {
			IRConfig ir = new IRConfig();
			ir.setIrname(irname.getValue());
			irmap.put(irname.getValue(), ir);
		}
		currentir = irmap.get(irname.getValue());
		iruserid.setText(currentir.getUserid());
		irpassword.setText(currentir.getPassword());
		irsend.setValue(currentir.getIrsend());
		importscore.setSelected(currentir.isImportscore());
		importrival.setSelected(currentir.isImportrival());

		primarybutton.setVisible(!(primary != null && irname.getValue().equals(primary)));
	}
}
