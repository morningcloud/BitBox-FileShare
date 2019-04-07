package unimelb.bitbox;

import unimelb.bitbox.util.*;
import java.util.ArrayList;


public class DocumentDriver
{

	public static void main(String[] args)
	{
		Document d = new Document();
		
		Document subD = new Document();
		
		subD.append("host","localhost");
		subD.append("port", 8111);
		
		d.append("comand", "HANDSHAKE_REQUEST");
		d.append("hostPort", subD);
		
		System.out.println(d.toJson());
		

	}

}
