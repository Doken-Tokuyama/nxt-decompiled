import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.json.simple.JSONObject;

class Nxt$8
  implements Runnable
{
  private final ConcurrentMap<Nxt.Account, Nxt.Block> lastBlocks = new ConcurrentHashMap();
  private final ConcurrentMap<Nxt.Account, BigInteger> hits = new ConcurrentHashMap();
  
  Nxt$8(Nxt paramNxt) {}
  
  public void run()
  {
    try
    {
      HashMap<Nxt.Account, Nxt.User> unlockedAccounts = new HashMap();
      for (Nxt.User user : Nxt.users.values()) {
        if (user.secretPhrase != null)
        {
          Nxt.Account account = (Nxt.Account)Nxt.accounts.get(Long.valueOf(Nxt.Account.getId(Nxt.Crypto.getPublicKey(user.secretPhrase))));
          if ((account != null) && (account.getEffectiveBalance() > 0)) {
            unlockedAccounts.put(account, user);
          }
        }
      }
      for (Map.Entry<Nxt.Account, Nxt.User> unlockedAccountEntry : unlockedAccounts.entrySet())
      {
        Nxt.Account account = (Nxt.Account)unlockedAccountEntry.getKey();
        Nxt.User user = (Nxt.User)unlockedAccountEntry.getValue();
        Nxt.Block lastBlock = Nxt.Block.getLastBlock();
        if (this.lastBlocks.get(account) != lastBlock)
        {
          MessageDigest digest = Nxt.getMessageDigest("SHA-256");
          byte[] generationSignatureHash;
          byte[] generationSignatureHash;
          if (lastBlock.height < 30000)
          {
            byte[] generationSignature = Nxt.Crypto.sign(lastBlock.generationSignature, user.secretPhrase);
            generationSignatureHash = digest.digest(generationSignature);
          }
          else
          {
            digest.update(lastBlock.generationSignature);
            generationSignatureHash = digest.digest(Nxt.Crypto.getPublicKey(user.secretPhrase));
          }
          BigInteger hit = new BigInteger(1, new byte[] { generationSignatureHash[7], generationSignatureHash[6], generationSignatureHash[5], generationSignatureHash[4], generationSignatureHash[3], generationSignatureHash[2], generationSignatureHash[1], generationSignatureHash[0] });
          
          this.lastBlocks.put(account, lastBlock);
          this.hits.put(account, hit);
          
          JSONObject response = new JSONObject();
          response.put("response", "setBlockGenerationDeadline");
          response.put("deadline", Long.valueOf(hit.divide(BigInteger.valueOf(Nxt.Block.getBaseTarget()).multiply(BigInteger.valueOf(account.getEffectiveBalance()))).longValue() - (Nxt.getEpochTime(System.currentTimeMillis()) - lastBlock.timestamp)));
          
          user.send(response);
        }
        int elapsedTime = Nxt.getEpochTime(System.currentTimeMillis()) - lastBlock.timestamp;
        if (elapsedTime > 0)
        {
          BigInteger target = BigInteger.valueOf(Nxt.Block.getBaseTarget()).multiply(BigInteger.valueOf(account.getEffectiveBalance())).multiply(BigInteger.valueOf(elapsedTime));
          if (((BigInteger)this.hits.get(account)).compareTo(target) < 0) {
            account.generateBlock(user.secretPhrase);
          }
        }
      }
    }
    catch (Exception e)
    {
      Nxt.logDebugMessage("Error in block generation thread", e);
    }
    catch (Throwable t)
