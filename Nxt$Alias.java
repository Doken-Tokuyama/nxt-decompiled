class Nxt$Alias
{
  final Nxt.Account account;
  final long id;
  final String alias;
  volatile String uri;
  volatile int timestamp;
  
  Nxt$Alias(Nxt.Account account, long id, String alias, String uri, int timestamp)
  {
