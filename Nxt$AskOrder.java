class Nxt$AskOrder
  implements Comparable<AskOrder>
{
  final long id;
  final long height;
  final Nxt.Account account;
  final long asset;
  volatile int quantity;
  final long price;
  
  Nxt$AskOrder(long id, Nxt.Account account, long asset, int quantity, long price)
  {
    this.id = id;
    this.height = Nxt.Block.getLastBlock().height;
    this.account = account;
    this.asset = asset;
    this.quantity = quantity;
    this.price = price;
  }
  
  public int compareTo(AskOrder o)
  {
    if (this.price < o.price) {
      return -1;
    }
    if (this.price > o.price) {
      return 1;
    }
    if (this.height < o.height) {
      return -1;
    }
    if (this.height > o.height) {
      return 1;
    }
    if (this.id < o.id) {
      return -1;
