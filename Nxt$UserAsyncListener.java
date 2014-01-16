import java.io.IOException;
import java.io.Writer;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletResponse;
import org.json.simple.JSONObject;

class Nxt$UserAsyncListener
  implements AsyncListener
{
  final Nxt.User user;
  
  Nxt$UserAsyncListener(Nxt.User user)
  {
    this.user = user;
  }
  
  public void onComplete(AsyncEvent asyncEvent)
    throws IOException
  {}
  
  public void onError(AsyncEvent asyncEvent)
    throws IOException
  {
    synchronized (this.user)
    {
      this.user.asyncContext.getResponse().setContentType("text/plain; charset=UTF-8");
      
      Writer writer = this.user.asyncContext.getResponse().getWriter();Throwable localThrowable2 = null;
      try
      {
        new JSONObject().writeJSONString(writer);
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
      this.user.asyncContext.complete();
      this.user.asyncContext = null;
    }
  }
  
  public void onStartAsync(AsyncEvent asyncEvent)
    throws IOException
  {}
  
  public void onTimeout(AsyncEvent asyncEvent)
    throws IOException
  {
    synchronized (this.user)
    {
      this.user.asyncContext.getResponse().setContentType("text/plain; charset=UTF-8");
      
      Writer writer = this.user.asyncContext.getResponse().getWriter();Throwable localThrowable2 = null;
      try
      {
        new JSONObject().writeJSONString(writer);
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
