import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

class Nxt$CountingInputStream
  extends FilterInputStream
{
  private long count;
  
  public Nxt$CountingInputStream(InputStream in)
  {
    super(in);
  }
  
  public int read()
    throws IOException
  {
    int read = super.read();
    if (read >= 0) {
      this.count += 1L;
    }
    return read;
  }
  
  public int read(byte[] b, int off, int len)
    throws IOException
  {
    int read = super.read(b, off, len);
    if (read >= 0) {
      this.count += 1L;
    }
    return read;
  }
  
  public long skip(long n)
    throws IOException
  {
    long skipped = super.skip(n);
    if (skipped >= 0L) {
      this.count += skipped;
    }
    return skipped;
