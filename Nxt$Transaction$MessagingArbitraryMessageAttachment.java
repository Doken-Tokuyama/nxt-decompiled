import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.json.simple.JSONObject;

class Nxt$Transaction$MessagingArbitraryMessageAttachment
  implements Nxt.Transaction.Attachment, Serializable
{
  static final long serialVersionUID = 0L;
  final byte[] message;
  
  Nxt$Transaction$MessagingArbitraryMessageAttachment(byte[] message)
  {
    this.message = message;
  }
  
  public int getSize()
  {
    return 4 + this.message.length;
  }
  
  public byte[] getBytes()
  {
    ByteBuffer buffer = ByteBuffer.allocate(getSize());
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(this.message.length);
    buffer.put(this.message);
    
    return buffer.array();
  }
  
  public JSONObject getJSONObject()
  {
    JSONObject attachment = new JSONObject();
    attachment.put("message", Nxt.convert(this.message));
    
    return attachment;
  }
  
  public long getRecipientDeltaBalance()
  {
    return 0L;
