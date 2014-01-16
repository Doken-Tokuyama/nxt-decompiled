import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import org.json.simple.JSONObject;

class Nxt$Account
{
  final long id;
  private long balance;
  final int height;
  final AtomicReference<byte[]> publicKey = new AtomicReference();
  private final Map<Long, Integer> assetBalances = new HashMap();
  private long unconfirmedBalance;
  private final Map<Long, Integer> unconfirmedAssetBalances = new HashMap();
  
  private Nxt$Account(long id)
  {
    this.id = id;
    this.height = Nxt.Block.getLastBlock().height;
  }
  
  static Account addAccount(long id)
  {
    Account account = new Account(id);
    Nxt.accounts.put(Long.valueOf(id), account);
    
    return account;
  }
  
  boolean setOrVerify(byte[] key)
  {
    return (this.publicKey.compareAndSet(null, key)) || (Arrays.equals(key, (byte[])this.publicKey.get()));
  }
  
  void generateBlock(String secretPhrase)
  {
    Set<Nxt.Transaction> sortedTransactions = new TreeSet();
    for (Nxt.Transaction transaction : Nxt.unconfirmedTransactions.values()) {
      if ((transaction.referencedTransaction == 0L) || (Nxt.transactions.get(Long.valueOf(transaction.referencedTransaction)) != null)) {
        sortedTransactions.add(transaction);
      }
    }
    Map<Long, Nxt.Transaction> newTransactions = new HashMap();
    Set<String> newAliases = new HashSet();
    Map<Long, Long> accumulatedAmounts = new HashMap();
    int payloadLength = 0;
    while (payloadLength <= 32640)
    {
      int prevNumberOfNewTransactions = newTransactions.size();
      for (Nxt.Transaction transaction : sortedTransactions)
      {
        int transactionLength = transaction.getSize();
        if ((newTransactions.get(Long.valueOf(transaction.getId())) == null) && (payloadLength + transactionLength <= 32640))
        {
          long sender = transaction.getSenderAccountId();
          Long accumulatedAmount = (Long)accumulatedAmounts.get(Long.valueOf(sender));
          if (accumulatedAmount == null) {
            accumulatedAmount = Long.valueOf(0L);
          }
          long amount = (transaction.amount + transaction.fee) * 100L;
          if ((accumulatedAmount.longValue() + amount <= ((Account)Nxt.accounts.get(Long.valueOf(sender))).getBalance()) && (transaction.validateAttachment())) {
            switch (transaction.type)
            {
            case 1: 
              switch (transaction.subtype)
              {
              case 1: 
                if (!newAliases.add(((Nxt.Transaction.MessagingAliasAssignmentAttachment)transaction.attachment).alias.toLowerCase())) {}
                break;
              }
            default: 
              accumulatedAmounts.put(Long.valueOf(sender), Long.valueOf(accumulatedAmount.longValue() + amount));
              
              newTransactions.put(Long.valueOf(transaction.getId()), transaction);
              payloadLength += transactionLength;
            }
          }
        }
      }
      if (newTransactions.size() == prevNumberOfNewTransactions) {
        break;
      }
    }
    Nxt.Block block;
    Nxt.Block block;
    if (Nxt.Block.getLastBlock().height < 30000)
    {
      block = new Nxt.Block(1, Nxt.getEpochTime(System.currentTimeMillis()), Nxt.lastBlock, newTransactions.size(), 0, 0, 0, null, Nxt.Crypto.getPublicKey(secretPhrase), null, new byte[64]);
    }
    else
    {
      byte[] previousBlockHash = Nxt.getMessageDigest("SHA-256").digest(Nxt.Block.getLastBlock().getBytes());
      block = new Nxt.Block(2, Nxt.getEpochTime(System.currentTimeMillis()), Nxt.lastBlock, newTransactions.size(), 0, 0, 0, null, Nxt.Crypto.getPublicKey(secretPhrase), null, new byte[64], previousBlockHash);
    }
    int i = 0;
    for (Map.Entry<Long, Nxt.Transaction> transactionEntry : newTransactions.entrySet())
    {
      Nxt.Transaction transaction = (Nxt.Transaction)transactionEntry.getValue();
      block.totalAmount += transaction.amount;
      block.totalFee += transaction.fee;
      block.payloadLength += transaction.getSize();
      block.transactions[(i++)] = ((Long)transactionEntry.getKey()).longValue();
    }
    Arrays.sort(block.transactions);
    MessageDigest digest = Nxt.getMessageDigest("SHA-256");
    for (long transactionId : block.transactions) {
      digest.update(((Nxt.Transaction)newTransactions.get(Long.valueOf(transactionId))).getBytes());
    }
    block.payloadHash = digest.digest();
    if (Nxt.Block.getLastBlock().height < 30000)
    {
      block.generationSignature = Nxt.Crypto.sign(Nxt.Block.getLastBlock().generationSignature, secretPhrase);
    }
    else
    {
      digest.update(Nxt.Block.getLastBlock().generationSignature);
      block.generationSignature = digest.digest(Nxt.Crypto.getPublicKey(secretPhrase));
    }
    byte[] data = block.getBytes();
    byte[] data2 = new byte[data.length - 64];
    System.arraycopy(data, 0, data2, 0, data2.length);
    block.blockSignature = Nxt.Crypto.sign(data2, secretPhrase);
    if ((block.verifyBlockSignature()) && (block.verifyGenerationSignature()))
    {
      JSONObject request = block.getJSONObject(newTransactions);
      request.put("requestType", "processBlock");
      Nxt.Peer.sendToSomePeers(request);
    }
    else
    {
      Nxt.logMessage("Generated an incorrect block. Waiting for the next one...");
    }
  }
  
