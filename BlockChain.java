// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

import java.util.HashMap;
import java.util.ArrayList;

public class BlockChain {
    public static final int CUT_OFF_AGE = 10;

    private Integer currentHeight = 0;
    HashMap<Integer, ArrayList<Block>> heightBlockMap;
    HashMap<ByteArrayWrapper, BlockModel> hashBlockMap;

    /**
     * create an empty block chain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {
        // IMPLEMENT THIS
        heightBlockMap = new HashMap<Integer, ArrayList<Block>>();
        hashBlockMap = new HashMap<ByteArrayWrapper, BlockModel>();
        addBlock(genesisBlock);
    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
        // IMPLEMENT THIS
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        // IMPLEMENT THIS
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
        // IMPLEMENT THIS
    }

    /**
     * Add {@code block} to the block chain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}.
     * 
     * <p>
     * For example, you can try creating a new block over the genesis block (block height 2) if the
     * block chain height is {@code <=
     * CUT_OFF_AGE + 1}. As soon as {@code height > CUT_OFF_AGE + 1}, you cannot create a new block
     * at height 2.
     * 
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
        // Genesis block check in a block chain where blocks exist.
        byte[] parentHash = block.getPrevBlockHash();
        if(currentHeight != 0 && parentHash == null) {
            return false;
        }

        // Process the genesis block
        if(parentHash == null) {
            // Genesis block. Add it to the blockChain and return.
            ArrayList<Block> blockList = new ArrayList<>();
            blockList.add(block);
            heightBlockMap.put(1, blockList);
            UTXOPool uPool = createUTXOPool(null, block);
            hashBlockMap.put(new ByteArrayWrapper(block.getHash()), new BlockModel(block, 1, uPool));
            return true;
        }

        BlockModel parentBlockModel = hashBlockMap.get(new ByteArrayWrapper(block.getPrevBlockHash()));
        if(parentBlockModel == null)
            return false;

        // Verify that incoming block is valid.
        if(!verifyBlock(block))
            return false;

        // If this is a newly found block corresponding to the root we are on..
        Integer blockHeight = parentBlockModel.height + 1;
        if(blockHeight > currentHeight) {
            currentHeight = blockHeight;
        }

        // Insert block in the heightBlock Map
        ArrayList<Block> blockHeightArrayList = heightBlockMap.get(blockHeight);
        if(blockHeightArrayList == null) {
            blockHeightArrayList = new ArrayList<>();
        }
        blockHeightArrayList.add(block);
        heightBlockMap.put(blockHeight, blockHeightArrayList);

        // Insert BlockModel in hashBlock Map
        UTXOPool uPool = createUTXOPool(parentBlockModel.utxoPool, block);
        BlockModel currentBlockModel = new BlockModel(block, blockHeight, uPool);
        hashBlockMap.put(new ByteArrayWrapper(block.getHash()), currentBlockModel);

        removeBlocksLowerThanCutoff(currentHeight);
        return true;
    }

    private UTXOPool createUTXOPool(UTXOPool currentPool, Block block) {
        if(currentPool == null) {
            UTXOPool uPool = new UTXOPool();
            // Genesis block. Add all outputs to utxoPool.
            ArrayList<Transaction> txList = block.getTransactions();
            for(Transaction tx : txList) {
                ArrayList<Transaction.Output> outputList = tx.getOutputs();
                int idx = 0;
                for(Transaction.Output output : outputList) {
                    UTXO utxo = new UTXO(tx.getHash(), idx);
                    uPool.addUTXO(utxo, output);
                    idx++;
                }
            }
            return uPool;
        }

        UTXOPool uPool = new UTXOPool(currentPool);
        ArrayList<Transaction> txList = block.getTransactions();

        // Important to add outpus first and then remove inputs ??

        // Add all outputs as UTXOs from the block to UTXO Pool
        for(Transaction tx : txList) {
            ArrayList<Transaction.Output> outputList = tx.getOutputs();
            int idx = 0;
            for(Transaction.Output output : outputList) {
                UTXO utxo = new UTXO(tx.getHash(), idx);
                uPool.addUTXO(utxo, output);
                idx++;
            }
        }

        // Remove all spent inputs from UTXO Pool
        for(Transaction tx : txList) {
            ArrayList<Transaction.Input> inputList = tx.getInputs();
            for(Transaction.Input input : inputList) {
                UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                uPool.removeUTXO(utxo);
            }
        }

        return uPool;

    }
    private void removeBlocksLowerThanCutoff(Integer currentHeight) {

    }

    private boolean verifyBlock(Block block) {


    }
    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        // IMPLEMENT THIS
    }

    private class BlockModel {
        Block block;
        Integer height;
        UTXOPool utxoPool;
        public BlockModel(Block block, Integer height, UTXOPool utxoPool) {
            this.block = block;
            this.height = height;
            this.utxoPool = utxoPool;
        }
    }

}