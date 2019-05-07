package unimelb.bitbox.util;

import java.io.FileInputStream;
import java.util.Scanner;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Simple wrapper for using Properties(). Example:
 * <pre>
 * {@code
 * int port = Integer.parseInt(Configuration.getConfigurationValue("port"));
 * String[] peers = Configuration.getConfigurationValue("peers").split(",");
 * }
 * </pre>
 * @author aaron
 *
 */
public class Configuration {
	private static Logger log = Logger.getLogger(Configuration.class.getName());
    // the configuration file is stored in the root of the class path as a .properties file
    private static final String CONFIGURATION_FILE = "configuration.properties";

    private static final Properties properties;

    // use static initializer to read the configuration file when the class is loaded
    static {
        properties = new Properties();
        try (InputStream inputStream = new FileInputStream(CONFIGURATION_FILE)) {
            properties.load(inputStream);
        } catch (IOException e) {
            log.warning("Could not read file " + CONFIGURATION_FILE);
        }
    }
    
    public static HashMap<String,ArrayList<String>> getAuthKeys()
    {
        HashMap<String,ArrayList<String>> authKeys=new HashMap<String,ArrayList<String>>();
        //String[2]; // = new String[2];
    	try (InputStream inputStream = new FileInputStream(CONFIGURATION_FILE);
    			Scanner fileReader = new Scanner(inputStream);) 
    	{
    		while (fileReader.hasNext())
    		{
    			String line = fileReader.nextLine();
    			//System.out.println("line num="+line.substring(0,4));
    			if (line.substring(0,4).toLowerCase().equals("auth"))
    			{
    				String[] keys = line.substring(line.indexOf("=")+1).trim().split(",");
    				ArrayList<String> idKeys=null;// = 
    				for (String key: keys)
    				{
    					String identity = key.substring(key.lastIndexOf(" ")+1);
    					String idKey = key.substring(0,key.lastIndexOf(" "));
    					idKeys = (authKeys.get(identity)==null?new ArrayList<String>():authKeys.get(identity));
    					idKeys.add(idKey);
    					authKeys.put(identity,idKeys);
    				}
    			}
    		}
        } catch (IOException e) {
            log.warning("Could not read file " + CONFIGURATION_FILE);
        }
    	return authKeys;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
	public static Map<String, String> getConfiguration() {
        // ugly workaround to get String as generics
        Map temp = properties;
        Map<String, String> map = new HashMap<String, String>(temp);
        // prevent the returned configuration from being modified 
        return Collections.unmodifiableMap(map);
    }


    public static String getConfigurationValue(String key) {
        return properties.getProperty(key);
    }

    // private constructor to prevent initialization
    private Configuration() {
    }

}