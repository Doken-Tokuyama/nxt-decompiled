import java.io.IOException;
import java.io.Writer;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import javax.servlet.AsyncContext;
import javax.servlet.ServletResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

class Nxt$User
{
  final ConcurrentLinkedQueue<JSONObject> pendingResponses;
  AsyncContext asyncContext;
  volatile boolean isInactive;
  volatile String secretPhrase;
  
  Nxt$User()
  {
    this.pendingResponses = new ConcurrentLinkedQueue();
  }
  
  void deinitializeKeyPair()
  {
    this.secretPhrase = null;
  }
  
  BigInteger initializeKeyPair(String secretPhrase)
  {
    this.secretPhrase = secretPhrase;
    byte[] publicKeyHash = Nxt.getMessageDigest("SHA-256").digest(Nxt.Crypto.getPublicKey(secretPhrase));
    return new BigInteger(1, new byte[] { publicKeyHash[7], publicKeyHash[6], publicKeyHash[5], publicKeyHash[4], publicKeyHash[3], publicKeyHash[2], publicKeyHash[1], publicKeyHash[0] });
  }
  
  void send(JSONObject response)
  {
    synchronized (this)
    {
      if (this.asyncContext == null)
      {
        if (this.isInactive) {
          return;
        }
        if (this.pendingResponses.size() > 1000)
        {
          this.pendingResponses.clear();
          
          this.isInactive = true;
          if (this.secretPhrase == null) {
            Nxt.users.values().remove(this);
          }
          return;
        }
        this.pendingResponses.offer(response);
      }
      else
      {
        JSONArray responses = new JSONArray();
        JSONObject pendingResponse;
        while ((pendingResponse = (JSONObject)this.pendingResponses.poll()) != null) {
          responses.add(pendingResponse);
        }
        responses.add(response);
        
        JSONObject combinedResponse = new JSONObject();
        combinedResponse.put("responses", responses);
        
        this.asyncContext.getResponse().setContentType("text/plain; charset=UTF-8");
        try
        {
          Writer writer = this.asyncContext.getResponse().getWriter();Throwable localThrowable2 = null;
          try
          {
            combinedResponse.writeJSONString(writer);
          }
          catch (Throwable localThrowable1)
          {
            localThrowable2 = localThrowable1;throw localThrowable1;
          }
          finally
          {
            if (writer != null) {
              if (localThrowable2 != null) {
                try
                {
                  writer.close();
                }
                catch (Throwable x2)
                {
                  localThrowable2.addSuppressed(x2);
                }
              } else {
                writer.close();
              }
            }
          }
        }
        catch (IOException e)
        {
          Nxt.logMessage("Error sending response to user", e);
