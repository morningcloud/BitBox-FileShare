package unimelb.bitbox;

import unimelb.bitbox.*;
import unimelb.bitbox.util.*;

public class Test
{

	public static void main(String[] args)
	{
		// TODO Auto-generated method stub
		Configuration.getConfiguration();
		String[] auth = Configuration.getConfigurationValue("authorized_keys").split(" ");
		System.out.printf("arg0=%s\narg1=%s\n",auth[0],auth[1]);
		

	}

}
