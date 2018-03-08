package scroogecoin;

import java.util.ArrayList;
import java.security.PublicKey;
import java.util.Arrays;

public class TxHandler {
    
    private UTXOPool Up;
    
    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        Up = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    
    public boolean isValidTx(Transaction tx) {
        ArrayList<Transaction.Output> outs = tx.getOutputs();
        ArrayList<UTXO> utxos = Up.getAllUTXO();
        ArrayList<Transaction.Output> utxoOuts = new ArrayList<Transaction.Output>();      
        for (int i=0; i<utxos.size(); i++) { 
            utxoOuts.add(Up.getTxOutput(utxos.get(i)));
        }
        
        double tTotal = 0;

        for (Transaction.Output op : outs) {
            if (op.value < 0) return false;
            tTotal -= op.value;
        }       
        
        for (int i=0; i<tx.numInputs(); i++) {
            Transaction.Input ip = tx.getInput(i);
            byte[] signature = ip.signature;
            byte[] message = tx.getRawDataToSign(i);
            PublicKey pubKey = null;
            
            for (int j=0; j<utxos.size(); j++) {
                if ((Arrays.equals(utxos.get(j).getTxHash(), ip.prevTxHash)) && (utxos.get(j).getIndex() == ip.outputIndex)) {
                    pubKey = Up.getTxOutput(utxos.get(j)).address;
                    tTotal += Up.getTxOutput(utxos.get(j)).value;
                    utxos.remove(j);
                    break;
                }
            }
            if (pubKey == null) return false;
            if (!Crypto.verifySignature(pubKey, message, signature)) return false;
        }
      
        return (tTotal>=0);
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        ArrayList<Transaction> txs = new ArrayList<Transaction>();
        ArrayList<Transaction.Input> ins = new ArrayList<Transaction.Input>();
        
        for (int i=0; i<possibleTxs.length; i++) {
            Transaction tx = possibleTxs[i];
            boolean doubleSpend = false;
            if (isValidTx(tx)) {              
                for (int j=0; j<tx.numInputs(); j++) {
                    for (int k=0; k<ins.size(); k++) {
                        if ((Arrays.equals(tx.getInput(j).prevTxHash,ins.get(k).prevTxHash)) && (tx.getInput(j).outputIndex == ins.get(k).outputIndex)) {
                            doubleSpend = true;
                        }
                    }
                }
                if (!doubleSpend) {
                    txs.add(tx);
                    for (int j=0; j<tx.numInputs(); j++) {
                        ins.add(tx.getInput(j));
                    }
                    for (int j=0; j<tx.numOutputs(); j++) {            
                        UTXO utxo = new UTXO(tx.getHash(), j);
                        Up.addUTXO(utxo, tx.getOutput(j));
                    }
                }
            }
        }
        
        ArrayList<UTXO> utxos = Up.getAllUTXO();
        
        for (Transaction tx : txs) {
            for (int i=0; i<tx.numInputs(); i++) {
                for (int j=0; j<utxos.size(); j++) {
                    if ((Arrays.equals(utxos.get(j).getTxHash(), tx.getInput(i).prevTxHash)) && (utxos.get(j).getIndex() == tx.getInput(i).outputIndex)) {
                        Up.removeUTXO(utxos.get(j));
                        break;
                    }
                }
            }
        }
        
        Transaction[] txsa = new Transaction[txs.size()];
        txs.toArray(txsa);
        
        return txsa;
    }

}
