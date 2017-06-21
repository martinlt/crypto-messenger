## Crypto Messenger
This software demonstrates the use of both symmetric and asymmetric encryption 
mechanisms by way of RSA and Diffie-Hellman/AES algorithms.

Everything has been written using the standard Java 8 libraries along with JavaFX8 for 
the user interface.
	
Follow this [link](https://github.com/martinlt/crypto-messenger/releases/download/v1.1/Crypto.Messenger-1.1.exe) to download a ready-compiled Windows installer version of this application.

### Encryption pattern 1
In this pattern, the RSA public key of the receiving party is used to encrypt a temporary session key. The session key is used to encrypt the message using AES encryption. Both the encrypted session key and the encrypted message are sent to the receiving party as one ciphertext. Only the receiving party can decrypt the session key (using their RSA private key) and therefore decrypt the message (using AES).

This pattern can be classified as "Symmetric message encryption using public key encryption for key exchange" and a scenario is provided below::

- Bob and Alice would like to exchange messages that are confidential and only
visible to each other.
```
    O                                        O
   /|\                                      /|\
   / \                                      / \

  ALICE                                     BOB
```
- Alice and Bob generate a 2048 bit pair of RSA public and private keys.
```
    O                                        O
   /|\                                      /|\
   / \                                      / \

  ALICE                                     BOB
  _ PUBLIC KEY                              _ PUBLIC KEY
  _ PRIVATE KEY                             _ PRIVATE KEY
```
- Alice and Bob exchange public keys with each other.
```
    O                                        O
   /|\                                      /|\
   / \                                      / \

  ALICE                                     BOB
  + public key                              + public key
  + private key                             + private key
  _ PUBLIC KEY <------------------------->  _ PUBLIC KEY
```
- Alice generates an temporary AES session key and encrypts it with Bob's public key. Then, Alice encrypts the message with the session key, concatenates both ciphers and sends to Bob
```
    O                                        O
   /|\ []-------------------------------->  /|\
   / \                                      / \

  ALICE                                     BOB
  + public key                              + public key
  + private key                             + private key
  + public key                              + public key
  + session key                             _ session key
  + message                                 _ MESSAGE
```
- Bob receives the important message, decrypts the AES session key with his private key then decrypts the message with the session key.
```
    O                     (((   (((   (((   \O/   )))
   /|\                                       |
   / \                                      / \

  ALICE                                     BOB
  + public key                              + public key
  + private key                             + private key
  + public key                              + public key
  + session key                             + session key
  + message                                 + message
```

### Encryption pattern 2
In this pattern, both parties generate Diffie-Hellman public/private keypairs and exchange the public key. The combination of the private key and the other party's public key are then used to generate a shared secret key. This shared secret key can then be used by both parties to encrypt/decrypt messages using AES without ever having to be sent across the wire.

This pattern can be classified as "Symmetric message encryption using Diffie-Hellman key exchange" and a scenario is provided below:

- Again, Bob and Alice would like to exchange messages that are confidential and only
visible to them.
```
    O                                        O
   /|\                                      /|\
   / \                                      / \

  ALICE                                     BOB
```
- Alice and Bob generate a 2048 bit pair of Diffie-Hellman public and private keys.
```
    O                                        O
   /|\                                      /|\
   / \                                      / \

  ALICE                                     BOB
  _ PUBLIC KEY                              _ PUBLIC KEY
  _ PRIVATE KEY                             _ PRIVATE KEY
```
- Alice and Bob exchange public keys with each other.
```
    O                                        O
   /|\                                      /|\
   / \                                      / \

  ALICE                                     BOB
  + public key                              + public key
  + private key                             + private key
  _ PUBLIC KEY <------------------------->  _ PUBLIC KEY
```
- Alice generates common secret key via using her private key and Bob's public key. Bob generates common secret key via using his private key and Alice's public key. Both secret keys are equal without TRANSFERRING. This is the magic of Diffie-Helman algorithm.
```
    O                                        O
   /|\                                      /|\
   / \                                      / \

  ALICE                                     BOB
  + public key                              + public key
  + private key                             + private key
  + public key                              + public key
  _ SECRET KEY                              _ SECRET KEY
```
- Alice encrypts message by using the secret key and applying the AES/CBC encryption algorithm, then sends to Bob.
```
    O                                        O
   /|\ []-------------------------------->  /|\
   / \                                      / \

  ALICE                                     BOB
  + public key                              + public key
  + private key                             + private key
  + public key                              + public key
  + secret key                              + secret key
  + message                                 _ MESSAGE
```
- Bob receives the important message and decrypts with secret key.
```
    O                     (((   (((   (((   \O/   )))
   /|\                                       |
   / \                                      / \

  ALICE                                     BOB
  + public key                              + public key
  + private key                             + private key
  + public key                              + public key
  + secret key                              + secret key
  + message                                 + message
```
