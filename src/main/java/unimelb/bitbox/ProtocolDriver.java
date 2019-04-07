package unimelb.bitbox;

import unimelb.bitbox.util.Constants.Command;
import unimelb.bitbox.util.*;

public class ProtocolDriver {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		String m = "{\"hostPort\":{\"port\":8111,\"host\":\"localhost\"},\"command\":\"HANDSHAKE_REQUESTY\"}";
		Document d = Document.parse(m);
		Document c = (Document) d.get("hostPort");
		System.out.println(c.getString("host"));
		//System.out.println(d.getString("hostPort"));
		
		System.out.println(Protocol.validate(Document.parse(m)));

		
		

	}

}
