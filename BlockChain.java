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
    TransactionPool txPool;

    /**
     * create an empty block chain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {
        // IMPLEMENT THIS
        heightBlockMap = new HashMap<Integer, ArrayList<Block>>();
        hashBlockMap = new HashMap<ByteArrayWrapper, BlockModel>();
        txPool = new TransactionPool();
        addBlock(genesisBlock);
    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
        // IMPLEMENT THIS
        ArrayList<Block> blockList = heightBlockMap.get(currentHeight);
        if (blockList != null) {
            Block maxHeightBlock = blockList.get(0);
            return maxHeightBlock;
        }
        return null;
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        // IMPLEMENT THIS
        ArrayList<Block> blockList = heightBlockMap.get(currentHeight);
        if(blockList != null) {
            Block maxHeightBlock = blockList.get(0);
            ByteArrayWrapper maxHeightBlockHash = new ByteArrayWrapper(maxHeightBlock.getHash());
            BlockModel blockModel = hashBlockMap.get(maxHeightBlockHash);
            return blockModel.utxoPool;
        }
        return null;
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
        // IMPLEMENT THIS
        return txPool;
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
            UTXOPool uPool = createUTXOPoolForGenesisBlock(block);
            hashBlockMap.put(new ByteArrayWrapper(block.getHash()), new BlockModel(block, 1, uPool));
            currentHeight++;
            return true;
        }

        BlockModel parentBlockModel = hashBlockMap.get(new ByteArrayWrapper(block.getPrevBlockHash()));
        if(parentBlockModel == null)
            return false;

        // Verify that incoming block is valid.
        UTXOPool uPoolAfterBlockAddition = verifyBlock(block, parentBlockModel.utxoPool);
        if(uPoolAfterBlockAddition == null)
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
        BlockModel currentBlockModel = new BlockModel(block, blockHeight, uPoolAfterBlockAddition);
        hashBlockMap.put(new ByteArrayWrapper(block.getHash()), currentBlockModel);

        removeBlocksLowerThanCutoff();
        return true;
    }

    private UTXOPool createUTXOPoolForGenesisBlock(Block block) {

        // Genesis block. Add all outputs to utxoPool.
        UTXOPool uPool = new UTXOPool();
        Transaction coinbaseTx = block.getCoinbase();
        int idx = 0;
        for(Transaction.Output output : coinbaseTx.getOutputs()) {
            UTXO utxo = new UTXO(coinbaseTx.getHash(), idx);
            uPool.addUTXO(utxo, output);
            idx++;
        }
        return uPool;
    }
    
    private void removeBlocksLowerThanCutoff() {
        Integer removeHeight = currentHeight - CUT_OFF_AGE;
        if(removeHeight < 0)
            return;
        removeBlockWithHeight(removeHeight);
    }

    private void removeBlockWithHeight(Integer height) {
        // First remove blocks from hashBlockMap
        ArrayList<Block> blockList = heightBlockMap.get(height);
        if(blockList == null)
            return;
        for(Block block : blockList) {
            ByteArrayWrapper blockHash = new ByteArrayWrapper(block.getHash());
            hashBlockMap.remove(blockHash);
        }
        heightBlockMap.remove(height);
    }

    private UTXOPool verifyBlock(Block block, UTXOPool uPool) {
        TxHandler txHandler = new TxHandler(uPool);
        ArrayList<Transaction> txList = block.getTransactions();
        Transaction[] txArray = txList.toArray(new Transaction[txList.size()]);
        Transaction[] acceptedTxs = txHandler.handleTxs(txArray);
        if(acceptedTxs.length != txList.size())
            return null;
        return txHandler.getUTXOPool();
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