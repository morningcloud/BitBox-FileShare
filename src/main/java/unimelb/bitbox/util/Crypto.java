package unimelb.bitbox.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.crypto.util.OpenSSHPrivateKeyUtil;
//import javax.xml.bind.DatatypeConverter;


import unimelb.bitbox.ServerMain;

public class Crypto
{
	private SecretKeySpec sessionKey;
	private Key clientPubKey;
	private Key privateKey;
	private Logger log = Logger.getLogger(ServerMain.class.getName());
	
	
	//TODO I need to implement decrypt(), decryptSessionKey()
	
	/**
	 * Constructor to be used by Peer. Initialises a session secret key.
	 * @param key
	 * @throws Exception
	 */
	
	public Crypto(Key key, Constants.CryptoUser cryptoUser) throws Exception
	{
		if (key!=null)
		{
			if (cryptoUser==Constants.CryptoUser.CLIENT)
			{
				this.privateKey = key;
			}
			else if (cryptoUser==Constants.CryptoUser.PEER)
			{
				this.clientPubKey = key;
				this.sessionKey = generateSecretKey();
			}
		}
	
		else
		{
			throw new Exception("Client public key is null");
		}
	}

	//TODO Delete the following constructor in final version.
	/**
	 * Simple constructor for testing purposes only.
	 */
	public Crypto()
	{
		loadPrivateKey();
		
	}
	/**
	 * Opens RSA Private Key, converts it to PKCS#1
	 */
	public void loadPrivateKey()
	{
		
		try
		{
			OpenSSHKeyV1KeyFile conv = new OpenSSHKeyV1KeyFile();
			conv.init(new File(Constants.PRIVATE_KEY_FILE_NAME));
			KeyPair kp = conv.readKeyPair();
			PrivateKey pk = kp.getPrivate();
			this.privateKey = pk;
			System.out.println(this.privateKey);

		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Encrypts a string using the provided key,
	 * @param key
	 * @param plainMsg
	 * @return
	 */
	public String encrypt(Key key, String plainMsg) 
	{
		String encMsg="";
		
		
		return encMsg;
	}
	
	
	/**
	 * Gets an encrypted & encoded session key.
	 * @return
	 * @throws Exception
	 */
	public String getEncodedSessionKey() throws Exception
	{
		return encodeSessionKey();
	}
	/**
	 * Encodes session key using Base64
	 * @return
	 */
	private String encodeSessionKey()
	{
		String encodedSessionKey="";
		
		try
		{
			encodedSessionKey = Base64.getEncoder().encodeToString(encryptSessionKey());
		}
		catch (Exception e)
		{
			log.warning("encoding session key failed");
		}
		
		return encodedSessionKey;
	}
	
	/**
	 * Encrypts a session key using Client's public key.
	 * @param key
	 * @return
	 */
	public byte[] encryptSessionKey() throws Exception
	{
		byte[] encryptedSessionKey;
		
		this.sessionKey = generateSecretKey();
		if (this.sessionKey!= null)
		{
			Cipher c = Cipher.getInstance("RSA");
			c.init(Cipher.ENCRYPT_MODE, this.clientPubKey);
			c.update(this.sessionKey.getEncoded());
			encryptedSessionKey = c.doFinal();
			//String encodedMsg = Base64.getEncoder().encodeToString(encMsg);
			//sessionKey = Base64.getEncoder().encodeToString(this.sessionKey.getEncoded());
		}
		else
		{
			throw new Exception("session key is null");
		}
		
		return encryptedSessionKey;
	}
	
	/**
	 * Generates a secret key to be used in crypto operations betwen Client and Peer.
	 * @return
	 */
	private SecretKeySpec generateSecretKey()
	{
		SecretKeySpec secretKey=null;
		
		
		SecureRandom sr = new SecureRandom();
		byte[] key = new byte[16];
		sr.nextBytes(key);
		byte[] initVector = new byte[16];
		sr.nextBytes(initVector);
		
		secretKey = new SecretKeySpec(key,Constants.SECRET_KEY_ALGORITHM);
		
		return secretKey;
	}
	

}
