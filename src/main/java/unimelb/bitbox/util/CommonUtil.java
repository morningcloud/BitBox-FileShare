package unimelb.bitbox.util;

import unimelb.bitbox.Message;
import unimelb.bitbox.util.Constants.Command;

public class CommonUtil {
	//contain any common helper methods that can be resused
	public boolean isNull(Object obj) {
		return (obj == null);
	}
	
	public boolean isNull(String obj) {
		if (obj == null)
			return true;
		return obj.trim().isEmpty();
	}
	
	public boolean isValidIP(String ip) {
		return true;
	}
}
