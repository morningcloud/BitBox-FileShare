package unimelb.bitbox;

import unimelb.bitbox.util.Constants.Command;
import unimelb.bitbox.util.*;
import java.util.*;
import unimelb.bitbox.util.*;
public class ProtocolDriver {

	public static void main(String[] args) {
		
		ArrayList<Document> peerList = new ArrayList<Document>();
		
		int i = 0;
		while (i<3)
		{
			HostPort h = new HostPort("localhost"+i,8000+i);
			peerList.add(h.toDoc());
			i++;
		}
		
		
		
		Document m = new Document();
		
		m.append("peer", peerList);
		
		System.out.println(m.toJson());
	}
}
