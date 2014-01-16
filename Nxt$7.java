import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

class Nxt$7
  implements Runnable
{
  private final JSONObject getCumulativeDifficultyRequest;
  private final JSONObject getMilestoneBlockIdsRequest;
  
  Nxt$7(Nxt paramNxt)
  {
    this.getCumulativeDifficultyRequest = new JSONObject();
    this.getMilestoneBlockIdsRequest = new JSONObject();
    
    this.getCumulativeDifficultyRequest.put("requestType", "getCumulativeDifficulty");
    this.getMilestoneBlockIdsRequest.put("requestType", "getMilestoneBlockIds");
  }
  
  public void run()
  {
    try
    {
      Nxt.Peer peer = Nxt.Peer.getAnyPeer(1, true);
      if (peer != null)
      {
        Nxt.lastBlockchainFeeder = peer;
        
        JSONObject response = peer.send(this.getCumulativeDifficultyRequest);
        if (response != null)
        {
          BigInteger curCumulativeDifficulty = Nxt.Block.getLastBlock().cumulativeDifficulty;
          String peerCumulativeDifficulty = (String)response.get("cumulativeDifficulty");
          if (peerCumulativeDifficulty == null) {
            return;
          }
          BigInteger betterCumulativeDifficulty = new BigInteger(peerCumulativeDifficulty);
          if (betterCumulativeDifficulty.compareTo(curCumulativeDifficulty) > 0)
          {
            response = peer.send(this.getMilestoneBlockIdsRequest);
            if (response != null)
            {
              long commonBlockId = 2680262203532249785L;
              
              JSONArray milestoneBlockIds = (JSONArray)response.get("milestoneBlockIds");
              for (Object milestoneBlockId : milestoneBlockIds)
              {
                long blockId = Nxt.parseUnsignedLong((String)milestoneBlockId);
                Nxt.Block block = (Nxt.Block)Nxt.blocks.get(Long.valueOf(blockId));
                if (block != null)
                {
                  commonBlockId = blockId;
                  
                  break;
                }
              }
              int numberOfBlocks;
              int i;
              do
              {
                JSONObject request = new JSONObject();
                request.put("requestType", "getNextBlockIds");
                request.put("blockId", Nxt.convert(commonBlockId));
                response = peer.send(request);
                if (response == null) {
                  return;
                }
                JSONArray nextBlockIds = (JSONArray)response.get("nextBlockIds");
                numberOfBlocks = nextBlockIds.size();
                if (numberOfBlocks == 0) {
                  return;
                }
                for (i = 0; i < numberOfBlocks; i++)
                {
                  long blockId = Nxt.parseUnsignedLong((String)nextBlockIds.get(i));
                  if (Nxt.blocks.get(Long.valueOf(blockId)) == null) {
                    break;
                  }
                  commonBlockId = blockId;
                }
              } while (i == numberOfBlocks);
              if (Nxt.Block.getLastBlock().height - ((Nxt.Block)Nxt.blocks.get(Long.valueOf(commonBlockId))).height < 720)
              {
                long curBlockId = commonBlockId;
                LinkedList<Nxt.Block> futureBlocks = new LinkedList();
                HashMap<Long, Nxt.Transaction> futureTransactions = new HashMap();
                for (;;)
                {
                  JSONObject request = new JSONObject();
                  request.put("requestType", "getNextBlocks");
                  request.put("blockId", Nxt.convert(curBlockId));
                  response = peer.send(request);
                  if (response == null) {
                    break;
                  }
                  JSONArray nextBlocks = (JSONArray)response.get("nextBlocks");
                  numberOfBlocks = nextBlocks.size();
                  if (numberOfBlocks == 0) {
                    break;
                  }
                  for (i = 0; i < numberOfBlocks; i++)
                  {
                    JSONObject blockData = (JSONObject)nextBlocks.get(i);
                    Nxt.Block block = Nxt.Block.getBlock(blockData);
                    if (block == null)
                    {
                      peer.blacklist();
                      return;
                    }
                    curBlockId = block.getId();
                    synchronized (Nxt.blocksAndTransactionsLock)
                    {
                      boolean alreadyPushed = false;
                      if (block.previousBlock == Nxt.lastBlock)
                      {
                        ByteBuffer buffer = ByteBuffer.allocate(224 + block.payloadLength);
                        buffer.order(ByteOrder.LITTLE_ENDIAN);
                        buffer.put(block.getBytes());
                        
                        JSONArray transactionsData = (JSONArray)blockData.get("transactions");
                        for (Object transaction : transactionsData) {
                          buffer.put(Nxt.Transaction.getTransaction((JSONObject)transaction).getBytes());
                        }
                        if (Nxt.Block.pushBlock(buffer, false))
                        {
                          alreadyPushed = true;
                        }
                        else
                        {
                          peer.blacklist();
                          
                          return;
                        }
                      }
                      if ((!alreadyPushed) && (Nxt.blocks.get(Long.valueOf(block.getId())) == null) && (block.transactions.length <= 255))
                      {
                        futureBlocks.add(block);
                        
                        JSONArray transactionsData = (JSONArray)blockData.get("transactions");
                        for (int j = 0; j < block.transactions.length; j++)
                        {
                          Nxt.Transaction transaction = Nxt.Transaction.getTransaction((JSONObject)transactionsData.get(j));
                          block.transactions[j] = transaction.getId();
                          futureTransactions.put(Long.valueOf(block.transactions[j]), transaction);
                        }
                      }
                    }
                  }
                }
                if ((!futureBlocks.isEmpty()) && (Nxt.Block.getLastBlock().height - ((Nxt.Block)Nxt.blocks.get(Long.valueOf(commonBlockId))).height < 720)) {
                  synchronized (Nxt.blocksAndTransactionsLock)
                  {
                    Nxt.Block.saveBlocks("blocks.nxt.bak", true);
                    Nxt.Transaction.saveTransactions("transactions.nxt.bak");
                    
                    curCumulativeDifficulty = Nxt.Block.getLastBlock().cumulativeDifficulty;
                    while ((Nxt.lastBlock != commonBlockId) && (Nxt.Block.popLastBlock())) {}
                    if (Nxt.lastBlock == commonBlockId) {
                      for (Nxt.Block block : futureBlocks) {
                        if (block.previousBlock == Nxt.lastBlock)
                        {
                          ByteBuffer buffer = ByteBuffer.allocate(224 + block.payloadLength);
                          buffer.order(ByteOrder.LITTLE_ENDIAN);
                          buffer.put(block.getBytes());
                          for (long transactionId : block.transactions) {
                            buffer.put(((Nxt.Transaction)futureTransactions.get(Long.valueOf(transactionId))).getBytes());
                          }
                          if (!Nxt.Block.pushBlock(buffer, false)) {
                            break;
                          }
                        }
                      }
                    }
                    if (Nxt.Block.getLastBlock().cumulativeDifficulty.compareTo(curCumulativeDifficulty) < 0)
                    {
                      Nxt.Block.loadBlocks("blocks.nxt.bak");
                      Nxt.Transaction.loadTransactions("transactions.nxt.bak");
                      
                      peer.blacklist();
                      
                      Nxt.accounts.clear();
                      Nxt.aliases.clear();
                      Nxt.aliasIdToAliasMappings.clear();
                      Nxt.unconfirmedTransactions.clear();
                      Nxt.doubleSpendingTransactions.clear();
                      
                      Nxt.logMessage("Re-scanning blockchain...");
                      Map<Long, Nxt.Block> loadedBlocks = new HashMap(Nxt.blocks);
                      Nxt.blocks.clear();
                      Nxt.lastBlock = 2680262203532249785L;
                      long currentBlockId = 2680262203532249785L;
                      do
                      {
                        Nxt.Block currentBlock = (Nxt.Block)loadedBlocks.get(Long.valueOf(currentBlockId));
                        long nextBlockId = currentBlock.nextBlock;
                        currentBlock.analyze();
                        currentBlockId = nextBlockId;
                      } while (currentBlockId != 0L);
                      Nxt.logMessage("...Done");
                    }
                  }
                }
                synchronized (Nxt.blocksAndTransactionsLock)
                {
                  Nxt.Block.saveBlocks("blocks.nxt", false);
                  Nxt.Transaction.saveTransactions("transactions.nxt");
                }
              }
            }
          }
        }
      }
    }
    catch (Exception e)
    {
      Nxt.logDebugMessage("Error in milestone blocks processing thread", e);
    }
    catch (Throwable t)
