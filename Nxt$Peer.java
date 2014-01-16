import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

class Nxt$Peer
  implements Comparable<Peer>
{
  static final int STATE_NONCONNECTED = 0;
  static final int STATE_CONNECTED = 1;
  static final int STATE_DISCONNECTED = 2;
  final int index;
  String platform;
  String announcedAddress;
  boolean shareAddress;
  String hallmark;
  long accountId;
  int weight;
  int date;
  long adjustedWeight;
  String application;
  String version;
  long blacklistingTime;
  int state;
  long downloadedVolume;
  long uploadedVolume;
  
  Nxt$Peer(String announcedAddress, int index)
  {
    this.announcedAddress = announcedAddress;
    this.index = index;
  }
  
  static Peer addPeer(String address, String announcedAddress)
  {
    try
    {
      new URL("http://" + address);
    }
    catch (MalformedURLException e)
    {
      Nxt.logDebugMessage("malformed peer address " + address, e);
      return null;
    }
    try
    {
      new URL("http://" + announcedAddress);
    }
    catch (MalformedURLException e)
    {
      Nxt.logDebugMessage("malformed peer announced address " + announcedAddress, e);
      announcedAddress = "";
    }
    if ((address.equals("localhost")) || (address.equals("127.0.0.1")) || (address.equals("0:0:0:0:0:0:0:1"))) {
      return null;
    }
    if ((Nxt.myAddress != null) && (Nxt.myAddress.length() > 0) && (Nxt.myAddress.equals(announcedAddress))) {
      return null;
    }
    Peer peer = (Peer)Nxt.peers.get(announcedAddress.length() > 0 ? announcedAddress : address);
    if (peer == null)
    {
      peer = new Peer(announcedAddress, Nxt.peerCounter.incrementAndGet());
      Nxt.peers.put(announcedAddress.length() > 0 ? announcedAddress : address, peer);
    }
    return peer;
  }
  
  boolean analyzeHallmark(String realHost, String hallmark)
  {
    if (hallmark == null) {
      return true;
    }
    try
    {
      byte[] hallmarkBytes;
      try
      {
        hallmarkBytes = Nxt.convert(hallmark);
      }
      catch (NumberFormatException e)
      {
        return false;
      }
      ByteBuffer buffer = ByteBuffer.wrap(hallmarkBytes);
      buffer.order(ByteOrder.LITTLE_ENDIAN);
      
      byte[] publicKey = new byte[32];
      buffer.get(publicKey);
      int hostLength = buffer.getShort();
      byte[] hostBytes = new byte[hostLength];
      buffer.get(hostBytes);
      String host = new String(hostBytes, "UTF-8");
      if ((host.length() > 100) || (!host.equals(realHost))) {
        return false;
      }
      int weight = buffer.getInt();
      if ((weight <= 0) || (weight > 1000000000L)) {
        return false;
      }
      int date = buffer.getInt();
      buffer.get();
      byte[] signature = new byte[64];
      buffer.get(signature);
      
      byte[] data = new byte[hallmarkBytes.length - 64];
      System.arraycopy(hallmarkBytes, 0, data, 0, data.length);
      if (Nxt.Crypto.verify(signature, data, publicKey))
      {
        this.hallmark = hallmark;
        
        long accountId = Nxt.Account.getId(publicKey);
        







        LinkedList<Peer> groupedPeers = new LinkedList();
        int validDate = 0;
        
        this.accountId = accountId;
        this.weight = weight;
        this.date = date;
        for (Peer peer : Nxt.peers.values()) {
          if (peer.accountId == accountId)
          {
            groupedPeers.add(peer);
            if (peer.date > validDate) {
              validDate = peer.date;
            }
          }
        }
        long totalWeight = 0L;
        for (Peer peer : groupedPeers) {
          if (peer.date == validDate)
          {
            totalWeight += peer.weight;
          }
          else
          {
            peer.adjustedWeight = 0L;
            peer.updateWeight();
          }
        }
        for (Peer peer : groupedPeers)
        {
          peer.adjustedWeight = (1000000000L * peer.weight / totalWeight);
          peer.updateWeight();
        }
        return true;
      }
    }
    catch (RuntimeException|UnsupportedEncodingException e)
    {
      Nxt.logDebugMessage("Failed to analyze hallmark for peer " + realHost, e);
    }
    return false;
  }
  
  void blacklist()
  {
    this.blacklistingTime = System.currentTimeMillis();
    
    JSONObject response = new JSONObject();
    response.put("response", "processNewData");
    
    JSONArray removedKnownPeers = new JSONArray();
    JSONObject removedKnownPeer = new JSONObject();
    removedKnownPeer.put("index", Integer.valueOf(this.index));
    removedKnownPeers.add(removedKnownPeer);
    response.put("removedKnownPeers", removedKnownPeers);
    
    JSONArray addedBlacklistedPeers = new JSONArray();
    JSONObject addedBlacklistedPeer = new JSONObject();
    addedBlacklistedPeer.put("index", Integer.valueOf(this.index));
    addedBlacklistedPeer.put("announcedAddress", this.announcedAddress.length() > 30 ? this.announcedAddress.substring(0, 30) + "..." : this.announcedAddress);
    for (String wellKnownPeer : Nxt.wellKnownPeers) {
      if (this.announcedAddress.equals(wellKnownPeer))
      {
        addedBlacklistedPeer.put("wellKnown", Boolean.valueOf(true));
        
        break;
      }
    }
    addedBlacklistedPeers.add(addedBlacklistedPeer);
    response.put("addedBlacklistedPeers", addedBlacklistedPeers);
    for (Nxt.User user : Nxt.users.values()) {
      user.send(response);
    }
  }
  
  public int compareTo(Peer o)
  {
    long weight = getWeight();long weight2 = o.getWeight();
    if (weight > weight2) {
      return -1;
    }
    if (weight < weight2) {
      return 1;
    }
    return this.index - o.index;
  }
  
  void connect()
  {
    JSONObject request = new JSONObject();
    request.put("requestType", "getInfo");
    if ((Nxt.myAddress != null) && (Nxt.myAddress.length() > 0)) {
      request.put("announcedAddress", Nxt.myAddress);
    }
    if ((Nxt.myHallmark != null) && (Nxt.myHallmark.length() > 0)) {
      request.put("hallmark", Nxt.myHallmark);
    }
    request.put("application", "NRS");
    request.put("version", "0.5.7");
    request.put("platform", Nxt.myPlatform);
    request.put("scheme", Nxt.myScheme);
    request.put("port", Integer.valueOf(Nxt.myPort));
    request.put("shareAddress", Boolean.valueOf(Nxt.shareMyAddress));
    JSONObject response = send(request);
    if (response != null)
    {
      this.application = ((String)response.get("application"));
      this.version = ((String)response.get("version"));
      this.platform = ((String)response.get("platform"));
      this.shareAddress = Boolean.TRUE.equals(response.get("shareAddress"));
      if (analyzeHallmark(this.announcedAddress, (String)response.get("hallmark"))) {
        setState(1);
      }
    }
  }
  
  void deactivate()
  {
    if (this.state == 1) {
      disconnect();
    }
    setState(0);
    
    JSONObject response = new JSONObject();
    response.put("response", "processNewData");
    
    JSONArray removedActivePeers = new JSONArray();
    JSONObject removedActivePeer = new JSONObject();
    removedActivePeer.put("index", Integer.valueOf(this.index));
    removedActivePeers.add(removedActivePeer);
    response.put("removedActivePeers", removedActivePeers);
    if (this.announcedAddress.length() > 0)
    {
      JSONArray addedKnownPeers = new JSONArray();
      JSONObject addedKnownPeer = new JSONObject();
      addedKnownPeer.put("index", Integer.valueOf(this.index));
      addedKnownPeer.put("announcedAddress", this.announcedAddress.length() > 30 ? this.announcedAddress.substring(0, 30) + "..." : this.announcedAddress);
      for (String wellKnownPeer : Nxt.wellKnownPeers) {
        if (this.announcedAddress.equals(wellKnownPeer))
        {
          addedKnownPeer.put("wellKnown", Boolean.valueOf(true));
          
          break;
        }
      }
      addedKnownPeers.add(addedKnownPeer);
      response.put("addedKnownPeers", addedKnownPeers);
    }
    for (Nxt.User user : Nxt.users.values()) {
      user.send(response);
    }
  }
  
  void disconnect()
  {
    setState(2);
  }
  
  static Peer getAnyPeer(int state, boolean applyPullThreshold)
  {
    List<Peer> selectedPeers = new ArrayList();
    for (Peer peer : Nxt.peers.values()) {
      if ((peer.blacklistingTime <= 0L) && (peer.state == state) && (peer.announcedAddress.length() > 0) && ((!applyPullThreshold) || (!Nxt.enableHallmarkProtection) || (peer.getWeight() >= Nxt.pullThreshold))) {
        selectedPeers.add(peer);
      }
    }
    long hit;
    if (selectedPeers.size() > 0)
    {
      long totalWeight = 0L;
      for (Peer peer : selectedPeers)
      {
        long weight = peer.getWeight();
        if (weight == 0L) {
          weight = 1L;
        }
        totalWeight += weight;
      }
      hit = ThreadLocalRandom.current().nextLong(totalWeight);
      for (Peer peer : selectedPeers)
      {
        long weight = peer.getWeight();
        if (weight == 0L) {
          weight = 1L;
        }
        if (hit -= weight < 0L) {
          return peer;
        }
      }
    }
    return null;
  }
  
  static int getNumberOfConnectedPublicPeers()
  {
    int numberOfConnectedPeers = 0;
    for (Peer peer : Nxt.peers.values()) {
      if ((peer.state == 1) && (peer.announcedAddress.length() > 0)) {
        numberOfConnectedPeers++;
      }
    }
    return numberOfConnectedPeers;
  }
  
  int getWeight()
  {
    if (this.accountId == 0L) {
      return 0;
    }
    Nxt.Account account = (Nxt.Account)Nxt.accounts.get(Long.valueOf(this.accountId));
    if (account == null) {
      return 0;
    }
    return (int)(this.adjustedWeight * (account.getBalance() / 100L) / 1000000000L);
  }
  
  String getSoftware()
  {
    StringBuilder buf = new StringBuilder();
    buf.append(this.application == null ? "?" : this.application.substring(0, Math.min(this.application.length(), 10)));
    buf.append(" (");
    buf.append(this.version == null ? "?" : this.version.substring(0, Math.min(this.version.length(), 10)));
    buf.append(")").append(" @ ");
    buf.append(this.platform == null ? "?" : this.platform.substring(0, Math.min(this.platform.length(), 12)));
    return buf.toString();
  }
  
  void removeBlacklistedStatus()
  {
    setState(0);
    this.blacklistingTime = 0L;
    
    JSONObject response = new JSONObject();
    response.put("response", "processNewData");
    
    JSONArray removedBlacklistedPeers = new JSONArray();
    JSONObject removedBlacklistedPeer = new JSONObject();
    removedBlacklistedPeer.put("index", Integer.valueOf(this.index));
    removedBlacklistedPeers.add(removedBlacklistedPeer);
    response.put("removedBlacklistedPeers", removedBlacklistedPeers);
    
    JSONArray addedKnownPeers = new JSONArray();
    JSONObject addedKnownPeer = new JSONObject();
    addedKnownPeer.put("index", Integer.valueOf(this.index));
    addedKnownPeer.put("announcedAddress", this.announcedAddress.length() > 30 ? this.announcedAddress.substring(0, 30) + "..." : this.announcedAddress);
    for (String wellKnownPeer : Nxt.wellKnownPeers) {
      if (this.announcedAddress.equals(wellKnownPeer))
      {
        addedKnownPeer.put("wellKnown", Boolean.valueOf(true));
        
        break;
      }
    }
    addedKnownPeers.add(addedKnownPeer);
    response.put("addedKnownPeers", addedKnownPeers);
    for (Nxt.User user : Nxt.users.values()) {
      user.send(response);
    }
  }
  
  void removePeer()
  {
    Nxt.peers.values().remove(this);
    
    JSONObject response = new JSONObject();
    response.put("response", "processNewData");
    
    JSONArray removedKnownPeers = new JSONArray();
    JSONObject removedKnownPeer = new JSONObject();
    removedKnownPeer.put("index", Integer.valueOf(this.index));
    removedKnownPeers.add(removedKnownPeer);
    response.put("removedKnownPeers", removedKnownPeers);
    for (Nxt.User user : Nxt.users.values()) {
      user.send(response);
    }
  }
  
  static void sendToSomePeers(JSONObject request)
  {
    int successful = 0;
    List<Future<JSONObject>> expectedResponses = new ArrayList();
    for (Peer peer : Nxt.peers.values()) {
      if ((!Nxt.enableHallmarkProtection) || (peer.getWeight() >= Nxt.pushThreshold))
      {
        if ((peer.blacklistingTime == 0L) && (peer.state == 1) && (peer.announcedAddress.length() > 0))
        {
          Future<JSONObject> futureResponse = Nxt.sendToPeersService.submit(new Nxt.Peer.1(peer, request));
          




          expectedResponses.add(futureResponse);
        }
        if (expectedResponses.size() >= Nxt.sendToPeersLimit - successful)
        {
          for (Future<JSONObject> future : expectedResponses) {
            try
            {
              JSONObject response = (JSONObject)future.get();
              if ((response != null) && (response.get("error") == null)) {
                successful++;
              }
            }
            catch (InterruptedException e)
            {
              Thread.currentThread().interrupt();
            }
            catch (ExecutionException e)
            {
              Nxt.logDebugMessage("Error in sendToSomePeers", e);
            }
          }
          expectedResponses.clear();
        }
        if (successful >= Nxt.sendToPeersLimit) {
          return;
        }
      }
    }
  }
  
  JSONObject send(JSONObject request)
  {
    String log = null;
    boolean showLog = false;
    
    HttpURLConnection connection = null;
    JSONObject response;
    try
    {
      if (Nxt.communicationLoggingMask != 0) {
        log = "\"" + this.announcedAddress + "\": " + request.toString();
      }
      request.put("protocol", Integer.valueOf(1));
      
      URL url = new URL("http://" + this.announcedAddress + (new URL("http://" + this.announcedAddress).getPort() < 0 ? ":7874" : "") + "/nxt");
      
      connection = (HttpURLConnection)url.openConnection();
      connection.setRequestMethod("POST");
      connection.setDoOutput(true);
      connection.setConnectTimeout(Nxt.connectTimeout);
      connection.setReadTimeout(Nxt.readTimeout);
      
      Nxt.CountingOutputStream cos = new Nxt.CountingOutputStream(connection.getOutputStream());
      Writer writer = new BufferedWriter(new OutputStreamWriter(cos, "UTF-8"));Throwable localThrowable4 = null;
      try
      {
        request.writeJSONString(writer);
      }
      catch (Throwable localThrowable1)
      {
        localThrowable4 = localThrowable1;throw localThrowable1;
      }
      finally
      {
        if (writer != null) {
          if (localThrowable4 != null) {
            try
            {
              writer.close();
            }
            catch (Throwable x2)
            {
              localThrowable4.addSuppressed(x2);
            }
          } else {
            writer.close();
          }
        }
      }
      updateUploadedVolume(cos.getCount());
      if (connection.getResponseCode() == 200)
      {
        int numberOfBytes;
        JSONObject response;
        if ((Nxt.communicationLoggingMask & 0x4) != 0)
        {
          ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
          byte[] buffer = new byte[65536];
          
          InputStream inputStream = connection.getInputStream();x2 = null;
          try
          {
            while ((numberOfBytes = inputStream.read(buffer)) > 0) {
              byteArrayOutputStream.write(buffer, 0, numberOfBytes);
            }
          }
          catch (Throwable localThrowable2)
          {
            x2 = localThrowable2;throw localThrowable2;
          }
          finally
          {
            if (inputStream != null) {
              if (x2 != null) {
                try
                {
                  inputStream.close();
                }
                catch (Throwable x2)
                {
                  x2.addSuppressed(x2);
                }
              } else {
                inputStream.close();
              }
            }
          }
          String responseValue = byteArrayOutputStream.toString("UTF-8");
          log = log + " >>> " + responseValue;
          showLog = true;
          updateDownloadedVolume(responseValue.getBytes("UTF-8").length);
          response = (JSONObject)JSONValue.parse(responseValue);
        }
        else
        {
          Nxt.CountingInputStream cis = new Nxt.CountingInputStream(connection.getInputStream());
          
          Object reader = new BufferedReader(new InputStreamReader(cis, "UTF-8"));numberOfBytes = null;
          try
          {
            response = (JSONObject)JSONValue.parse((Reader)reader);
          }
          catch (Throwable localThrowable5)
          {
            JSONObject response;
            numberOfBytes = localThrowable5;throw localThrowable5;
          }
          finally
          {
            if (reader != null) {
              if (numberOfBytes != null) {
                try
                {
                  ((Reader)reader).close();
                }
                catch (Throwable x2)
                {
                  numberOfBytes.addSuppressed(x2);
                }
              } else {
                ((Reader)reader).close();
              }
            }
          }
          updateDownloadedVolume(cis.getCount());
        }
      }
      else
      {
        if ((Nxt.communicationLoggingMask & 0x2) != 0)
        {
          log = log + " >>> Peer responded with HTTP " + connection.getResponseCode() + " code!";
          showLog = true;
        }
        disconnect();
        
        response = null;
      }
    }
    catch (RuntimeException|IOException e)
    {
      if ((!(e instanceof ConnectException)) && (!(e instanceof UnknownHostException)) && (!(e instanceof NoRouteToHostException)) && (!(e instanceof SocketTimeoutException)) && (!(e instanceof SocketException))) {
        Nxt.logDebugMessage("Error sending JSON request", e);
      }
      if ((Nxt.communicationLoggingMask & 0x1) != 0)
      {
        log = log + " >>> " + e.toString();
        showLog = true;
      }
      if (this.state == 0) {
        blacklist();
      } else {
        disconnect();
      }
      response = null;
    }
    if (showLog) {
      Nxt.logMessage(log + "\n");
    }
    if (connection != null) {
      connection.disconnect();
    }
    return response;
  }
  
  void setState(int state)
  {
    JSONObject response;
    JSONObject response;
    if ((this.state == 0) && (state != 0))
    {
      response = new JSONObject();
      response.put("response", "processNewData");
      if (this.announcedAddress.length() > 0)
      {
        JSONArray removedKnownPeers = new JSONArray();
        JSONObject removedKnownPeer = new JSONObject();
        removedKnownPeer.put("index", Integer.valueOf(this.index));
        removedKnownPeers.add(removedKnownPeer);
        response.put("removedKnownPeers", removedKnownPeers);
      }
      JSONArray addedActivePeers = new JSONArray();
      JSONObject addedActivePeer = new JSONObject();
      addedActivePeer.put("index", Integer.valueOf(this.index));
      if (state == 2) {
        addedActivePeer.put("disconnected", Boolean.valueOf(true));
      }
      for (Map.Entry<String, Peer> peerEntry : Nxt.peers.entrySet()) {
        if (peerEntry.getValue() == this)
        {
          addedActivePeer.put("address", ((String)peerEntry.getKey()).length() > 30 ? ((String)peerEntry.getKey()).substring(0, 30) + "..." : (String)peerEntry.getKey());
          
          break;
        }
      }
      addedActivePeer.put("announcedAddress", this.announcedAddress.length() > 30 ? this.announcedAddress.substring(0, 30) + "..." : this.announcedAddress);
      addedActivePeer.put("weight", Integer.valueOf(getWeight()));
      addedActivePeer.put("downloaded", Long.valueOf(this.downloadedVolume));
      addedActivePeer.put("uploaded", Long.valueOf(this.uploadedVolume));
      addedActivePeer.put("software", getSoftware());
      for (String wellKnownPeer : Nxt.wellKnownPeers) {
        if (this.announcedAddress.equals(wellKnownPeer))
        {
          addedActivePeer.put("wellKnown", Boolean.valueOf(true));
          
          break;
        }
      }
      addedActivePeers.add(addedActivePeer);
      response.put("addedActivePeers", addedActivePeers);
      for (Nxt.User user : Nxt.users.values()) {
        user.send(response);
      }
    }
    else if ((this.state != 0) && (state != 0))
    {
      response = new JSONObject();
      response.put("response", "processNewData");
      
      JSONArray changedActivePeers = new JSONArray();
      JSONObject changedActivePeer = new JSONObject();
      changedActivePeer.put("index", Integer.valueOf(this.index));
      changedActivePeer.put(state == 1 ? "connected" : "disconnected", Boolean.valueOf(true));
      changedActivePeers.add(changedActivePeer);
      response.put("changedActivePeers", changedActivePeers);
      for (Nxt.User user : Nxt.users.values()) {
        user.send(response);
      }
    }
    this.state = state;
  }
  
  void updateDownloadedVolume(long volume)
  {
    this.downloadedVolume += volume;
    
    JSONObject response = new JSONObject();
    response.put("response", "processNewData");
    
    JSONArray changedActivePeers = new JSONArray();
    JSONObject changedActivePeer = new JSONObject();
    changedActivePeer.put("index", Integer.valueOf(this.index));
    changedActivePeer.put("downloaded", Long.valueOf(this.downloadedVolume));
    changedActivePeers.add(changedActivePeer);
    response.put("changedActivePeers", changedActivePeers);
    for (Nxt.User user : Nxt.users.values()) {
      user.send(response);
    }
  }
  
  void updateUploadedVolume(long volume)
  {
    this.uploadedVolume += volume;
    
    JSONObject response = new JSONObject();
    response.put("response", "processNewData");
    
    JSONArray changedActivePeers = new JSONArray();
    JSONObject changedActivePeer = new JSONObject();
    changedActivePeer.put("index", Integer.valueOf(this.index));
    changedActivePeer.put("uploaded", Long.valueOf(this.uploadedVolume));
    changedActivePeers.add(changedActivePeer);
    response.put("changedActivePeers", changedActivePeers);
    for (Nxt.User user : Nxt.users.values()) {
      user.send(response);
    }
  }
  
  void updateWeight()
  {
    JSONObject response = new JSONObject();
    response.put("response", "processNewData");
    
    JSONArray changedActivePeers = new JSONArray();
    JSONObject changedActivePeer = new JSONObject();
    changedActivePeer.put("index", Integer.valueOf(this.index));
    changedActivePeer.put("weight", Integer.valueOf(getWeight()));
