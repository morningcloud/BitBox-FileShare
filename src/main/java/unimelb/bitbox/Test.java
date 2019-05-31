package unimelb.bitbox;



import unimelb.bitbox.util.*;
import java.util.Scanner;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

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
	public static void main(String[] args) throws Exception
	{
		String t1 = "LIST_PEERS_REQUEST";
		HostPort hp = new HostPort("localshit:8999");
		
		Document d = hp.toDoc();
		int port = d.getInteger("port");
		System.out.println(port);
		System.out.println(d.toString());
		switch (t1)
		{
		case "LIST_PEERS_REQUEST":
		{
			System.out.println("alright");
			break;
		}
		
		case "EMM":
		{
			System.out.println("no");
			break;
		}
		}
	}
}
