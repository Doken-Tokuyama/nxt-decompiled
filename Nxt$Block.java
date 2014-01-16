import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

class Nxt$Block
  implements Serializable
{
  static final long serialVersionUID = 0L;
  final int version;
  final int timestamp;
  final long previousBlock;
  int totalAmount;
  int totalFee;
  int payloadLength;
  byte[] payloadHash;
  final byte[] generatorPublicKey;
  byte[] generationSignature;
  byte[] blockSignature;
  final byte[] previousBlockHash;
  int index;
  final long[] transactions;
  volatile long baseTarget;
  int height;
  volatile long nextBlock;
  volatile BigInteger cumulativeDifficulty;
  volatile transient long id;
  
  Nxt$Block(int version, int timestamp, long previousBlock, int numberOfTransactions, int totalAmount, int totalFee, int payloadLength, byte[] payloadHash, byte[] generatorPublicKey, byte[] generationSignature, byte[] blockSignature)
  {
    this(version, timestamp, previousBlock, numberOfTransactions, totalAmount, totalFee, payloadLength, payloadHash, generatorPublicKey, generationSignature, blockSignature, null);
  }
  
  Nxt$Block(int version, int timestamp, long previousBlock, int numberOfTransactions, int totalAmount, int totalFee, int payloadLength, byte[] payloadHash, byte[] generatorPublicKey, byte[] generationSignature, byte[] blockSignature, byte[] previousBlockHash)
  {
    if ((numberOfTransactions > 255) || (numberOfTransactions < 0)) {
      throw new IllegalArgumentException("attempted to create a block with " + numberOfTransactions + " transactions");
    }
    if ((payloadLength > 32640) || (payloadLength < 0)) {
      throw new IllegalArgumentException("attempted to create a block with payloadLength " + payloadLength);
    }
    this.version = version;
    this.timestamp = timestamp;
    this.previousBlock = previousBlock;
    this.totalAmount = totalAmount;
    this.totalFee = totalFee;
    this.payloadLength = payloadLength;
    this.payloadHash = payloadHash;
    this.generatorPublicKey = generatorPublicKey;
    this.generationSignature = generationSignature;
    this.blockSignature = blockSignature;
    
    this.previousBlockHash = previousBlockHash;
    this.transactions = new long[numberOfTransactions];
  }
  
  void analyze()
  {
    synchronized (Nxt.blocksAndTransactionsLock)
    {
      if (this.previousBlock == 0L)
      {
        Nxt.lastBlock = 2680262203532249785L;
        this.baseTarget = 153722867L;
        this.cumulativeDifficulty = BigInteger.ZERO;
        Nxt.blocks.put(Long.valueOf(Nxt.lastBlock), this);
        
        Nxt.Account.addAccount(1739068987193023818L);
      }
      else
      {
        getLastBlock().nextBlock = getId();
        
        this.height = (getLastBlock().height + 1);
        Nxt.lastBlock = getId();
        if (Nxt.blocks.putIfAbsent(Long.valueOf(Nxt.lastBlock), this) != null) {
          throw new RuntimeException("duplicate block id: " + getId());
        }
        this.baseTarget = getBaseTarget();
        this.cumulativeDifficulty = ((Block)Nxt.blocks.get(Long.valueOf(this.previousBlock))).cumulativeDifficulty.add(Nxt.two64.divide(BigInteger.valueOf(this.baseTarget)));
        
        Nxt.Account generatorAccount = (Nxt.Account)Nxt.accounts.get(Long.valueOf(getGeneratorAccountId()));
        generatorAccount.addToBalanceAndUnconfirmedBalance(this.totalFee * 100L);
      }
      for (long transactionId : this.transactions)
      {
        Nxt.Transaction transaction = (Nxt.Transaction)Nxt.transactions.get(Long.valueOf(transactionId));
        transaction.height = this.height;
        
        long sender = transaction.getSenderAccountId();
        Nxt.Account senderAccount = (Nxt.Account)Nxt.accounts.get(Long.valueOf(sender));
        if (!senderAccount.setOrVerify(transaction.senderPublicKey)) {
          throw new RuntimeException("sender public key mismatch");
        }
        senderAccount.addToBalanceAndUnconfirmedBalance(-(transaction.amount + transaction.fee) * 100L);
        
        Nxt.Account recipientAccount = (Nxt.Account)Nxt.accounts.get(Long.valueOf(transaction.recipient));
        if (recipientAccount == null) {
          recipientAccount = Nxt.Account.addAccount(transaction.recipient);
        }
        switch (transaction.type)
        {
        case 0: 
          switch (transaction.subtype)
          {
          case 0: 
            recipientAccount.addToBalanceAndUnconfirmedBalance(transaction.amount * 100L);
          }
          break;
        case 1: 
          switch (transaction.subtype)
          {
          case 1: 
            Nxt.Transaction.MessagingAliasAssignmentAttachment attachment = (Nxt.Transaction.MessagingAliasAssignmentAttachment)transaction.attachment;
            
            String normalizedAlias = attachment.alias.toLowerCase();
            
            Nxt.Alias alias = (Nxt.Alias)Nxt.aliases.get(normalizedAlias);
            if (alias == null)
            {
              long aliasId = transaction.getId();
              alias = new Nxt.Alias(senderAccount, aliasId, attachment.alias, attachment.uri, this.timestamp);
              Nxt.aliases.put(normalizedAlias, alias);
              Nxt.aliasIdToAliasMappings.put(Long.valueOf(aliasId), alias);
            }
            else
            {
              alias.uri = attachment.uri;
              alias.timestamp = this.timestamp;
            }
            break;
          }
          break;
        case 2: 
          switch (transaction.subtype)
          {
          case 0: 
            Nxt.Transaction.ColoredCoinsAssetIssuanceAttachment attachment = (Nxt.Transaction.ColoredCoinsAssetIssuanceAttachment)transaction.attachment;
            
            long assetId = transaction.getId();
            Nxt.Asset asset = new Nxt.Asset(sender, attachment.name, attachment.description, attachment.quantity);
            Nxt.assets.put(Long.valueOf(assetId), asset);
            Nxt.assetNameToIdMappings.put(attachment.name.toLowerCase(), Long.valueOf(assetId));
            Nxt.sortedAskOrders.put(Long.valueOf(assetId), new TreeSet());
            Nxt.sortedBidOrders.put(Long.valueOf(assetId), new TreeSet());
            senderAccount.addToAssetAndUnconfirmedAssetBalance(Long.valueOf(assetId), attachment.quantity);
            

            break;
          case 1: 
            Nxt.Transaction.ColoredCoinsAssetTransferAttachment attachment = (Nxt.Transaction.ColoredCoinsAssetTransferAttachment)transaction.attachment;
            
            senderAccount.addToAssetAndUnconfirmedAssetBalance(Long.valueOf(attachment.asset), -attachment.quantity);
            recipientAccount.addToAssetAndUnconfirmedAssetBalance(Long.valueOf(attachment.asset), attachment.quantity);
            

            break;
          case 2: 
            Nxt.Transaction.ColoredCoinsAskOrderPlacementAttachment attachment = (Nxt.Transaction.ColoredCoinsAskOrderPlacementAttachment)transaction.attachment;
            
            Nxt.AskOrder order = new Nxt.AskOrder(transaction.getId(), senderAccount, attachment.asset, attachment.quantity, attachment.price);
            senderAccount.addToAssetAndUnconfirmedAssetBalance(Long.valueOf(attachment.asset), -attachment.quantity);
            Nxt.askOrders.put(Long.valueOf(order.id), order);
            ((TreeSet)Nxt.sortedAskOrders.get(Long.valueOf(attachment.asset))).add(order);
            Nxt.matchOrders(attachment.asset);
            

            break;
          case 3: 
            Nxt.Transaction.ColoredCoinsBidOrderPlacementAttachment attachment = (Nxt.Transaction.ColoredCoinsBidOrderPlacementAttachment)transaction.attachment;
            
            Nxt.BidOrder order = new Nxt.BidOrder(transaction.getId(), senderAccount, attachment.asset, attachment.quantity, attachment.price);
            
            senderAccount.addToBalanceAndUnconfirmedBalance(-attachment.quantity * attachment.price);
            
            Nxt.bidOrders.put(Long.valueOf(order.id), order);
            ((TreeSet)Nxt.sortedBidOrders.get(Long.valueOf(attachment.asset))).add(order);
            
            Nxt.matchOrders(attachment.asset);
            

            break;
          case 4: 
            Nxt.Transaction.ColoredCoinsAskOrderCancellationAttachment attachment = (Nxt.Transaction.ColoredCoinsAskOrderCancellationAttachment)transaction.attachment;
            

            Nxt.AskOrder order = (Nxt.AskOrder)Nxt.askOrders.remove(Long.valueOf(attachment.order));
            ((TreeSet)Nxt.sortedAskOrders.get(Long.valueOf(order.asset))).remove(order);
            senderAccount.addToAssetAndUnconfirmedAssetBalance(Long.valueOf(order.asset), order.quantity);
            

            break;
          case 5: 
            Nxt.Transaction.ColoredCoinsBidOrderCancellationAttachment attachment = (Nxt.Transaction.ColoredCoinsBidOrderCancellationAttachment)transaction.attachment;
            

            Nxt.BidOrder order = (Nxt.BidOrder)Nxt.bidOrders.remove(Long.valueOf(attachment.order));
            ((TreeSet)Nxt.sortedBidOrders.get(Long.valueOf(order.asset))).remove(order);
            senderAccount.addToBalanceAndUnconfirmedBalance(order.quantity * order.price);
          }
          break;
        }
      }
    }
  }
  
  static long getBaseTarget()
  {
    if (Nxt.lastBlock == 2680262203532249785L) {
      return ((Block)Nxt.blocks.get(Long.valueOf(2680262203532249785L))).baseTarget;
    }
    Block lastBlock = getLastBlock();Block previousBlock = (Block)Nxt.blocks.get(Long.valueOf(lastBlock.previousBlock));
    long curBaseTarget = previousBlock.baseTarget;long newBaseTarget = BigInteger.valueOf(curBaseTarget).multiply(BigInteger.valueOf(lastBlock.timestamp - previousBlock.timestamp)).divide(BigInteger.valueOf(60L)).longValue();
    if ((newBaseTarget < 0L) || (newBaseTarget > 153722867000000000L)) {
      newBaseTarget = 153722867000000000L;
    }
    if (newBaseTarget < curBaseTarget / 2L) {
      newBaseTarget = curBaseTarget / 2L;
    }
    if (newBaseTarget == 0L) {
      newBaseTarget = 1L;
    }
    long twofoldCurBaseTarget = curBaseTarget * 2L;
    if (twofoldCurBaseTarget < 0L) {
      twofoldCurBaseTarget = 153722867000000000L;
    }
    if (newBaseTarget > twofoldCurBaseTarget) {
      newBaseTarget = twofoldCurBaseTarget;
    }
    return newBaseTarget;
  }
  
  static Block getBlock(JSONObject blockData)
  {
    int version = ((Long)blockData.get("version")).intValue();
    int timestamp = ((Long)blockData.get("timestamp")).intValue();
    long previousBlock = Nxt.parseUnsignedLong((String)blockData.get("previousBlock"));
    int numberOfTransactions = ((Long)blockData.get("numberOfTransactions")).intValue();
    int totalAmount = ((Long)blockData.get("totalAmount")).intValue();
    int totalFee = ((Long)blockData.get("totalFee")).intValue();
    int payloadLength = ((Long)blockData.get("payloadLength")).intValue();
    byte[] payloadHash = Nxt.convert((String)blockData.get("payloadHash"));
    byte[] generatorPublicKey = Nxt.convert((String)blockData.get("generatorPublicKey"));
    byte[] generationSignature = Nxt.convert((String)blockData.get("generationSignature"));
    byte[] blockSignature = Nxt.convert((String)blockData.get("blockSignature"));
    
    byte[] previousBlockHash = version == 1 ? null : Nxt.convert((String)blockData.get("previousBlockHash"));
    if ((numberOfTransactions > 255) || (payloadLength > 32640)) {
      return null;
    }
    return new Block(version, timestamp, previousBlock, numberOfTransactions, totalAmount, totalFee, payloadLength, payloadHash, generatorPublicKey, generationSignature, blockSignature, previousBlockHash);
  }
  
  byte[] getBytes()
  {
    ByteBuffer buffer = ByteBuffer.allocate(224);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(this.version);
    buffer.putInt(this.timestamp);
    buffer.putLong(this.previousBlock);
    buffer.putInt(this.transactions.length);
    buffer.putInt(this.totalAmount);
    buffer.putInt(this.totalFee);
    buffer.putInt(this.payloadLength);
    buffer.put(this.payloadHash);
    buffer.put(this.generatorPublicKey);
    
    buffer.put(this.generationSignature);
    if (this.version > 1) {
      buffer.put(this.previousBlockHash);
    }
    buffer.put(this.blockSignature);
    
    return buffer.array();
  }
  
  volatile transient String stringId = null;
  volatile transient long generatorAccountId;
  private transient SoftReference<JSONStreamAware> jsonRef;
  
  long getId()
  {
    calculateIds();
    return this.id;
  }
  
  String getStringId()
  {
    calculateIds();
    return this.stringId;
  }
  
  long getGeneratorAccountId()
  {
    calculateIds();
    return this.generatorAccountId;
  }
  
  private void calculateIds()
  {
    if (this.stringId != null) {
      return;
    }
    byte[] hash = Nxt.getMessageDigest("SHA-256").digest(getBytes());
    BigInteger bigInteger = new BigInteger(1, new byte[] { hash[7], hash[6], hash[5], hash[4], hash[3], hash[2], hash[1], hash[0] });
    this.id = bigInteger.longValue();
    this.stringId = bigInteger.toString();
    this.generatorAccountId = Nxt.Account.getId(this.generatorPublicKey);
  }
  
  JSONObject getJSONObject(Map<Long, Nxt.Transaction> transactions)
  {
    JSONObject block = new JSONObject();
    
    block.put("version", Integer.valueOf(this.version));
    block.put("timestamp", Integer.valueOf(this.timestamp));
    block.put("previousBlock", Nxt.convert(this.previousBlock));
    block.put("numberOfTransactions", Integer.valueOf(this.transactions.length));
    block.put("totalAmount", Integer.valueOf(this.totalAmount));
    block.put("totalFee", Integer.valueOf(this.totalFee));
    block.put("payloadLength", Integer.valueOf(this.payloadLength));
    block.put("payloadHash", Nxt.convert(this.payloadHash));
    block.put("generatorPublicKey", Nxt.convert(this.generatorPublicKey));
    block.put("generationSignature", Nxt.convert(this.generationSignature));
    if (this.version > 1) {
      block.put("previousBlockHash", Nxt.convert(this.previousBlockHash));
    }
    block.put("blockSignature", Nxt.convert(this.blockSignature));
    
    JSONArray transactionsData = new JSONArray();
    for (long transactionId : this.transactions) {
      transactionsData.add(((Nxt.Transaction)transactions.get(Long.valueOf(transactionId))).getJSONObject());
    }
    block.put("transactions", transactionsData);
    
    return block;
  }
  
  synchronized JSONStreamAware getJSONStreamAware()
  {
    if (this.jsonRef != null)
    {
      JSONStreamAware json = (JSONStreamAware)this.jsonRef.get();
      if (json != null) {
        return json;
      }
    }
    JSONStreamAware json = new Nxt.Block.1(this);
    





    this.jsonRef = new SoftReference(json);
    return json;
  }
  
  static ArrayList<Block> getLastBlocks(int numberOfBlocks)
  {
    ArrayList<Block> lastBlocks = new ArrayList(numberOfBlocks);
    
    long curBlock = Nxt.lastBlock;
    do
    {
      Block block = (Block)Nxt.blocks.get(Long.valueOf(curBlock));
      lastBlocks.add(block);
      curBlock = block.previousBlock;
    } while ((lastBlocks.size() < numberOfBlocks) && (curBlock != 0L));
    return lastBlocks;
  }
  
  public static final Comparator<Block> heightComparator = new Nxt.Block.2();
  
  static Block getLastBlock()
  {
    return (Block)Nxt.blocks.get(Long.valueOf(Nxt.lastBlock));
  }
  
  static void loadBlocks(String fileName)
    throws FileNotFoundException
  {
    try
    {
      FileInputStream fileInputStream = new FileInputStream(fileName);Throwable localThrowable3 = null;
      try
      {
        ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);Throwable localThrowable4 = null;
        try
        {
          Nxt.blockCounter.set(objectInputStream.readInt());
          Nxt.blocks.clear();
          Nxt.blocks.putAll((HashMap)objectInputStream.readObject());
          Nxt.lastBlock = objectInputStream.readLong();
        }
        catch (Throwable localThrowable1)
        {
          localThrowable4 = localThrowable1;throw localThrowable1;
        }
        finally {}
      }
      catch (Throwable localThrowable2)
      {
        localThrowable3 = localThrowable2;throw localThrowable2;
      }
      finally
      {
        if (fileInputStream != null) {
          if (localThrowable3 != null) {
            try
            {
              fileInputStream.close();
            }
            catch (Throwable x2)
            {
              localThrowable3.addSuppressed(x2);
            }
          } else {
            fileInputStream.close();
          }
        }
      }
    }
    catch (FileNotFoundException e)
    {
      throw e;
    }
    catch (IOException|ClassNotFoundException e)
    {
      Nxt.logMessage("Error loading blocks from " + fileName, e);
      System.exit(1);
    }
  }
  
  static boolean popLastBlock()
  {
    if (Nxt.lastBlock == 2680262203532249785L) {
      return false;
    }
    try
    {
      response = new JSONObject();
      response.put("response", "processNewData");
      
      JSONArray addedUnconfirmedTransactions = new JSONArray();
      Block block;
      synchronized (Nxt.blocksAndTransactionsLock)
      {
        block = getLastBlock();
        
        Nxt.Account generatorAccount = (Nxt.Account)Nxt.accounts.get(Long.valueOf(block.getGeneratorAccountId()));
        generatorAccount.addToBalanceAndUnconfirmedBalance(-block.totalFee * 100L);
        for (long transactionId : block.transactions)
        {
          Nxt.Transaction transaction = (Nxt.Transaction)Nxt.transactions.remove(Long.valueOf(transactionId));
          Nxt.unconfirmedTransactions.put(Long.valueOf(transactionId), transaction);
          
          Nxt.Account senderAccount = (Nxt.Account)Nxt.accounts.get(Long.valueOf(transaction.getSenderAccountId()));
          senderAccount.addToBalance((transaction.amount + transaction.fee) * 100L);
          
          Nxt.Account recipientAccount = (Nxt.Account)Nxt.accounts.get(Long.valueOf(transaction.recipient));
          recipientAccount.addToBalanceAndUnconfirmedBalance(-transaction.amount * 100L);
          
          JSONObject addedUnconfirmedTransaction = new JSONObject();
          addedUnconfirmedTransaction.put("index", Integer.valueOf(transaction.index));
          addedUnconfirmedTransaction.put("timestamp", Integer.valueOf(transaction.timestamp));
          addedUnconfirmedTransaction.put("deadline", Short.valueOf(transaction.deadline));
          addedUnconfirmedTransaction.put("recipient", Nxt.convert(transaction.recipient));
          addedUnconfirmedTransaction.put("amount", Integer.valueOf(transaction.amount));
          addedUnconfirmedTransaction.put("fee", Integer.valueOf(transaction.fee));
          addedUnconfirmedTransaction.put("sender", Nxt.convert(transaction.getSenderAccountId()));
          addedUnconfirmedTransaction.put("id", transaction.getStringId());
          addedUnconfirmedTransactions.add(addedUnconfirmedTransaction);
        }
        Nxt.lastBlock = block.previousBlock;
      }
      JSONArray addedOrphanedBlocks = new JSONArray();
      JSONObject addedOrphanedBlock = new JSONObject();
      addedOrphanedBlock.put("index", Integer.valueOf(block.index));
      addedOrphanedBlock.put("timestamp", Integer.valueOf(block.timestamp));
      addedOrphanedBlock.put("numberOfTransactions", Integer.valueOf(block.transactions.length));
      addedOrphanedBlock.put("totalAmount", Integer.valueOf(block.totalAmount));
      addedOrphanedBlock.put("totalFee", Integer.valueOf(block.totalFee));
      addedOrphanedBlock.put("payloadLength", Integer.valueOf(block.payloadLength));
      addedOrphanedBlock.put("generator", Nxt.convert(block.getGeneratorAccountId()));
      addedOrphanedBlock.put("height", Integer.valueOf(block.height));
      addedOrphanedBlock.put("version", Integer.valueOf(block.version));
      addedOrphanedBlock.put("block", block.getStringId());
      addedOrphanedBlock.put("baseTarget", BigInteger.valueOf(block.baseTarget).multiply(BigInteger.valueOf(100000L)).divide(BigInteger.valueOf(153722867L)));
      addedOrphanedBlocks.add(addedOrphanedBlock);
      response.put("addedOrphanedBlocks", addedOrphanedBlocks);
      if (addedUnconfirmedTransactions.size() > 0) {
        response.put("addedUnconfirmedTransactions", addedUnconfirmedTransactions);
      }
      for (Nxt.User user : Nxt.users.values()) {
        user.send(response);
      }
    }
    catch (RuntimeException e)
    {
      JSONObject response;
      Nxt.logMessage("Error popping last block", e);
      
      return false;
    }
    return true;
  }
  
  static boolean pushBlock(ByteBuffer buffer, boolean savingFlag)
  {
    buffer.flip();
    
    int version = buffer.getInt();
    if (version != (getLastBlock().height < 30000 ? 1 : 2)) {
      return false;
    }
    if (getLastBlock().height == 30000)
    {
      byte[] checksum = Nxt.Transaction.calculateTransactionsChecksum();
      if (Nxt.CHECKSUM_TRANSPARENT_FORGING == null)
      {
        System.out.println(Arrays.toString(checksum));
      }
      else if (!Arrays.equals(checksum, Nxt.CHECKSUM_TRANSPARENT_FORGING))
      {
        Nxt.logMessage("Checksum failed at block 30000");
        return false;
      }
    }
    int blockTimestamp = buffer.getInt();
    long previousBlock = buffer.getLong();
    int numberOfTransactions = buffer.getInt();
    int totalAmount = buffer.getInt();
    int totalFee = buffer.getInt();
    int payloadLength = buffer.getInt();
    byte[] payloadHash = new byte[32];
    buffer.get(payloadHash);
    byte[] generatorPublicKey = new byte[32];
    buffer.get(generatorPublicKey);
    byte[] previousBlockHash;
    byte[] generationSignature;
    byte[] previousBlockHash;
    if (version == 1)
    {
      byte[] generationSignature = new byte[64];
      buffer.get(generationSignature);
      previousBlockHash = null;
    }
    else
    {
      generationSignature = new byte[32];
      buffer.get(generationSignature);
      previousBlockHash = new byte[32];
      buffer.get(previousBlockHash);
      if (!Arrays.equals(Nxt.getMessageDigest("SHA-256").digest(getLastBlock().getBytes()), previousBlockHash)) {
        return false;
      }
    }
    byte[] blockSignature = new byte[64];
    buffer.get(blockSignature);
    
    int curTime = Nxt.getEpochTime(System.currentTimeMillis());
    if ((blockTimestamp > curTime + 15) || (blockTimestamp <= getLastBlock().timestamp)) {
      return false;
    }
    if ((payloadLength > 32640) || (224 + payloadLength != buffer.capacity()) || (numberOfTransactions > 255)) {
      return false;
    }
    Block block = new Block(version, blockTimestamp, previousBlock, numberOfTransactions, totalAmount, totalFee, payloadLength, payloadHash, generatorPublicKey, generationSignature, blockSignature, previousBlockHash);
    
    block.index = Nxt.blockCounter.incrementAndGet();
    try
    {
      if ((block.transactions.length > 255) || (block.previousBlock != Nxt.lastBlock) || (Nxt.blocks.get(Long.valueOf(block.getId())) != null) || (!block.verifyGenerationSignature()) || (!block.verifyBlockSignature())) {
        return false;
      }
      HashMap<Long, Nxt.Transaction> blockTransactions = new HashMap();
      HashSet<String> blockAliases = new HashSet();
      for (int i = 0; i < block.transactions.length; i++)
      {
        Nxt.Transaction transaction = Nxt.Transaction.getTransaction(buffer);
        transaction.index = Nxt.transactionCounter.incrementAndGet();
        if (blockTransactions.put(Long.valueOf(block.transactions[i] = transaction.getId()), transaction) != null) {
          return false;
        }
        switch (transaction.type)
        {
        case 1: 
          switch (transaction.subtype)
          {
          case 1: 
            if (!blockAliases.add(((Nxt.Transaction.MessagingAliasAssignmentAttachment)transaction.attachment).alias.toLowerCase())) {
              return false;
            }
            break;
          }
          break;
        }
      }
      Arrays.sort(block.transactions);
      
      HashMap<Long, Long> accumulatedAmounts = new HashMap();
      HashMap<Long, HashMap<Long, Long>> accumulatedAssetQuantities = new HashMap();
      int calculatedTotalAmount = 0;int calculatedTotalFee = 0;
      for (int i = 0; i < block.transactions.length; i++)
      {
        Nxt.Transaction transaction = (Nxt.Transaction)blockTransactions.get(Long.valueOf(block.transactions[i]));
        if ((transaction.timestamp > curTime + 15) || (transaction.deadline < 1) || ((transaction.timestamp + transaction.deadline * 60 < blockTimestamp) && (getLastBlock().height > 303)) || (transaction.fee <= 0) || (transaction.fee > 1000000000L) || (transaction.amount < 0) || (transaction.amount > 1000000000L) || (!transaction.validateAttachment()) || (Nxt.transactions.get(Long.valueOf(block.transactions[i])) != null) || ((transaction.referencedTransaction != 0L) && (Nxt.transactions.get(Long.valueOf(transaction.referencedTransaction)) == null) && (blockTransactions.get(Long.valueOf(transaction.referencedTransaction)) == null)) || ((Nxt.unconfirmedTransactions.get(Long.valueOf(block.transactions[i])) == null) && (!transaction.verify()))) {
          break;
        }
        long sender = transaction.getSenderAccountId();
        Long accumulatedAmount = (Long)accumulatedAmounts.get(Long.valueOf(sender));
        if (accumulatedAmount == null) {
          accumulatedAmount = Long.valueOf(0L);
        }
        accumulatedAmounts.put(Long.valueOf(sender), Long.valueOf(accumulatedAmount.longValue() + (transaction.amount + transaction.fee) * 100L));
        if (transaction.type == 0)
        {
          if (transaction.subtype != 0) {
            break;
          }
          calculatedTotalAmount += transaction.amount;
        }
        else if (transaction.type == 1)
        {
          if ((transaction.subtype != 0) && (transaction.subtype != 1)) {
            break;
          }
        }
        else
        {
          if (transaction.type != 2) {
            break;
          }
          if (transaction.subtype == 1)
          {
            Nxt.Transaction.ColoredCoinsAssetTransferAttachment attachment = (Nxt.Transaction.ColoredCoinsAssetTransferAttachment)transaction.attachment;
            HashMap<Long, Long> accountAccumulatedAssetQuantities = (HashMap)accumulatedAssetQuantities.get(Long.valueOf(sender));
            if (accountAccumulatedAssetQuantities == null)
            {
              accountAccumulatedAssetQuantities = new HashMap();
              accumulatedAssetQuantities.put(Long.valueOf(sender), accountAccumulatedAssetQuantities);
            }
            Long assetAccumulatedAssetQuantities = (Long)accountAccumulatedAssetQuantities.get(Long.valueOf(attachment.asset));
            if (assetAccumulatedAssetQuantities == null) {
              assetAccumulatedAssetQuantities = Long.valueOf(0L);
            }
            accountAccumulatedAssetQuantities.put(Long.valueOf(attachment.asset), Long.valueOf(assetAccumulatedAssetQuantities.longValue() + attachment.quantity));
          }
          else if (transaction.subtype == 2)
          {
            Nxt.Transaction.ColoredCoinsAskOrderPlacementAttachment attachment = (Nxt.Transaction.ColoredCoinsAskOrderPlacementAttachment)transaction.attachment;
            HashMap<Long, Long> accountAccumulatedAssetQuantities = (HashMap)accumulatedAssetQuantities.get(Long.valueOf(sender));
            if (accountAccumulatedAssetQuantities == null)
            {
              accountAccumulatedAssetQuantities = new HashMap();
              accumulatedAssetQuantities.put(Long.valueOf(sender), accountAccumulatedAssetQuantities);
            }
            Long assetAccumulatedAssetQuantities = (Long)accountAccumulatedAssetQuantities.get(Long.valueOf(attachment.asset));
            if (assetAccumulatedAssetQuantities == null) {
              assetAccumulatedAssetQuantities = Long.valueOf(0L);
            }
            accountAccumulatedAssetQuantities.put(Long.valueOf(attachment.asset), Long.valueOf(assetAccumulatedAssetQuantities.longValue() + attachment.quantity));
          }
          else if (transaction.subtype == 3)
          {
            Nxt.Transaction.ColoredCoinsBidOrderPlacementAttachment attachment = (Nxt.Transaction.ColoredCoinsBidOrderPlacementAttachment)transaction.attachment;
            accumulatedAmounts.put(Long.valueOf(sender), Long.valueOf(accumulatedAmount.longValue() + attachment.quantity * attachment.price));
          }
          else
          {
            if ((transaction.subtype != 0) && (transaction.subtype != 4) && (transaction.subtype != 5)) {
              break;
            }
          }
        }
        calculatedTotalFee += transaction.fee;
      }
      if ((i != block.transactions.length) || (calculatedTotalAmount != block.totalAmount) || (calculatedTotalFee != block.totalFee)) {
        return false;
      }
      MessageDigest digest = Nxt.getMessageDigest("SHA-256");
      for (long transactionId : block.transactions) {
        digest.update(((Nxt.Transaction)blockTransactions.get(Long.valueOf(transactionId))).getBytes());
      }
      if (!Arrays.equals(digest.digest(), block.payloadHash)) {
        return false;
      }
      JSONArray addedConfirmedTransactions;
      JSONArray removedUnconfirmedTransactions;
      synchronized (Nxt.blocksAndTransactionsLock)
      {
        for (Map.Entry<Long, Long> accumulatedAmountEntry : accumulatedAmounts.entrySet())
        {
          Nxt.Account senderAccount = (Nxt.Account)Nxt.accounts.get(accumulatedAmountEntry.getKey());
          if (senderAccount.getBalance() < ((Long)accumulatedAmountEntry.getValue()).longValue()) {
            return false;
          }
        }
        for (Map.Entry<Long, HashMap<Long, Long>> accumulatedAssetQuantitiesEntry : accumulatedAssetQuantities.entrySet())
        {
          senderAccount = (Nxt.Account)Nxt.accounts.get(accumulatedAssetQuantitiesEntry.getKey());
          for (Map.Entry<Long, Long> accountAccumulatedAssetQuantitiesEntry : ((HashMap)accumulatedAssetQuantitiesEntry.getValue()).entrySet())
          {
            long asset = ((Long)accountAccumulatedAssetQuantitiesEntry.getKey()).longValue();
            long quantity = ((Long)accountAccumulatedAssetQuantitiesEntry.getValue()).longValue();
            if (senderAccount.getAssetBalance(Long.valueOf(asset)).intValue() < quantity) {
              return false;
            }
          }
        }
        Nxt.Account senderAccount;
        if (block.previousBlock != Nxt.lastBlock) {
          return false;
        }
        block.height = (getLastBlock().height + 1);
        for (Map.Entry<Long, Nxt.Transaction> transactionEntry : blockTransactions.entrySet())
        {
          Nxt.Transaction transaction = (Nxt.Transaction)transactionEntry.getValue();
          transaction.height = block.height;
          if (Nxt.transactions.putIfAbsent(transactionEntry.getKey(), transaction) != null)
          {
            Nxt.logMessage("duplicate transaction id " + transactionEntry.getKey());
            return false;
          }
        }
        block.analyze();
        
        addedConfirmedTransactions = new JSONArray();
        removedUnconfirmedTransactions = new JSONArray();
        for (Map.Entry<Long, Nxt.Transaction> transactionEntry : blockTransactions.entrySet())
        {
          Nxt.Transaction transaction = (Nxt.Transaction)transactionEntry.getValue();
          
          JSONObject addedConfirmedTransaction = new JSONObject();
          addedConfirmedTransaction.put("index", Integer.valueOf(transaction.index));
          addedConfirmedTransaction.put("blockTimestamp", Integer.valueOf(block.timestamp));
          addedConfirmedTransaction.put("transactionTimestamp", Integer.valueOf(transaction.timestamp));
          addedConfirmedTransaction.put("sender", Nxt.convert(transaction.getSenderAccountId()));
          addedConfirmedTransaction.put("recipient", Nxt.convert(transaction.recipient));
          addedConfirmedTransaction.put("amount", Integer.valueOf(transaction.amount));
          addedConfirmedTransaction.put("fee", Integer.valueOf(transaction.fee));
          addedConfirmedTransaction.put("id", transaction.getStringId());
          addedConfirmedTransactions.add(addedConfirmedTransaction);
          
          Nxt.Transaction removedTransaction = (Nxt.Transaction)Nxt.unconfirmedTransactions.remove(transactionEntry.getKey());
          if (removedTransaction != null)
          {
            JSONObject removedUnconfirmedTransaction = new JSONObject();
            removedUnconfirmedTransaction.put("index", Integer.valueOf(removedTransaction.index));
            removedUnconfirmedTransactions.add(removedUnconfirmedTransaction);
            
            Nxt.Account senderAccount = (Nxt.Account)Nxt.accounts.get(Long.valueOf(removedTransaction.getSenderAccountId()));
            senderAccount.addToUnconfirmedBalance((removedTransaction.amount + removedTransaction.fee) * 100L);
          }
        }
        long blockId = block.getId();
        for (long transactionId : block.transactions) {
          ((Nxt.Transaction)Nxt.transactions.get(Long.valueOf(transactionId))).block = blockId;
        }
        if (savingFlag)
        {
          Nxt.Transaction.saveTransactions("transactions.nxt");
          saveBlocks("blocks.nxt", false);
        }
      }
      if (block.timestamp >= curTime - 15)
      {
        JSONObject request = block.getJSONObject(Nxt.transactions);
        request.put("requestType", "processBlock");
        
        Nxt.Peer.sendToSomePeers(request);
      }
      JSONArray addedRecentBlocks = new JSONArray();
      JSONObject addedRecentBlock = new JSONObject();
      addedRecentBlock.put("index", Integer.valueOf(block.index));
      addedRecentBlock.put("timestamp", Integer.valueOf(block.timestamp));
      addedRecentBlock.put("numberOfTransactions", Integer.valueOf(block.transactions.length));
      addedRecentBlock.put("totalAmount", Integer.valueOf(block.totalAmount));
      addedRecentBlock.put("totalFee", Integer.valueOf(block.totalFee));
      addedRecentBlock.put("payloadLength", Integer.valueOf(block.payloadLength));
      addedRecentBlock.put("generator", Nxt.convert(block.getGeneratorAccountId()));
      addedRecentBlock.put("height", Integer.valueOf(block.height));
      addedRecentBlock.put("version", Integer.valueOf(block.version));
      addedRecentBlock.put("block", block.getStringId());
      addedRecentBlock.put("baseTarget", BigInteger.valueOf(block.baseTarget).multiply(BigInteger.valueOf(100000L)).divide(BigInteger.valueOf(153722867L)));
      addedRecentBlocks.add(addedRecentBlock);
      
      JSONObject response = new JSONObject();
      response.put("response", "processNewData");
      response.put("addedConfirmedTransactions", addedConfirmedTransactions);
      if (removedUnconfirmedTransactions.size() > 0) {
        response.put("removedUnconfirmedTransactions", removedUnconfirmedTransactions);
      }
      response.put("addedRecentBlocks", addedRecentBlocks);
      for (Nxt.User user : Nxt.users.values()) {
        user.send(response);
      }
      return true;
    }
    catch (RuntimeException e)
    {
      Nxt.logMessage("Error pushing block", e);
    }
    return false;
  }
  
  static void saveBlocks(String fileName, boolean flag)
  {
    try
    {
      FileOutputStream fileOutputStream = new FileOutputStream(fileName);Throwable localThrowable3 = null;
      try
      {
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);Throwable localThrowable4 = null;
        try
        {
          objectOutputStream.writeInt(Nxt.blockCounter.get());
          objectOutputStream.writeObject(new HashMap(Nxt.blocks));
          objectOutputStream.writeLong(Nxt.lastBlock);
        }
        catch (Throwable localThrowable1)
        {
          localThrowable4 = localThrowable1;throw localThrowable1;
        }
        finally {}
      }
      catch (Throwable localThrowable2)
      {
        localThrowable3 = localThrowable2;throw localThrowable2;
      }
      finally
      {
        if (fileOutputStream != null) {
          if (localThrowable3 != null) {
            try
            {
              fileOutputStream.close();
            }
            catch (Throwable x2)
            {
              localThrowable3.addSuppressed(x2);
            }
          } else {
            fileOutputStream.close();
          }
        }
      }
    }
    catch (IOException e)
    {
      Nxt.logMessage("Error saving blocks to " + fileName, e);
      throw new RuntimeException(e);
    }
  }
  
  boolean verifyBlockSignature()
  {
    Nxt.Account account = (Nxt.Account)Nxt.accounts.get(Long.valueOf(getGeneratorAccountId()));
    if (account == null) {
      return false;
    }
    byte[] data = getBytes();
    byte[] data2 = new byte[data.length - 64];
    System.arraycopy(data, 0, data2, 0, data2.length);
    
    return (Nxt.Crypto.verify(this.blockSignature, data2, this.generatorPublicKey)) && (account.setOrVerify(this.generatorPublicKey));
  }
  
  boolean verifyGenerationSignature()
  {
    try
    {
      Block previousBlock = (Block)Nxt.blocks.get(Long.valueOf(this.previousBlock));
      if (previousBlock == null) {
        return false;
      }
      if ((this.version == 1) && (!Nxt.Crypto.verify(this.generationSignature, previousBlock.generationSignature, this.generatorPublicKey))) {
        return false;
      }
      Nxt.Account account = (Nxt.Account)Nxt.accounts.get(Long.valueOf(getGeneratorAccountId()));
      if ((account == null) || (account.getEffectiveBalance() <= 0)) {
        return false;
      }
      int elapsedTime = this.timestamp - previousBlock.timestamp;
      BigInteger target = BigInteger.valueOf(getBaseTarget()).multiply(BigInteger.valueOf(account.getEffectiveBalance())).multiply(BigInteger.valueOf(elapsedTime));
      
      MessageDigest digest = Nxt.getMessageDigest("SHA-256");
      byte[] generationSignatureHash;
      byte[] generationSignatureHash;
      if (this.version == 1)
      {
        generationSignatureHash = digest.digest(this.generationSignature);
      }
      else
      {
        digest.update(previousBlock.generationSignature);
        generationSignatureHash = digest.digest(this.generatorPublicKey);
        if (!Arrays.equals(this.generationSignature, generationSignatureHash)) {
          return false;
        }
      }
      BigInteger hit = new BigInteger(1, new byte[] { generationSignatureHash[7], generationSignatureHash[6], generationSignatureHash[5], generationSignatureHash[4], generationSignatureHash[3], generationSignatureHash[2], generationSignatureHash[1], generationSignatureHash[0] });
      
      return hit.compareTo(target) < 0;
    }
