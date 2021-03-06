package secureemailserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server{
    
    private static String PUBLIC_KEY_FILE;
    private static String PRIVATE_KEY_FILE;
    
    public static void main(String[] args){
        
        URL pubUrl = Server.class.getResource("keys/public");
        URL privUrl = Server.class.getResource("keys/private");
        PUBLIC_KEY_FILE = pubUrl.getPath();
        PRIVATE_KEY_FILE = privUrl.getPath();
    
        try {
            create(18300);
        } catch (Exception ex) {
            System.err.println("Error creating server: "+ex);
        }
    }
    
    public static void create(int port) throws Exception{
        ServerSocket welcomeSocket;
        Socket connectionSocket;
        welcomeSocket = new ServerSocket(port);
        
        try {
            saveKeyPair(genKeyPair());
        } catch (IOException ex) {
            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Server created on port: "+port+".  Waiting for client");
        while(true) { 
            connectionSocket = welcomeSocket.accept();
            System.out.println("Accepted connection.  Creating new thread.");
            Thread thread = new Thread(new ServerThread(connectionSocket));
            thread.start();
        }
    }
    
    public static KeyPair genKeyPair() throws NoSuchAlgorithmException{
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(1024);
        KeyPair generatedKeyPair = keyGen.genKeyPair();
        return generatedKeyPair;
    }

    public static void saveKeyPair(KeyPair keyPair) throws IOException {
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();
        
        String path = "secureemailserver/keys";
        
        File keysDir = new File(path);
        
        if(!keysDir.exists()){
            keysDir.mkdirs();
            System.out.println("Creating directory at: "+keysDir);
        }
        
        String pubKeyFile = path+"/public";
        String privKeyFile = path+"/private";

        // Store Public Key.
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(
                        publicKey.getEncoded());
        FileOutputStream fos = new FileOutputStream(pubKeyFile);
        fos.write(x509EncodedKeySpec.getEncoded());
        fos.close();

        // Store Private Key.
        PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(
                        privateKey.getEncoded());
        fos = new FileOutputStream(privKeyFile);
        fos.write(pkcs8EncodedKeySpec.getEncoded());
        fos.close();
    }

    public static KeyPair loadKeyPair() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
            String path = "secureemailserver/keys";

            File keysDir = new File(path);

            if(!keysDir.exists()){
                throw new IOException("Could not find file");
            }

            String pubKeyFile = path+"/public";
            String privKeyFile = path+"/private";
            
            // Read Public Key.
            File filePublicKey = new File(pubKeyFile);
            FileInputStream fis = new FileInputStream(pubKeyFile);
            byte[] encodedPublicKey = new byte[(int) filePublicKey.length()];
            fis.read(encodedPublicKey);
            fis.close();

            // Read Private Key.
            File filePrivateKey = new File(privKeyFile);
            fis = new FileInputStream(privKeyFile);
            byte[] encodedPrivateKey = new byte[(int) filePrivateKey.length()];
            fis.read(encodedPrivateKey);
            fis.close();

            // Generate KeyPair.
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(
                            encodedPublicKey);
            PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(
                            encodedPrivateKey);
            PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

            return new KeyPair(publicKey, privateKey);
    }
}
