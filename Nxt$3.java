import java.util.concurrent.ConcurrentMap;

class Nxt$3
  implements Runnable
{
  Nxt$3(Nxt paramNxt) {}
  
  public void run()
  {
    try
    {
      curTime = System.currentTimeMillis();
      for (Nxt.Peer peer : Nxt.peers.values()) {
        if ((peer.blacklistingTime > 0L) && (peer.blacklistingTime + Nxt.blacklistingPeriod <= curTime)) {
          peer.removeBlacklistedStatus();
        }
      }
    }
    catch (Exception e)
    {
      long curTime;
      Nxt.logDebugMessage("Error un-blacklisting peer", e);
    }
    catch (Throwable t)
