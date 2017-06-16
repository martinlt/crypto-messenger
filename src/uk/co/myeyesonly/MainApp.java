package uk.co.myeyesonly;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import uk.co.myeyesonly.exception.NoSuchPublicKeyException;
import uk.co.myeyesonly.exception.SecurityException;
import uk.co.myeyesonly.model.Party;
import uk.co.myeyesonly.view.PartyOverviewController;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class MainApp extends Application
{
   // JavaFX variables
   private Stage primaryStage;
   private BorderPane rootLayout;

   /**
    * A unique name representing the party, i.e. "Bob" or "Alice".
    */
   private final String name;

   /**
    * The private key for this party.
    */
   private final PrivateKey privateKey;

   /**
    * The public key for this party. These should be exchanged by parties
    * intending to communicate securely.
    *
    * See the {@link #receivePublicKeyFrom(String, String)} method.
    */
   private final PublicKey publicKey;
   private final StringProperty publicKeyBase64;

   /**
    * A hashmap containing public keys that this party is aware of. Each public
    * key is keyed using the name of the related party.
    */
   private HashMap<String, PublicKey> receivedPublicKeys = new HashMap<String, PublicKey>();

   /**
    * The data as an observable list of Parties.
    */
   private ObservableList<Party> partyData = FXCollections.observableArrayList();

   /**
    * A hashmap containing the generated secret keys for each known party public
    * key.
    */
   private HashMap<String, SecretKeySpec> secretKeys = new HashMap<String, SecretKeySpec>();

   /**
    * A plaintext string decrypted from an encrypted message.
    */
   private String plainText;

   /**
    * A plaintext string decrypted from an encrypted message.
    */
   private byte[] cipherText;

   /**
    * The basis used for key generation and encryption
    */
   private final String ALGORITHM_BASIS;

   /**
    * Used to determine how keys are generated and which algorithm is used for
    * encryption.
    */
   public static enum AlgorithmMode {
      RSA, DH_AES
   };

   private static final int KEY_SIZE = 2048;
   private static final int IV_SIZE = 16;

   private final String PRIVATE_KEY_FILE;
   private final String PUBLIC_KEY_FILE;

   /**
    * Default Constructor
    *
    * @throws NoSuchAlgorithmException
    * @throws ClassNotFoundException
    * @throws IOException
    * @throws SecurityException
    */
   public MainApp() throws NoSuchAlgorithmException, ClassNotFoundException, IOException, SecurityException
   {
      this("Bob", AlgorithmMode.RSA);
      receivePublicKeyFrom("alice", getPublicKey());
      receivePublicKeyFrom("eve", getPublicKey());
      receivePublicKeyFrom("john", getPublicKey());
      receivePublicKeyFrom("jane", getPublicKey());
   }
   /**
    * Instantiates a new party instance. This constructor will set the name for
    * this party and will generate a keypair (public/private) specific to the
    * algorithm selected.
    *
    * @param name
    *           unique identifier for the party.
    * @param mode
    *           use RSA for key generation + encryption, or DH for key
    *           generation and AES for encryption
    * @throws IOException
    * @throws NoSuchAlgorithmException
    * @throws ClassNotFoundException
    */
   public MainApp(final String name, AlgorithmMode mode)
         throws IOException, NoSuchAlgorithmException, ClassNotFoundException
   {
      this.name = name;

      if (AlgorithmMode.RSA == mode) {
         ALGORITHM_BASIS = "RSA";
      } else {
         ALGORITHM_BASIS = "DH";
      }

      this.PRIVATE_KEY_FILE = "private_" + name + "_" + ALGORITHM_BASIS + ".key";
      this.PUBLIC_KEY_FILE = "public_" + name + "_" + ALGORITHM_BASIS + ".key";

      if (!areKeysPresent()) {
         generateKeyPair();
      }

      ObjectInputStream inputStream;

      // Load the public key
      inputStream = new ObjectInputStream(new FileInputStream(PUBLIC_KEY_FILE));
      publicKey = (PublicKey) inputStream.readObject();
      inputStream.close();

      // Load the private key
      inputStream = new ObjectInputStream(new FileInputStream(PRIVATE_KEY_FILE));
      privateKey = (PrivateKey) inputStream.readObject();
      inputStream.close();

      publicKeyBase64 = new SimpleStringProperty(encodeBytes(publicKey.getEncoded()));
   }


   @Override
   public void start(Stage primaryStage)
   {
      this.primaryStage = primaryStage;
      this.primaryStage.setTitle("My Eyes Only");

      initRootLayout();

      showPartyOverview();
   }

   /**
    * Initializes the root layout.
    */
   public void initRootLayout()
   {
      try {
         // Load root layout from fxml file.
         FXMLLoader loader = new FXMLLoader();
         loader.setLocation(MainApp.class.getResource("view/RootLayout.fxml"));
         rootLayout = (BorderPane) loader.load();

         // Show the scene containing the root layout.
         Scene scene = new Scene(rootLayout);
         primaryStage.setScene(scene);
         primaryStage.show();
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   /**
    * Shows the party overview inside the root layout.
    */
   public void showPartyOverview()
   {
      try {
         // Load party overview.
         FXMLLoader loader = new FXMLLoader();
         loader.setLocation(MainApp.class.getResource("view/PartyOverview.fxml"));
         AnchorPane partyOverview = (AnchorPane) loader.load();

         // Set party overview into the center of root layout.
         rootLayout.setCenter(partyOverview);

         // Give the controller access to the main app.
         PartyOverviewController controller = loader.getController();
         controller.setMainApp(this);

      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   /**
    * Returns the data as an observable list of Partys.
    * @return
    */
   public ObservableList<Party> getPartyData() {
       return partyData;
   }

   /**
    * Returns the main stage.
    *
    * @return
    */
   public Stage getPrimaryStage()
   {
      return primaryStage;
   }

   public static void main(String[] args)
   {
      launch(args);
   }

   /**
    * Generate key which contains a pair of private and public key. Store the
    * set of keys in files.
    *
    * @throws NoSuchAlgorithmException
    * @throws IOException
    * @throws FileNotFoundException
    */
   private void generateKeyPair() throws NoSuchAlgorithmException, IOException
   {
      final KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM_BASIS);
      keyGen.initialize(KEY_SIZE);
      final KeyPair keyPair = keyGen.generateKeyPair();

      File privateKeyFile = new File(PRIVATE_KEY_FILE);
      File publicKeyFile = new File(PUBLIC_KEY_FILE);

      // Create files to store public and private key
      if (privateKeyFile.getParentFile() != null) {
         privateKeyFile.getParentFile().mkdirs();
      }
      privateKeyFile.createNewFile();

      if (publicKeyFile.getParentFile() != null) {
         publicKeyFile.getParentFile().mkdirs();
      }
      publicKeyFile.createNewFile();

      // Saving the Public key in a file
      ObjectOutputStream publicKeyOS = new ObjectOutputStream(new FileOutputStream(publicKeyFile));
      publicKeyOS.writeObject(keyPair.getPublic());
      publicKeyOS.close();

      // Saving the Private key in a file
      ObjectOutputStream privateKeyOS = new ObjectOutputStream(
            new FileOutputStream(privateKeyFile));
      privateKeyOS.writeObject(keyPair.getPrivate());
      privateKeyOS.close();

   }

   /**
    * The method checks if the pair of public and private key has already been
    * generated.
    *
    * @return flag indicating if the pair of keys were generated.
    */
   private boolean areKeysPresent()
   {

      File privateKey = new File(PRIVATE_KEY_FILE);
      File publicKey = new File(PUBLIC_KEY_FILE);

      if (privateKey.exists() && publicKey.exists()) {
         return true;
      }
      return false;
   }

   /**
    * A helper method to Base64 a byte array.
    *
    * @param bytes
    *           an array of bytes to be encoded.
    * @return Base64 encoded string representation of the bytes.
    */
   private String encodeBytes(final byte[] bytes)
   {
      return Base64.getEncoder().encodeToString(bytes);
   }

   public String getCipherText()
   {
      if(cipherText.length > 0)
         return encodeBytes(cipherText);
      else
         return "";
   }

   /**
    * Encrypt a message and send to a known party.
    * <p>
    * This method will retrieve the shared secret key from the hashmap of known
    * secret keys (see {@link #secretKeys}) based on the name of the recipient
    * party.
    * </p>
    * <p>
    * Following this, the message will be encrypted using the AES/ECB symmetric
    * encryption algorithm and then sent to the recipient by invoking the
    * {@link #receiveAndDecryptMessage(byte[], Party)} method.
    * </p>
    *
    * @param message
    *           the cleartext to be encrypted and sent to the recipient.
    * @param recipient
    *           the party to whom the encrypted message should be sent (see
    *           {@link #receiveAndDecryptMessage(byte[], Party)}).
    * @throws SecurityException
    *            failed to encrypt and send message
    */
   public void encryptMessage(final String message, final String recipient)
         throws SecurityException
   {
      // Diffie-Hellman key exchange + AES encryption
      if (ALGORITHM_BASIS.compareTo("DH") == 0) {
         SecretKeySpec secretKey = secretKeys.get(recipient);
         if (secretKey == null)
            throw new NoSuchPublicKeyException(recipient);

         cipherText = encrypt(message, secretKey);
      } else { // RSA keys + encryption
         PublicKey receivedPublicKey = receivedPublicKeys.get(recipient);
         if (receivedPublicKey == null)
            throw new NoSuchPublicKeyException(recipient);

         cipherText = encrypt(message, receivedPublicKey);
      }
   }

   /**
    * Generate ciphertext using AES/CBC/PKCS5Padding
    *
    * @param plainText
    *           the plain text message
    * @param secretKeySpec
    *           key to be used to encrypt the message
    * @return the ciphertext generated
    * @throws SecurityException
    *            if the encryption could not be completed
    */
   private byte[] encrypt(String plainText, SecretKeySpec secretKeySpec) throws SecurityException
   {
      try {
         // Generate the IV.
         byte[] iv = new byte[IV_SIZE];
         SecureRandom random = new SecureRandom();
         random.nextBytes(iv);
         IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

         // Encrypt the plaintext
         Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
         cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
         byte[] encryptedText = cipher.doFinal(plainText.getBytes());

         // Combine IV and encrypted part to form the ciphertext
         byte[] cipherText = new byte[IV_SIZE + encryptedText.length];
         System.arraycopy(iv, 0, cipherText, 0, IV_SIZE);
         System.arraycopy(encryptedText, 0, cipherText, IV_SIZE, encryptedText.length);

         return cipherText;
      } catch (Exception e) {
         throw new SecurityException("Encryption failed : ", e);
      }
   }

   /**
    * Generate ciphertext using RSA encryption
    *
    * @param plainText
    *           the plain text message
    * @param publicKey
    *           the RSA public key to be used to encrypt the message
    * @return the ciphertext generated
    * @throws SecurityException
    *            if the encryption could not be completed
    */
   private byte[] encrypt(String plainText, PublicKey publicKey) throws SecurityException
   {
      try {
         // Encrypt the plaintext
         Cipher cipher = Cipher.getInstance("RSA");
         cipher.init(Cipher.ENCRYPT_MODE, publicKey);
         byte[] cipherText = cipher.doFinal(plainText.getBytes());

         return cipherText;
      } catch (Exception e) {
         throw new SecurityException("Encryption failed : ", e);
      }
   }

   /**
    * Decipher text encrypted using AES/CBC/PKCS5Padding
    *
    * @param cipherText
    *           the encrypted message
    * @param secretKeySpec
    *           key used to encrypt the message
    * @return the decrypted message
    * @throws SecurityException
    *            if the decryption could not be completed
    */
   private String decrypt(byte[] cipherText, SecretKeySpec secretKeySpec) throws SecurityException
   {
      try {
         // Extract the IV from the ciphertext
         byte[] iv = new byte[IV_SIZE];
         System.arraycopy(cipherText, 0, iv, 0, iv.length);
         IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

         // Extract encrypted portion.
         int encryptedSize = cipherText.length - IV_SIZE;
         byte[] encryptedBytes = new byte[encryptedSize];
         System.arraycopy(cipherText, IV_SIZE, encryptedBytes, 0, encryptedSize);

         // Decrypt
         final Cipher cipherDecrypt = Cipher.getInstance("AES/CBC/PKCS5Padding");
         cipherDecrypt.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
         byte[] plainText = cipherDecrypt.doFinal(encryptedBytes);

         return new String(plainText);
      } catch (Exception e) {
         throw new SecurityException("Decryption failed : ", e);
      }
   }

   /**
    * Decipher text encrypted using RSA
    *
    * @param cipherText
    *           the encrypted message
    * @return the decrypted message
    * @throws SecurityException
    *            if the decryption could not be completed
    */
   private String decrypt(byte[] cipherText) throws SecurityException
   {
      try {
         // Decrypt
         final Cipher cipherDecrypt = Cipher.getInstance("RSA");
         cipherDecrypt.init(Cipher.DECRYPT_MODE, privateKey);
         byte[] plainText = cipherDecrypt.doFinal(cipherText);

         return new String(plainText);
      } catch (Exception e) {
         throw new SecurityException("Decryption failed : ", e);
      }
   }

   /**
    * Returns the unique identifier for the party.
    *
    * @return the name of the party (which should be unique).
    */
   public String getName()
   {
      return this.name;
   }

   /**
    * Returns the Base64 encoded public key for the party.
    *
    * @return the public key for the party.
    */
   public String getPublicKey()
   {
      return encodeBytes(publicKey.getEncoded());
   }

   /**
    * Receives an encrypted message and decrypts it.
    * <p>
    * This method will retrieve the shared secret key from the hashmap of known
    * secret keys (see {@link #secretKeys}) based on the name of the sender
    * party.
    * </p>
    * <p>
    * Following this, the message will be decrypted using the AES/ECB symmetric
    * encryption algorithm.
    * </p>
    *
    * @param message
    *           byte array containing an AES/ECB encrypted message
    * @param sender
    *           the sending party
    * @throws SecurityException
    *            decryption of message failed
    */
   public void receiveAndDecryptMessage(final byte[] message, String sender) throws SecurityException
   {
      try {
         SecretKeySpec secretKey = secretKeys.get(sender);
         if (secretKey == null)
            throw new NoSuchPublicKeyException(sender);

         this.plainText = decrypt(message, secretKey);

      } catch (Exception e) {
         throw new SecurityException("Decryption failed : ", e);
      }
   }

   /**
    * Receives an encrypted message and decrypts it.
    * <p>
    * This method will decrypt the message that has been encrypted with this
    * party's public key (RSA).
    * </p>
    *
    * @param message
    *           byte array containing a message encrypted with this party's
    *           public key
    * @throws SecurityException
    *            decryption of message failed
    */
   public void receiveAndDecryptMessage(final byte[] message) throws SecurityException
   {
      try {
         this.plainText = decrypt(message);

      } catch (Exception e) {
         throw new SecurityException("Decryption failed : ", e);
      }
   }

   /**
    * Returns the last decrypted message received.
    *
    * @return plaintext message.
    */
   public String getPlainText()
   {
      return this.plainText;
   }

   /**
    * Store the senders public key and generate a shared secret key.
    * <p>
    * This method will store the public key for the sender in the hashmap
    * {@link #receivedPublicKeys}, using the senderName as the hashmap key.
    * </p>
    * <p>
    * Following this, the shared SecretKeySpec will be generated and stored in
    * the corresponding hashmap {@link #secretKeys}, again using the senderName
    * as the hashmap key.
    * </p>
    *
    * @param senderName
    *           the unique name identifiying the sending party.
    * @param publicKey
    *           the Base64 representation of the publickey for the sending
    *           party.
    * @throws SecurityException
    *            failed to generate secret key from public key
    */
   public void receivePublicKeyFrom(final String senderName, final String publicKey)
         throws SecurityException
   {
      try {
         byte[] byteKey = Base64.getDecoder().decode(publicKey);

         X509EncodedKeySpec X509publicKey = new X509EncodedKeySpec(byteKey);
         KeyFactory kf = KeyFactory.getInstance(ALGORITHM_BASIS);

         PublicKey receivedPublicKey = kf.generatePublic(X509publicKey);

         receivedPublicKeys.put(senderName, receivedPublicKey);

         // add the party to our observable list
         partyData.add(new Party(senderName, publicKey));

         // generate the shared secret for Diffie-Hellman
         if (ALGORITHM_BASIS.compareTo("DH") == 0) {
            // generate and hash the shared secret key using my private key
            // and the senders public key
            final KeyAgreement keyAgreement = KeyAgreement.getInstance(ALGORITHM_BASIS);
            keyAgreement.init(privateKey);

            keyAgreement.doPhase(receivedPublicKey, true);

            byte[] key;
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            key = sha.digest(keyAgreement.generateSecret());
            key = Arrays.copyOf(key, 16);

            secretKeys.put(senderName, new SecretKeySpec(key, "AES"));
         }
      } catch (Exception e) {
         throw new SecurityException("Secret key generation failed : ", e);
      }
   }

   /**
    * @return the publicKeyBase64
    */
   public StringProperty getPublicKeyBase64()
   {
      return publicKeyBase64;
   }


}
