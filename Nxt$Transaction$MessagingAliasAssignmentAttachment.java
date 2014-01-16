import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.json.simple.JSONObject;

class Nxt$Transaction$MessagingAliasAssignmentAttachment
  implements Nxt.Transaction.Attachment, Serializable
{
  static final long serialVersionUID = 0L;
  final String alias;
  final String uri;
  
  Nxt$Transaction$MessagingAliasAssignmentAttachment(String alias, String uri)
  {
    this.alias = alias;
    this.uri = uri;
  }
  
  public int getSize()
  {
    try
    {
      return 1 + this.alias.getBytes("UTF-8").length + 2 + this.uri.getBytes("UTF-8").length;
    }
    catch (RuntimeException|UnsupportedEncodingException e)
    {
      Nxt.logMessage("Error in getBytes", e);
    }
    return 0;
  }
  
  public byte[] getBytes()
  {
    try
    {
      byte[] alias = this.alias.getBytes("UTF-8");
      byte[] uri = this.uri.getBytes("UTF-8");
      
      ByteBuffer buffer = ByteBuffer.allocate(1 + alias.length + 2 + uri.length);
      buffer.order(ByteOrder.LITTLE_ENDIAN);
      buffer.put((byte)alias.length);
      buffer.put(alias);
      buffer.putShort((short)uri.length);
      buffer.put(uri);
      
      return buffer.array();
    }
    catch (RuntimeException|UnsupportedEncodingException e)
    {
      Nxt.logMessage("Error in getBytes", e);
    }
    return null;
  }
  
  public JSONObject getJSONObject()
  {
    JSONObject attachment = new JSONObject();
    attachment.put("alias", this.alias);
    attachment.put("uri", this.uri);
    
    return attachment;
  }
  
  public long getRecipientDeltaBalance()
  {
    return 0L;
