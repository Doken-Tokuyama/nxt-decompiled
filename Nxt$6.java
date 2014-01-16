import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentMap;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

class Nxt$6
  implements Runnable
{
  Nxt$6(Nxt paramNxt) {}
  
  public void run()
  {
    try
    {
      int curTime = Nxt.getEpochTime(System.currentTimeMillis());
      JSONArray removedUnconfirmedTransactions = new JSONArray();
      
      Iterator<Nxt.Transaction> iterator = Nxt.unconfirmedTransactions.values().iterator();
      while (iterator.hasNext())
      {
        Nxt.Transaction transaction = (Nxt.Transaction)iterator.next();
        if ((transaction.timestamp + transaction.deadline * 60 < curTime) || (!transaction.validateAttachment()))
        {
          iterator.remove();
          
          Nxt.Account account = (Nxt.Account)Nxt.accounts.get(Long.valueOf(transaction.getSenderAccountId()));
          account.addToUnconfirmedBalance((transaction.amount + transaction.fee) * 100L);
          
          JSONObject removedUnconfirmedTransaction = new JSONObject();
          removedUnconfirmedTransaction.put("index", Integer.valueOf(transaction.index));
          removedUnconfirmedTransactions.add(removedUnconfirmedTransaction);
        }
      }
      if (removedUnconfirmedTransactions.size() > 0)
      {
        response = new JSONObject();
        response.put("response", "processNewData");
        
        response.put("removedUnconfirmedTransactions", removedUnconfirmedTransactions);
        for (Nxt.User user : Nxt.users.values()) {
          user.send(response);
        }
      }
    }
    catch (Exception e)
    {
      JSONObject response;
      Nxt.logDebugMessage("Error removing unconfirmed transactions", e);
    }
    catch (Throwable t)
