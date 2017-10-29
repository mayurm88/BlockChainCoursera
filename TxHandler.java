import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TxHandler {

    private UTXOPool uPool;
    private UTXOPool spentPool;
    //private ArrayList<UTXO> spentUTXOs;
    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        uPool = new UTXOPool(utxoPool);
        spentPool = new UTXOPool();
        //spentUTXOs = new ArrayList<UTXO>();
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
        // IMPLEMENT THIS
        double inputSum = 0;
        double outputSum = 0;
        ArrayList<UTXO> currentTxSpentUTXOs = new ArrayList<UTXO>();
        UTXOPool pool = new UTXOPool();

        for (int i = 0; i < tx.getInputs().size(); i++) {
            // For each input check the transaction output from which it originates and check if that is a UTXO
            Transaction.Input in = tx.getInput(i);
            UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);
            Transaction.Output utxoOutput = null;

            // If the global pool does not contain the UXTO, check the local pool
            if (uPool.contains(utxo)) {
                utxoOutput = uPool.getTxOutput(utxo);
            } else {
                return false;
            }

            // If the UTXO is present either in local or global pool, see if it's already spent or not.
            if (isSpentInCurrentTx(currentTxSpentUTXOs, utxo))
                return false;

            // Verify the signature on this input
            PublicKey inputPublicKey = utxoOutput.address;
            byte[] rawData = tx.getRawDataToSign(i);
            if (!Crypto.verifySignature(inputPublicKey, rawData, in.signature))
                return false;

            // Add the utxo to spent UTXOs list for current transaction
            currentTxSpentUTXOs.add(utxo);
            inputSum += utxoOutput.value;
        }

        for (int i = 0; i < tx.getOutputs().size(); i++) {
            Transaction.Output op = tx.getOutput(i);
            if (op.value >= 0) {
                outputSum += op.value;
                UTXO utxo = new UTXO(tx.getHash(), i);
                pool.addUTXO(utxo, op);
            } else {
                return false;
            }
        }

        if (inputSum >= outputSum) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isSpentInCurrentTx(ArrayList<UTXO> curTxSpentUTXOs, UTXO ut) {
        for (UTXO utxo : curTxSpentUTXOs
             ) {
            if(ut.equals(utxo))
                return true;
        }
        return false;
    }

    boolean isTxInputSpentInOtherTransactionsInBlock(Transaction tx) {
        for(int i = 0; i < tx.getInputs().size(); i++) {
            Transaction.Input in = tx.getInput(i);
            UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);
            if(spentPool.contains(utxo)) {
                return true;
            }
        }
        return false;
    }

    void markAllUTXOsSpent(Transaction tx) {
        for(int i = 0; i < tx.getInputs().size(); i++) {
            Transaction.Input in = tx.getInput(i);
            UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);
            uPool.removeUTXO(utxo);
        }
    }

    void addAllOutputsAsUTXOs(Transaction tx) {
        for(int i = 0; i < tx.getOutputs().size(); i++) {
            UTXO utxo = new UTXO(tx.getHash(), i);
            uPool.addUTXO(utxo, tx.getOutput(i));
        }
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        ArrayList<Transaction> txList = new ArrayList<Transaction>();
        for(int i = 0; i < possibleTxs.length; i++) {
            Transaction tx = possibleTxs[i];
            if(isValidTx(tx)) {
                if(isTxInputSpentInOtherTransactionsInBlock(tx)) {
                    continue;
                }
                txList.add(tx);
                markAllUTXOsSpent(tx);
                addAllOutputsAsUTXOs(tx);
            }
        }

        Transaction[] txArray = txList.toArray(new Transaction[txList.size()]);
        return txArray;
    }

    public void removeSpentUTXOs(ArrayList<UTXO> spentUTXOs) {
        for (UTXO utxo : spentUTXOs
             ) {
            uPool.removeUTXO(utxo);
        }
        spentUTXOs.clear();
    }

    public UTXOPool getUTXOPool() {
        return uPool;
    }
}
