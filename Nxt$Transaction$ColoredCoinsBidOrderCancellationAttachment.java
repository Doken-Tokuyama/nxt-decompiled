import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ConcurrentMap;
import org.json.simple.JSONObject;

class Nxt$Transaction$ColoredCoinsBidOrderCancellationAttachment
  implements Nxt.Transaction.Attachment, Serializable
{
  static final long serialVersionUID = 0L;
  long order;
  
  Nxt$Transaction$ColoredCoinsBidOrderCancellationAttachment(long order)
  {
    this.order = order;
  }
  
  public int getSize()
  {
    return 8;
  }
  
  public byte[] getBytes()
  {
    ByteBuffer buffer = ByteBuffer.allocate(getSize());
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putLong(this.order);
    
    return buffer.array();
  }
  
  public JSONObject getJSONObject()
  {
    JSONObject attachment = new JSONObject();
    attachment.put("order", Nxt.convert(this.order));
    
    return attachment;
  }
  
  public long getRecipientDeltaBalance()
  {
    return 0L;
  }
  
  public long getSenderDeltaBalance()
  {