  int getEffectiveBalance()
  {
    if (this.height < 47000)
    {
      if (this.height == 0) {
        return (int)(getBalance() / 100L);
      }
      if (Nxt.Block.getLastBlock().height - this.height < 1440) {
        return 0;
      }
      int amount = 0;
      for (long transactionId : Nxt.Block.getLastBlock().transactions)
      {
        Nxt.Transaction transaction = (Nxt.Transaction)Nxt.transactions.get(Long.valueOf(transactionId));
        if (transaction.recipient == this.id) {
          amount += transaction.amount;
        }
      }
      return (int)(getBalance() / 100L) - amount;
    }
    return (int)(getGuaranteedBalance(1440) / 100L);
  }
  
  static long getId(byte[] publicKey)
  {
    byte[] publicKeyHash = Nxt.getMessageDigest("SHA-256").digest(publicKey);
    BigInteger bigInteger = new BigInteger(1, new byte[] { publicKeyHash[7], publicKeyHash[6], publicKeyHash[5], publicKeyHash[4], publicKeyHash[3], publicKeyHash[2], publicKeyHash[1], publicKeyHash[0] });
    return bigInteger.longValue();
  }
  
  synchronized Integer getAssetBalance(Long assetId)
  {
    return (Integer)this.assetBalances.get(assetId);
  }
  
  synchronized Integer getUnconfirmedAssetBalance(Long assetId)
  {
    return (Integer)this.unconfirmedAssetBalances.get(assetId);
  }
  
  synchronized void addToAssetBalance(Long assetId, int quantity)
  {
    Integer assetBalance = (Integer)this.assetBalances.get(assetId);
    if (assetBalance == null) {
      this.assetBalances.put(assetId, Integer.valueOf(quantity));
    } else {
      this.assetBalances.put(assetId, Integer.valueOf(assetBalance.intValue() + quantity));
    }
  }
  
  synchronized void addToUnconfirmedAssetBalance(Long assetId, int quantity)
  {
    Integer unconfirmedAssetBalance = (Integer)this.unconfirmedAssetBalances.get(assetId);
    if (unconfirmedAssetBalance == null) {
      this.unconfirmedAssetBalances.put(assetId, Integer.valueOf(quantity));
    } else {
      this.unconfirmedAssetBalances.put(assetId, Integer.valueOf(unconfirmedAssetBalance.intValue() + quantity));
    }
  }
  
