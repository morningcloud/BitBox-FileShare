package unimelb.bitbox;



import unimelb.bitbox.util.*;
import java.util.Scanner;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.File;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.KeySpec;

import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;

import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.OpenSSHPrivateKeyUtil;
import org.bouncycastle.crypto.util.OpenSSHPublicKeyUtil;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.crypto.encodings.*;
import org.bouncycastle.jcajce.provider.asymmetric.rsa.*;
public class Test
{
	public static void main(String[] args)
	{
		loadAuthorisedKeys();
		
	}
	
	private static void loadAuthorisedKeys()
	{
		Configuration.getConfiguration();
		HashMap<String,String> authKeys = Configuration.getAuthKeys();
		
		String msg = "This message should go dark!";
		Key identityRSAKey=null;
		
		for (String identity: authKeys.keySet())
		{
			OpenSSHToRSAPubKeyConverter keyConverter = new OpenSSHToRSAPubKeyConverter(authKeys.get(identity).getBytes());
			try
			{
				KeySpec spec = keyConverter.convertToRSAPublicKey();
				KeyFactory kf = KeyFactory.getInstance("RSA");
				identityRSAKey = kf.generatePublic(spec);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		
		try
		{
			Cipher c = Cipher.getInstance("RSA");
			c.init(Cipher.ENCRYPT_MODE, identityRSAKey);
			c.update(msg.getBytes());
			byte[] encMsg = c.doFinal();
			String encodedMsg = Base64.getEncoder().encodeToString(encMsg);
			System.out.println(encodedMsg);
		} catch (NoSuchAlgorithmException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeyException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalBlockSizeException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}


}
