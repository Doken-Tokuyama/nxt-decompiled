import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.json.simple.JSONObject;

class Nxt$Transaction$ColoredCoinsAssetIssuanceAttachment
  implements Nxt.Transaction.Attachment, Serializable
{
  static final long serialVersionUID = 0L;
  String name;
  String description;
  int quantity;
  
  Nxt$Transaction$ColoredCoinsAssetIssuanceAttachment(String name, String description, int quantity)
  {
    this.name = name;
    this.description = (description == null ? "" : description);
    this.quantity = quantity;
  }
  
  public int getSize()
  {
    try
    {
      return 1 + this.name.getBytes("UTF-8").length + 2 + this.description.getBytes("UTF-8").length + 4;
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
      byte[] name = this.name.getBytes("UTF-8");
      byte[] description = this.description.getBytes("UTF-8");
      
      ByteBuffer buffer = ByteBuffer.allocate(1 + name.length + 2 + description.length + 4);
      buffer.order(ByteOrder.LITTLE_ENDIAN);
      buffer.put((byte)name.length);
      buffer.put(name);
      buffer.putShort((short)description.length);
      buffer.put(description);
      buffer.putInt(this.quantity);
      
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
    attachment.put("name", this.name);
    attachment.put("description", this.description);
    attachment.put("quantity", Integer.valueOf(this.quantity));
    
    return attachment;
  }
  
  public long getRecipientDeltaBalance()
  {
    return 0L;