  synchronized void addToAssetAndUnconfirmedAssetBalance(Long assetId, int quantity)
  {
    Integer assetBalance = (Integer)this.assetBalances.get(assetId);
    if (assetBalance == null)
    {
      this.assetBalances.put(assetId, Integer.valueOf(quantity));
      this.unconfirmedAssetBalances.put(assetId, Integer.valueOf(quantity));
    }
    else
    {
      this.assetBalances.put(assetId, Integer.valueOf(assetBalance.intValue() + quantity));
      this.unconfirmedAssetBalances.put(assetId, Integer.valueOf(((Integer)this.unconfirmedAssetBalances.get(assetId)).intValue() + quantity));
    }
  }
  
  synchronized long getBalance()
  {
    return this.balance;
  }
  
  long getGuaranteedBalance(int numberOfConfirmations)
  {
    long guaranteedBalance = getBalance();
    ArrayList<Nxt.Block> lastBlocks = Nxt.Block.getLastBlocks(numberOfConfirmations - 1);
    byte[] accountPublicKey = (byte[])this.publicKey.get();
    for (Iterator i$ = lastBlocks.iterator(); i$.hasNext();)
    {
      block = (Nxt.Block)i$.next();
      if (Arrays.equals(block.generatorPublicKey, accountPublicKey)) {
        if (guaranteedBalance -= block.totalFee * 100L <= 0L) {
          return 0L;
        }
      }
      for (i = block.transactions.length; i-- > 0;)
      {
        Nxt.Transaction transaction = (Nxt.Transaction)Nxt.transactions.get(Long.valueOf(block.transactions[i]));
        if (Arrays.equals(transaction.senderPublicKey, accountPublicKey))
        {
          long deltaBalance = transaction.getSenderDeltaBalance();
          if ((deltaBalance > 0L) && (guaranteedBalance -= deltaBalance <= 0L)) {
            return 0L;
          }
          if ((deltaBalance < 0L) && (guaranteedBalance += deltaBalance <= 0L)) {
            return 0L;
          }
        }
        if (transaction.recipient == this.id)
        {
          long deltaBalance = transaction.getRecipientDeltaBalance();
          if ((deltaBalance > 0L) && (guaranteedBalance -= deltaBalance <= 0L)) {
            return 0L;
          }
          if ((deltaBalance < 0L) && (guaranteedBalance += deltaBalance <= 0L)) {
            return 0L;
          }
        }
      }
    }
    Nxt.Block block;
    int i;
    return guaranteedBalance;
  }
  
  synchronized long getUnconfirmedBalance()
  {
    return this.unconfirmedBalance;
  }
  
  void addToBalance(long amount)
  {
    synchronized (this)
    {
      this.balance += amount;
    }
    updatePeerWeights();
  }
  
  void addToUnconfirmedBalance(long amount)
  {
    synchronized (this)
    {
      this.unconfirmedBalance += amount;
    }
    updateUserUnconfirmedBalance();
  }
  
  void addToBalanceAndUnconfirmedBalance(long amount)
  {
    synchronized (this)
    {
      this.balance += amount;
      this.unconfirmedBalance += amount;
    }
    updatePeerWeights();
    updateUserUnconfirmedBalance();
  }
  
  private void updatePeerWeights()
  {
    for (Nxt.Peer peer : Nxt.peers.values()) {
      if ((peer.accountId == this.id) && (peer.adjustedWeight > 0L)) {
        peer.updateWeight();
      }
    }
  }
  
  private void updateUserUnconfirmedBalance()
  {
    JSONObject response = new JSONObject();
    response.put("response", "setBalance");
    response.put("balance", Long.valueOf(getUnconfirmedBalance()));
    byte[] accountPublicKey = (byte[])this.publicKey.get();
