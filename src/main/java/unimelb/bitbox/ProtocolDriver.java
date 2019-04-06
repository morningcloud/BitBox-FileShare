package unimelb.bitbox;

import unimelb.bitbox.util.Constants;
import unimelb.bitbox.util.Constants.Command;

public class ProtocolDriver {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Protocol p = new Protocol();
		
		System.out.println(p.createMessage(Constants.Command.INVALID_PROTOCOL, null));
		

	}

}
