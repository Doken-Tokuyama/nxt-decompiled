import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.json.simple.JSONObject;

class Nxt$Transaction$ColoredCoinsAssetTransferAttachment
  implements Nxt.Transaction.Attachment, Serializable
{
  static final long serialVersionUID = 0L;
  long asset;
  int quantity;
  
  Nxt$Transaction$ColoredCoinsAssetTransferAttachment(long asset, int quantity)
  {
    this.asset = asset;
    this.quantity = quantity;
  }
  
  public int getSize()
  {
    return 12;
  }
  
  public byte[] getBytes()
  {
    ByteBuffer buffer = ByteBuffer.allocate(getSize());
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putLong(this.asset);
    buffer.putInt(this.quantity);
    
    return buffer.array();
  }
  
  public JSONObject getJSONObject()
  {
    JSONObject attachment = new JSONObject();
    attachment.put("asset", Nxt.convert(this.asset));
    attachment.put("quantity", Integer.valueOf(this.quantity));
    
    return attachment;
  }
  
  public long getRecipientDeltaBalance()
  {
    return 0L;
