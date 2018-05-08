/**
 * An account (aka wallet)
 */
package LTO.core;

import org.json.simple.JSONObject;
import com.muquit.libsodiumjna.SodiumKeyPair;

import LTO.exceptions.DecryptException;

import Util.utils.CryptoUtil;
import Util.utils.StringUtil;

/**
 * @author moonbi
 *
 */
public class Account {
	/**
	 * Account public address
	 * @var string
	 */
	public String address;
	
	/**
	 * Sign kyes
	 * @var object
	 */
	public JSONObject sign;
	
	/**
	 * Encryption keys
	 * @var object
	 */	
	public JSONObject encrypt;
	
	/**
	 * Get a random nonce
	 * @codeCoverageIgnore
	 * 
	 * @return string
	 */
	protected String getNonce()
	{
		return new String(CryptoUtil.random_bytes(CryptoUtil.crypto_box_noncebytes()));
	}
	
	/**
     * Get base58 encoded address
     * 
     * @param string encoding  'raw', 'base58' or 'base64'
     * @return string
     */
	public String getAddress(String encoding) {
		return address != null ? encode(address, encoding) : null;
	}
	public String getAddress() {
		return getAddress("base58");
	}
	
	/**
     * Get base58 encoded public sign key
     * 
     * @param string $encoding  'raw', 'base58' or 'base64'
     * @return string
     */
	public String getPublicSignKey(String encoding)
	{
		return sign != null ? encode(sign.get("publickey").toString(), encoding) : null;
	}
	public String getPublicSignKey()
	{
		return getPublicSignKey("base58");
	}
	
	/**
     * Get base58 encoded public encryption key
     * 
     * @param string $encoding  'raw', 'base58' or 'base64'
     * @return string
     */
    public String getPublicEncryptKey(String encoding)
    {
    	return encrypt == null ? encode(encrypt.get("publickey").toString(), encoding) : null;
    }
    public String getPublicEncryptKey()
    {
    	return getPublicEncryptKey("base58");
    }
	
	/**
     * Create an encoded signature of a message.
     * 
     * @param string $message
     * @param string $encoding  'raw', 'base58' or 'base64'
     * @return string
     */
	public String sign(String message, String encoding)
	{
		if (sign.get("secretkey") == null) {
			throw new RuntimeException("Unable to sign message; no secret sign key");
		}
		
		String signature = new String(CryptoUtil.crypto_sign_detached(message, sign.get("secretkey").toString()));
		return encode(signature, encoding);
	}
	public String sign(String message)
	{
		if (sign.get("secretkey") == null) {
			throw new RuntimeException("Unable to sign message; no secret sign key");
		}
		
		String signature = new String(CryptoUtil.crypto_sign_detached(message, sign.get("secretkey").toString()));
		return encode(signature, "base58");
	}
	
	/**
     * Sign an event
     * 
     * @param Event $event
     * @return $event
     */
    public Event signEvent(Event event)
    {
        event.signkey = getPublicSignKey();
        event.signature = sign(event.getMessage());
        event.hash = event.getHash();
        
        return event;
    }
    
    /**
     * Verify a signature of a message
     * 
     * @param string $signature
     * @param string $message
     * @param string $encoding   signature encoding 'raw', 'base58' or 'base64'
     * @return boolean
     */
    public boolean verify(String signature, String message, String encoding)
    {
    	if (this.sign.get("publickey") == null) {
    		throw new RuntimeException("Unable to verify message; no public sign key");
    	}
        
    	String rawSignature = decode(signature, encoding);
    	
    	return rawSignature.length() == CryptoUtil.crypto_sign_bytes() &&
    			sign.get("publickey").toString().length() == CryptoUtil.crypto_sign_publickeybytes() &&
    			CryptoUtil.crypto_sign_verify_detached(signature, message, sign.get("publickey").toString());
    }
    
    /**
     * Encrypt a message for another account.
     * The nonce is appended.
     * 
     * @param Account $recipient 
     * @param string  $message
     * @return string
     */
    public String encryptFor(Account recipient, String message)
    {
    	if (encrypt.get("secretkey") == null) {
    		throw new RuntimeException("Unable to encrypt message; no secret encryption key");
    	}
    	if (encrypt.get("publickey") == null) {
    		throw new RuntimeException("Unable to encrypt message; no public encryption key for recipient");
    	}
    	
    	SodiumKeyPair encryptionKey = CryptoUtil.crypto_box_keypair_from_secretkey_and_publickey(encrypt.get("secretkey").toString(), encrypt.get("publickey").toString());
    	
    	return new String(CryptoUtil.crypto_box(message, getNonce(), encryptionKey) + getNonce());
    }
    
    /**
     * Decrypt a message from another account.
     * 
     * @param Account $sender 
     * @param string  $cyphertext
     * @return string
     * @throws 
     */
    public String decryptFrom(Account sender, String cyphertext)
    {
    	if (encrypt.get("secretkey") == null) {
    		throw new RuntimeException("Unable to decrypt message; no secret encryption key");
    	}
    	if (encrypt.get("publickey") == null) {
    		throw new RuntimeException("Unable to decrypt message; no public encryption key for recipient");
    	}
        
        String encryptedMessage = cyphertext.substring(0, cyphertext.length() - 24);
        String nonce = cyphertext.substring(cyphertext.length()-24);

        SodiumKeyPair encryptionKey = CryptoUtil.crypto_box_keypair_from_secretkey_and_publickey(encrypt.get("secretkey").toString(), encrypt.get("publickey").toString());
    	
        byte[] message = CryptoUtil.crypto_box_open(encryptedMessage, nonce, encryptionKey);
        
        if (message == null) {
            throw new DecryptException("Failed to decrypt message from " + sender.getAddress());
        }
        
        return new String(message);
    }
	
	protected static String encode(String string, String encoding ) {
		if (encoding == "base58") {
			string = StringUtil.encodeBase58(string);
		}
		
		if (encoding == "base64" ) {
			string = string;
		}
		
		return string;
	}
	
	protected static String encode(String string) {
		return encode(string, "base58");
	}
	
	/**
     * Base58 or base64 decode a string
     * 
     * @param string $string
     * @param string $encoding  'raw', 'base58' or 'base64'
     * @return string
     */
    protected static String decode(String string, String encoding)
    {
    	if (encoding == "base58" ) {
    		string = StringUtil.decodeBase58(string);
    	}
    	
    	if (encoding == "base64" ) {
    		string = string;
    	}
    	
    	return string;
    }
    
    protected static String decode(String string) {
    	return decode(string, "base58");
    }
}