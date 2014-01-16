import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.json.simple.JSONObject;

class Nxt$Transaction$ColoredCoinsBidOrderPlacementAttachment
  implements Nxt.Transaction.Attachment, Serializable
{
  static final long serialVersionUID = 0L;
  long asset;
  int quantity;
  long price;
  
  Nxt$Transaction$ColoredCoinsBidOrderPlacementAttachment(long asset, int quantity, long price)
  {
    this.asset = asset;
    this.quantity = quantity;
    this.price = price;
  }
  
  public int getSize()
  {
    return 20;
  }
  
  public byte[] getBytes()
  {
    ByteBuffer buffer = ByteBuffer.allocate(getSize());
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putLong(this.asset);
    buffer.putInt(this.quantity);
    buffer.putLong(this.price);
    
    return buffer.array();
  }
  
  public JSONObject getJSONObject()
  {
    JSONObject attachment = new JSONObject();
    attachment.put("asset", Nxt.convert(this.asset));
    attachment.put("quantity", Integer.valueOf(this.quantity));
    attachment.put("price", Long.valueOf(this.price));
    
    return attachment;
  }
  
  public long getRecipientDeltaBalance()
  {
    return 0L;
