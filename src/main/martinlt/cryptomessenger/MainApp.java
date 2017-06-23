package martinlt.cryptomessenger;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import martinlt.cryptomessenger.exception.NoSuchPublicKeyException;
import martinlt.cryptomessenger.exception.SecurityException;
import martinlt.cryptomessenger.model.Party;
import martinlt.cryptomessenger.model.PartyListWrapper;
import martinlt.cryptomessenger.view.PartyEditDialogController;
import martinlt.cryptomessenger.view.PartyOverviewController;
import martinlt.cryptomessenger.view.RootLayoutController;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
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
import java.util.ListIterator;
import java.util.prefs.Preferences;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class MainApp extends Application
{
   /**
    * Used to determine how keys are generated and which algorithm is used for
    * encryption.
    */
   public static enum AlgorithmMode {
      RSA, DH_AES
   }

   /**
    * The size (in bits) of the keys
    */
   private static final int KEY_SIZE = 2048;

   /**
    * The size (in bits) of the initialisation vector used
    */
   private static final int IV_SIZE = 16;

   public static void main(String[] args)
   {
      launch(args);
   }

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

   /**
    * Base 64 representation of this party's public key.
    */
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
    * The basis used for key generation and key exchange
    */
   private final String algorithmBasis;

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
   public MainApp()
         throws NoSuchAlgorithmException, ClassNotFoundException, IOException, SecurityException
   {
      this("my", AlgorithmMode.RSA);
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
         algorithmBasis = "RSA";
      } else {
         algorithmBasis = "DH";
      }

      this.PRIVATE_KEY_FILE = "private_" + name + "_" + algorithmBasis + ".key";
      this.PUBLIC_KEY_FILE = "public_" + name + "_" + algorithmBasis + ".key";

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
   public void encryptMessage(final String message, final String recipient) throws SecurityException
   {
      // Diffie-Hellman key exchange + AES encryption
      if (algorithmBasis.compareTo("DH") == 0) {
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
    * Returns the key exchange pattern currently in use (RSA or DH)
    *
    * @return
    */
   public String getAlgorithmBasis()
   {
      return algorithmBasis;
   }

   /**
    * Returns a Base64 encoded version of the ciphertext.
    *
    * @return
    */
   public String getCipherText()
   {
      if (cipherText.length > 0)
         return encodeBytes(cipherText);
      else
         return "";
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
    * Returns the data as an observable list of Partys.
    *
    * @return
    */
   public ObservableList<Party> getPartyData()
   {
      return partyData;
   }

   /**
    * Returns the party file preference, i.e. the file that was last opened. The
    * preference is read from the OS specific registry. If no such preference
    * can be found, null is returned.
    *
    * @return
    */
   public File getPartyFilePath()
   {
      Preferences prefs = Preferences.userNodeForPackage(MainApp.class);
      String filePath = prefs.get("filePath", null);
      if (filePath != null) {
         return new File(filePath);
      } else {
         return null;
      }
   }

   /**
    * Returns the last decrypted message received.
    *
    * @return plaintext message.
    */
   public String getPlainText()
   {
      if (this.plainText == null)
         return "";
      else
         return this.plainText;
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

   /**
    * Returns the Base64 encoded public key for the party.
    *
    * @return the public key for the party.
    */
   public String getPublicKey()
   {
      // return encodeBytes(publicKey.getEncoded());
      return getKeyFormattedAsPEM(this.publicKey);
   }

   private String getKeyFormattedAsPEM(PublicKey key)
   {
      // Base 64 encode the key
      String base64Key = Base64.getEncoder().encodeToString(key.getEncoded());

      // Return in PEM format
      return "-----BEGIN PUBLIC KEY-----\n" + base64Key.replaceAll("(.{64})", "$1\n")
            + "\n-----END PUBLIC KEY-----\n";
   }

   /**
    * @return the publicKeyBase64
    */
   public StringProperty getPublicKeyBase64()
   {
      return publicKeyBase64;
   }

   /**
    * Initializes the root layout and tries to load the last opened party file.
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

         // Give the controller access to the main app.
         RootLayoutController controller = loader.getController();
         controller.setMainApp(this);

         primaryStage.show();
      } catch (IOException e) {
         e.printStackTrace();
      }

      // Try to load last opened party file.
      File file = getPartyFilePath();
      if (file != null) {
         loadPartyDataFromFile(file);
      }
   }

   /**
    * Loads party data from the specified file. The current party data will be
    * replaced.
    *
    * @param file
    */
   public void loadPartyDataFromFile(File file)
   {
      try {
         JAXBContext context = JAXBContext.newInstance(PartyListWrapper.class);
         Unmarshaller um = context.createUnmarshaller();

         // Reading XML from the file and unmarshalling.
         PartyListWrapper wrapper = (PartyListWrapper) um.unmarshal(file);

         // Clear down the current lists
         partyData.clear();
         receivedPublicKeys.clear();
         secretKeys.clear();

         // Load the party public keys, generate secret keys (if DH) and
         // observable list
         for (Party p : wrapper.getPartys()) {
            receivePublicKeyFrom(p.getIdentifier(), p.getPublicKey());
         }

         // Save the file path to the registry.
         setPartyFilePath(file);

      } catch (Exception e) { // catches ANY exception
         Alert alert = new Alert(AlertType.ERROR);
         alert.setTitle("Error");
         alert.setHeaderText("Could not load data");
         alert.setContentText("Could not load data from file:\n" + file.getPath());

         alert.showAndWait();
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
   public void receiveAndDecryptMessage(final byte[] message, String sender)
         throws SecurityException
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
         String unwrappedPEM = publicKey.replace("-----BEGIN PUBLIC KEY-----", "")
               .replace("-----END PUBLIC KEY-----", "").replace("\n", "");

         byte[] byteKey = Base64.getDecoder().decode(unwrappedPEM);

         X509EncodedKeySpec X509publicKey = new X509EncodedKeySpec(byteKey);
         KeyFactory kf = KeyFactory.getInstance(algorithmBasis);

         PublicKey receivedPublicKey = kf.generatePublic(X509publicKey);

         receivedPublicKeys.put(senderName, receivedPublicKey);

         // add the party to our observable list
         partyData.add(new Party(senderName, publicKey));

         // generate the shared secret for Diffie-Hellman
         if (algorithmBasis.compareTo("DH") == 0) {
            // generate and hash the shared secret key using my private key
            // and the senders public key
            final KeyAgreement keyAgreement = KeyAgreement.getInstance(algorithmBasis);
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
    * Removes a known party by name
    *
    * @param name
    */
   public void remove(String name)
   {
      receivedPublicKeys.values().remove(name);
      secretKeys.values().remove(name);

      ListIterator<Party> iter = partyData.listIterator();
      while (iter.hasNext()) {
         if (iter.next().getIdentifier().compareTo(name) == 0) {
            iter.remove();
         }
      }
   }

   /**
    * Saves the current party data to the specified file.
    *
    * @param file
    */
   public void savePartyDataToFile(File file)
   {
      try {
         JAXBContext context = JAXBContext.newInstance(PartyListWrapper.class);
         Marshaller m = context.createMarshaller();
         m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

         // Wrapping our party data.
         PartyListWrapper wrapper = new PartyListWrapper();
         wrapper.setPartys(partyData);

         // Marshalling and saving XML to the file.
         m.marshal(wrapper, file);

         // Save the file path to the registry.
         setPartyFilePath(file);
      } catch (Exception e) { // catches ANY exception
         Alert alert = new Alert(AlertType.ERROR);
         alert.setTitle("Error");
         alert.setHeaderText("Could not save data");
         alert.setContentText("Could not save data to file:\n" + file.getPath());

         alert.showAndWait();
      }
   }

   /**
    * Sets the file path of the currently loaded file. The path is persisted in
    * the OS specific registry.
    *
    * @param file
    *           the file or null to remove the path
    */
   public void setPartyFilePath(File file)
   {
      Preferences prefs = Preferences.userNodeForPackage(MainApp.class);
      if (file != null) {
         prefs.put("filePath", file.getPath());

         // Update the stage title.
         primaryStage.setTitle("Crypto Messenger - " + file.getName());
      } else {
         prefs.remove("filePath");

         // Update the stage title.
         primaryStage.setTitle("Crypto Messenger");
      }
   }

   /**
    * Opens a dialog to edit details for the specified party. If the user clicks
    * OK, the changes are saved into the provided party object and true is
    * returned.
    *
    * @param party
    *           the party object to be edited
    * @return true if the user clicked OK, false otherwise.
    */
   public boolean showPartyEditDialog(Party party)
   {
      try {
         // Load the fxml file and create a new stage for the popup dialog.
         FXMLLoader loader = new FXMLLoader();
         loader.setLocation(MainApp.class.getResource("view/PartyEditDialog.fxml"));
         AnchorPane page = (AnchorPane) loader.load();

         // Create the dialog Stage.
         Stage dialogStage = new Stage();
         dialogStage.setTitle("Edit Party");
         dialogStage.initModality(Modality.WINDOW_MODAL);
         dialogStage.initOwner(primaryStage);
         Scene scene = new Scene(page);
         dialogStage.setScene(scene);

         // Set the party into the controller.
         PartyEditDialogController controller = loader.getController();
         controller.setDialogStage(dialogStage);
         controller.setParty(party);
         controller.disableIdentifier();

         // Show the dialog and wait until the user closes it
         dialogStage.showAndWait();

         return controller.isOkClicked();
      } catch (IOException e) {
         e.printStackTrace();
         return false;
      }
   }

   /**
    * Opens a dialog to edit details for the specified party. If the user clicks
    * OK, the changes are saved into the provided party object and true is
    * returned.
    *
    * @param party
    *           the party object to be edited
    * @return true if the user clicked OK, false otherwise.
    */
   public boolean showPartyNewDialog(Party party)
   {
      try {
         // Load the fxml file and create a new stage for the popup dialog.
         FXMLLoader loader = new FXMLLoader();
         loader.setLocation(MainApp.class.getResource("view/PartyEditDialog.fxml"));
         AnchorPane page = (AnchorPane) loader.load();

         // Create the dialog Stage.
         Stage dialogStage = new Stage();
         dialogStage.setTitle("New Party");
         dialogStage.initModality(Modality.WINDOW_MODAL);
         dialogStage.initOwner(primaryStage);
         Scene scene = new Scene(page);
         dialogStage.setScene(scene);

         // Set the party into the controller.
         PartyEditDialogController controller = loader.getController();
         controller.setDialogStage(dialogStage);
         controller.setParty(party);
         controller.enableIdentifier();

         // Show the dialog and wait until the user closes it
         dialogStage.showAndWait();

         return controller.isOkClicked();
      } catch (IOException e) {
         e.printStackTrace();
         return false;
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

   @Override
   public void start(Stage primaryStage)
   {
      this.primaryStage = primaryStage;
      this.primaryStage.setTitle("Crypto Messenger");

      // Set the application icon.
      this.primaryStage.getIcons().add(new Image("file:resources/images/email_message.png"));

      initRootLayout();

      showPartyOverview();
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
    * Decipher a ciphertext which contains and RSA encrypted symmetric AES key
    * and a message encrypted with that key and the AES encryption algorithm.
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
         // Extract the cipher key from the ciphertext
         byte[] cipherKey = new byte[KEY_SIZE / 8];
         System.arraycopy(cipherText, 0, cipherKey, 0, cipherKey.length);

         // Decrypt the cipher key using my private key
         final Cipher cipherDecrypt = Cipher.getInstance("RSA");
         cipherDecrypt.init(Cipher.DECRYPT_MODE, privateKey);
         byte[] aesKey = cipherDecrypt.doFinal(cipherKey);
         SecretKeySpec aeskeySpec = new SecretKeySpec(aesKey, "AES");

         // Extract encrypted portion.
         int encryptedSize = cipherText.length - cipherKey.length;
         byte[] encryptedBytes = new byte[encryptedSize];
         System.arraycopy(cipherText, cipherKey.length, encryptedBytes, 0, encryptedSize);

         return decrypt(encryptedBytes, aeskeySpec);
      } catch (Exception e) {
         e.printStackTrace();
         throw new SecurityException("Decryption failed : ", e);
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

   /**
    * Generate ciphertext using a combination of RSA encryption for a randomly
    * generated symmetric key and AES encryption for the message.
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
         // generate a random AES key
         KeyGenerator kgen = KeyGenerator.getInstance("AES");
         kgen.init(128);
         SecretKey key = kgen.generateKey();
         byte[] aesKey = key.getEncoded();
         SecretKeySpec aeskeySpec = new SecretKeySpec(aesKey, "AES");

         // Encrypt the AES key with the other parties public RSA key
         Cipher cipher = Cipher.getInstance("RSA");
         cipher.init(Cipher.ENCRYPT_MODE, publicKey);
         byte[] cipherKey = cipher.doFinal(aesKey);

         // Now use AES encryption to generate the cipherText
         byte[] encryptedText = encrypt(plainText, aeskeySpec);

         // Combine secret key and encrypted part to form the ciphertext
         byte[] cipherText = new byte[cipherKey.length + encryptedText.length];
         System.arraycopy(cipherKey, 0, cipherText, 0, cipherKey.length);
         System.arraycopy(encryptedText, 0, cipherText, cipherKey.length, encryptedText.length);

         return cipherText;
      } catch (Exception e) {
         e.printStackTrace();
         throw new SecurityException("Encryption failed : ", e);
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
    * Generate key which contains a pair of private and public key. Store the
    * set of keys in files.
    *
    * @throws NoSuchAlgorithmException
    * @throws IOException
    * @throws FileNotFoundException
    */
   private void generateKeyPair() throws NoSuchAlgorithmException, IOException
   {
      final KeyPairGenerator keyGen = KeyPairGenerator.getInstance(algorithmBasis);
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

   protected void removeKeys()
   {
      try {

         new File(PRIVATE_KEY_FILE).delete();
         new File(PUBLIC_KEY_FILE).delete();

      } catch (Exception ignored) { }
   }
}
