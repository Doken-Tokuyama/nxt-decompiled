import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

class Nxt$CountingOutputStream
  extends FilterOutputStream
{
  private long count;
  
  public Nxt$CountingOutputStream(OutputStream out)
  {
    super(out);
  }
  
  public void write(int b)
    throws IOException
  {
    this.count += 1L;
    super.write(b);
