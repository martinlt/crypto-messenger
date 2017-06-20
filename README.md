## Crypto Messenger
This software demonstrates the use of both symmetric and asymmetric encryption 
mechanisms by way of RSA and Diffie-Hellman/AES algorithms.

Everything has been written using the standard Java 8 libraries along with JavaFX8 for 
the user interface.

### Usage Scenario
  Bob and Alice would like to exchange messages that are confidential and only
visible to them.

1. Bob and Alice would like to exchange messages that are confidential and only
visible to each other.

//
//    O                                        O
//   /|\                                      /|\
//   / \                                      / \
//
//  ALICE                                     BOB

// 2. Alice and Bob generate public and private keys.

//
//    O                                        O
//   /|\                                      /|\
//   / \                                      / \
//
//  ALICE                                     BOB
//  _ PUBLIC KEY                              _ PUBLIC KEY
//  _ PRIVATE KEY                             _ PRIVATE KEY

// 3. Alice and Bob exchange public keys with each other.

//
//    O                                        O
//   /|\                                      /|\
//   / \                                      / \
//
//  ALICE                                     BOB
//  + public key                              + public key
//  + private key                             + private key
//  _ PUBLIC KEY <------------------------->  _ PUBLIC KEY

// 4. Alice generates common secret key via using her private key and Bob's public key. Bob generates common secret key via using his private key and Alice's public key. Both secret keys are equal without TRANSFERRING. This is the magic of Diffie-Helman algorithm.

//
//    O                                        O
//   /|\                                      /|\
//   / \                                      / \
//
//  ALICE                                     BOB
//  + public key                              + public key
//  + private key                             + private key
//  + public key                              + public key
//  _ SECRET KEY                              _ SECRET KEY

// 5. ------------------------------------------------------------------
// Alice encrypts message using the secret key and sends to Bob

//
//    O                                        O
//   /|\ []-------------------------------->  /|\
//   / \                                      / \
//
//  ALICE                                     BOB
//  + public key                              + public key
//  + private key                             + private key
//  + public key                              + public key
//  + secret key                              + secret key
//  + message                                 _ MESSAGE

// 6. Bob receives the important message and decrypts with secret key.

//
//    O                     (((   (((   (((   \O/   )))
//   /|\                                       |
//   / \                                      / \
//
//  ALICE                                     BOB
//  + public key                              + public key
//  + private key                             + private key
//  + public key                              + public key
//  + secret key                              + secret key
//  + message                                 + message

``
