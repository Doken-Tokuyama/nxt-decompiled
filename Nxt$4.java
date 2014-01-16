import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

class Nxt$4
  implements Runnable
{
  private final JSONObject getPeersRequest;
  
  Nxt$4(Nxt paramNxt)
  {
    this.getPeersRequest = new JSONObject();
    
    this.getPeersRequest.put("requestType", "getPeers");
  }
  
  public void run()
  {
    try
    {
      Nxt.Peer peer = Nxt.Peer.getAnyPeer(1, true);
      if (peer != null)
      {
        JSONObject response = peer.send(this.getPeersRequest);
        if (response != null)
        {
          JSONArray peers = (JSONArray)response.get("peers");
          for (Object peerAddress : peers)
          {
            String address = ((String)peerAddress).trim();
            if (address.length() > 0) {
              Nxt.Peer.addPeer(address, address);
            }
          }
        }
      }
    }
    catch (Exception e)
    {
      Nxt.logDebugMessage("Error requesting peers from a peer", e);
    }
    catch (Throwable t)
