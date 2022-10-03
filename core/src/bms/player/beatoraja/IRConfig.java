package bms.player.beatoraja;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import bms.player.beatoraja.ir.IRConnectionManager;

public class IRConfig implements Validatable{
	private String irname = "";

	private String userid = "";

	private String cuserid = "";

	private String password = "";

	private String cpassword = "";

	private int irsend = 0;
	
	private boolean importscore = false;
	
	private boolean importrival = true;
	
	private static final String KEY = "0123456789abcdef";

	public static final int IR_SEND_ALWAYS = 0;
	public static final int IR_SEND_COMPLETE_SONG = 1;
	public static final int IR_SEND_UPDATE_SCORE = 2;

	public String getUserid() {
		if(cuserid != null && cuserid.length() > 0) {
			try {
				return CipherUtils.decrypt(cuserid, KEY, "AES");
			} catch (Exception e) {
				e.printStackTrace();
			}				
		}
		return userid;
	}

	public void setUserid(String userid) {
		this.userid = userid;
		if(userid.length() == 0) {
			cuserid = "";
		}
		validate();
	}

	public String getPassword() {
		if(cpassword != null && cpassword.length() > 0) {
			try {
				return CipherUtils.decrypt(cpassword, KEY, "AES");
			} catch (Exception e) {
				e.printStackTrace();
			}				
		}
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
		if(password.length() == 0) {
			cpassword = "";
		}
		validate();
	}

	public String getIrname() {
		return irname;
	}

	public void setIrname(String irname) {
		this.irname = irname;
	}

	public int getIrsend() {
		return irsend;
	}

	public void setIrsend(int irsend) {
		this.irsend = irsend;
	}
	
	public boolean isImportscore() {
		return importscore;
	}

	public void setImportscore(boolean importscore) {
		this.importscore = importscore;
	}

	public boolean isImportrival() {
		return importrival;
	}

	public void setImportrival(boolean importrival) {
		this.importrival = importrival;
	}

	public boolean validate() {
		if(irname == null || irname.length() == 0 || IRConnectionManager.getIRConnectionClass(irname) == null) {
			return false;
		}
		
		if(userid != null && userid.length() > 0) {
			try {
				cuserid = CipherUtils.encrypt(userid, KEY, "AES");
				userid = "";
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
		if(password != null && password.length() > 0) {
			try {
				cpassword = CipherUtils.encrypt(password, KEY, "AES");
				password = "";
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return true;
	}
}

class CipherUtils {
	
    public static String encrypt(String source, String key, String algorithm) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key.getBytes(), algorithm));
        return new String(Base64.getEncoder().encode(cipher.doFinal(source.getBytes())));
    }
    
    public static String decrypt(String encryptSource, String key, String algorithm) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key.getBytes(), algorithm));
        return new String(cipher.doFinal(Base64.getDecoder().decode(encryptSource.getBytes())));
    }
}
