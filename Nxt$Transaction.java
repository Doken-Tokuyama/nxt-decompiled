import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

class Nxt$Transaction
  implements Comparable<Transaction>, Serializable
{
  static final long serialVersionUID = 0L;
  static final byte TYPE_PAYMENT = 0;
  static final byte TYPE_MESSAGING = 1;
  static final byte TYPE_COLORED_COINS = 2;
  static final byte SUBTYPE_PAYMENT_ORDINARY_PAYMENT = 0;
  static final byte SUBTYPE_MESSAGING_ARBITRARY_MESSAGE = 0;
  static final byte SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT = 1;
  static final byte SUBTYPE_COLORED_COINS_ASSET_ISSUANCE = 0;
  static final byte SUBTYPE_COLORED_COINS_ASSET_TRANSFER = 1;
  static final byte SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT = 2;
  static final byte SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT = 3;
  static final byte SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION = 4;
  static final byte SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION = 5;
  static final int ASSET_ISSUANCE_FEE = 1000;
  final byte type;
  final byte subtype;
  int timestamp;
  final short deadline;
  final byte[] senderPublicKey;
  final long recipient;
  final int amount;
  final int fee;
  final long referencedTransaction;
  byte[] signature;
  Nxt.Transaction.Attachment attachment;
  int index;
  volatile long block;
  int height;
  
  Nxt$Transaction(byte type, byte subtype, int timestamp, short deadline, byte[] senderPublicKey, long recipient, int amount, int fee, long referencedTransaction, byte[] signature)
  {
    this.type = type;
    this.subtype = subtype;
    this.timestamp = timestamp;
    this.deadline = deadline;
    this.senderPublicKey = senderPublicKey;
    this.recipient = recipient;
    this.amount = amount;
    this.fee = fee;
    this.referencedTransaction = referencedTransaction;
    this.signature = signature;
    
    this.height = 2147483647;
  }
  
  public int compareTo(Transaction o)
  {
    if (this.height < o.height) {
      return -1;
    }
    if (this.height > o.height) {
      return 1;
    }
    if (this.fee * o.getSize() > o.fee * getSize()) {
      return -1;
    }
    if (this.fee * o.getSize() < o.fee * getSize()) {
      return 1;
    }
    if (this.timestamp < o.timestamp) {
      return -1;
    }
    if (this.timestamp > o.timestamp) {
      return 1;
    }
    if (this.index < o.index) {
      return -1;
    }
    if (this.index > o.index) {
      return 1;
    }
    return 0;
  }
  
  public static final Comparator<Transaction> timestampComparator = new Nxt.Transaction.1();
  private static final int TRANSACTION_BYTES_LENGTH = 128;
  volatile transient long id;
  
  int getSize()
  {
    return 128 + (this.attachment == null ? 0 : this.attachment.getSize());
  }
  
  byte[] getBytes()
  {
    ByteBuffer buffer = ByteBuffer.allocate(getSize());
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.put(this.type);
    buffer.put(this.subtype);
    buffer.putInt(this.timestamp);
    buffer.putShort(this.deadline);
    buffer.put(this.senderPublicKey);
    buffer.putLong(this.recipient);
    buffer.putInt(this.amount);
    buffer.putInt(this.fee);
    buffer.putLong(this.referencedTransaction);
    buffer.put(this.signature);
    if (this.attachment != null) {
      buffer.put(this.attachment.getBytes());
    }
    return buffer.array();
  }
  
  volatile transient String stringId = null;
  volatile transient long senderAccountId;
  
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
  
  long getSenderAccountId()
  {
    calculateIds();
    return this.senderAccountId;
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
    this.senderAccountId = Nxt.Account.getId(this.senderPublicKey);
  }
  
  JSONObject getJSONObject()
  {
    JSONObject transaction = new JSONObject();
    
    transaction.put("type", Byte.valueOf(this.type));
    transaction.put("subtype", Byte.valueOf(this.subtype));
    transaction.put("timestamp", Integer.valueOf(this.timestamp));
    transaction.put("deadline", Short.valueOf(this.deadline));
    transaction.put("senderPublicKey", Nxt.convert(this.senderPublicKey));
    transaction.put("recipient", Nxt.convert(this.recipient));
    transaction.put("amount", Integer.valueOf(this.amount));
    transaction.put("fee", Integer.valueOf(this.fee));
    transaction.put("referencedTransaction", Nxt.convert(this.referencedTransaction));
    transaction.put("signature", Nxt.convert(this.signature));
    if (this.attachment != null) {
      transaction.put("attachment", this.attachment.getJSONObject());
    }
    return transaction;
  }
  
  long getRecipientDeltaBalance()
  {
    return this.amount * 100L + (this.attachment == null ? 0L : this.attachment.getRecipientDeltaBalance());
  }
  
  long getSenderDeltaBalance()
  {
    return -(this.amount + this.fee) * 100L + (this.attachment == null ? 0L : this.attachment.getSenderDeltaBalance());
  }
  
  static Transaction getTransaction(ByteBuffer buffer)
  {
    byte type = buffer.get();
    byte subtype = buffer.get();
    int timestamp = buffer.getInt();
    short deadline = buffer.getShort();
    byte[] senderPublicKey = new byte[32];
    buffer.get(senderPublicKey);
    long recipient = buffer.getLong();
    int amount = buffer.getInt();
    int fee = buffer.getInt();
    long referencedTransaction = buffer.getLong();
    byte[] signature = new byte[64];
    buffer.get(signature);
    
    Transaction transaction = new Transaction(type, subtype, timestamp, deadline, senderPublicKey, recipient, amount, fee, referencedTransaction, signature);
    switch (type)
    {
    case 1: 
      switch (subtype)
      {
      case 0: 
        int messageLength = buffer.getInt();
        if (messageLength <= 1000)
        {
          byte[] message = new byte[messageLength];
          buffer.get(message);
          
          transaction.attachment = new Nxt.Transaction.MessagingArbitraryMessageAttachment(message);
        }
        break;
      case 1: 
        int aliasLength = buffer.get();
        byte[] alias = new byte[aliasLength];
        buffer.get(alias);
        int uriLength = buffer.getShort();
        byte[] uri = new byte[uriLength];
        buffer.get(uri);
        try
        {
          transaction.attachment = new Nxt.Transaction.MessagingAliasAssignmentAttachment(new String(alias, "UTF-8"), new String(uri, "UTF-8"));
        }
        catch (RuntimeException|UnsupportedEncodingException e)
        {
          Nxt.logDebugMessage("Error parsing alias assignment", e);
        }
      }
      break;
    case 2: 
      switch (subtype)
      {
      case 0: 
        int nameLength = buffer.get();
        byte[] name = new byte[nameLength];
        buffer.get(name);
        int descriptionLength = buffer.getShort();
        byte[] description = new byte[descriptionLength];
        buffer.get(description);
        int quantity = buffer.getInt();
        try
        {
          transaction.attachment = new Nxt.Transaction.ColoredCoinsAssetIssuanceAttachment(new String(name, "UTF-8"), new String(description, "UTF-8"), quantity);
        }
        catch (RuntimeException|UnsupportedEncodingException e)
        {
          Nxt.logDebugMessage("Error in asset issuance", e);
        }
        break;
      case 1: 
        long asset = buffer.getLong();
        int quantity = buffer.getInt();
        
        transaction.attachment = new Nxt.Transaction.ColoredCoinsAssetTransferAttachment(asset, quantity);
        

        break;
      case 2: 
        long asset = buffer.getLong();
        int quantity = buffer.getInt();
        long price = buffer.getLong();
        
        transaction.attachment = new Nxt.Transaction.ColoredCoinsAskOrderPlacementAttachment(asset, quantity, price);
        

        break;
      case 3: 
        long asset = buffer.getLong();
        int quantity = buffer.getInt();
        long price = buffer.getLong();
        
        transaction.attachment = new Nxt.Transaction.ColoredCoinsBidOrderPlacementAttachment(asset, quantity, price);
        

        break;
      case 4: 
        long order = buffer.getLong();
        
        transaction.attachment = new Nxt.Transaction.ColoredCoinsAskOrderCancellationAttachment(order);
        

        break;
      case 5: 
        long order = buffer.getLong();
        
        transaction.attachment = new Nxt.Transaction.ColoredCoinsBidOrderCancellationAttachment(order);
      }
      break;
    }
    return transaction;
  }
  
  static Transaction getTransaction(JSONObject transactionData)
  {
    byte type = ((Long)transactionData.get("type")).byteValue();
    byte subtype = ((Long)transactionData.get("subtype")).byteValue();
    int timestamp = ((Long)transactionData.get("timestamp")).intValue();
    short deadline = ((Long)transactionData.get("deadline")).shortValue();
    byte[] senderPublicKey = Nxt.convert((String)transactionData.get("senderPublicKey"));
    long recipient = Nxt.parseUnsignedLong((String)transactionData.get("recipient"));
    int amount = ((Long)transactionData.get("amount")).intValue();
    int fee = ((Long)transactionData.get("fee")).intValue();
    long referencedTransaction = Nxt.parseUnsignedLong((String)transactionData.get("referencedTransaction"));
    byte[] signature = Nxt.convert((String)transactionData.get("signature"));
    
    Transaction transaction = new Transaction(type, subtype, timestamp, deadline, senderPublicKey, recipient, amount, fee, referencedTransaction, signature);
    
    JSONObject attachmentData = (JSONObject)transactionData.get("attachment");
    switch (type)
    {
    case 1: 
      switch (subtype)
      {
      case 0: 
        String message = (String)attachmentData.get("message");
        transaction.attachment = new Nxt.Transaction.MessagingArbitraryMessageAttachment(Nxt.convert(message));
        

        break;
      case 1: 
        String alias = (String)attachmentData.get("alias");
        String uri = (String)attachmentData.get("uri");
        transaction.attachment = new Nxt.Transaction.MessagingAliasAssignmentAttachment(alias.trim(), uri.trim());
      }
      break;
    case 2: 
      switch (subtype)
      {
      case 0: 
        String name = (String)attachmentData.get("name");
        String description = (String)attachmentData.get("description");
        int quantity = ((Long)attachmentData.get("quantity")).intValue();
        transaction.attachment = new Nxt.Transaction.ColoredCoinsAssetIssuanceAttachment(name.trim(), description.trim(), quantity);
        

        break;
      case 1: 
        long asset = Nxt.parseUnsignedLong((String)attachmentData.get("asset"));
        int quantity = ((Long)attachmentData.get("quantity")).intValue();
        transaction.attachment = new Nxt.Transaction.ColoredCoinsAssetTransferAttachment(asset, quantity);
        

        break;
      case 2: 
        long asset = Nxt.parseUnsignedLong((String)attachmentData.get("asset"));
        int quantity = ((Long)attachmentData.get("quantity")).intValue();
        long price = ((Long)attachmentData.get("price")).longValue();
        transaction.attachment = new Nxt.Transaction.ColoredCoinsAskOrderPlacementAttachment(asset, quantity, price);
        

        break;
      case 3: 
        long asset = Nxt.parseUnsignedLong((String)attachmentData.get("asset"));
        int quantity = ((Long)attachmentData.get("quantity")).intValue();
        long price = ((Long)attachmentData.get("price")).longValue();
        transaction.attachment = new Nxt.Transaction.ColoredCoinsBidOrderPlacementAttachment(asset, quantity, price);
        

        break;
      case 4: 
        transaction.attachment = new Nxt.Transaction.ColoredCoinsAskOrderCancellationAttachment(Nxt.parseUnsignedLong((String)attachmentData.get("order")));
        

        break;
      case 5: 
        transaction.attachment = new Nxt.Transaction.ColoredCoinsBidOrderCancellationAttachment(Nxt.parseUnsignedLong((String)attachmentData.get("order")));
      }
      break;
    }
    return transaction;
  }
  
  static void loadTransactions(String fileName)
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
          Nxt.transactionCounter.set(objectInputStream.readInt());
          Nxt.transactions.clear();
          Nxt.transactions.putAll((HashMap)objectInputStream.readObject());
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
      Nxt.logMessage("Error loading transactions from " + fileName, e);
      System.exit(1);
    }
  }
  
  static void processTransactions(JSONObject request, String parameterName)
  {
    JSONArray transactionsData = (JSONArray)request.get(parameterName);
    JSONArray validTransactionsData = new JSONArray();
    for (Object transactionData : transactionsData)
    {
      Transaction transaction = getTransaction((JSONObject)transactionData);
      try
      {
        int curTime = Nxt.getEpochTime(System.currentTimeMillis());
        if ((transaction.timestamp > curTime + 15) || (transaction.deadline < 1) || (transaction.timestamp + transaction.deadline * 60 < curTime) || (transaction.fee <= 0) || (transaction.validateAttachment()))
        {
          long senderId;
          boolean doubleSpendingTransaction;
          synchronized (Nxt.blocksAndTransactionsLock)
          {
            long id = transaction.getId();
            if ((Nxt.transactions.get(Long.valueOf(id)) == null) && (Nxt.unconfirmedTransactions.get(Long.valueOf(id)) == null) && (Nxt.doubleSpendingTransactions.get(Long.valueOf(id)) == null) && (!transaction.verify())) {
              continue;
            }
            senderId = transaction.getSenderAccountId();
            Nxt.Account account = (Nxt.Account)Nxt.accounts.get(Long.valueOf(senderId));
            boolean doubleSpendingTransaction;
            if (account == null)
            {
              doubleSpendingTransaction = true;
            }
            else
            {
              int amount = transaction.amount + transaction.fee;
              synchronized (account)
              {
                boolean doubleSpendingTransaction;
                if (account.getUnconfirmedBalance() < amount * 100L)
                {
                  doubleSpendingTransaction = true;
                }
                else
                {
                  doubleSpendingTransaction = false;
                  
                  account.addToUnconfirmedBalance(-amount * 100L);
                  if (transaction.type == 2) {
                    if (transaction.subtype == 1)
                    {
                      Nxt.Transaction.ColoredCoinsAssetTransferAttachment attachment = (Nxt.Transaction.ColoredCoinsAssetTransferAttachment)transaction.attachment;
                      Integer unconfirmedAssetBalance = account.getUnconfirmedAssetBalance(Long.valueOf(attachment.asset));
                      if ((unconfirmedAssetBalance == null) || (unconfirmedAssetBalance.intValue() < attachment.quantity))
                      {
                        doubleSpendingTransaction = true;
                        
                        account.addToUnconfirmedBalance(amount * 100L);
                      }
                      else
                      {
                        account.addToUnconfirmedAssetBalance(Long.valueOf(attachment.asset), -attachment.quantity);
                      }
                    }
                    else if (transaction.subtype == 2)
                    {
                      Nxt.Transaction.ColoredCoinsAskOrderPlacementAttachment attachment = (Nxt.Transaction.ColoredCoinsAskOrderPlacementAttachment)transaction.attachment;
                      Integer unconfirmedAssetBalance = account.getUnconfirmedAssetBalance(Long.valueOf(attachment.asset));
                      if ((unconfirmedAssetBalance == null) || (unconfirmedAssetBalance.intValue() < attachment.quantity))
                      {
                        doubleSpendingTransaction = true;
                        
                        account.addToUnconfirmedBalance(amount * 100L);
                      }
                      else
                      {
                        account.addToUnconfirmedAssetBalance(Long.valueOf(attachment.asset), -attachment.quantity);
                      }
                    }
                    else if (transaction.subtype == 3)
                    {
                      Nxt.Transaction.ColoredCoinsBidOrderPlacementAttachment attachment = (Nxt.Transaction.ColoredCoinsBidOrderPlacementAttachment)transaction.attachment;
                      if (account.getUnconfirmedBalance() < attachment.quantity * attachment.price)
                      {
                        doubleSpendingTransaction = true;
                        
                        account.addToUnconfirmedBalance(amount * 100L);
                      }
                      else
                      {
                        account.addToUnconfirmedBalance(-attachment.quantity * attachment.price);
                      }
                    }
                  }
                }
              }
            }
            transaction.index = Nxt.transactionCounter.incrementAndGet();
            if (doubleSpendingTransaction)
            {
              Nxt.doubleSpendingTransactions.put(Long.valueOf(transaction.getId()), transaction);
            }
            else
            {
              Nxt.unconfirmedTransactions.put(Long.valueOf(transaction.getId()), transaction);
              if (parameterName.equals("transactions")) {
                validTransactionsData.add(transactionData);
              }
            }
          }
          response = new JSONObject();
          response.put("response", "processNewData");
          
          JSONArray newTransactions = new JSONArray();
          JSONObject newTransaction = new JSONObject();
          newTransaction.put("index", Integer.valueOf(transaction.index));
          newTransaction.put("timestamp", Integer.valueOf(transaction.timestamp));
          newTransaction.put("deadline", Short.valueOf(transaction.deadline));
          newTransaction.put("recipient", Nxt.convert(transaction.recipient));
          newTransaction.put("amount", Integer.valueOf(transaction.amount));
          newTransaction.put("fee", Integer.valueOf(transaction.fee));
          newTransaction.put("sender", Nxt.convert(senderId));
          newTransaction.put("id", transaction.getStringId());
          newTransactions.add(newTransaction);
          if (doubleSpendingTransaction) {
            response.put("addedDoubleSpendingTransactions", newTransactions);
          } else {
            response.put("addedUnconfirmedTransactions", newTransactions);
          }
          for (Nxt.User user : Nxt.users.values()) {
            user.send(response);
          }
        }
      }
      catch (RuntimeException e)
      {
        JSONObject response;
        Nxt.logMessage("Error processing transaction", e);
      }
    }
    if (validTransactionsData.size() > 0)
    {
      JSONObject peerRequest = new JSONObject();
      peerRequest.put("requestType", "processTransactions");
      peerRequest.put("transactions", validTransactionsData);
      
      Nxt.Peer.sendToSomePeers(peerRequest);
    }
  }
  
  static void saveTransactions(String fileName)
  {
    try
    {
      FileOutputStream fileOutputStream = new FileOutputStream(fileName);Throwable localThrowable3 = null;
      try
      {
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);Throwable localThrowable4 = null;
        try
        {
          objectOutputStream.writeInt(Nxt.transactionCounter.get());
          objectOutputStream.writeObject(new HashMap(Nxt.transactions));
          objectOutputStream.close();
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
      Nxt.logMessage("Error saving transactions to " + fileName, e);
      throw new RuntimeException(e);
    }
  }
  
  void sign(String secretPhrase)
  {
    this.signature = Nxt.Crypto.sign(getBytes(), secretPhrase);
    try
    {
      while (!verify())
      {
        this.timestamp += 1;
        
        this.signature = new byte[64];
        this.signature = Nxt.Crypto.sign(getBytes(), secretPhrase);
      }
    }
    catch (RuntimeException e)
    {
      Nxt.logMessage("Error signing transaction", e);
    }
  }
  
  boolean validateAttachment()
  {
    if (this.fee > 1000000000L) {
      return false;
    }
    switch (this.type)
    {
    case 0: 
      switch (this.subtype)
      {
      case 0: 
        if ((this.amount <= 0) || (this.amount > 1000000000L)) {
          return false;
        }
        return true;
      }
      return false;
    case 1: 
      switch (this.subtype)
      {
      case 0: 
        if (Nxt.Block.getLastBlock().height < 40000) {
          return false;
        }
        try
        {
          Nxt.Transaction.MessagingArbitraryMessageAttachment attachment = (Nxt.Transaction.MessagingArbitraryMessageAttachment)this.attachment;
          return (this.amount == 0) && (attachment.message.length <= 1000);
        }
        catch (RuntimeException e)
        {
          Nxt.logDebugMessage("Error validating arbitrary message", e);
          return false;
        }
      case 1: 
        if (Nxt.Block.getLastBlock().height < 22000) {
          return false;
        }
        try
        {
          Nxt.Transaction.MessagingAliasAssignmentAttachment attachment = (Nxt.Transaction.MessagingAliasAssignmentAttachment)this.attachment;
          if ((this.recipient != 1739068987193023818L) || (this.amount != 0) || (attachment.alias.length() == 0) || (attachment.alias.length() > 100) || (attachment.uri.length() > 1000)) {
            return false;
          }
          String normalizedAlias = attachment.alias.toLowerCase();
          for (int i = 0; i < normalizedAlias.length(); i++) {
            if ("0123456789abcdefghijklmnopqrstuvwxyz".indexOf(normalizedAlias.charAt(i)) < 0) {
              return false;
            }
          }
          Nxt.Alias alias = (Nxt.Alias)Nxt.aliases.get(normalizedAlias);
          
          return (alias == null) || (Arrays.equals((byte[])alias.account.publicKey.get(), this.senderPublicKey));
        }
        catch (RuntimeException e)
        {
          Nxt.logDebugMessage("Error validation alias assignment", e);
          return false;
        }
      }
      return false;
    }
    return false;
  }
  
  boolean verify()
  {
    Nxt.Account account = (Nxt.Account)Nxt.accounts.get(Long.valueOf(getSenderAccountId()));
    if (account == null) {
      return false;
    }
    byte[] data = getBytes();
    for (int i = 64; i < 128; i++) {
      data[i] = 0;
    }
    return (Nxt.Crypto.verify(this.signature, data, this.senderPublicKey)) && (account.setOrVerify(this.senderPublicKey));
  }
  
  public static byte[] calculateTransactionsChecksum()
  {
    synchronized (Nxt.blocksAndTransactionsLock)
    {
      PriorityQueue<Transaction> sortedTransactions = new PriorityQueue(Nxt.transactions.size(), new Nxt.Transaction.2());
      






      sortedTransactions.addAll(Nxt.transactions.values());
      MessageDigest digest = Nxt.getMessageDigest("SHA-256");
