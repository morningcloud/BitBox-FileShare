package unimelb.bitbox;



import unimelb.bitbox.util.*;
import java.util.Scanner;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.File;
import javax.crypto.Cipher;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import javax.crypto.KeyGenerator;
import java.util.ArrayList;
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
		String key = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDcxZOQOtlelHngf3IZQyaMxvs00GvWTTVc/EegAhoXS/Av1O1x40lRo8cZ8MvdKGmg8ZeyksUi0fe6mL3iwMkVUUcmxqrN5SsCZmR73Enjk3pgSf2WPNy4sJ/0hbcgn5ebkKUjquIZ/Ui8fa4BUfj8JHlOoPFmfgY9WyADuXlB55OW+FWQdJzROpc9OrSJtCWD7TbAW3jHTr+xbF/kasnzx1OsKwfxqoUcOt37Tk9uHG9m9Jt5fsy+H63y+HcJXmh+KlBfTUlbCZR1aW9C8nQU2M0T6E1Ri0i3k4rZCcHsvVrobgpKI8M0e0sI02qwqCsqPqfJ/FAKBRP081B9t6F/ tel@DESKTOP-MG41RRU";
		AsymmetricKeyParameter kp = OpenSSHPublicKeyUtil.parsePublicKey(key.getBytes());
		//BCRSAPublicKey pubKey = new BCRSAPublicKey((BCRSAPublicKey)kp);
		//KeyFactory kf =

		
		
	}
	public static byte[] toPKCS8Format(final PrivateKey privateKey) throws IOException
	{
		String keyFormat = privateKey.getFormat();
		if (keyFormat.equals("PKCS#1")) {
			final byte[] encoded = privateKey.getEncoded();
			final PrivateKeyInfo privateKeyInfo = PrivateKeyInfo.getInstance(encoded);
			final ASN1Encodable asn1Encodable = privateKeyInfo.parsePrivateKey();
			final ASN1Primitive asn1Primitive = asn1Encodable.toASN1Primitive();
			final byte[] privateKeyPKCS8Formatted = asn1Primitive.getEncoded(ASN1Encoding.DER);
			return privateKeyPKCS8Formatted;			
        }
		return privateKey.getEncoded();
	}		

}
