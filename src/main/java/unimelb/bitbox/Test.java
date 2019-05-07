package unimelb.bitbox;



import unimelb.bitbox.util.*;
import java.util.Scanner;
import java.io.FileInputStream;
import java.io.File;
import javax.crypto.Cipher;
import java.security.Key;
import java.security.KeyStore;
import java.security.PublicKey;
import javax.crypto.KeyGenerator;
import java.util.ArrayList;
import java.util.HashMap;

public class Test
{

	public static void main(String[] args)
	{

		
		Configuration.getConfiguration();
		//String[] auth = Configuration.getConfigurationValue("authorized_keys").split(" ");
		HashMap<String,ArrayList<String>> authKeys = Configuration.getAuthKeys();//Configuration.getConfigurationValue("authorized_keys");
		for (String identity: authKeys.keySet())
		{
			System.out.printf("identity=%s, key count=%s\n",identity,authKeys.get(identity).size());
			for (String key: authKeys.get(identity))
			{
				System.out.printf("\tidentityKey=%s\n",key);
			}
		}
	}

}
