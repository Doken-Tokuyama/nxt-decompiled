import org.json.simple.JSONObject;

class Nxt$5
  implements Runnable
{
  private final JSONObject getUnconfirmedTransactionsRequest;
  
  Nxt$5(Nxt paramNxt)
  {
    this.getUnconfirmedTransactionsRequest = new JSONObject();
    
    this.getUnconfirmedTransactionsRequest.put("requestType", "getUnconfirmedTransactions");
  }
  
  public void run()
  {
    try
    {
      Nxt.Peer peer = Nxt.Peer.getAnyPeer(1, true);
      if (peer != null)
      {
        JSONObject response = peer.send(this.getUnconfirmedTransactionsRequest);
        if (response != null) {
          Nxt.Transaction.processTransactions(response, "unconfirmedTransactions");
        }
      }
    }
    catch (Exception e)
    {
      Nxt.logDebugMessage("Error processing unconfirmed transactions from peer", e);
    }
    catch (Throwable t)
