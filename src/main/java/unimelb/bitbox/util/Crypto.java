package unimelb.bitbox.util;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.crypto.util.OpenSSHPrivateKeyUtil;
//import javax.xml.bind.DatatypeConverter;


import unimelb.bitbox.ServerMain;

public class Crypto
{
	private SecretKeySpec sessionKey;
	private final int KEY_SIZE = 2048;
	private Key clientPubKey;
	private Key privateKey;
	private Logger log = Logger.getLogger(ServerMain.class.getName());
	public HashMap<String,Key> authorisedKeys;
	
	
	//TODO I need to implement decrypt(), decryptSessionKey()
	
	/**
	 * Constructor to be used by Peer or Client. Initialises a session secret key.
	 * @param key if the crypto user is Client, it's a private key, if Peer, it's 
	 * the client's public key.
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

	/**
	 * Reads authorized_keys property, converts keys to PKCS#8 that is readable by java
	 * and creates corresponding Key objects, load them in HashMap along with the key's identity  
	 */
	public void loadAuthorisedKeys()
	{
		Configuration.getConfiguration();
		HashMap<String,String> authKeys = Configuration.getAuthKeys();
		this.authorisedKeys = new HashMap<String,Key>();
		for (String identity: authKeys.keySet())
		{
			OpenSSHToRSAPubKeyConverter keyConverter = new OpenSSHToRSAPubKeyConverter(authKeys.get(identity).getBytes());
			try
			{
				KeySpec spec = keyConverter.convertToRSAPublicKey();
				KeyFactory kf = KeyFactory.getInstance("RSA");
				Key identityRSAKey = kf.generatePublic(spec);
				this.authorisedKeys.put(identity, identityRSAKey);
			}
			catch (Exception e)
			{
				log.warning("Key couldn't be converted..check key format");
			}
		}
	}
	
	
	public SecretKeySpec getSessionKey()
	{
		return sessionKey;
	}

	//TODO Delete the following constructor in final version.
	/**
	 * Simple constructor for testing purposes only.
	 */
	public Crypto(String encodedSessionKey) throws Exception
	{
		loadPrivateKey();
	
		this.sessionKey = decryptSessionKey(decodeSessionKey(encodedSessionKey));
		
		if (this.sessionKey==null)
		{
			//TODO reinstate the exception below
			throw new Exception("Session key has not been decoded.");
		}
	}
	
	/**
	 * No-argument constructor.
	 */
	public Crypto()
	{
		loadAuthorisedKeys();
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

		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Encrypts a string using the provided key, encodes the result in Base64
	 * @param key
	 * @param plainMsg
	 * @return
	 */
	public String encrypt(String plainMsg) 
	{
		
		String encEncMsg = null;
		
		Cipher c;
		try
		{
			c = Cipher.getInstance(Constants.SECRET_KEY_ALGORITHM);
			c.init(Cipher.ENCRYPT_MODE, this.sessionKey);
			byte[] encMsg = c.doFinal(plainMsg.getBytes());
			if (encMsg!=null) encEncMsg = Base64.getEncoder().encodeToString(encMsg);

		} catch (Exception e)
		{
			// TODO Auto-generated catch block
			log.warning(e.getMessage());
		}
		
		return encEncMsg;
		
	}
	
	/**
	 * Decrypts an encoded&encrypted msg.
	 * 1) Decodes it using Bas64
	 * 2) Decrypts it using session key
	 * @param msg: Base64 representation of the encrypted msg.
	 * @return Plain message: the original message.
	 */
	public String decrypt(String msg) 
	{
		
		String plainMsg = null;
		try
		{
			byte[] decodedMsgBytes = Base64.getDecoder().decode(msg);
			Cipher c;
			c = Cipher.getInstance(Constants.SECRET_KEY_ALGORITHM);
			c.init(Cipher.DECRYPT_MODE, this.sessionKey);
			byte[] decMsg = c.doFinal(decodedMsgBytes);
			if (decMsg!=null) plainMsg = new String(decMsg);
			
		} catch (Exception e)
		{
			log.warning(e.getMessage());
		}
		return plainMsg;
		
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
	 * Decodes a Base64 representation of the encrypted session key.
	 * @param encodedSessionKey
	 * @return
	 */
	//TODO to made private
	public byte[] decodeSessionKey(String encodedSessionKey)
	{
		byte[] decodedBytes = Base64.getDecoder().decode(encodedSessionKey);
		return decodedBytes;
	}
	
	/**
	 * Encodes session key using Base64
	 * @return
	 * @throws Exception 
	 */
	private String encodeSessionKey() throws Exception
	{
		String encodedSessionKey="";
		byte[] encryptedSessionKeyBytes = encryptSessionKey();
		System.out.println("TE: encrypted byte size:"+encryptedSessionKeyBytes.length);
		try
		{
			encodedSessionKey = Base64.getEncoder().encodeToString(encryptedSessionKeyBytes);
			System.out.println("TE: Encoded key size:"+ encodedSessionKey.length());
		}
		catch (Exception e)
		{
			log.warning("encoding session key failed");
		}
		
		return encodedSessionKey;
	}
	
	/**
	 * Sets Crypto instance secret key as received from Peer.
	 */
	public void setSessionKey(String encodedSessionKey) throws Exception
	{
		this.sessionKey = decryptSessionKey(decodeSessionKey(encodedSessionKey));
		if (this.sessionKey==null)
		{
			throw new Exception("Setting session key failed");
		}
	}
	/**
	 * Decrypts session key and creates an secret key object.
	 * @param encryptedKeyBytes
	 * @return SecretKeySpec.
	 */
	public SecretKeySpec decryptSessionKey(byte[] encryptedKeyBytes)
	{
		loadPrivateKey();
		SecretKeySpec key=null;
		byte[] decryptedKeyBytes=null;
		try
		{
			Cipher d = Cipher.getInstance(Constants.RSA_ALGORITHM);
			d.init(Cipher.DECRYPT_MODE, this.privateKey);
			
			decryptedKeyBytes=d.doFinal(encryptedKeyBytes);
			key = new SecretKeySpec(decryptedKeyBytes,"AES");
			
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e)
		{
			e.printStackTrace();
		}
		
		return key;
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
		System.out.println("TE: Session key size is:"+this.sessionKey.getEncoded().length);
		if (this.sessionKey!= null)
		{
			Cipher c = Cipher.getInstance(Constants.RSA_ALGORITHM);
			
			c.init(Cipher.ENCRYPT_MODE, this.clientPubKey);
			c.update(this.sessionKey.getEncoded());
			encryptedSessionKey = c.doFinal();
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
		byte[] keyBytes = new byte[Constants.SECRET_KEY_SIZE];
		sr.nextBytes(keyBytes);
		secretKey = new SecretKeySpec(keyBytes,Constants.SECRET_KEY_ALGORITHM);
		return secretKey;
	}
	

}
