package martinlt.cryptomessenger;

import java.util.Base64;

import org.junit.Test;

import junit.framework.TestCase;
import martinlt.cryptomessenger.MainApp.AlgorithmMode;

public class MainAppTest extends TestCase
{
   private MainApp bob, alice;

   private static String CONFIDENTIAL_MESSAGE = "Lorem ipsum dolor sit amet, consectetur "
         + "adipisicing elit, sed do eiusmod tempor "
         + "incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud "
         + "exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute "
         + "irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla "
         + "pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia "
         + "deserunt mollit anim id est laborum.";

   public MainAppTest(String testName)
   {
      super(testName);
   }

   protected void setUp() throws Exception
   {
      super.setUp();
   }

   protected void tearDown() throws Exception
   {
      super.tearDown();
   }

   @Test
   public void testEncryptandDecryptMessageRSA()
   {
      try {
         // create bob and alice as two new parties
         bob = new MainApp("bob", AlgorithmMode.RSA);
         alice = new MainApp("alice", AlgorithmMode.RSA);

         // exchange public keys
         alice.receivePublicKeyFrom("bob", bob.getPublicKey());
         bob.receivePublicKeyFrom("alice", alice.getPublicKey());

         // alice encrypts a message for bob
         alice.encryptMessage(CONFIDENTIAL_MESSAGE, "bob");

         // bob decrypts the message from alice
         bob.receiveAndDecryptMessage(Base64.getDecoder().decode(alice.getCipherText()));

         assertEquals(CONFIDENTIAL_MESSAGE, bob.getPlainText());

      } catch (Exception e) {
         fail(e.getMessage());
      }
   }

   @Test
   public void testEncryptandDecryptMessageDH()
   {
      try {
         // create bob and alice as two new parties
         bob = new MainApp("bob", AlgorithmMode.DH_AES);
         alice = new MainApp("alice", AlgorithmMode.DH_AES);

         // exchange public keys
         alice.receivePublicKeyFrom("bob", bob.getPublicKey());
         bob.receivePublicKeyFrom("alice", alice.getPublicKey());

         // alice encrypts a message for bob
         alice.encryptMessage(CONFIDENTIAL_MESSAGE, "bob");

         // bob decrypts the message from alice
         bob.receiveAndDecryptMessage(Base64.getDecoder().decode(alice.getCipherText()), "alice");

         assertEquals(CONFIDENTIAL_MESSAGE, bob.getPlainText());

      } catch (Exception e) {
         fail(e.getMessage());
      }
   }

}
