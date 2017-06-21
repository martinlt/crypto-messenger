## Crypto Messenger
This software demonstrates the use of both symmetric and asymmetric encryption 
mechanisms by way of RSA and Diffie-Hellman/AES algorithms.

Everything has been written using the standard Java 8 libraries along with JavaFX8 for 
the user interface.
	
Follow this [link](https://github.com/martinlt/crypto-messenger/releases/download/v1.1/Crypto.Messenger-1.1.exe) to download a ready-compiled Windows installer version of this application.

### Usage Scenario 1 : Public Key Encryption (RSA / Asymmetric Encryption)
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

### Usage Scenario 2 : Symmetric Encryption and Diffie-Hellman Key Exchange
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
