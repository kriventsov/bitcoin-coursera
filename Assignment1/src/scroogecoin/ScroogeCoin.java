package scroogecoin;

import java.util.ArrayList;
import java.security.*;

public class ScroogeCoin {

  public static void main(String[] args) {
 
     byte[] h1 = {26, 52};  
     
     UTXO u1 = new UTXO(h1, 1);
     UTXO u2 = new UTXO(h1, 2);
     
     UTXOPool up = new UTXOPool();
     
     KeyPairGenerator keyGen;
     SecureRandom random;
     
     try { keyGen = KeyPairGenerator.getInstance("RSA"); }
     catch ( NoSuchAlgorithmException e) {
        throw new RuntimeException(e);
     }
     
     
     try { random = SecureRandom.getInstance("SHA1PRNG", "SUN"); }
     catch ( NoSuchAlgorithmException e) {
        throw new RuntimeException(e);
     }
     catch ( NoSuchProviderException e) {
        throw new RuntimeException(e);
     } 
     
     keyGen.initialize(1024, random);
     
     KeyPair pair = keyGen.generateKeyPair();
     PrivateKey priv = pair.getPrivate();
     PublicKey pub = pair.getPublic();
     
     Transaction t1 = new Transaction();
     t1.addOutput(100.0, pub);
     t1.addOutput(200.0, pub);
     ArrayList<Transaction.Output> outs = t1.getOutputs();

     up.addUTXO(u1, outs.get(0));
     up.addUTXO(u2, outs.get(1));
     
     TxHandler txH = new TxHandler(up);
     
     Transaction t2 = new Transaction();
     t2.addInput(h1, 1);
     System.out.println(txH.isValidTx(t2));
  }
    
}
