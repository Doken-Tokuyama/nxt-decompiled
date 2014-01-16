import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;
import java.math.BigInteger;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.AsyncContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class Nxt
  extends HttpServlet
{
  static final String VERSION = "0.5.7";
  static final long GENESIS_BLOCK_ID = 2680262203532249785L;
  static final long CREATOR_ID = 1739068987193023818L;
  static final int BLOCK_HEADER_LENGTH = 224;
  static final int MAX_NUMBER_OF_TRANSACTIONS = 255;
  static final int MAX_PAYLOAD_LENGTH = 32640;
  static final int MAX_ARBITRARY_MESSAGE_LENGTH = 1000;
  static final int ALIAS_SYSTEM_BLOCK = 22000;
  static final int TRANSPARENT_FORGING_BLOCK = 30000;
  static final int ARBITRARY_MESSAGES_BLOCK = 40000;
  static final int TRANSPARENT_FORGING_BLOCK_2 = 47000;
  static final byte[] CHECKSUM_TRANSPARENT_FORGING = { 27, -54, -59, -98, 49, -42, 48, -68, -112, 49, 41, 94, -41, 78, -84, 27, -87, -22, -28, 36, -34, -90, 112, -50, -9, 5, 89, -35, 80, -121, -128, 112 };
  static final long MAX_BALANCE = 1000000000L;
  static final long initialBaseTarget = 153722867L;
  static final long maxBaseTarget = 153722867000000000L;
  static final long MAX_ASSET_QUANTITY = 1000000000L;
  static final BigInteger two64 = new BigInteger("18446744073709551616");
  static long epochBeginning;
  static final String alphabet = "0123456789abcdefghijklmnopqrstuvwxyz";
  static String myPlatform;
  static String myScheme;
  static String myAddress;
  static String myHallmark;
  static int myPort;
  static boolean shareMyAddress;
  static Set<String> allowedUserHosts;
  static Set<String> allowedBotHosts;
  static int blacklistingPeriod;
  static final int LOGGING_MASK_EXCEPTIONS = 1;
  static final int LOGGING_MASK_NON200_RESPONSES = 2;
  static final int LOGGING_MASK_200_RESPONSES = 4;
  static int communicationLoggingMask;
  static final AtomicInteger transactionCounter = new AtomicInteger();
  static final ConcurrentMap<Long, Nxt.Transaction> transactions = new ConcurrentHashMap();
  static final ConcurrentMap<Long, Nxt.Transaction> unconfirmedTransactions = new ConcurrentHashMap();
  static final ConcurrentMap<Long, Nxt.Transaction> doubleSpendingTransactions = new ConcurrentHashMap();
  static Set<String> wellKnownPeers;
  static int maxNumberOfConnectedPublicPeers;
  static int connectTimeout;
  static int readTimeout;
  static boolean enableHallmarkProtection;
  static int pushThreshold;
  static int pullThreshold;
  static int sendToPeersLimit;
  static final AtomicInteger peerCounter = new AtomicInteger();
  static final ConcurrentMap<String, Nxt.Peer> peers = new ConcurrentHashMap();
  static final Object blocksAndTransactionsLock = new Object();
  static final AtomicInteger blockCounter = new AtomicInteger();
  static final ConcurrentMap<Long, Nxt.Block> blocks = new ConcurrentHashMap();
  static volatile long lastBlock;
  static volatile Nxt.Peer lastBlockchainFeeder;
  static final ConcurrentMap<Long, Nxt.Account> accounts = new ConcurrentHashMap();
  static final ConcurrentMap<String, Nxt.Alias> aliases = new ConcurrentHashMap();
  static final ConcurrentMap<Long, Nxt.Alias> aliasIdToAliasMappings = new ConcurrentHashMap();
  static final ConcurrentMap<Long, Nxt.Asset> assets = new ConcurrentHashMap();
  static final ConcurrentMap<String, Long> assetNameToIdMappings = new ConcurrentHashMap();
  static final ConcurrentMap<Long, Nxt.AskOrder> askOrders = new ConcurrentHashMap();
  static final ConcurrentMap<Long, Nxt.BidOrder> bidOrders = new ConcurrentHashMap();
  static final ConcurrentMap<Long, TreeSet<Nxt.AskOrder>> sortedAskOrders = new ConcurrentHashMap();
  static final ConcurrentMap<Long, TreeSet<Nxt.BidOrder>> sortedBidOrders = new ConcurrentHashMap();
  static final ConcurrentMap<String, Nxt.User> users = new ConcurrentHashMap();
  static final ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(7);
  static final ExecutorService sendToPeersService = Executors.newFixedThreadPool(10);
  
  static int getEpochTime(long time)
  {
    return (int)((time - epochBeginning + 500L) / 1000L);
  }
  
  static final ThreadLocal<SimpleDateFormat> logDateFormat = new Nxt.1();
  static final boolean debug = System.getProperty("nxt.debug") != null;
  static final boolean enableStackTraces = System.getProperty("nxt.enableStackTraces") != null;
  
  static void logMessage(String message)
  {
    System.out.println(((SimpleDateFormat)logDateFormat.get()).format(new Date()) + message);
  }
  
  static void logMessage(String message, Exception e)
  {
    if (enableStackTraces)
    {
      logMessage(message);
      e.printStackTrace();
    }
    else
    {
      logMessage(message + ":\n" + e.toString());
    }
  }
  
  static void logDebugMessage(String message)
  {
    if (debug) {
      logMessage("DEBUG: " + message);
    }
  }
  
  static void logDebugMessage(String message, Exception e)
  {
    if (debug) {
      if (enableStackTraces)
      {
        logMessage("DEBUG: " + message);
        e.printStackTrace();
      }
      else
      {
        logMessage("DEBUG: " + message + ":\n" + e.toString());
      }
    }
  }
  
  static byte[] convert(String string)
  {
    byte[] bytes = new byte[string.length() / 2];
    for (int i = 0; i < bytes.length; i++) {
      bytes[i] = ((byte)Integer.parseInt(string.substring(i * 2, i * 2 + 2), 16));
    }
    return bytes;
  }
  
  static String convert(byte[] bytes)
  {
    StringBuilder string = new StringBuilder();
    for (byte b : bytes)
    {
      int number;
      string.append("0123456789abcdefghijklmnopqrstuvwxyz".charAt((number = b & 0xFF) >> 4)).append("0123456789abcdefghijklmnopqrstuvwxyz".charAt(number & 0xF));
    }
    return string.toString();
  }
  
  static String convert(long objectId)
  {
    BigInteger id = BigInteger.valueOf(objectId);
    if (objectId < 0L) {
      id = id.add(two64);
    }
    return id.toString();
  }
  
  static long parseUnsignedLong(String number)
  {
    if (number == null) {
      throw new IllegalArgumentException("trying to parse null");
    }
    BigInteger bigInt = new BigInteger(number.trim());
    if ((bigInt.signum() < 0) || (bigInt.compareTo(two64) != -1)) {
      throw new IllegalArgumentException("overflow: " + number);
    }
    return bigInt.longValue();
  }
  
  static MessageDigest getMessageDigest(String algorithm)
  {
    try
    {
      return MessageDigest.getInstance(algorithm);
    }
    catch (NoSuchAlgorithmException e)
    {
      logMessage("Missing message digest algorithm: " + algorithm);
      System.exit(1);
    }
    return null;
  }
  
  static void matchOrders(long assetId)
  {
    TreeSet<Nxt.AskOrder> sortedAskOrders = (TreeSet)sortedAskOrders.get(Long.valueOf(assetId));
    TreeSet<Nxt.BidOrder> sortedBidOrders = (TreeSet)sortedBidOrders.get(Long.valueOf(assetId));
    while ((!sortedAskOrders.isEmpty()) && (!sortedBidOrders.isEmpty()))
    {
      Nxt.AskOrder askOrder = (Nxt.AskOrder)sortedAskOrders.first();
      Nxt.BidOrder bidOrder = (Nxt.BidOrder)sortedBidOrders.first();
      if (askOrder.price > bidOrder.price) {
        break;
      }
      int quantity = askOrder.quantity < bidOrder.quantity ? askOrder.quantity : bidOrder.quantity;
      long price = (askOrder.height < bidOrder.height) || ((askOrder.height == bidOrder.height) && (askOrder.id < bidOrder.id)) ? askOrder.price : bidOrder.price;
      if (askOrder.quantity -= quantity == 0)
      {
        askOrders.remove(Long.valueOf(askOrder.id));
        sortedAskOrders.remove(askOrder);
      }
      askOrder.account.addToBalanceAndUnconfirmedBalance(quantity * price);
      if (bidOrder.quantity -= quantity == 0)
      {
        bidOrders.remove(Long.valueOf(bidOrder.id));
        sortedBidOrders.remove(bidOrder);
      }
      bidOrder.account.addToAssetAndUnconfirmedAssetBalance(Long.valueOf(assetId), quantity);
    }
  }
  
  public void init(ServletConfig servletConfig)
    throws ServletException
  {
    logMessage("NRS 0.5.7 starting...");
    if (debug) {
      logMessage("DEBUG logging enabled");
    }
    if (enableStackTraces) {
      logMessage("logging of exception stack traces enabled");
    }
    try
    {
      Calendar calendar = Calendar.getInstance();
      calendar.set(15, 0);
      calendar.set(1, 2013);
      calendar.set(2, 10);
      calendar.set(5, 24);
      calendar.set(11, 12);
      calendar.set(12, 0);
      calendar.set(13, 0);
      calendar.set(14, 0);
      epochBeginning = calendar.getTimeInMillis();
      




      myPlatform = servletConfig.getInitParameter("myPlatform");
      logMessage("\"myPlatform\" = \"" + myPlatform + "\"");
      if (myPlatform == null) {
        myPlatform = "PC";
      } else {
        myPlatform = myPlatform.trim();
      }
      myScheme = servletConfig.getInitParameter("myScheme");
      logMessage("\"myScheme\" = \"" + myScheme + "\"");
      if (myScheme == null) {
        myScheme = "http";
      } else {
        myScheme = myScheme.trim();
      }
      String myPort = servletConfig.getInitParameter("myPort");
      logMessage("\"myPort\" = \"" + myPort + "\"");
      try
      {
        myPort = Integer.parseInt(myPort);
      }
      catch (NumberFormatException e)
      {
        myPort = myScheme.equals("https") ? 7875 : 7874;
        logMessage("Invalid value for myPort " + myPort + ", using default " + myPort);
      }
      myAddress = servletConfig.getInitParameter("myAddress");
      logMessage("\"myAddress\" = \"" + myAddress + "\"");
      if (myAddress != null) {
        myAddress = myAddress.trim();
      }
      String shareMyAddress = servletConfig.getInitParameter("shareMyAddress");
      logMessage("\"shareMyAddress\" = \"" + shareMyAddress + "\"");
      shareMyAddress = Boolean.parseBoolean(shareMyAddress);
      
      myHallmark = servletConfig.getInitParameter("myHallmark");
      logMessage("\"myHallmark\" = \"" + myHallmark + "\"");
      if (myHallmark != null)
      {
        myHallmark = myHallmark.trim();
        try
        {
          convert(myHallmark);
        }
        catch (NumberFormatException e)
        {
          logMessage("Your hallmark is invalid: " + myHallmark);
          System.exit(1);
        }
      }
      String wellKnownPeers = servletConfig.getInitParameter("wellKnownPeers");
      logMessage("\"wellKnownPeers\" = \"" + wellKnownPeers + "\"");
      if (wellKnownPeers != null)
      {
        Set<String> set = new HashSet();
        for (String wellKnownPeer : wellKnownPeers.split(";"))
        {
          wellKnownPeer = wellKnownPeer.trim();
          if (wellKnownPeer.length() > 0)
          {
            set.add(wellKnownPeer);
            Nxt.Peer.addPeer(wellKnownPeer, wellKnownPeer);
          }
        }
        wellKnownPeers = Collections.unmodifiableSet(set);
      }
      else
      {
        wellKnownPeers = Collections.emptySet();
        logMessage("No wellKnownPeers defined, it is unlikely to work");
      }
      String maxNumberOfConnectedPublicPeers = servletConfig.getInitParameter("maxNumberOfConnectedPublicPeers");
      logMessage("\"maxNumberOfConnectedPublicPeers\" = \"" + maxNumberOfConnectedPublicPeers + "\"");
      try
      {
        maxNumberOfConnectedPublicPeers = Integer.parseInt(maxNumberOfConnectedPublicPeers);
      }
      catch (NumberFormatException e)
      {
        maxNumberOfConnectedPublicPeers = 10;
        logMessage("Invalid value for maxNumberOfConnectedPublicPeers " + maxNumberOfConnectedPublicPeers + ", using default " + maxNumberOfConnectedPublicPeers);
      }
      String connectTimeout = servletConfig.getInitParameter("connectTimeout");
      logMessage("\"connectTimeout\" = \"" + connectTimeout + "\"");
      try
      {
        connectTimeout = Integer.parseInt(connectTimeout);
      }
      catch (NumberFormatException e)
      {
        connectTimeout = 1000;
        logMessage("Invalid value for connectTimeout " + connectTimeout + ", using default " + connectTimeout);
      }
      String readTimeout = servletConfig.getInitParameter("readTimeout");
      logMessage("\"readTimeout\" = \"" + readTimeout + "\"");
      try
      {
        readTimeout = Integer.parseInt(readTimeout);
      }
      catch (NumberFormatException e)
      {
        readTimeout = 1000;
        logMessage("Invalid value for readTimeout " + readTimeout + ", using default " + readTimeout);
      }
      String enableHallmarkProtection = servletConfig.getInitParameter("enableHallmarkProtection");
      logMessage("\"enableHallmarkProtection\" = \"" + enableHallmarkProtection + "\"");
      enableHallmarkProtection = Boolean.parseBoolean(enableHallmarkProtection);
      
      String pushThreshold = servletConfig.getInitParameter("pushThreshold");
      logMessage("\"pushThreshold\" = \"" + pushThreshold + "\"");
      try
      {
        pushThreshold = Integer.parseInt(pushThreshold);
      }
      catch (NumberFormatException e)
      {
        pushThreshold = 0;
        logMessage("Invalid value for pushThreshold " + pushThreshold + ", using default " + pushThreshold);
      }
      String pullThreshold = servletConfig.getInitParameter("pullThreshold");
      logMessage("\"pullThreshold\" = \"" + pullThreshold + "\"");
      try
      {
        pullThreshold = Integer.parseInt(pullThreshold);
      }
      catch (NumberFormatException e)
      {
        pullThreshold = 0;
        logMessage("Invalid value for pullThreshold " + pullThreshold + ", using default " + pullThreshold);
      }
      String allowedUserHosts = servletConfig.getInitParameter("allowedUserHosts");
      logMessage("\"allowedUserHosts\" = \"" + allowedUserHosts + "\"");
      if (allowedUserHosts != null) {
        if (!allowedUserHosts.trim().equals("*"))
        {
          Set<String> set = new HashSet();
          for (String allowedUserHost : allowedUserHosts.split(";"))
          {
            allowedUserHost = allowedUserHost.trim();
            if (allowedUserHost.length() > 0) {
              set.add(allowedUserHost);
            }
          }
          allowedUserHosts = Collections.unmodifiableSet(set);
        }
      }
      String allowedBotHosts = servletConfig.getInitParameter("allowedBotHosts");
      logMessage("\"allowedBotHosts\" = \"" + allowedBotHosts + "\"");
      if (allowedBotHosts != null) {
        if (!allowedBotHosts.trim().equals("*"))
        {
          Set<String> set = new HashSet();
          for (String allowedBotHost : allowedBotHosts.split(";"))
          {
            allowedBotHost = allowedBotHost.trim();
            if (allowedBotHost.length() > 0) {
              set.add(allowedBotHost);
            }
          }
          allowedBotHosts = Collections.unmodifiableSet(set);
        }
      }
      String blacklistingPeriod = servletConfig.getInitParameter("blacklistingPeriod");
      logMessage("\"blacklistingPeriod\" = \"" + blacklistingPeriod + "\"");
      try
      {
        blacklistingPeriod = Integer.parseInt(blacklistingPeriod);
      }
      catch (NumberFormatException e)
      {
        blacklistingPeriod = 300000;
        logMessage("Invalid value for blacklistingPeriod " + blacklistingPeriod + ", using default " + blacklistingPeriod);
      }
      String communicationLoggingMask = servletConfig.getInitParameter("communicationLoggingMask");
      logMessage("\"communicationLoggingMask\" = \"" + communicationLoggingMask + "\"");
      try
      {
        communicationLoggingMask = Integer.parseInt(communicationLoggingMask);
      }
      catch (NumberFormatException e)
      {
        logMessage("Invalid value for communicationLogginMask " + communicationLoggingMask + ", using default 0");
      }
      String sendToPeersLimit = servletConfig.getInitParameter("sendToPeersLimit");
      logMessage("\"sendToPeersLimit\" = \"" + sendToPeersLimit + "\"");
      try
      {
        sendToPeersLimit = Integer.parseInt(sendToPeersLimit);
      }
      catch (NumberFormatException e)
      {
        sendToPeersLimit = 10;
        logMessage("Invalid value for sendToPeersLimit " + sendToPeersLimit + ", using default " + sendToPeersLimit);
      }
      try
      {
        logMessage("Loading transactions...");
        Nxt.Transaction.loadTransactions("transactions.nxt");
        logMessage("...Done");
      }
      catch (FileNotFoundException e)
      {
        logMessage("transactions.nxt not found, starting from scratch");
        transactions.clear();
        
        long[] recipients = { new BigInteger("163918645372308887").longValue(), new BigInteger("620741658595224146").longValue(), new BigInteger("723492359641172834").longValue(), new BigInteger("818877006463198736").longValue(), new BigInteger("1264744488939798088").longValue(), new BigInteger("1600633904360147460").longValue(), new BigInteger("1796652256451468602").longValue(), new BigInteger("1814189588814307776").longValue(), new BigInteger("1965151371996418680").longValue(), new BigInteger("2175830371415049383").longValue(), new BigInteger("2401730748874927467").longValue(), new BigInteger("2584657662098653454").longValue(), new BigInteger("2694765945307858403").longValue(), new BigInteger("3143507805486077020").longValue(), new BigInteger("3684449848581573439").longValue(), new BigInteger("4071545868996394636").longValue(), new BigInteger("4277298711855908797").longValue(), new BigInteger("4454381633636789149").longValue(), new BigInteger("4747512364439223888").longValue(), new BigInteger("4777958973882919649").longValue(), new BigInteger("4803826772380379922").longValue(), new BigInteger("5095742298090230979").longValue(), new BigInteger("5271441507933314159").longValue(), new BigInteger("5430757907205901788").longValue(), new BigInteger("5491587494620055787").longValue(), new BigInteger("5622658764175897611").longValue(), new BigInteger("5982846390354787993").longValue(), new BigInteger("6290807999797358345").longValue(), new BigInteger("6785084810899231190").longValue(), new BigInteger("6878906112724074600").longValue(), new BigInteger("7017504655955743955").longValue(), new BigInteger("7085298282228890923").longValue(), new BigInteger("7446331133773682477").longValue(), new BigInteger("7542917420413518667").longValue(), new BigInteger("7549995577397145669").longValue(), new BigInteger("7577840883495855927").longValue(), new BigInteger("7579216551136708118").longValue(), new BigInteger("8278234497743900807").longValue(), new BigInteger("8517842408878875334").longValue(), new BigInteger("8870453786186409991").longValue(), new BigInteger("9037328626462718729").longValue(), new BigInteger("9161949457233564608").longValue(), new BigInteger("9230759115816986914").longValue(), new BigInteger("9306550122583806885").longValue(), new BigInteger("9433259657262176905").longValue(), new BigInteger("9988839211066715803").longValue(), new BigInteger("10105875265190846103").longValue(), new BigInteger("10339765764359265796").longValue(), new BigInteger("10738613957974090819").longValue(), new BigInteger("10890046632913063215").longValue(), new BigInteger("11494237785755831723").longValue(), new BigInteger("11541844302056663007").longValue(), new BigInteger("11706312660844961581").longValue(), new BigInteger("12101431510634235443").longValue(), new BigInteger("12186190861869148835").longValue(), new BigInteger("12558748907112364526").longValue(), new BigInteger("13138516747685979557").longValue(), new BigInteger("13330279748251018740").longValue(), new BigInteger("14274119416917666908").longValue(), new BigInteger("14557384677985343260").longValue(), new BigInteger("14748294830376619968").longValue(), new BigInteger("14839596582718854826").longValue(), new BigInteger("15190676494543480574").longValue(), new BigInteger("15253761794338766759").longValue(), new BigInteger("15558257163011348529").longValue(), new BigInteger("15874940801139996458").longValue(), new BigInteger("16516270647696160902").longValue(), new BigInteger("17156841960446798306").longValue(), new BigInteger("17228894143802851995").longValue(), new BigInteger("17240396975291969151").longValue(), new BigInteger("17491178046969559641").longValue(), new BigInteger("18345202375028346230").longValue(), new BigInteger("18388669820699395594").longValue() };
        







































































        int[] amounts = { 36742, 1970092, 349130, 24880020, 2867856, 9975150, 2690963, 7648, 5486333, 34913026, 997515, 30922966, 6650, 44888, 2468850, 49875751, 49875751, 9476393, 49875751, 14887912, 528683, 583546, 7315, 19925363, 29856290, 5320, 4987575, 5985, 24912938, 49875751, 2724712, 1482474, 200999, 1476156, 498758, 987540, 16625250, 5264386, 15487585, 2684479, 14962725, 34913026, 5033128, 2916900, 49875751, 4962637, 170486123, 8644631, 22166945, 6668388, 233751, 4987575, 11083556, 1845403, 49876, 3491, 3491, 9476, 49876, 6151, 682633, 49875751, 482964, 4988, 49875751, 4988, 9144, 503745, 49875751, 52370, 29437998, 585375, 9975150 };
        







































































        byte[][] signatures = { { 41, 115, -41, 7, 37, 21, -3, -41, 120, 119, 63, -101, 108, 48, -117, 1, -43, 32, 85, 95, 65, 42, 92, -22, 123, -36, 6, -99, -61, -53, 93, 7, 23, 8, -30, 65, 57, -127, -2, 42, -92, -104, 11, 72, -66, 108, 17, 113, 99, -117, -75, 123, 110, 107, 119, -25, 67, 64, 32, 117, 111, 54, 82, -14 }, { 118, 43, 84, -91, -110, -102, 100, -40, -33, -47, -13, -7, -88, 2, -42, -66, -38, -22, 105, -42, -69, 78, 51, -55, -48, 49, -89, 116, -96, -104, -114, 14, 94, 58, -115, -8, 111, -44, 76, -104, 54, -15, 126, 31, 6, -80, 65, 6, 124, 37, -73, 92, 4, 122, 122, -108, 1, -54, 31, -38, -117, -1, -52, -56 }, { 79, 100, -101, 107, -6, -61, 40, 32, -98, 32, 80, -59, -76, -23, -62, 38, 4, 105, -106, -105, -121, -85, 13, -98, -77, 126, -125, 103, 12, -41, 1, 2, 45, -62, -69, 102, 116, -61, 101, -14, -68, -31, 9, 110, 18, 2, 33, -98, -37, -128, 17, -19, 124, 125, -63, 92, -70, 96, 96, 125, 91, 8, -65, -12 }, { 58, -99, 14, -97, -75, -10, 110, -102, 119, -3, -2, -12, -82, -33, -27, 118, -19, 55, -109, 6, 110, -10, 108, 30, 94, -113, -5, -98, 19, 12, -125, 14, -77, 33, -128, -21, 36, -120, -12, -81, 64, 95, 67, -3, 100, 122, -47, 127, -92, 114, 68, 72, 2, -40, 80, 117, -17, -56, 103, 37, -119, 3, 22, 23 }, { 76, 22, 121, -4, -77, -127, 18, -102, 7, 94, -73, -96, 108, -11, 81, -18, -37, -85, -75, 86, -119, 94, 126, 95, 47, -36, -16, -50, -9, 95, 60, 15, 14, 93, -108, -83, -67, 29, 2, -53, 10, -118, -51, -46, 92, -23, -56, 60, 46, -90, -84, 126, 60, 78, 12, 53, 61, 121, -6, 77, 112, 60, 40, 63 }, { 64, 121, -73, 68, 4, -103, 81, 55, -41, -81, -63, 10, 117, -74, 54, -13, -85, 79, 21, 116, -25, -12, 21, 120, -36, -80, 53, -78, 103, 25, 55, 6, -75, 96, 80, -125, -11, -103, -20, -41, 121, -61, -40, 63, 24, -81, -125, 90, -12, -40, -52, -1, -114, 14, -44, -112, -80, 83, -63, 88, -107, -10, -114, -86 }, { -81, 126, -41, -34, 66, -114, -114, 114, 39, 32, -125, -19, -95, -50, -111, -51, -33, 51, 99, -127, 58, 50, -110, 44, 80, -94, -96, 68, -69, 34, 86, 3, -82, -69, 28, 20, -111, 69, 18, -41, -23, 27, -118, 20, 72, 21, -112, 53, -87, -81, -47, -101, 123, -80, 99, -15, 33, -120, -8, 82, 80, -8, -10, -45 }, { 92, 77, 53, -87, 26, 13, -121, -39, -62, -42, 47, 4, 7, 108, -15, 112, 103, 38, -50, -74, 60, 56, -63, 43, -116, 49, -106, 69, 118, 65, 17, 12, 31, 127, -94, 55, -117, -29, -117, 31, -95, -110, -2, 63, -73, -106, -88, -41, -19, 69, 60, -17, -16, 61, 32, -23, 88, -106, -96, 37, -96, 114, -19, -99 }, { 68, -26, 57, -56, -30, 108, 61, 24, 106, -56, -92, 99, -59, 107, 25, -110, -57, 80, 79, -92, -107, 90, 54, -73, -40, -39, 78, 109, -57, -62, -17, 6, -25, -29, 37, 90, -24, -27, -61, -69, 44, 121, 107, -72, -57, 108, 32, -69, -21, -41, 126, 91, 118, 11, -91, 50, -11, 116, 126, -96, -39, 110, 105, -52 }, { 48, 108, 123, 50, -50, -58, 33, 14, 59, 102, 17, -18, -119, 4, 10, -29, 36, -56, -31, 43, -71, -48, -14, 87, 119, -119, 40, 104, -44, -76, -24, 2, 48, -96, -7, 16, -119, -3, 108, 78, 125, 88, 61, -53, -3, -16, 20, -83, 74, 124, -47, -17, -15, -21, -23, -119, -47, 105, -4, 115, -20, 77, 57, 88 }, { 33, 101, 79, -35, 32, -119, 20, 120, 34, -80, -41, 90, -22, 93, -20, -45, 9, 24, -46, 80, -55, -9, -24, -78, -124, 27, -120, -36, -51, 59, -38, 7, 113, 125, 68, 109, 24, -121, 111, 37, -71, 100, -111, 78, -43, -14, -76, -44, 64, 103, 16, -28, -44, -103, 74, 81, -118, -74, 47, -77, -65, 8, 42, -100 }, { -63, -96, -95, -111, -85, -98, -85, 42, 87, 29, -62, -57, 57, 48, 9, -39, -110, 63, -103, -114, -48, -11, -92, 105, -26, -79, -11, 78, -118, 14, -39, 1, -115, 74, 70, -41, -119, -68, -39, -60, 64, 31, 25, -111, -16, -20, 61, -22, 17, -13, 57, -110, 24, 61, -104, 21, -72, -69, 56, 116, -117, 93, -1, -123 }, { -18, -70, 12, 112, -111, 10, 22, 31, -120, 26, 53, 14, 10, 69, 51, 45, -50, -127, -22, 95, 54, 17, -8, 54, -115, 36, -79, 12, -79, 82, 4, 1, 92, 59, 23, -13, -85, -87, -110, -58, 84, -31, -48, -105, -101, -92, -9, 28, -109, 77, -47, 100, -48, -83, 106, -102, 70, -95, 94, -1, -99, -15, 21, 99 }, { 109, 123, 54, 40, -120, 32, -118, 49, -52, 0, -103, 103, 101, -9, 32, 78, 124, -56, 88, -19, 101, -32, 70, 67, -41, 85, -103, 1, 1, -105, -51, 10, 4, 51, -26, -19, 39, -43, 63, -41, -101, 80, 106, -66, 125, 47, -117, -120, -93, -120, 99, -113, -17, 61, 102, -2, 72, 9, -124, 123, -128, 78, 43, 96 }, { -22, -63, 20, 65, 5, -89, -123, -61, 14, 34, 83, -113, 34, 85, 26, -21, 1, 16, 88, 55, -92, -111, 14, -31, -37, -67, -8, 85, 39, -112, -33, 8, 28, 16, 107, -29, 1, 3, 100, -53, 2, 81, 52, -94, -14, 36, -123, -82, -6, -118, 104, 75, -99, -82, -100, 7, 30, -66, 0, -59, 108, -54, 31, 20 }, { 0, 13, -74, 28, -54, -12, 45, 36, -24, 55, 43, -110, -72, 117, -110, -56, -72, 85, 79, -89, -92, 65, -67, -34, -24, 38, 67, 42, 84, -94, 91, 13, 100, 89, 20, -95, -76, 2, 116, 34, 67, 52, -80, -101, -22, -32, 51, 32, -76, 44, -93, 11, 42, -69, -12, 7, -52, -55, 122, -10, 48, 21, 92, 110 }, { -115, 19, 115, 28, -56, 118, 111, 26, 18, 123, 111, -96, -115, 120, 105, 62, -123, -124, 101, 51, 3, 18, -89, 127, 48, -27, 39, -78, -118, 5, -2, 6, -105, 17, 123, 26, 25, -62, -37, 49, 117, 3, 10, 97, -7, 54, 121, -90, -51, -49, 11, 104, -66, 11, -6, 57, 5, -64, -8, 59, 82, -126, 26, -113 }, { 16, -53, 94, 99, -46, -29, 64, -89, -59, 116, -21, 53, 14, -77, -71, 95, 22, -121, -51, 125, -14, -96, 95, 95, 32, 96, 79, 41, -39, -128, 79, 0, 5, 6, -115, 104, 103, 77, -92, 93, -109, 58, 96, 97, -22, 116, -62, 11, 30, -122, 14, 28, 69, 124, 63, -119, 19, 80, -36, -116, -76, -58, 36, 87 }, { 109, -82, 33, -119, 17, 109, -109, -16, 98, 108, 111, 5, 98, 1, -15, -32, 22, 46, -65, 117, -78, 119, 35, -35, -3, 41, 23, -97, 55, 69, 58, 9, 20, -113, -121, -13, -41, -48, 22, -73, -1, -44, -73, 3, -10, -122, 19, -103, 10, -26, -128, 62, 34, 55, 54, -43, 35, -30, 115, 64, -80, -20, -25, 67 }, { -16, -74, -116, -128, 52, 96, -75, 17, -22, 72, -43, 22, -95, -16, 32, -72, 98, 46, -4, 83, 34, -58, -108, 18, 17, -58, -123, 53, -108, 116, 18, 2, 7, -94, -126, -45, 72, -69, -65, -89, 64, 31, -78, 78, -115, 106, 67, 55, -123, 104, -128, 36, -23, -90, -14, -87, 78, 19, 18, -128, 39, 73, 35, 120 }, { 20, -30, 15, 111, -82, 39, -108, 57, -80, 98, -19, -27, 100, -18, 47, 77, -41, 95, 80, -113, -128, -88, -76, 115, 65, -53, 83, 115, 7, 2, -104, 3, 120, 115, 14, -116, 33, -15, -120, 22, -56, -8, -69, 5, -75, 94, 124, 12, -126, -48, 51, -105, 22, -66, -93, 16, -63, -74, 32, 114, -54, -3, -47, -126 }, { 56, -101, 55, -1, 64, 4, -64, 95, 31, -15, 72, 46, 67, -9, 68, -43, -55, 28, -63, -17, -16, 65, 11, -91, -91, 32, 88, 41, 60, 67, 105, 8, 58, 102, -79, -5, -113, -113, -67, 82, 50, -26, 116, -78, -103, 107, 102, 23, -74, -47, 115, -50, -35, 63, -80, -32, 72, 117, 47, 68, 86, -20, -35, 8 }, { 21, 27, 20, -59, 117, -102, -42, 22, -10, 121, 41, -59, 115, 15, -43, 54, -79, -62, -16, 58, 116, 15, 88, 108, 114, 67, 3, -30, -99, 78, 103, 11, 49, 63, -4, -110, -27, 41, 70, -57, -69, -18, 70, 30, -21, 66, -104, -27, 3, 53, 50, 100, -33, 54, -3, -78, 92, 85, -78, 54, 19, 32, 95, 9 }, { -93, 65, -64, -79, 82, 85, -34, -90, 122, -29, -40, 3, -80, -40, 32, 26, 102, -73, 17, 53, -93, -29, 122, 86, 107, -100, 50, 56, -28, 124, 90, 14, 93, -88, 97, 101, -85, -50, 46, -109, -88, -127, -112, 63, -89, 24, -34, -9, -116, -59, -87, -86, -12, 111, -111, 87, -87, -13, -73, -124, -47, 7, 1, 9 }, { 60, -99, -77, -20, 112, -75, -34, 100, -4, -96, 81, 71, -116, -62, 38, -68, 105, 7, -126, 21, -125, -25, -56, -11, -59, 95, 117, 108, 32, -38, -65, 13, 46, 65, -46, -89, 0, 120, 5, 23, 40, 110, 114, 79, 111, -70, 8, 16, -49, -52, -82, -18, 108, -43, 81, 96, 72, -65, 70, 7, -37, 58, 46, -14 }, { -95, -32, 85, 78, 74, -53, 93, -102, -26, -110, 86, 1, -93, -50, -23, -108, -37, 97, 19, 103, 94, -65, -127, -21, 60, 98, -51, -118, 82, -31, 27, 7, -112, -45, 79, 95, -20, 90, -4, -40, 117, 100, -6, 19, -47, 53, 53, 48, 105, 91, -70, -34, -5, -87, -57, -103, -112, -108, -40, 87, -25, 13, -76, -116 }, { 44, -122, -70, 125, -60, -32, 38, 69, -77, -103, 49, -124, -4, 75, -41, -84, 68, 74, 118, 15, -13, 115, 117, -78, 42, 89, 0, -20, -12, -58, -97, 10, -48, 95, 81, 101, 23, -67, -23, 74, -79, 21, 97, 123, 103, 101, -50, -115, 116, 112, 51, 50, -124, 27, 76, 40, 74, 10, 65, -49, 102, 95, 5, 35 }, { -6, 57, 71, 5, -61, -100, -21, -9, 47, -60, 59, 108, -75, 105, 56, 41, -119, 31, 37, 27, -86, 120, -125, -108, 121, 104, -21, -70, -57, -104, 2, 11, 118, 104, 68, 6, 7, -90, -70, -61, -16, 77, -8, 88, 31, -26, 35, -44, 8, 50, 51, -88, -62, -103, 54, -41, -2, 117, 98, -34, 49, -123, 83, -58 }, { 54, 21, -36, 126, -50, -72, 82, -5, -122, -116, 72, -19, -18, -68, -71, -27, 97, -22, 53, -94, 47, -6, 15, -92, -55, 127, 13, 13, -69, 81, -82, 8, -50, 10, 84, 110, -87, -44, 61, -78, -65, 84, -32, 48, -8, -105, 35, 116, -68, -116, -6, 75, -77, 120, -95, 74, 73, 105, 39, -87, 98, -53, 47, 10 }, { -113, 116, 37, -1, 95, -89, -93, 113, 36, -70, -57, -99, 94, 52, -81, -118, 98, 58, -36, 73, 82, -67, -80, 46, 83, -127, -8, 73, 66, -27, 43, 7, 108, 32, 73, 1, -56, -108, 41, -98, -15, 49, 1, 107, 65, 44, -68, 126, -28, -53, 120, -114, 126, -79, -14, -105, -33, 53, 5, -119, 67, 52, 35, -29 }, { 98, 23, 23, 83, 78, -89, 13, 55, -83, 97, -30, -67, 99, 24, 47, -4, -117, -34, -79, -97, 95, 74, 4, 21, 66, -26, 15, 80, 60, -25, -118, 14, 36, -55, -41, -124, 90, -1, 84, 52, 31, 88, 83, 121, -47, -59, -10, 17, 51, -83, 23, 108, 19, 104, 32, 29, -66, 24, 21, 110, 104, -71, -23, -103 }, { 12, -23, 60, 35, 6, -52, -67, 96, 15, -128, -47, -15, 40, 3, 54, -81, 3, 94, 3, -98, -94, -13, -74, -101, 40, -92, 90, -64, -98, 68, -25, 2, -62, -43, 112, 32, -78, -123, 26, -80, 126, 120, -88, -92, 126, -128, 73, -43, 87, -119, 81, 111, 95, -56, -128, -14, 51, -40, -42, 102, 46, 106, 6, 6 }, { -38, -120, -11, -114, -7, -105, -98, 74, 114, 49, 64, -100, 4, 40, 110, -21, 25, 6, -92, -40, -61, 48, 94, -116, -71, -87, 75, -31, 13, -119, 1, 5, 33, -69, -16, -125, -79, -46, -36, 3, 40, 1, -88, -118, -107, 95, -23, -107, -49, 44, -39, 2, 108, -23, 39, 50, -51, -59, -4, -42, -10, 60, 10, -103 }, { 67, -53, 55, -32, -117, 3, 94, 52, -115, -127, -109, 116, -121, -27, -115, -23, 98, 90, -2, 48, -54, -76, 108, -56, 99, 30, -35, -18, -59, 25, -122, 3, 43, -13, -109, 34, -10, 123, 117, 113, -112, -85, -119, -62, -78, -114, -96, 101, 72, -98, 28, 89, -98, -121, 20, 115, 89, -20, 94, -55, 124, 27, -76, 94 }, { 15, -101, 98, -21, 8, 5, -114, -64, 74, 123, 99, 28, 125, -33, 22, 23, -2, -56, 13, 91, 27, -105, -113, 19, 60, -7, -67, 107, 70, 103, -107, 13, -38, -108, -77, -29, 2, 9, -12, 21, 12, 65, 108, -16, 69, 77, 96, -54, 55, -78, -7, 41, -48, 124, -12, 64, 113, -45, -21, -119, -113, 88, -116, 113 }, { -17, 77, 10, 84, -57, -12, 101, 21, -91, 92, 17, -32, -26, 77, 70, 46, 81, -55, 40, 44, 118, -35, -97, 47, 5, 125, 41, -127, -72, 66, -18, 2, 115, -13, -74, 126, 86, 80, 11, -122, -29, -68, 113, 54, -117, 107, -75, -107, -54, 72, -44, 98, -111, -33, -56, -40, 93, -47, 84, -43, -45, 86, 65, -84 }, { -126, 60, -56, 121, 31, -124, -109, 100, -118, -29, 106, 94, 5, 27, 13, -79, 91, -111, -38, -42, 18, 61, -100, 118, -18, -4, -60, 121, 46, -22, 6, 4, -37, -20, 124, -43, 51, -57, -49, -44, -24, -38, 81, 60, -14, -97, -109, -11, -5, -85, 75, -17, -124, -96, -53, 52, 64, 100, -118, -120, 6, 60, 76, -110 }, { -12, -40, 115, -41, 68, 85, 20, 91, -44, -5, 73, -105, -81, 32, 116, 32, -28, 69, 88, -54, 29, -53, -51, -83, 54, 93, -102, -102, -23, 7, 110, 15, 34, 122, 84, 52, -121, 37, -103, -91, 37, -77, -101, 64, -18, 63, -27, -75, -112, -11, 1, -69, -25, -123, -99, -31, 116, 11, 4, -42, -124, 98, -2, 53 }, { -128, -69, -16, -33, -8, -112, 39, -57, 113, -76, -29, -37, 4, 121, -63, 12, -54, -66, -121, 13, -4, -44, 50, 27, 103, 101, 44, -115, 12, -4, -8, 10, 53, 108, 90, -47, 46, -113, 5, -3, -111, 8, -66, -73, 57, 72, 90, -33, 47, 99, 50, -55, 53, -4, 96, 87, 57, 26, 53, -45, -83, 39, -17, 45 }, { -121, 125, 60, -9, -79, -128, -19, 113, 54, 77, -23, -89, 105, -5, 47, 114, -120, -88, 31, 25, -96, -75, -6, 76, 9, -83, 75, -109, -126, -47, -6, 2, -59, 64, 3, 74, 100, 110, -96, 66, -3, 10, -124, -6, 8, 50, 109, 14, -109, 79, 73, 77, 67, 63, -50, 10, 86, -63, -125, -86, 35, -26, 7, 83 }, { 36, 31, -77, 126, 106, 97, 63, 81, -37, -126, 69, -127, -22, -69, 104, -111, 93, -49, 77, -3, -38, -112, 47, -55, -23, -68, -8, 78, -127, -28, -59, 10, 22, -61, -127, -13, -72, -14, -87, 14, 61, 76, -89, 81, -97, -97, -105, 94, -93, -9, -3, -104, -83, 59, 104, 121, -30, 106, -2, 62, -51, -72, -63, 55 }, { 81, -88, -8, -96, -31, 118, -23, -38, -94, 80, 35, -20, -93, -102, 124, 93, 0, 15, 36, -127, -41, -19, 6, -124, 94, -49, 44, 26, -69, 43, -58, 9, -18, -3, -2, 60, -122, -30, -47, 124, 71, 47, -74, -68, 4, -101, -16, 77, -120, -15, 45, -12, 68, -77, -74, 63, -113, 44, -71, 56, 122, -59, 53, -44 }, { 122, 30, 27, -79, 32, 115, -104, -28, -53, 109, 120, 121, -115, -65, -87, 101, 23, 10, 122, 101, 29, 32, 56, 63, -23, -48, -51, 51, 16, -124, -41, 6, -71, 49, -20, 26, -57, 65, 49, 45, 7, 49, -126, 54, -122, -43, 1, -5, 111, 117, 104, 117, 126, 114, -77, 66, -127, -50, 69, 14, 70, 73, 60, 112 }, { 104, -117, 105, -118, 35, 16, -16, 105, 27, -87, -43, -59, -13, -23, 5, 8, -112, -28, 18, -1, 48, 94, -82, 55, 32, 16, 59, -117, 108, -89, 101, 9, -35, 58, 70, 62, 65, 91, 14, -43, -104, 97, 1, -72, 16, -24, 79, 79, -85, -51, -79, -55, -128, 23, 109, -95, 17, 92, -38, 109, 65, -50, 46, -114 }, { 44, -3, 102, -60, -85, 66, 121, -119, 9, 82, -47, -117, 67, -28, 108, 57, -47, -52, -24, -82, 65, -13, 85, 107, -21, 16, -24, -85, 102, -92, 73, 5, 7, 21, 41, 47, -118, 72, 43, 51, -5, -64, 100, -34, -25, 53, -45, -115, 30, -72, -114, 126, 66, 60, -24, -67, 44, 48, 22, 117, -10, -33, -89, -108 }, { -7, 71, -93, -66, 3, 30, -124, -116, -48, -76, -7, -62, 125, -122, -60, -104, -30, 71, 36, -110, 34, -126, 31, 10, 108, 102, -53, 56, 104, -56, -48, 12, 25, 21, 19, -90, 45, -122, -73, -112, 97, 96, 115, 71, 127, -7, -46, 84, -24, 102, -104, -96, 28, 8, 37, -84, -13, -65, -6, 61, -85, -117, -30, 70 }, { -112, 39, -39, -24, 127, -115, 68, -1, -111, -43, 101, 20, -12, 39, -70, 67, -50, 68, 105, 69, -91, -106, 91, 4, -52, 75, 64, -121, 46, -117, 31, 10, -125, 77, 51, -3, -93, 58, 79, 121, 126, -29, 56, -101, 1, -28, 49, 16, -80, 92, -62, 83, 33, 17, 106, 89, -9, 60, 79, 38, -74, -48, 119, 24 }, { 105, -118, 34, 52, 111, 30, 38, -73, 125, -116, 90, 69, 2, 126, -34, -25, -41, -67, -23, -105, -12, -75, 10, 69, -51, -95, -101, 92, -80, -73, -120, 2, 71, 46, 11, -85, -18, 125, 81, 117, 33, -89, -42, 118, 51, 60, 89, 110, 97, -118, -111, -36, 75, 112, -4, -8, -36, -49, -55, 35, 92, 70, -37, 36 }, { 71, 4, -113, 13, -48, 29, -56, 82, 115, -38, -20, -79, -8, 126, -111, 5, -12, -56, -107, 98, 111, 19, 127, -10, -42, 24, -38, -123, 59, 51, -64, 3, 47, -1, -83, -127, -58, 86, 33, -76, 5, 71, -80, -50, -62, 116, 75, 20, -126, 23, -31, -21, 24, -83, -19, 114, -17, 1, 110, -70, -119, 126, 82, -83 }, { -77, -69, -45, -78, -78, 69, 35, 85, 84, 25, -66, -25, 53, -38, -2, 125, -38, 103, 88, 31, -9, -43, 15, -93, 69, -22, -13, -20, 73, 3, -100, 7, 26, -18, 123, -14, -78, 113, 79, -57, -109, -118, 105, -104, 75, -88, -24, -109, 73, -126, 9, 55, 98, -120, 93, 114, 74, 0, -86, -68, 47, 29, 75, 67 }, { -104, 11, -85, 16, -124, -91, 66, -91, 18, -67, -122, -57, -114, 88, 79, 11, -60, -119, 89, 64, 57, 120, -11, 8, 52, -18, -67, -127, 26, -19, -69, 2, -82, -56, 11, -90, -104, 110, -10, -68, 87, 21, 28, 87, -5, -74, -21, -84, 120, 70, -17, 102, 72, -116, -69, 108, -86, -79, -74, 115, -78, -67, 6, 45 }, { -6, -101, -17, 38, -25, -7, -93, 112, 13, -33, 121, 71, -79, -122, -95, 22, 47, -51, 16, 84, 55, -39, -26, 37, -36, -18, 11, 119, 106, -57, 42, 8, -1, 23, 7, -63, -9, -50, 30, 35, -125, 83, 9, -60, -94, -15, -76, 120, 18, -103, -70, 95, 26, 48, -103, -95, 10, 113, 66, 54, -96, -4, 37, 111 }, { -124, -53, 43, -59, -73, 99, 71, -36, -31, 61, -25, -14, -71, 48, 17, 10, -26, -21, -22, 104, 64, -128, 27, -40, 111, -70, -90, 91, -81, -88, -10, 11, -62, 127, -124, -2, -67, -69, 65, 73, 40, 82, 112, -112, 100, -26, 30, 86, 30, 1, -105, 45, 103, -47, -124, 58, 105, 24, 20, 108, -101, 84, -34, 80 }, { 28, -1, 84, 111, 43, 109, 57, -23, 52, -95, 110, -50, 77, 15, 80, 85, 125, -117, -10, 8, 59, -58, 18, 97, -58, 45, 92, -3, 56, 24, -117, 9, -73, -9, 48, -99, 50, -24, -3, -41, -43, 48, -77, -8, -89, -42, 126, 73, 28, -65, -108, 54, 6, 34, 32, 2, -73, -123, -106, -52, -73, -106, -112, 109 }, { 73, -76, -7, 49, 67, -34, 124, 80, 111, -91, -22, -121, -74, 42, -4, -18, 84, -3, 38, 126, 31, 54, -120, 65, -122, -14, -38, -80, -124, 90, 37, 1, 51, 123, 69, 48, 109, -112, -63, 27, 67, -127, 29, 79, -26, 99, -24, -100, 51, 103, -105, 13, 85, 74, 12, -37, 43, 80, -113, 6, 70, -107, -5, -80 }, { 110, -54, 109, 21, -124, 98, 90, -26, 69, -44, 17, 117, 78, -91, -7, -18, -81, -43, 20, 80, 48, -109, 117, 125, -67, 19, -15, 69, -28, 47, 15, 4, 34, -54, 51, -128, 18, 61, -77, -122, 100, -58, -118, -36, 5, 32, 43, 15, 60, -55, 120, 123, -77, -76, -121, 77, 93, 16, -73, 54, 46, -83, -39, 125 }, { 115, -15, -42, 111, -124, 52, 29, -124, -10, -23, 41, -128, 65, -60, -121, 6, -42, 14, 98, -80, 80, -46, -38, 64, 16, 84, -50, 47, -97, 11, -88, 12, 68, -127, -92, 87, -22, 54, -49, 33, -4, -68, 21, -7, -45, 84, 107, 57, 8, -106, 0, -87, -104, 93, -43, -98, -92, -72, 110, -14, -66, 119, 14, -68 }, { -19, 7, 7, 66, -94, 18, 36, 8, -58, -31, 21, -113, -124, -5, -12, 105, 40, -62, 57, -56, 25, 117, 49, 17, -33, 49, 105, 113, -26, 78, 97, 2, -22, -84, 49, 67, -6, 33, 89, 28, 30, 12, -3, -23, -45, 7, -4, -39, -20, 25, -91, 55, 53, 21, -94, 17, -54, 109, 125, 124, 122, 117, -125, 60 }, { -28, -104, -46, -22, 71, -79, 100, 48, -90, -57, -30, -23, -24, 1, 2, -31, 85, -6, -113, -116, 105, -31, -109, 106, 1, 78, -3, 103, -6, 100, -44, 15, -100, 97, 59, -42, 22, 83, 113, -118, 112, -57, 80, -45, -86, 72, 77, -26, -106, 50, 28, -24, 41, 22, -73, 108, 18, -93, 30, 8, -11, -16, 124, 106 }, { 16, -119, -109, 115, 67, 36, 28, 74, 101, -58, -82, 91, 4, -97, 111, -77, -37, -125, 126, 3, 10, -99, -115, 91, -66, -83, -81, 10, 7, 92, 26, 6, -45, 66, -26, 118, -77, 13, -91, 20, -18, -33, -103, 43, 75, -100, -5, -64, 117, 30, 5, -100, -90, 13, 18, -52, 26, 24, -10, 24, -31, 53, 88, 112 }, { 7, -90, 46, 109, -42, 108, -84, 124, -28, -63, 34, -19, -76, 88, -121, 23, 54, -73, -15, -52, 84, -119, 64, 20, 92, -91, -58, -121, -117, -90, -102, 1, 49, 21, 3, -85, -3, 38, 117, 73, -38, -71, -37, 40, -2, -50, -47, -46, 75, -105, 125, 126, -13, 68, 50, -81, -43, -93, 85, -79, 52, 98, 118, 50 }, { -104, 65, -61, 12, 68, 106, 37, -64, 40, -114, 61, 73, 74, 61, -113, -79, 57, 47, -57, -21, -68, -62, 23, -18, 93, -7, -55, -88, -106, 104, -126, 5, 53, 97, 100, -67, -112, -88, 41, 24, 95, 15, 25, -67, 79, -69, 53, 21, -128, -101, 73, 17, 7, -98, 5, -2, 33, -113, 99, -72, 125, 7, 18, -105 }, { -17, 28, 79, 34, 110, 86, 43, 27, -114, -112, -126, -98, -121, 126, -21, 111, 58, -114, -123, 75, 117, -116, 7, 107, 90, 80, -75, -121, 116, -11, -76, 0, -117, -52, 76, -116, 115, -117, 61, -7, 55, -34, 38, 101, 86, -19, -36, -92, -94, 61, 88, -128, -121, -103, 84, 19, -83, -102, 122, -111, 62, 112, 20, 3 }, { -127, -90, 28, -77, -48, -56, -10, 84, -41, 59, -115, 68, -74, -104, -119, -49, -37, -90, -57, 66, 108, 110, -62, -107, 88, 90, 29, -65, 74, -38, 95, 8, 120, 88, 96, -65, -109, 68, -63, -4, -16, 90, 7, 39, -56, -110, -100, 86, -39, -53, -89, -35, 127, -42, -48, -36, 53, -66, 109, -51, 51, -23, -12, 73 }, { -12, 78, 81, 30, 124, 22, 56, -112, 58, -99, 30, -98, 103, 66, 89, 92, -52, -20, 26, 82, -92, -18, 96, 7, 38, 21, -9, -25, -17, 4, 43, 15, 111, 103, -48, -50, -83, 52, 59, 103, 102, 83, -105, 87, 20, -120, 35, -7, -39, -24, 29, -39, -35, -87, 88, 120, 126, 19, 108, 34, -59, -20, 86, 47 }, { 19, -70, 36, 55, -42, -49, 33, 100, 105, -5, 89, 43, 3, -85, 60, -96, 43, -46, 86, -33, 120, -123, -99, -100, -34, 48, 82, -37, 34, 78, 127, 12, -39, -76, -26, 117, 74, -60, -68, -2, -37, -56, -6, 94, -27, 81, 32, -96, -19, -32, -77, 22, -56, -49, -38, -60, 45, -69, 40, 106, -106, -34, 101, -75 }, { 57, -92, -44, 8, -79, -88, -82, 58, -116, 93, 103, -127, 87, -121, -28, 31, -108, -14, -23, 38, 57, -83, -33, -110, 24, 6, 68, 124, -89, -35, -127, 5, -118, -78, -127, -35, 112, -34, 30, 24, -70, -71, 126, 39, -124, 122, -35, -97, -18, 25, 119, 79, 119, -65, 59, -20, -84, 120, -47, 4, -106, -125, -38, -113 }, { 18, -93, 34, -80, -43, 127, 57, -118, 24, -119, 25, 71, 59, -29, -108, -99, -122, 58, 44, 0, 42, -111, 25, 94, -36, 41, -64, -53, -78, -119, 85, 7, -45, -70, 81, -84, 71, -61, -68, -79, 112, 117, 19, 18, 70, 95, 108, -58, 48, 116, -89, 43, 66, 55, 37, -37, -60, 104, 47, -19, -56, 97, 73, 26 }, { 78, 4, -111, -36, 120, 111, -64, 46, 99, 125, -5, 97, -126, -21, 60, -78, -33, 89, 25, -60, 0, -49, 59, -118, 18, 3, -60, 30, 105, -92, -101, 15, 63, 50, 25, 2, -116, 78, -5, -25, -59, 74, -116, 64, -55, -121, 1, 69, 51, -119, 43, -6, -81, 14, 5, 84, -67, -73, 67, 24, 82, -37, 109, -93 }, { -44, -30, -64, -68, -21, 74, 124, 122, 114, -89, -91, -51, 89, 32, 96, -1, -101, -112, -94, 98, -24, -31, -50, 100, -72, 56, 24, 30, 105, 115, 15, 3, -67, 107, -18, 111, -38, -93, -11, 24, 36, 73, -23, 108, 14, -41, -65, 32, 51, 22, 95, 41, 85, -121, -35, -107, 0, 105, -112, 59, 48, -22, -84, 46 }, { 4, 38, 54, -84, -78, 24, -48, 8, -117, 78, -95, 24, 25, -32, -61, 26, -97, -74, 46, -120, -125, 27, 73, 107, -17, -21, -6, -52, 47, -68, 66, 5, -62, -12, -102, -127, 48, -69, -91, -81, -33, -13, -9, -12, -44, -73, 40, -58, 120, -120, 108, 101, 18, -14, -17, -93, 113, 49, 76, -4, -113, -91, -93, -52 }, { 28, -48, 70, -35, 123, -31, 16, -52, 72, 84, -51, 78, 104, 59, -102, -112, 29, 28, 25, 66, 12, 75, 26, -85, 56, -12, -4, -92, 49, 86, -27, 12, 44, -63, 108, 82, -76, -97, -41, 95, -48, -95, -115, 1, 64, -49, -97, 90, 65, 46, -114, -127, -92, 79, 100, 49, 116, -58, -106, 9, 117, -7, -91, 96 }, { 58, 26, 18, 76, 127, -77, -58, -87, -116, -44, 60, -32, -4, -76, -124, 4, -60, 82, -5, -100, -95, 18, 2, -53, -50, -96, -126, 105, 93, 19, 74, 13, 87, 125, -72, -10, 42, 14, 91, 44, 78, 52, 60, -59, -27, -37, -57, 17, -85, 31, -46, 113, 100, -117, 15, 108, -42, 12, 47, 63, 1, 11, -122, -3 } };
        for (int i = 0; i < recipients.length; i++)
        {
          Nxt.Transaction transaction = new Nxt.Transaction((byte)0, (byte)0, 0, (short)0, new byte[] { 18, 89, -20, 33, -45, 26, 48, -119, -115, 124, -47, 96, -97, -128, -39, 102, -117, 71, 120, -29, -39, 126, -108, 16, 68, -77, -97, 12, 68, -46, -27, 27 }, recipients[i], amounts[i], 0, 0L, signatures[i]);
          
          transactions.put(Long.valueOf(transaction.getId()), transaction);
        }
        for (Nxt.Transaction transaction : transactions.values())
        {
          transaction.index = transactionCounter.incrementAndGet();
          transaction.block = 2680262203532249785L;
        }
        Nxt.Transaction.saveTransactions("transactions.nxt");
      }
      try
      {
        logMessage("Loading blocks...");
        Nxt.Block.loadBlocks("blocks.nxt");
        logMessage("...Done");
      }
      catch (FileNotFoundException e)
      {
        logMessage("blocks.nxt not found, starting from scratch");
        blocks.clear();
        
        Nxt.Block block = new Nxt.Block(-1, 0, 0L, transactions.size(), 1000000000, 0, transactions.size() * 128, null, new byte[] { 18, 89, -20, 33, -45, 26, 48, -119, -115, 124, -47, 96, -97, -128, -39, 102, -117, 71, 120, -29, -39, 126, -108, 16, 68, -77, -97, 12, 68, -46, -27, 27 }, new byte[64], new byte[] { 105, -44, 38, -60, -104, -73, 10, -58, -47, 103, -127, -128, 53, 101, 39, -63, -2, -32, 48, -83, 115, 47, -65, 118, 114, -62, 38, 109, 22, 106, 76, 8, -49, -113, -34, -76, 82, 79, -47, -76, -106, -69, -54, -85, 3, -6, 110, 103, 118, 15, 109, -92, 82, 37, 20, 2, 36, -112, 21, 72, 108, 72, 114, 17 });
        block.index = blockCounter.incrementAndGet();
        blocks.put(Long.valueOf(2680262203532249785L), block);
        
        int i = 0;
        for (Iterator i$ = transactions.keySet().iterator(); i$.hasNext();)
        {
          long transaction = ((Long)i$.next()).longValue();
          
          block.transactions[(i++)] = transaction;
        }
        Arrays.sort(block.transactions);
        MessageDigest digest = getMessageDigest("SHA-256");
        for (long transactionId : block.transactions) {
          digest.update(((Nxt.Transaction)transactions.get(Long.valueOf(transactionId))).getBytes());
        }
        block.payloadHash = digest.digest();
        
        block.baseTarget = 153722867L;
        lastBlock = 2680262203532249785L;
        block.cumulativeDifficulty = BigInteger.ZERO;
        
        Nxt.Block.saveBlocks("blocks.nxt", false);
      }
      logMessage("Scanning blockchain...");
      Map<Long, Nxt.Block> loadedBlocks = new HashMap(blocks);
      blocks.clear();
      lastBlock = 2680262203532249785L;
      long curBlockId = 2680262203532249785L;
      do
      {
        Nxt.Block curBlock = (Nxt.Block)loadedBlocks.get(Long.valueOf(curBlockId));
        long nextBlockId = curBlock.nextBlock;
        curBlock.analyze();
        curBlockId = nextBlockId;
      } while (curBlockId != 0L);
      logMessage("...Done");
      
      scheduledThreadPool.scheduleWithFixedDelay(new Nxt.2(this), 0L, 5L, TimeUnit.SECONDS);
      




























      scheduledThreadPool.scheduleWithFixedDelay(new Nxt.3(this), 0L, 1L, TimeUnit.SECONDS);
      





























      scheduledThreadPool.scheduleWithFixedDelay(new Nxt.4(this), 0L, 5L, TimeUnit.SECONDS);
      












































      scheduledThreadPool.scheduleWithFixedDelay(new Nxt.5(this), 0L, 5L, TimeUnit.SECONDS);
      


































      scheduledThreadPool.scheduleWithFixedDelay(new Nxt.6(this), 0L, 1L, TimeUnit.SECONDS);
      























































      scheduledThreadPool.scheduleWithFixedDelay(new Nxt.7(this), 0L, 1L, TimeUnit.SECONDS);
      




























































































































































































































































































      scheduledThreadPool.scheduleWithFixedDelay(new Nxt.8(this), 0L, 1L, TimeUnit.SECONDS);
      




















































































      logMessage("NRS 0.5.7 started successfully.");
    }
    catch (Exception e)
    {
      logMessage("Error initializing Nxt servlet", e);
      System.exit(1);
    }
  }
  
  public void doGet(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException
  {
    resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private");
    resp.setHeader("Pragma", "no-cache");
    resp.setDateHeader("Expires", 0L);
    
    Nxt.User user = null;
    long accountId;
    try
    {
      String userPasscode = req.getParameter("user");
      String secretPhrase;
      if (userPasscode == null)
      {
        JSONObject response = new JSONObject();
        if ((allowedBotHosts != null) && (!allowedBotHosts.contains(req.getRemoteHost())))
        {
          response.put("errorCode", Integer.valueOf(7));
          response.put("errorDescription", "Not allowed");
        }
        else
        {
          String requestType = req.getParameter("requestType");
          if (requestType == null)
          {
            response.put("errorCode", Integer.valueOf(1));
            response.put("errorDescription", "Incorrect request");
          }
          else
          {
            localObject1 = requestType;int i = -1;
            switch (((String)localObject1).hashCode())
            {
            case 1708941985: 
              if (((String)localObject1).equals("assignAlias")) {
                i = 0;
              }
              break;
            case 62209885: 
              if (((String)localObject1).equals("broadcastTransaction")) {
                i = 1;
              }
              break;
            case -907924844: 
              if (((String)localObject1).equals("decodeHallmark")) {
                i = 2;
              }
              break;
            case 1183136939: 
              if (((String)localObject1).equals("decodeToken")) {
                i = 3;
              }
              break;
            case -347064830: 
              if (((String)localObject1).equals("getAccountBlockIds")) {
                i = 4;
              }
              break;
            case -1836634766: 
              if (((String)localObject1).equals("getAccountId")) {
                i = 5;
              }
              break;
            case -1594290433: 
              if (((String)localObject1).equals("getAccountPublicKey")) {
                i = 6;
              }
              break;
            case -1415951151: 
              if (((String)localObject1).equals("getAccountTransactionIds")) {
                i = 7;
              }
              break;
            case 1948728474: 
              if (((String)localObject1).equals("getAlias")) {
                i = 8;
              }
              break;
            case 122324821: 
              if (((String)localObject1).equals("getAliasId")) {
                i = 9;
              }
              break;
            case -502897730: 
              if (((String)localObject1).equals("getAliasIds")) {
                i = 10;
              }
              break;
            case -502886798: 
              if (((String)localObject1).equals("getAliasURI")) {
                i = 11;
              }
              break;
            case 697674406: 
              if (((String)localObject1).equals("getBalance")) {
                i = 12;
              }
              break;
            case 1949657815: 
              if (((String)localObject1).equals("getBlock")) {
                i = 13;
              }
              break;
            case -431881575: 
              if (((String)localObject1).equals("getConstants")) {
                i = 14;
              }
              break;
            case 1755958186: 
              if (((String)localObject1).equals("getGuaranteedBalance")) {
                i = 15;
              }
              break;
            case 635655024: 
              if (((String)localObject1).equals("getMyInfo")) {
                i = 16;
              }
              break;
            case -75245096: 
              if (((String)localObject1).equals("getPeer")) {
                i = 17;
              }
              break;
            case 1962369435: 
              if (((String)localObject1).equals("getPeers")) {
                i = 18;
              }
              break;
            case 1965583067: 
              if (((String)localObject1).equals("getState")) {
                i = 19;
              }
              break;
            case -75121853: 
              if (((String)localObject1).equals("getTime")) {
                i = 20;
              }
              break;
            case 1500977576: 
              if (((String)localObject1).equals("getTransaction")) {
                i = 21;
              }
              break;
            case -996573277: 
              if (((String)localObject1).equals("getTransactionBytes")) {
                i = 22;
              }
              break;
            case -1835768118: 
              if (((String)localObject1).equals("getUnconfirmedTransactionIds")) {
                i = 23;
              }
              break;
            case -944172977: 
              if (((String)localObject1).equals("listAccountAliases")) {
                i = 24;
              }
              break;
            case 246104597: 
              if (((String)localObject1).equals("markHost")) {
                i = 25;
              }
              break;
            case 691453791: 
              if (((String)localObject1).equals("sendMessage")) {
                i = 26;
              }
              break;
            case 9950744: 
              if (((String)localObject1).equals("sendMoney")) {
                i = 27;
              }
              break;
            }
            switch (i)
            {
            case 0: 
              String secretPhrase = req.getParameter("secretPhrase");
              String alias = req.getParameter("alias");
              String uri = req.getParameter("uri");
              String feeValue = req.getParameter("fee");
              String deadlineValue = req.getParameter("deadline");
              String referencedTransactionValue = req.getParameter("referencedTransaction");
              if (secretPhrase == null)
              {
                response.put("errorCode", Integer.valueOf(3));
                response.put("errorDescription", "\"secretPhrase\" not specified");
              }
              else if (alias == null)
              {
                response.put("errorCode", Integer.valueOf(3));
                response.put("errorDescription", "\"alias\" not specified");
              }
              else if (uri == null)
              {
                response.put("errorCode", Integer.valueOf(3));
                response.put("errorDescription", "\"uri\" not specified");
              }
              else if (feeValue == null)
              {
                response.put("errorCode", Integer.valueOf(3));
                response.put("errorDescription", "\"fee\" not specified");
              }
              else if (deadlineValue == null)
              {
                response.put("errorCode", Integer.valueOf(3));
                response.put("errorDescription", "\"deadline\" not specified");
              }
              else
              {
                alias = alias.trim();
                if ((alias.length() == 0) || (alias.length() > 100))
                {
                  response.put("errorCode", Integer.valueOf(4));
                  response.put("errorDescription", "Incorrect \"alias\" (length must be in [1..100] range)");
                }
                else
                {
                  String normalizedAlias = alias.toLowerCase();
                  for (int i = 0; i < normalizedAlias.length(); i++) {
                    if ("0123456789abcdefghijklmnopqrstuvwxyz".indexOf(normalizedAlias.charAt(i)) < 0) {
                      break;
                    }
                  }
                  if (i != normalizedAlias.length())
                  {
                    response.put("errorCode", Integer.valueOf(4));
                    response.put("errorDescription", "Incorrect \"alias\" (must contain only digits and latin letters)");
                  }
                  else
                  {
                    uri = uri.trim();
                    if (uri.length() > 1000)
                    {
                      response.put("errorCode", Integer.valueOf(4));
                      response.put("errorDescription", "Incorrect \"uri\" (length must be not longer than 1000 characters)");
                    }
                    else
                    {
                      try
                      {
                        int fee = Integer.parseInt(feeValue);
                        if ((fee <= 0) || (fee >= 1000000000L)) {
                          throw new Exception();
                        }
                        try
                        {
                          short deadline = Short.parseShort(deadlineValue);
                          if (deadline < 1) {
                            throw new Exception();
                          }
                          long referencedTransaction = referencedTransactionValue == null ? 0L : parseUnsignedLong(referencedTransactionValue);
                          
                          byte[] publicKey = Nxt.Crypto.getPublicKey(secretPhrase);
                          long accountId = Nxt.Account.getId(publicKey);
                          Nxt.Account account = (Nxt.Account)accounts.get(Long.valueOf(accountId));
                          if (account == null)
                          {
                            response.put("errorCode", Integer.valueOf(6));
                            response.put("errorDescription", "Not enough funds");
                          }
                          else if (fee * 100L > account.getUnconfirmedBalance())
                          {
                            response.put("errorCode", Integer.valueOf(6));
                            response.put("errorDescription", "Not enough funds");
                          }
                          else
                          {
                            Nxt.Alias aliasData = (Nxt.Alias)aliases.get(normalizedAlias);
                            if ((aliasData != null) && (aliasData.account != account))
                            {
                              response.put("errorCode", Integer.valueOf(8));
                              response.put("errorDescription", "\"" + alias + "\" is already used");
                            }
                            else
                            {
                              int timestamp = getEpochTime(System.currentTimeMillis());
                              
                              Nxt.Transaction transaction = new Nxt.Transaction((byte)1, (byte)1, timestamp, deadline, publicKey, 1739068987193023818L, 0, fee, referencedTransaction, new byte[64]);
                              transaction.attachment = new Nxt.Transaction.MessagingAliasAssignmentAttachment(alias, uri);
                              transaction.sign(secretPhrase);
                              
                              JSONObject peerRequest = new JSONObject();
                              peerRequest.put("requestType", "processTransactions");
                              JSONArray transactionsData = new JSONArray();
                              transactionsData.add(transaction.getJSONObject());
                              peerRequest.put("transactions", transactionsData);
                              
                              Nxt.Peer.sendToSomePeers(peerRequest);
                              
                              response.put("transaction", transaction.getStringId());
                            }
                          }
                        }
                        catch (Exception e)
                        {
                          response.put("errorCode", Integer.valueOf(4));
                          response.put("errorDescription", "Incorrect \"deadline\"");
                        }
                      }
                      catch (Exception e)
                      {
                        response.put("errorCode", Integer.valueOf(4));
                        response.put("errorDescription", "Incorrect \"fee\"");
                      }
                    }
                  }
                }
              }
              break;
            case 1: 
              String transactionBytes = req.getParameter("transactionBytes");
              if (transactionBytes == null)
              {
                response.put("errorCode", Integer.valueOf(3));
                response.put("errorDescription", "\"transactionBytes\" not specified");
              }
              else
              {
                try
                {
                  ByteBuffer buffer = ByteBuffer.wrap(convert(transactionBytes));
                  buffer.order(ByteOrder.LITTLE_ENDIAN);
                  Nxt.Transaction transaction = Nxt.Transaction.getTransaction(buffer);
                  
                  JSONObject peerRequest = new JSONObject();
                  peerRequest.put("requestType", "processTransactions");
                  JSONArray transactionsData = new JSONArray();
                  transactionsData.add(transaction.getJSONObject());
                  peerRequest.put("transactions", transactionsData);
                  
                  Nxt.Peer.sendToSomePeers(peerRequest);
                  
                  response.put("transaction", transaction.getStringId());
                }
                catch (Exception e)
                {
                  response.put("errorCode", Integer.valueOf(4));
                  response.put("errorDescription", "Incorrect \"transactionBytes\"");
                }
              }
              break;
            case 2: 
              String hallmarkValue = req.getParameter("hallmark");
              if (hallmarkValue == null)
              {
                response.put("errorCode", Integer.valueOf(3));
                response.put("errorDescription", "\"hallmark\" not specified");
              }
              else
              {
                try
                {
                  byte[] hallmark = convert(hallmarkValue);
                  
                  ByteBuffer buffer = ByteBuffer.wrap(hallmark);
                  buffer.order(ByteOrder.LITTLE_ENDIAN);
                  
                  byte[] publicKey = new byte[32];
                  buffer.get(publicKey);
                  int hostLength = buffer.getShort();
                  byte[] hostBytes = new byte[hostLength];
                  buffer.get(hostBytes);
                  String host = new String(hostBytes, "UTF-8");
                  int weight = buffer.getInt();
                  int date = buffer.getInt();
                  buffer.get();
                  byte[] signature = new byte[64];
                  buffer.get(signature);
                  
                  response.put("account", convert(Nxt.Account.getId(publicKey)));
                  response.put("host", host);
                  response.put("weight", Integer.valueOf(weight));
                  int year = date / 10000;
                  int month = date % 10000 / 100;
                  int day = date % 100;
                  response.put("date", (year < 1000 ? "0" : year < 100 ? "00" : year < 10 ? "000" : "") + year + "-" + (month < 10 ? "0" : "") + month + "-" + (day < 10 ? "0" : "") + day);
                  byte[] data = new byte[hallmark.length - 64];
                  System.arraycopy(hallmark, 0, data, 0, data.length);
                  response.put("valid", Boolean.valueOf((host.length() > 100) || (weight <= 0) || (weight > 1000000000L) ? false : Nxt.Crypto.verify(signature, data, publicKey)));
                }
                catch (Exception e)
                {
                  response.put("errorCode", Integer.valueOf(4));
                  response.put("errorDescription", "Incorrect \"hallmark\"");
                }
              }
              break;
            case 3: 
              String website = req.getParameter("website");
              String token = req.getParameter("token");
              if (website == null)
              {
                response.put("errorCode", Integer.valueOf(3));
                response.put("errorDescription", "\"website\" not specified");
              }
              else if (token == null)
              {
                response.put("errorCode", Integer.valueOf(3));
                response.put("errorDescription", "\"token\" not specified");
              }
              else
              {
                byte[] websiteBytes = website.trim().getBytes("UTF-8");
                byte[] tokenBytes = new byte[100];
                int i = 0;int j = 0;
                try
                {
                  for (; i < token.length(); j += 5)
                  {
                    long number = Long.parseLong(token.substring(i, i + 8), 32);
                    tokenBytes[j] = ((byte)(int)number);
                    tokenBytes[(j + 1)] = ((byte)(int)(number >> 8));
                    tokenBytes[(j + 2)] = ((byte)(int)(number >> 16));
                    tokenBytes[(j + 3)] = ((byte)(int)(number >> 24));
                    tokenBytes[(j + 4)] = ((byte)(int)(number >> 32));i += 8;
                  }
                }
                catch (Exception e) {}
                if (i != 160)
                {
                  response.put("errorCode", Integer.valueOf(4));
                  response.put("errorDescription", "Incorrect \"token\"");
                }
                else
                {
                  byte[] publicKey = new byte[32];
                  System.arraycopy(tokenBytes, 0, publicKey, 0, 32);
                  int timestamp = tokenBytes[32] & 0xFF | (tokenBytes[33] & 0xFF) << 8 | (tokenBytes[34] & 0xFF) << 16 | (tokenBytes[35] & 0xFF) << 24;
                  byte[] signature = new byte[64];
                  System.arraycopy(tokenBytes, 36, signature, 0, 64);
                  
                  byte[] data = new byte[websiteBytes.length + 36];
                  System.arraycopy(websiteBytes, 0, data, 0, websiteBytes.length);
                  System.arraycopy(tokenBytes, 0, data, websiteBytes.length, 36);
                  boolean valid = Nxt.Crypto.verify(signature, data, publicKey);
                  
                  response.put("account", convert(Nxt.Account.getId(publicKey)));
                  response.put("timestamp", Integer.valueOf(timestamp));
                  response.put("valid", Boolean.valueOf(valid));
                }
              }
              break;
            case 4: 
              String account = req.getParameter("account");
              String timestampValue = req.getParameter("timestamp");
              if (account == null)
              {
                response.put("errorCode", Integer.valueOf(3));
                response.put("errorDescription", "\"account\" not specified");
              }
              else if (timestampValue == null)
              {
                response.put("errorCode", Integer.valueOf(3));
                response.put("errorDescription", "\"timestamp\" not specified");
              }
              else
              {
                try
                {
                  Nxt.Account accountData = (Nxt.Account)accounts.get(Long.valueOf(parseUnsignedLong(account)));
                  if (accountData == null)
                  {
                    response.put("errorCode", Integer.valueOf(5));
                    response.put("errorDescription", "Unknown account");
                  }
                  else
                  {
                    try
                    {
                      int timestamp = Integer.parseInt(timestampValue);
                      if (timestamp < 0) {
                        throw new Exception();
                      }
                      PriorityQueue<Nxt.Block> sortedBlocks = new PriorityQueue(11, Nxt.Block.heightComparator);
                      byte[] accountPublicKey = (byte[])accountData.publicKey.get();
                      for (Nxt.Block block : blocks.values()) {
                        if ((block.timestamp >= timestamp) && (Arrays.equals(block.generatorPublicKey, accountPublicKey))) {
                          sortedBlocks.offer(block);
                        }
                      }
                      JSONArray blockIds = new JSONArray();
                      while (!sortedBlocks.isEmpty()) {
                        blockIds.add(((Nxt.Block)sortedBlocks.poll()).getStringId());
                      }
                      response.put("blockIds", blockIds);
                    }
                    catch (Exception e)
                    {
                      response.put("errorCode", Integer.valueOf(4));
                      response.put("errorDescription", "Incorrect \"timestamp\"");
                    }
                  }
                }
                catch (Exception e)
                {
                  response.put("errorCode", Integer.valueOf(4));
                  response.put("errorDescription", "Incorrect \"account\"");
                }
              }
              break;
            case 5: 
              String secretPhrase = req.getParameter("secretPhrase");
              if (secretPhrase == null)
              {
                response.put("errorCode", Integer.valueOf(3));
                response.put("errorDescription", "\"secretPhrase\" not specified");
              }
              else
              {
                byte[] publicKeyHash = getMessageDigest("SHA-256").digest(Nxt.Crypto.getPublicKey(secretPhrase));
                BigInteger bigInteger = new BigInteger(1, new byte[] { publicKeyHash[7], publicKeyHash[6], publicKeyHash[5], publicKeyHash[4], publicKeyHash[3], publicKeyHash[2], publicKeyHash[1], publicKeyHash[0] });
                response.put("accountId", bigInteger.toString());
              }
              break;
            case 6: 
              String account = req.getParameter("account");
              if (account == null)
              {
                response.put("errorCode", Integer.valueOf(3));
                response.put("errorDescription", "\"account\" not specified");
              }
              else
              {
                try
                {
                  Nxt.Account accountData = (Nxt.Account)accounts.get(Long.valueOf(parseUnsignedLong(account)));
                  if (accountData == null)
                  {
                    response.put("errorCode", Integer.valueOf(5));
                    response.put("errorDescription", "Unknown account");
                  }
                  else if (accountData.publicKey.get() != null)
                  {
                    response.put("publicKey", convert((byte[])accountData.publicKey.get()));
                  }
                }
                catch (Exception e)
                {
                  response.put("errorCode", Integer.valueOf(4));
                  response.put("errorDescription", "Incorrect \"account\"");
                }
              }
              break;
            case 7: 
              String account = req.getParameter("account");
              String timestampValue = req.getParameter("timestamp");
              if (account == null)
              {
                response.put("errorCode", Integer.valueOf(3));
                response.put("errorDescription", "\"account\" not specified");
              }
              else if (timestampValue == null)
              {
                response.put("errorCode", Integer.valueOf(3));
                response.put("errorDescription", "\"timestamp\" not specified");
              }
              else
              {
                try
                {
                  Nxt.Account accountData = (Nxt.Account)accounts.get(Long.valueOf(parseUnsignedLong(account)));
                  if (accountData == null)
                  {
                    response.put("errorCode", Integer.valueOf(5));
                    response.put("errorDescription", "Unknown account");
                  }
                  else
                  {
                    try
                    {
                      int timestamp = Integer.parseInt(timestampValue);
                      if (timestamp < 0) {
                        throw new Exception();
                      }
                      PriorityQueue<Nxt.Transaction> sortedTransactions = new PriorityQueue(11, Nxt.Transaction.timestampComparator);
                      byte[] accountPublicKey = (byte[])accountData.publicKey.get();
                      for (Nxt.Transaction transaction : transactions.values()) {
                        if ((((Nxt.Block)blocks.get(Long.valueOf(transaction.block))).timestamp >= timestamp) && ((Arrays.equals(transaction.senderPublicKey, accountPublicKey)) || (transaction.recipient == accountData.id))) {
                          sortedTransactions.offer(transaction);
                        }
                      }
                      JSONArray transactionIds = new JSONArray();
                      while (!sortedTransactions.isEmpty()) {
                        transactionIds.add(((Nxt.Transaction)sortedTransactions.poll()).getStringId());
                      }
                      response.put("transactionIds", transactionIds);
                    }
                    catch (Exception e)
                    {
                      response.put("errorCode", Integer.valueOf(4));
                      response.put("errorDescription", "Incorrect \"timestamp\"");
                    }
                  }
                }
                catch (Exception e)
                {
                  response.put("errorCode", Integer.valueOf(4));
                  response.put("errorDescription", "Incorrect \"account\"");
                }
              }
              break;
            case 8: 
              String alias = req.getParameter("alias");
              if (alias == null)
              {
                response.put("errorCode", Integer.valueOf(3));
                response.put("errorDescription", "\"alias\" not specified");
              }
              else
              {
                try
                {
                  Nxt.Alias aliasData = (Nxt.Alias)aliasIdToAliasMappings.get(Long.valueOf(parseUnsignedLong(alias)));
                  if (aliasData == null)
                  {
                    response.put("errorCode", Integer.valueOf(5));
                    response.put("errorDescription", "Unknown alias");
                  }
                  else
                  {
                    response.put("account", convert(aliasData.account.id));
                    response.put("alias", aliasData.alias);
                    if (aliasData.uri.length() > 0) {
                      response.put("uri", aliasData.uri);
                    }
                    response.put("timestamp", Integer.valueOf(aliasData.timestamp));
                  }
                }
                catch (Exception e)
                {
                  response.put("errorCode", Integer.valueOf(4));
                  response.put("errorDescription", "Incorrect \"alias\"");
                }
              }
              break;
            case 9: 
              String alias = req.getParameter("alias");
              if (alias == null)
              {
                response.put("errorCode", Integer.valueOf(3));
                response.put("errorDescription", "\"alias\" not specified");
              }
              else
              {
                Nxt.Alias aliasData = (Nxt.Alias)aliases.get(alias.toLowerCase());
                if (aliasData == null)
                {
                  response.put("errorCode", Integer.valueOf(5));
                  response.put("errorDescription", "Unknown alias");
                }
                else
                {
                  response.put("id", convert(aliasData.id));
                }
              }
              break;
            case 10: 
              String timestampValue = req.getParameter("timestamp");
              if (timestampValue == null)
              {
                response.put("errorCode", Integer.valueOf(3));
                response.put("errorDescription", "\"timestamp\" not specified");
              }
              else
              {
                try
                {
                  int timestamp = Integer.parseInt(timestampValue);
                  if (timestamp < 0) {
                    throw new Exception();
                  }
                  JSONArray aliasIds = new JSONArray();
                  for (Map.Entry<Long, Nxt.Alias> aliasEntry : aliasIdToAliasMappings.entrySet()) {
                    if (((Nxt.Alias)aliasEntry.getValue()).timestamp >= timestamp) {
                      aliasIds.add(convert(((Long)aliasEntry.getKey()).longValue()));
                    }
                  }
                  response.put("aliasIds", aliasIds);
                }
                catch (Exception e)
                {
                  response.put("errorCode", Integer.valueOf(4));
                  response.put("errorDescription", "Incorrect \"timestamp\"");
                }
              }
              break;
            case 11: 
              String alias = req.getParameter("alias");
              if (alias == null)
              {
                response.put("errorCode", Integer.valueOf(3));
                response.put("errorDescription", "\"alias\" not specified");
              }
              else
              {
                Nxt.Alias aliasData = (Nxt.Alias)aliases.get(alias.toLowerCase());
                if (aliasData == null)
                {
                  response.put("errorCode", Integer.valueOf(5));
                  response.put("errorDescription", "Unknown alias");
                }
                else if (aliasData.uri.length() > 0)
                {
                  response.put("uri", aliasData.uri);
                }
              }
              break;
            case 12: 
              String account = req.getParameter("account");
              if (account == null)
              {
                response.put("errorCode", Integer.valueOf(3));
                response.put("errorDescription", "\"account\" not specified");
              }
              else
              {
                try
                {
                  Nxt.Account accountData = (Nxt.Account)accounts.get(Long.valueOf(parseUnsignedLong(account)));
                  if (accountData == null)
                  {
                    response.put("balance", Integer.valueOf(0));
                    response.put("unconfirmedBalance", Integer.valueOf(0));
                    response.put("effectiveBalance", Integer.valueOf(0));
                  }
                  else
                  {
                    synchronized (accountData)
                    {
                      response.put("balance", Long.valueOf(accountData.getBalance()));
                      response.put("unconfirmedBalance", Long.valueOf(accountData.getUnconfirmedBalance()));
                      response.put("effectiveBalance", Long.valueOf(accountData.getEffectiveBalance() * 100L));
                    }
                  }
                }
                catch (Exception e)
                {
                  response.put("errorCode", Integer.valueOf(4));
                  response.put("errorDescription", "Incorrect \"account\"");
                }
              }
              break;
            case 13: 
              String block = req.getParameter("block");
              if (block == null)
              {
                response.put("errorCode", Integer.valueOf(3));
                response.put("errorDescription", "\"block\" not specified");
              }
              else
              {
                try
                {
                  Nxt.Block blockData = (Nxt.Block)blocks.get(Long.valueOf(parseUnsignedLong(block)));
                  if (blockData == null)
                  {
                    response.put("errorCode", Integer.valueOf(5));
                    response.put("errorDescription", "Unknown block");
                  }
                  else
                  {
                    response.put("height", Integer.valueOf(blockData.height));
                    response.put("generator", convert(blockData.getGeneratorAccountId()));
                    response.put("timestamp", Integer.valueOf(blockData.timestamp));
                    response.put("numberOfTransactions", Integer.valueOf(blockData.transactions.length));
                    response.put("totalAmount", Integer.valueOf(blockData.totalAmount));
                    response.put("totalFee", Integer.valueOf(blockData.totalFee));
                    response.put("payloadLength", Integer.valueOf(blockData.payloadLength));
                    response.put("version", Integer.valueOf(blockData.version));
                    response.put("baseTarget", convert(blockData.baseTarget));
                    if (blockData.previousBlock != 0L) {
                      response.put("previousBlock", convert(blockData.previousBlock));
                    }
                    if (blockData.nextBlock != 0L) {
                      response.put("nextBlock", convert(blockData.nextBlock));
                    }
                    response.put("payloadHash", convert(blockData.payloadHash));
                    response.put("generationSignature", convert(blockData.generationSignature));
                    if (blockData.version > 1) {
                      response.put("previousBlockHash", convert(blockData.previousBlockHash));
                    }
                    response.put("blockSignature", convert(blockData.blockSignature));
                    JSONArray transactions = new JSONArray();
                    for (long transactionId : blockData.transactions) {
                      transactions.add(convert(transactionId));
                    }
                    response.put("transactions", transactions);
                  }
                }
                catch (Exception e)
                {
                  response.put("errorCode", Integer.valueOf(4));
                  response.put("errorDescription", "Incorrect \"block\"");
                }
              }
              break;
            case 14: 
              response.put("genesisBlockId", convert(2680262203532249785L));
              response.put("genesisAccountId", convert(1739068987193023818L));
              response.put("maxBlockPayloadLength", Integer.valueOf(32640));
              response.put("maxArbitraryMessageLength", Integer.valueOf(1000));
              
              JSONArray transactionTypes = new JSONArray();
              JSONObject transactionType = new JSONObject();
              transactionType.put("value", Byte.valueOf((byte)0));
              transactionType.put("description", "Payment");
              JSONArray subtypes = new JSONArray();
              JSONObject subtype = new JSONObject();
              subtype.put("value", Byte.valueOf((byte)0));
              subtype.put("description", "Ordinary payment");
              subtypes.add(subtype);
              transactionType.put("subtypes", subtypes);
              transactionTypes.add(transactionType);
              transactionType = new JSONObject();
              transactionType.put("value", Byte.valueOf((byte)1));
              transactionType.put("description", "Messaging");
              subtypes = new JSONArray();
              subtype = new JSONObject();
              subtype.put("value", Byte.valueOf((byte)0));
              subtype.put("description", "Arbitrary message");
              subtypes.add(subtype);
              subtype = new JSONObject();
              subtype.put("value", Byte.valueOf((byte)1));
              subtype.put("description", "Alias assignment");
              subtypes.add(subtype);
              transactionType.put("subtypes", subtypes);
              transactionTypes.add(transactionType);
              transactionType = new JSONObject();
              transactionType.put("value", Byte.valueOf((byte)2));
              transactionType.put("description", "Colored coins");
              subtypes = new JSONArray();
              subtype = new JSONObject();
              subtype.put("value", Byte.valueOf((byte)0));
              subtype.put("description", "Asset issuance");
              subtypes.add(subtype);
              subtype = new JSONObject();
              subtype.put("value", Byte.valueOf((byte)1));
              subtype.put("description", "Asset transfer");
              subtypes.add(subtype);
              subtype = new JSONObject();
              subtype.put("value", Byte.valueOf((byte)2));
              subtype.put("description", "Ask order placement");
              subtypes.add(subtype);
              subtype = new JSONObject();
              subtype.put("value", Byte.valueOf((byte)3));
              subtype.put("description", "Bid order placement");
              subtypes.add(subtype);
              subtype = new JSONObject();
              subtype.put("value", Byte.valueOf((byte)4));
              subtype.put("description", "Ask order cancellation");
              subtypes.add(subtype);
              subtype = new JSONObject();
              subtype.put("value", Byte.valueOf((byte)5));
              subtype.put("description", "Bid order cancellation");
              subtypes.add(subtype);
              transactionType.put("subtypes", subtypes);
              transactionTypes.add(transactionType);
              response.put("transactionTypes", transactionTypes);
              
              JSONArray peerStates = new JSONArray();
              JSONObject peerState = new JSONObject();
              peerState.put("value", Integer.valueOf(0));
              peerState.put("description", "Non-connected");
              peerStates.add(peerState);
              peerState = new JSONObject();
              peerState.put("value", Integer.valueOf(1));
              peerState.put("description", "Connected");
              peerStates.add(peerState);
              peerState = new JSONObject();
              peerState.put("value", Integer.valueOf(2));
              peerState.put("description", "Disconnected");
              peerStates.add(peerState);
              response.put("peerStates", peerStates);
              

              break;
            case 15: 
              String account = req.getParameter("account");
              String numberOfConfirmationsValue = req.getParameter("numberOfConfirmations");
              if (account == null)
              {
                response.put("errorCode", Integer.valueOf(3));
                response.put("errorDescription", "\"account\" not specified");
              }
              else if (numberOfConfirmationsValue == null)
              {
                response.put("errorCode", Integer.valueOf(3));
                response.put("errorDescription", "\"numberOfConfirmations\" not specified");
              }
              else
              {
                try
                {
                  Nxt.Account accountData = (Nxt.Account)accounts.get(Long.valueOf(parseUnsignedLong(account)));
                  if (accountData == null) {
                    response.put("guaranteedBalance", Integer.valueOf(0));
                  } else {
                    try
                    {
                      int numberOfConfirmations = Integer.parseInt(numberOfConfirmationsValue);
                      response.put("guaranteedBalance", Long.valueOf(accountData.getGuaranteedBalance(numberOfConfirmations)));
                    }
                    catch (Exception e)
                    {
                      response.put("errorCode", Integer.valueOf(4));
                      response.put("errorDescription", "Incorrect \"numberOfConfirmations\"");
                    }
                  }
                }
                catch (Exception e)
                {
                  response.put("errorCode", Integer.valueOf(4));
                  response.put("errorDescription", "Incorrect \"account\"");
                }
              }
              break;
            case 16: 
              response.put("host", req.getRemoteHost());
              response.put("address", req.getRemoteAddr());
              

              break;
            case 17: 
              String peer = req.getParameter("peer");
              if (peer == null)
              {
                response.put("errorCode", Integer.valueOf(3));
                response.put("errorDescription", "\"peer\" not specified");
              }
              else
              {
                Nxt.Peer peerData = (Nxt.Peer)peers.get(peer);
                if (peerData == null)
                {
                  response.put("errorCode", Integer.valueOf(5));
                  response.put("errorDescription", "Unknown peer");
                }
                else
                {
                  response.put("state", Integer.valueOf(peerData.state));
                  response.put("announcedAddress", peerData.announcedAddress);
                  if (peerData.hallmark != null) {
                    response.put("hallmark", peerData.hallmark);
                  }
                  response.put("weight", Integer.valueOf(peerData.getWeight()));
                  response.put("downloadedVolume", Long.valueOf(peerData.downloadedVolume));
                  response.put("uploadedVolume", Long.valueOf(peerData.uploadedVolume));
                  response.put("application", peerData.application);
                  response.put("version", peerData.version);
                  response.put("platform", peerData.platform);
                }
              }
              break;
            case 18: 
              JSONArray peers = new JSONArray();
              peers.addAll(peers.keySet());
              response.put("peers", peers);
              

              break;
            case 19: 
              response.put("version", "0.5.7");
              response.put("time", Integer.valueOf(getEpochTime(System.currentTimeMillis())));
              response.put("lastBlock", Nxt.Block.getLastBlock().getStringId());
              response.put("cumulativeDifficulty", Nxt.Block.getLastBlock().cumulativeDifficulty.toString());
              
              long totalEffectiveBalance = 0L;
              for (Nxt.Account account : accounts.values())
              {
                long effectiveBalance = account.getEffectiveBalance();
                if (effectiveBalance > 0L) {
                  totalEffectiveBalance += effectiveBalance;
                }
              }
              response.put("totalEffectiveBalance", Long.valueOf(totalEffectiveBalance * 100L));
              
              response.put("numberOfBlocks", Integer.valueOf(blocks.size()));
              response.put("numberOfTransactions", Integer.valueOf(transactions.size()));
              response.put("numberOfAccounts", Integer.valueOf(accounts.size()));
              response.put("numberOfAssets", Integer.valueOf(assets.size()));
              response.put("numberOfOrders", Integer.valueOf(askOrders.size() + bidOrders.size()));
              response.put("numberOfAliases", Integer.valueOf(aliases.size()));
              response.put("numberOfPeers", Integer.valueOf(peers.size()));
              response.put("numberOfUsers", Integer.valueOf(users.size()));
              response.put("lastBlockchainFeeder", lastBlockchainFeeder == null ? null : lastBlockchainFeeder.announcedAddress);
              response.put("availableProcessors", Integer.valueOf(Runtime.getRuntime().availableProcessors()));
              response.put("maxMemory", Long.valueOf(Runtime.getRuntime().maxMemory()));
              response.put("totalMemory", Long.valueOf(Runtime.getRuntime().totalMemory()));
              response.put("freeMemory", Long.valueOf(Runtime.getRuntime().freeMemory()));
              

              break;
            case 20: 
              response.put("time", Integer.valueOf(getEpochTime(System.currentTimeMillis())));
              

              break;
            case 21: 
              String transaction = req.getParameter("transaction");
              if (transaction == null)
              {
                response.put("errorCode", Integer.valueOf(3));
                response.put("errorDescription", "\"transaction\" not specified");
              }
              else
              {
                try
                {
                  long transactionId = parseUnsignedLong(transaction);
                  Nxt.Transaction transactionData = (Nxt.Transaction)transactions.get(Long.valueOf(transactionId));
                  if (transactionData == null)
                  {
                    transactionData = (Nxt.Transaction)unconfirmedTransactions.get(Long.valueOf(transactionId));
                    if (transactionData == null)
                    {
                      response.put("errorCode", Integer.valueOf(5));
                      response.put("errorDescription", "Unknown transaction");
                    }
                    else
                    {
                      response = transactionData.getJSONObject();
                      response.put("sender", convert(transactionData.getSenderAccountId()));
                    }
                  }
                  else
                  {
                    response = transactionData.getJSONObject();
                    
                    response.put("sender", convert(transactionData.getSenderAccountId()));
                    Nxt.Block block = (Nxt.Block)blocks.get(Long.valueOf(transactionData.block));
                    response.put("block", block.getStringId());
                    response.put("confirmations", Integer.valueOf(Nxt.Block.getLastBlock().height - block.height + 1));
                  }
                }
                catch (Exception e)
                {
                  response.put("errorCode", Integer.valueOf(4));
                  response.put("errorDescription", "Incorrect \"transaction\"");
                }
              }
              break;
            case 22: 
              String transaction = req.getParameter("transaction");
              if (transaction == null)
              {
                response.put("errorCode", Integer.valueOf(3));
                response.put("errorDescription", "\"transaction\" not specified");
              }
              else
              {
                try
                {
                  long transactionId = parseUnsignedLong(transaction);
                  Nxt.Transaction transactionData = (Nxt.Transaction)transactions.get(Long.valueOf(transactionId));
                  if (transactionData == null)
                  {
                    transactionData = (Nxt.Transaction)unconfirmedTransactions.get(Long.valueOf(transactionId));
                    if (transactionData == null)
                    {
                      response.put("errorCode", Integer.valueOf(5));
                      response.put("errorDescription", "Unknown transaction");
                    }
                    else
                    {
                      response.put("bytes", convert(transactionData.getBytes()));
                    }
                  }
                  else
                  {
                    response.put("bytes", convert(transactionData.getBytes()));
                    Nxt.Block block = (Nxt.Block)blocks.get(Long.valueOf(transactionData.block));
                    response.put("confirmations", Integer.valueOf(Nxt.Block.getLastBlock().height - block.height + 1));
                  }
                }
                catch (Exception e)
                {
                  response.put("errorCode", Integer.valueOf(4));
                  response.put("errorDescription", "Incorrect \"transaction\"");
                }
              }
              break;
            case 23: 
              JSONArray transactionIds = new JSONArray();
              for (Nxt.Transaction transaction : unconfirmedTransactions.values()) {
                transactionIds.add(transaction.getStringId());
              }
              response.put("unconfirmedTransactionIds", transactionIds);
              

              break;
            case 24: 
              String account = req.getParameter("account");
              if (account == null)
              {
                response.put("errorCode", Integer.valueOf(3));
                response.put("errorDescription", "\"account\" not specified");
              }
              else
              {
                try
                {
                  long accountId = parseUnsignedLong(account);
                  Nxt.Account accountData = (Nxt.Account)accounts.get(Long.valueOf(accountId));
                  if (accountData == null)
                  {
                    response.put("errorCode", Integer.valueOf(5));
                    response.put("errorDescription", "Unknown account");
                  }
                  else
                  {
                    JSONArray aliases = new JSONArray();
                    for (Nxt.Alias alias : aliases.values()) {
                      if (alias.account.id == accountId)
                      {
                        JSONObject aliasData = new JSONObject();
                        aliasData.put("alias", alias.alias);
                        aliasData.put("uri", alias.uri);
                        aliases.add(aliasData);
                      }
                    }
                    response.put("aliases", aliases);
                  }
                }
                catch (Exception e)
                {
                  response.put("errorCode", Integer.valueOf(4));
                  response.put("errorDescription", "Incorrect \"account\"");
                }
              }
              break;
            case 25: 
              String secretPhrase = req.getParameter("secretPhrase");
              String host = req.getParameter("host");
              String weightValue = req.getParameter("weight");
              String dateValue = req.getParameter("date");
              if (secretPhrase == null)
              {
                response.put("errorCode", Integer.valueOf(3));
                response.put("errorDescription", "\"secretPhrase\" not specified");
              }
              else if (host == null)
              {
                response.put("errorCode", Integer.valueOf(3));
                response.put("errorDescription", "\"host\" not specified");
              }
              else if (weightValue == null)
              {
                response.put("errorCode", Integer.valueOf(3));
                response.put("errorDescription", "\"weight\" not specified");
              }
              else if (dateValue == null)
              {
                response.put("errorCode", Integer.valueOf(3));
                response.put("errorDescription", "\"date\" not specified");
              }
              else if (host.length() > 100)
              {
                response.put("errorCode", Integer.valueOf(4));
                response.put("errorDescription", "Incorrect \"host\" (the length exceeds 100 chars limit)");
              }
              else
              {
                try
                {
                  int weight = Integer.parseInt(weightValue);
                  if ((weight <= 0) || (weight > 1000000000L)) {
                    throw new Exception();
                  }
                  try
                  {
                    int date = Integer.parseInt(dateValue.substring(0, 4)) * 10000 + Integer.parseInt(dateValue.substring(5, 7)) * 100 + Integer.parseInt(dateValue.substring(8, 10));
                    
                    byte[] publicKey = Nxt.Crypto.getPublicKey(secretPhrase);
                    byte[] hostBytes = host.getBytes("UTF-8");
                    
                    ByteBuffer buffer = ByteBuffer.allocate(34 + hostBytes.length + 4 + 4 + 1);
                    buffer.order(ByteOrder.LITTLE_ENDIAN);
                    buffer.put(publicKey);
                    buffer.putShort((short)hostBytes.length);
                    buffer.put(hostBytes);
                    buffer.putInt(weight);
                    buffer.putInt(date);
                    
                    byte[] data = buffer.array();
                    byte[] signature;
                    do
                    {
                      data[(data.length - 1)] = ((byte)ThreadLocalRandom.current().nextInt());
                      signature = Nxt.Crypto.sign(data, secretPhrase);
                    } while (!Nxt.Crypto.verify(signature, data, publicKey));
                    response.put("hallmark", convert(data) + convert(signature));
                  }
                  catch (Exception e)
                  {
                    response.put("errorCode", Integer.valueOf(4));
                    response.put("errorDescription", "Incorrect \"date\"");
                  }
                }
                catch (Exception e)
                {
                  response.put("errorCode", Integer.valueOf(4));
                  response.put("errorDescription", "Incorrect \"weight\"");
                }
              }
              break;
            case 26: 
              String secretPhrase = req.getParameter("secretPhrase");
              String recipientValue = req.getParameter("recipient");
              String messageValue = req.getParameter("message");
              String feeValue = req.getParameter("fee");
              String deadlineValue = req.getParameter("deadline");
              String referencedTransactionValue = req.getParameter("referencedTransaction");
              if (secretPhrase == null)
              {
                response.put("errorCode", Integer.valueOf(3));
                response.put("errorDescription", "\"secretPhrase\" not specified");
              }
              else if (recipientValue == null)
              {
                response.put("errorCode", Integer.valueOf(3));
                response.put("errorDescription", "\"recipient\" not specified");
              }
              else if (messageValue == null)
              {
                response.put("errorCode", Integer.valueOf(3));
                response.put("errorDescription", "\"message\" not specified");
              }
              else if (feeValue == null)
              {
                response.put("errorCode", Integer.valueOf(3));
                response.put("errorDescription", "\"fee\" not specified");
              }
              else if (deadlineValue == null)
              {
                response.put("errorCode", Integer.valueOf(3));
                response.put("errorDescription", "\"deadline\" not specified");
              }
              else
              {
                try
                {
                  long recipient = parseUnsignedLong(recipientValue);
                  try
                  {
                    byte[] message = convert(messageValue);
                    if (message.length > 1000)
                    {
                      response.put("errorCode", Integer.valueOf(4));
                      response.put("errorDescription", "Incorrect \"message\" (length must be not longer than 1000 bytes)");
                    }
                    else
                    {
                      try
                      {
                        int fee = Integer.parseInt(feeValue);
                        if ((fee <= 0) || (fee >= 1000000000L)) {
                          throw new Exception();
                        }
                        try
                        {
                          short deadline = Short.parseShort(deadlineValue);
                          if (deadline < 1) {
                            throw new Exception();
                          }
                          long referencedTransaction = referencedTransactionValue == null ? 0L : parseUnsignedLong(referencedTransactionValue);
                          
                          byte[] publicKey = Nxt.Crypto.getPublicKey(secretPhrase);
                          
                          Nxt.Account account = (Nxt.Account)accounts.get(Long.valueOf(Nxt.Account.getId(publicKey)));
                          if ((account == null) || (fee * 100L > account.getUnconfirmedBalance()))
                          {
                            response.put("errorCode", Integer.valueOf(6));
                            response.put("errorDescription", "Not enough funds");
                          }
                          else
                          {
                            int timestamp = getEpochTime(System.currentTimeMillis());
                            
                            Nxt.Transaction transaction = new Nxt.Transaction((byte)1, (byte)0, timestamp, deadline, publicKey, recipient, 0, fee, referencedTransaction, new byte[64]);
                            transaction.attachment = new Nxt.Transaction.MessagingArbitraryMessageAttachment(message);
                            transaction.sign(secretPhrase);
                            
                            JSONObject peerRequest = new JSONObject();
                            peerRequest.put("requestType", "processTransactions");
                            JSONArray transactionsData = new JSONArray();
                            transactionsData.add(transaction.getJSONObject());
                            peerRequest.put("transactions", transactionsData);
                            
                            Nxt.Peer.sendToSomePeers(peerRequest);
                            
                            response.put("transaction", transaction.getStringId());
                            response.put("bytes", convert(transaction.getBytes()));
                          }
                        }
                        catch (Exception e)
                        {
                          response.put("errorCode", Integer.valueOf(4));
                          response.put("errorDescription", "Incorrect \"deadline\"");
                        }
                      }
                      catch (Exception e)
                      {
                        response.put("errorCode", Integer.valueOf(4));
                        response.put("errorDescription", "Incorrect \"fee\"");
                      }
                    }
                  }
                  catch (Exception e)
                  {
                    response.put("errorCode", Integer.valueOf(4));
                    response.put("errorDescription", "Incorrect \"message\"");
                  }
                }
                catch (Exception e)
                {
                  response.put("errorCode", Integer.valueOf(4));
                  response.put("errorDescription", "Incorrect \"recipient\"");
                }
              }
              break;
            case 27: 
              secretPhrase = req.getParameter("secretPhrase");
              String recipientValue = req.getParameter("recipient");
              String amountValue = req.getParameter("amount");
              String feeValue = req.getParameter("fee");
              String deadlineValue = req.getParameter("deadline");
              String referencedTransactionValue = req.getParameter("referencedTransaction");
              if (secretPhrase == null)
              {
                response.put("errorCode", Integer.valueOf(3));
                response.put("errorDescription", "\"secretPhrase\" not specified");
              }
              else if (recipientValue == null)
              {
                response.put("errorCode", Integer.valueOf(3));
                response.put("errorDescription", "\"recipient\" not specified");
              }
              else if (amountValue == null)
              {
                response.put("errorCode", Integer.valueOf(3));
                response.put("errorDescription", "\"amount\" not specified");
              }
              else if (feeValue == null)
              {
                response.put("errorCode", Integer.valueOf(3));
                response.put("errorDescription", "\"fee\" not specified");
              }
              else if (deadlineValue == null)
              {
                response.put("errorCode", Integer.valueOf(3));
                response.put("errorDescription", "\"deadline\" not specified");
              }
              else
              {
                try
                {
                  long recipient = parseUnsignedLong(recipientValue);
                  try
                  {
                    int amount = Integer.parseInt(amountValue);
                    if ((amount <= 0) || (amount >= 1000000000L)) {
                      throw new Exception();
                    }
                    try
                    {
                      int fee = Integer.parseInt(feeValue);
                      if ((fee <= 0) || (fee >= 1000000000L)) {
                        throw new Exception();
                      }
                      try
                      {
                        short deadline = Short.parseShort(deadlineValue);
                        if (deadline < 1) {
                          throw new Exception();
                        }
                        long referencedTransaction = referencedTransactionValue == null ? 0L : parseUnsignedLong(referencedTransactionValue);
                        
                        byte[] publicKey = Nxt.Crypto.getPublicKey(secretPhrase);
                        
                        Nxt.Account account = (Nxt.Account)accounts.get(Long.valueOf(Nxt.Account.getId(publicKey)));
                        if (account == null)
                        {
                          response.put("errorCode", Integer.valueOf(6));
                          response.put("errorDescription", "Not enough funds");
                        }
                        else if ((amount + fee) * 100L > account.getUnconfirmedBalance())
                        {
                          response.put("errorCode", Integer.valueOf(6));
                          response.put("errorDescription", "Not enough funds");
                        }
                        else
                        {
                          Nxt.Transaction transaction = new Nxt.Transaction((byte)0, (byte)0, getEpochTime(System.currentTimeMillis()), deadline, publicKey, recipient, amount, fee, referencedTransaction, new byte[64]);
                          transaction.sign(secretPhrase);
                          
                          JSONObject peerRequest = new JSONObject();
                          peerRequest.put("requestType", "processTransactions");
                          JSONArray transactionsData = new JSONArray();
                          transactionsData.add(transaction.getJSONObject());
                          peerRequest.put("transactions", transactionsData);
                          
                          Nxt.Peer.sendToSomePeers(peerRequest);
                          
                          response.put("transaction", transaction.getStringId());
                          response.put("bytes", convert(transaction.getBytes()));
                        }
                      }
                      catch (Exception e)
                      {
                        response.put("errorCode", Integer.valueOf(4));
                        response.put("errorDescription", "Incorrect \"deadline\"");
                      }
                    }
                    catch (Exception e)
                    {
                      response.put("errorCode", Integer.valueOf(4));
                      response.put("errorDescription", "Incorrect \"fee\"");
                    }
                  }
                  catch (Exception e)
                  {
                    response.put("errorCode", Integer.valueOf(4));
                    response.put("errorDescription", "Incorrect \"amount\"");
                  }
                }
                catch (Exception e)
                {
                  response.put("errorCode", Integer.valueOf(4));
                  response.put("errorDescription", "Incorrect \"recipient\"");
                }
              }
              break;
            default: 
              response.put("errorCode", Integer.valueOf(1));
              response.put("errorDescription", "Incorrect request");
            }
          }
        }
        resp.setContentType("text/plain; charset=UTF-8");
        
        Writer writer = resp.getWriter();Object localObject1 = null;
        try
        {
          response.writeJSONString(writer);
        }
        catch (Throwable localThrowable6)
        {
          localObject1 = localThrowable6;throw localThrowable6;
        }
        finally
        {
          if (writer != null) {
            if (localObject1 != null) {
              try
              {
                writer.close();
              }
              catch (Throwable x2)
              {
                ((Throwable)localObject1).addSuppressed(x2);
              }
            } else {
              writer.close();
            }
          }
        }
        return;
      }
      if ((allowedUserHosts != null) && (!allowedUserHosts.contains(req.getRemoteHost())))
      {
        JSONObject response = new JSONObject();
        response.put("response", "denyAccess");
        ??? = new JSONArray();
        ???.add(response);
        JSONObject combinedResponse = new JSONObject();
        combinedResponse.put("responses", ???);
        
        resp.setContentType("text/plain; charset=UTF-8");
        
        Object writer = resp.getWriter();secretPhrase = null;
        try
        {
          combinedResponse.writeJSONString((Writer)writer);
        }
        catch (Throwable localThrowable2)
        {
          secretPhrase = localThrowable2;throw localThrowable2;
        }
        finally
        {
          if (writer != null) {
            if (secretPhrase != null) {
              try
              {
                ((Writer)writer).close();
              }
              catch (Throwable x2)
              {
                secretPhrase.addSuppressed(x2);
              }
            } else {
              ((Writer)writer).close();
            }
          }
        }
        return;
      }
      user = (Nxt.User)users.get(userPasscode);
      if (user == null)
      {
        user = new Nxt.User();
        ??? = (Nxt.User)users.putIfAbsent(userPasscode, user);
        if (??? != null)
        {
          user = ???;
          user.isInactive = false;
        }
      }
      else
      {
        user.isInactive = false;
      }
      int index;
      int index;
      int index;
      switch (req.getParameter("requestType"))
      {
      case "generateAuthorizationToken": 
        String secretPhrase = req.getParameter("secretPhrase");
        if (!user.secretPhrase.equals(secretPhrase))
        {
          JSONObject response = new JSONObject();
          response.put("response", "showMessage");
          response.put("message", "Invalid secret phrase!");
          user.pendingResponses.offer(response);
        }
        else
        {
          byte[] website = req.getParameter("website").trim().getBytes("UTF-8");
          byte[] data = new byte[website.length + 32 + 4];
          System.arraycopy(website, 0, data, 0, website.length);
          System.arraycopy(Nxt.Crypto.getPublicKey(user.secretPhrase), 0, data, website.length, 32);
          int timestamp = getEpochTime(System.currentTimeMillis());
          data[(website.length + 32)] = ((byte)timestamp);
          data[(website.length + 32 + 1)] = ((byte)(timestamp >> 8));
          data[(website.length + 32 + 2)] = ((byte)(timestamp >> 16));
          data[(website.length + 32 + 3)] = ((byte)(timestamp >> 24));
          
          byte[] token = new byte[100];
          System.arraycopy(data, website.length, token, 0, 36);
          System.arraycopy(Nxt.Crypto.sign(data, user.secretPhrase), 0, token, 36, 64);
          String tokenString = "";
          for (int ptr = 0; ptr < 100; ptr += 5)
          {
            long number = token[ptr] & 0xFF | (token[(ptr + 1)] & 0xFF) << 8 | (token[(ptr + 2)] & 0xFF) << 16 | (token[(ptr + 3)] & 0xFF) << 24 | (token[(ptr + 4)] & 0xFF) << 32;
            if (number < 32L) {
              tokenString = tokenString + "0000000";
            } else if (number < 1024L) {
              tokenString = tokenString + "000000";
            } else if (number < 32768L) {
              tokenString = tokenString + "00000";
            } else if (number < 1048576L) {
              tokenString = tokenString + "0000";
            } else if (number < 33554432L) {
              tokenString = tokenString + "000";
            } else if (number < 1073741824L) {
              tokenString = tokenString + "00";
            } else if (number < 34359738368L) {
              tokenString = tokenString + "0";
            }
            tokenString = tokenString + Long.toString(number, 32);
          }
          JSONObject response = new JSONObject();
          response.put("response", "showAuthorizationToken");
          response.put("token", tokenString);
          
          user.pendingResponses.offer(response);
        }
        break;
      case "getInitialData": 
        JSONArray unconfirmedTransactions = new JSONArray();
        JSONArray activePeers = new JSONArray();JSONArray knownPeers = new JSONArray();JSONArray blacklistedPeers = new JSONArray();
        JSONArray recentBlocks = new JSONArray();
        for (Nxt.Transaction transaction : unconfirmedTransactions.values())
        {
          JSONObject unconfirmedTransaction = new JSONObject();
          unconfirmedTransaction.put("index", Integer.valueOf(transaction.index));
          unconfirmedTransaction.put("timestamp", Integer.valueOf(transaction.timestamp));
          unconfirmedTransaction.put("deadline", Short.valueOf(transaction.deadline));
          unconfirmedTransaction.put("recipient", convert(transaction.recipient));
          unconfirmedTransaction.put("amount", Integer.valueOf(transaction.amount));
          unconfirmedTransaction.put("fee", Integer.valueOf(transaction.fee));
          unconfirmedTransaction.put("sender", convert(transaction.getSenderAccountId()));
          
          unconfirmedTransactions.add(unconfirmedTransaction);
        }
        for (Map.Entry<String, Nxt.Peer> peerEntry : peers.entrySet())
        {
          String address = (String)peerEntry.getKey();
          Nxt.Peer peer = (Nxt.Peer)peerEntry.getValue();
          if (peer.blacklistingTime > 0L)
          {
            JSONObject blacklistedPeer = new JSONObject();
            blacklistedPeer.put("index", Integer.valueOf(peer.index));
            blacklistedPeer.put("announcedAddress", peer.announcedAddress.length() > 0 ? peer.announcedAddress : peer.announcedAddress.length() > 30 ? peer.announcedAddress.substring(0, 30) + "..." : address);
            for (String wellKnownPeer : wellKnownPeers) {
              if (peer.announcedAddress.equals(wellKnownPeer))
              {
                blacklistedPeer.put("wellKnown", Boolean.valueOf(true));
                
                break;
              }
            }
            blacklistedPeers.add(blacklistedPeer);
          }
          else if (peer.state == 0)
          {
            if (peer.announcedAddress.length() > 0)
            {
              JSONObject knownPeer = new JSONObject();
              knownPeer.put("index", Integer.valueOf(peer.index));
              knownPeer.put("announcedAddress", peer.announcedAddress.length() > 30 ? peer.announcedAddress.substring(0, 30) + "..." : peer.announcedAddress);
              for (String wellKnownPeer : wellKnownPeers) {
                if (peer.announcedAddress.equals(wellKnownPeer))
                {
                  knownPeer.put("wellKnown", Boolean.valueOf(true));
                  
                  break;
                }
              }
              knownPeers.add(knownPeer);
            }
          }
          else
          {
            JSONObject activePeer = new JSONObject();
            activePeer.put("index", Integer.valueOf(peer.index));
            if (peer.state == 2) {
              activePeer.put("disconnected", Boolean.valueOf(true));
            }
            activePeer.put("address", address.length() > 30 ? address.substring(0, 30) + "..." : address);
            activePeer.put("announcedAddress", peer.announcedAddress.length() > 30 ? peer.announcedAddress.substring(0, 30) + "..." : peer.announcedAddress);
            activePeer.put("weight", Integer.valueOf(peer.getWeight()));
            activePeer.put("downloaded", Long.valueOf(peer.downloadedVolume));
            activePeer.put("uploaded", Long.valueOf(peer.uploadedVolume));
            activePeer.put("software", peer.getSoftware());
            for (String wellKnownPeer : wellKnownPeers) {
              if (peer.announcedAddress.equals(wellKnownPeer))
              {
                activePeer.put("wellKnown", Boolean.valueOf(true));
                
                break;
              }
            }
            activePeers.add(activePeer);
          }
        }
        long blockId = lastBlock;
        int numberOfBlocks = 0;
        while (numberOfBlocks < 60)
        {
          numberOfBlocks++;
          
          Nxt.Block block = (Nxt.Block)blocks.get(Long.valueOf(blockId));
          JSONObject recentBlock = new JSONObject();
          recentBlock.put("index", Integer.valueOf(block.index));
          recentBlock.put("timestamp", Integer.valueOf(block.timestamp));
          recentBlock.put("numberOfTransactions", Integer.valueOf(block.transactions.length));
          recentBlock.put("totalAmount", Integer.valueOf(block.totalAmount));
          recentBlock.put("totalFee", Integer.valueOf(block.totalFee));
          recentBlock.put("payloadLength", Integer.valueOf(block.payloadLength));
          recentBlock.put("generator", convert(block.getGeneratorAccountId()));
          recentBlock.put("height", Integer.valueOf(block.height));
          recentBlock.put("version", Integer.valueOf(block.version));
          recentBlock.put("block", block.getStringId());
          recentBlock.put("baseTarget", BigInteger.valueOf(block.baseTarget).multiply(BigInteger.valueOf(100000L)).divide(BigInteger.valueOf(153722867L)));
          
          recentBlocks.add(recentBlock);
          if (blockId == 2680262203532249785L) {
            break;
          }
          blockId = block.previousBlock;
        }
        JSONObject response = new JSONObject();
        response.put("response", "processInitialData");
        response.put("version", "0.5.7");
        if (unconfirmedTransactions.size() > 0) {
          response.put("unconfirmedTransactions", unconfirmedTransactions);
        }
        if (activePeers.size() > 0) {
          response.put("activePeers", activePeers);
        }
        if (knownPeers.size() > 0) {
          response.put("knownPeers", knownPeers);
        }
        if (blacklistedPeers.size() > 0) {
          response.put("blacklistedPeers", blacklistedPeers);
        }
        if (recentBlocks.size() > 0) {
          response.put("recentBlocks", recentBlocks);
        }
        user.pendingResponses.offer(response);
        

        break;
      case "getNewData": 
        break;
      case "lockAccount": 
        user.deinitializeKeyPair();
        
        JSONObject response = new JSONObject();
        response.put("response", "lockAccount");
        
        user.pendingResponses.offer(response);
        

        break;
      case "removeActivePeer": 
        if ((allowedUserHosts == null) && (!InetAddress.getByName(req.getRemoteAddr()).isLoopbackAddress()))
        {
          JSONObject response = new JSONObject();
          response.put("response", "showMessage");
          response.put("message", "This operation is allowed to local host users only!");
          
          user.pendingResponses.offer(response);
        }
        else
        {
          index = Integer.parseInt(req.getParameter("peer"));
          for (Nxt.Peer peer : peers.values()) {
            if (peer.index == index)
            {
              if ((peer.blacklistingTime != 0L) || (peer.state == 0)) {
                break;
              }
              peer.deactivate(); break;
            }
          }
        }
        break;
      case "removeBlacklistedPeer": 
        if ((allowedUserHosts == null) && (!InetAddress.getByName(req.getRemoteAddr()).isLoopbackAddress()))
        {
          JSONObject response = new JSONObject();
          response.put("response", "showMessage");
          response.put("message", "This operation is allowed to local host users only!");
          
          user.pendingResponses.offer(response);
        }
        else
        {
          index = Integer.parseInt(req.getParameter("peer"));
          for (Nxt.Peer peer : peers.values()) {
            if (peer.index == index)
            {
              if (peer.blacklistingTime <= 0L) {
                break;
              }
              peer.removeBlacklistedStatus(); break;
            }
          }
        }
        break;
      case "removeKnownPeer": 
        if ((allowedUserHosts == null) && (!InetAddress.getByName(req.getRemoteAddr()).isLoopbackAddress()))
        {
          JSONObject response = new JSONObject();
          response.put("response", "showMessage");
          response.put("message", "This operation is allowed to local host users only!");
          
          user.pendingResponses.offer(response);
        }
        else
        {
          index = Integer.parseInt(req.getParameter("peer"));
          for (Nxt.Peer peer : peers.values()) {
            if (peer.index == index)
            {
              peer.removePeer();
              
              break;
            }
          }
        }
        break;
      case "sendMoney": 
        if (user.secretPhrase != null)
        {
          String recipientValue = req.getParameter("recipient");String amountValue = req.getParameter("amount");String feeValue = req.getParameter("fee");String deadlineValue = req.getParameter("deadline");
          String secretPhrase = req.getParameter("secretPhrase");
          

          int amount = 0;int fee = 0;
          short deadline = 0;
          long recipient;
          try
          {
            recipient = parseUnsignedLong(recipientValue);
            amount = Integer.parseInt(amountValue.trim());
            fee = Integer.parseInt(feeValue.trim());
            deadline = (short)(int)(Double.parseDouble(deadlineValue) * 60.0D);
          }
          catch (Exception e)
          {
            JSONObject response = new JSONObject();
            response.put("response", "notifyOfIncorrectTransaction");
            response.put("message", "One of the fields is filled incorrectly!");
            response.put("recipient", recipientValue);
            response.put("amount", amountValue);
            response.put("fee", feeValue);
            response.put("deadline", deadlineValue);
            
            user.pendingResponses.offer(response);
            
            break;
          }
          if (!user.secretPhrase.equals(secretPhrase))
          {
            JSONObject response = new JSONObject();
            response.put("response", "notifyOfIncorrectTransaction");
            response.put("message", "Wrong secret phrase!");
            response.put("recipient", recipientValue);
            response.put("amount", amountValue);
            response.put("fee", feeValue);
            response.put("deadline", deadlineValue);
            
            user.pendingResponses.offer(response);
          }
          else if ((amount <= 0) || (amount > 1000000000L))
          {
            JSONObject response = new JSONObject();
            response.put("response", "notifyOfIncorrectTransaction");
            response.put("message", "\"Amount\" must be greater than 0!");
            response.put("recipient", recipientValue);
            response.put("amount", amountValue);
            response.put("fee", feeValue);
            response.put("deadline", deadlineValue);
            
            user.pendingResponses.offer(response);
          }
          else if ((fee <= 0) || (fee > 1000000000L))
          {
            JSONObject response = new JSONObject();
            response.put("response", "notifyOfIncorrectTransaction");
            response.put("message", "\"Fee\" must be greater than 0!");
            response.put("recipient", recipientValue);
            response.put("amount", amountValue);
            response.put("fee", feeValue);
            response.put("deadline", deadlineValue);
            
            user.pendingResponses.offer(response);
          }
          else if (deadline < 1)
          {
            JSONObject response = new JSONObject();
            response.put("response", "notifyOfIncorrectTransaction");
            response.put("message", "\"Deadline\" must be greater or equal to 1 minute!");
            response.put("recipient", recipientValue);
            response.put("amount", amountValue);
            response.put("fee", feeValue);
            response.put("deadline", deadlineValue);
            
            user.pendingResponses.offer(response);
          }
          else
          {
            byte[] publicKey = Nxt.Crypto.getPublicKey(user.secretPhrase);
            Nxt.Account account = (Nxt.Account)accounts.get(Long.valueOf(Nxt.Account.getId(publicKey)));
            if ((account == null) || ((amount + fee) * 100L > account.getUnconfirmedBalance()))
            {
              JSONObject response = new JSONObject();
              response.put("response", "notifyOfIncorrectTransaction");
              response.put("message", "Not enough funds!");
              response.put("recipient", recipientValue);
              response.put("amount", amountValue);
              response.put("fee", feeValue);
              response.put("deadline", deadlineValue);
              
              user.pendingResponses.offer(response);
            }
            else
            {
              Nxt.Transaction transaction = new Nxt.Transaction((byte)0, (byte)0, getEpochTime(System.currentTimeMillis()), deadline, publicKey, recipient, amount, fee, 0L, new byte[64]);
              transaction.sign(user.secretPhrase);
              
              JSONObject peerRequest = new JSONObject();
              peerRequest.put("requestType", "processTransactions");
              JSONArray transactionsData = new JSONArray();
              transactionsData.add(transaction.getJSONObject());
              peerRequest.put("transactions", transactionsData);
              
              Nxt.Peer.sendToSomePeers(peerRequest);
              
              JSONObject response = new JSONObject();
              response.put("response", "notifyOfAcceptedTransaction");
              
              user.pendingResponses.offer(response);
            }
          }
        }
        break;
      case "unlockAccount": 
        String secretPhrase = req.getParameter("secretPhrase");
        for (Nxt.User u : users.values()) {
          if (secretPhrase.equals(u.secretPhrase))
          {
            u.deinitializeKeyPair();
            if (!u.isInactive)
            {
              JSONObject response = new JSONObject();
              response.put("response", "lockAccount");
              u.pendingResponses.offer(response);
            }
          }
        }
        BigInteger bigInt = user.initializeKeyPair(secretPhrase);
        accountId = bigInt.longValue();
        
        JSONObject response = new JSONObject();
        response.put("response", "unlockAccount");
        response.put("account", bigInt.toString());
        if (secretPhrase.length() < 30) {
          response.put("secretPhraseStrength", Integer.valueOf(1));
        } else {
          response.put("secretPhraseStrength", Integer.valueOf(5));
        }
        Nxt.Account account = (Nxt.Account)accounts.get(Long.valueOf(accountId));
        if (account == null)
        {
          response.put("balance", Integer.valueOf(0));
        }
        else
        {
          response.put("balance", Long.valueOf(account.getUnconfirmedBalance()));
          if (account.getEffectiveBalance() > 0)
          {
            JSONObject response2 = new JSONObject();
            response2.put("response", "setBlockGenerationDeadline");
            
            Nxt.Block lastBlock = Nxt.Block.getLastBlock();
            MessageDigest digest = getMessageDigest("SHA-256");
            byte[] generationSignatureHash;
            byte[] generationSignatureHash;
            if (lastBlock.height < 30000)
            {
              byte[] generationSignature = Nxt.Crypto.sign(lastBlock.generationSignature, user.secretPhrase);
              generationSignatureHash = digest.digest(generationSignature);
            }
            else
            {
              digest.update(lastBlock.generationSignature);
              generationSignatureHash = digest.digest(Nxt.Crypto.getPublicKey(user.secretPhrase));
            }
            BigInteger hit = new BigInteger(1, new byte[] { generationSignatureHash[7], generationSignatureHash[6], generationSignatureHash[5], generationSignatureHash[4], generationSignatureHash[3], generationSignatureHash[2], generationSignatureHash[1], generationSignatureHash[0] });
            response2.put("deadline", Long.valueOf(hit.divide(BigInteger.valueOf(Nxt.Block.getBaseTarget()).multiply(BigInteger.valueOf(account.getEffectiveBalance()))).longValue() - (getEpochTime(System.currentTimeMillis()) - lastBlock.timestamp)));
            
            user.pendingResponses.offer(response2);
          }
          JSONArray myTransactions = new JSONArray();
          byte[] accountPublicKey = (byte[])account.publicKey.get();
          for (Nxt.Transaction transaction : unconfirmedTransactions.values()) {
            if (Arrays.equals(transaction.senderPublicKey, accountPublicKey))
            {
              JSONObject myTransaction = new JSONObject();
              myTransaction.put("index", Integer.valueOf(transaction.index));
              myTransaction.put("transactionTimestamp", Integer.valueOf(transaction.timestamp));
              myTransaction.put("deadline", Short.valueOf(transaction.deadline));
              myTransaction.put("account", convert(transaction.recipient));
              myTransaction.put("sentAmount", Integer.valueOf(transaction.amount));
              if (transaction.recipient == accountId) {
                myTransaction.put("receivedAmount", Integer.valueOf(transaction.amount));
              }
              myTransaction.put("fee", Integer.valueOf(transaction.fee));
              myTransaction.put("numberOfConfirmations", Integer.valueOf(0));
              myTransaction.put("id", transaction.getStringId());
              
              myTransactions.add(myTransaction);
            }
            else if (transaction.recipient == accountId)
            {
              JSONObject myTransaction = new JSONObject();
              myTransaction.put("index", Integer.valueOf(transaction.index));
              myTransaction.put("transactionTimestamp", Integer.valueOf(transaction.timestamp));
              myTransaction.put("deadline", Short.valueOf(transaction.deadline));
              myTransaction.put("account", convert(transaction.getSenderAccountId()));
              myTransaction.put("receivedAmount", Integer.valueOf(transaction.amount));
              myTransaction.put("fee", Integer.valueOf(transaction.fee));
              myTransaction.put("numberOfConfirmations", Integer.valueOf(0));
              myTransaction.put("id", transaction.getStringId());
              
              myTransactions.add(myTransaction);
            }
          }
          long blockId = lastBlock;
          int numberOfConfirmations = 1;
          while (myTransactions.size() < 1000)
          {
            Nxt.Block block = (Nxt.Block)blocks.get(Long.valueOf(blockId));
            if ((block.totalFee > 0) && (Arrays.equals(block.generatorPublicKey, accountPublicKey)))
            {
              JSONObject myTransaction = new JSONObject();
              myTransaction.put("index", block.getStringId());
              myTransaction.put("blockTimestamp", Integer.valueOf(block.timestamp));
              myTransaction.put("block", block.getStringId());
              myTransaction.put("earnedAmount", Integer.valueOf(block.totalFee));
              myTransaction.put("numberOfConfirmations", Integer.valueOf(numberOfConfirmations));
              myTransaction.put("id", "-");
              
              myTransactions.add(myTransaction);
            }
            for (long transactionId : block.transactions)
            {
              Nxt.Transaction transaction = (Nxt.Transaction)transactions.get(Long.valueOf(transactionId));
              if (Arrays.equals(transaction.senderPublicKey, accountPublicKey))
              {
                JSONObject myTransaction = new JSONObject();
                myTransaction.put("index", Integer.valueOf(transaction.index));
                myTransaction.put("blockTimestamp", Integer.valueOf(block.timestamp));
                myTransaction.put("transactionTimestamp", Integer.valueOf(transaction.timestamp));
                myTransaction.put("account", convert(transaction.recipient));
                myTransaction.put("sentAmount", Integer.valueOf(transaction.amount));
                if (transaction.recipient == accountId) {
                  myTransaction.put("receivedAmount", Integer.valueOf(transaction.amount));
                }
                myTransaction.put("fee", Integer.valueOf(transaction.fee));
                myTransaction.put("numberOfConfirmations", Integer.valueOf(numberOfConfirmations));
                myTransaction.put("id", transaction.getStringId());
                
                myTransactions.add(myTransaction);
              }
              else if (transaction.recipient == accountId)
              {
                JSONObject myTransaction = new JSONObject();
                myTransaction.put("index", Integer.valueOf(transaction.index));
                myTransaction.put("blockTimestamp", Integer.valueOf(block.timestamp));
                myTransaction.put("transactionTimestamp", Integer.valueOf(transaction.timestamp));
                myTransaction.put("account", convert(transaction.getSenderAccountId()));
                myTransaction.put("receivedAmount", Integer.valueOf(transaction.amount));
                myTransaction.put("fee", Integer.valueOf(transaction.fee));
                myTransaction.put("numberOfConfirmations", Integer.valueOf(numberOfConfirmations));
                myTransaction.put("id", transaction.getStringId());
                
                myTransactions.add(myTransaction);
              }
            }
            if (blockId == 2680262203532249785L) {
              break;
            }
            blockId = block.previousBlock;
            numberOfConfirmations++;
          }
          if (myTransactions.size() > 0)
          {
            JSONObject response2 = new JSONObject();
            response2.put("response", "processNewData");
            response2.put("addedMyTransactions", myTransactions);
            
            user.pendingResponses.offer(response2);
          }
        }
        user.pendingResponses.offer(response);
        

        break;
      default: 
        JSONObject response = new JSONObject();
        response.put("response", "showMessage");
        response.put("message", "Incorrect request!");
        
        user.pendingResponses.offer(response);
      }
    }
    catch (Exception e)
    {
      if (user != null)
      {
        logMessage("Error processing GET request", e);
        
        JSONObject response = new JSONObject();
        response.put("response", "showMessage");
        response.put("message", e.toString());
        
        user.pendingResponses.offer(response);
      }
      else
      {
        logDebugMessage("Error processing GET request", e);
      }
    }
    if (user != null) {
      synchronized (user)
      {
        JSONArray responses = new JSONArray();
        JSONObject pendingResponse;
        while ((pendingResponse = (JSONObject)user.pendingResponses.poll()) != null) {
          responses.add(pendingResponse);
        }
        Object writer;
        if (responses.size() > 0)
        {
          JSONObject combinedResponse = new JSONObject();
          combinedResponse.put("responses", responses);
          if (user.asyncContext != null)
          {
            user.asyncContext.getResponse().setContentType("text/plain; charset=UTF-8");
            
            Object writer = user.asyncContext.getResponse().getWriter();accountId = null;
            try
            {
              combinedResponse.writeJSONString((Writer)writer);
            }
            catch (Throwable localThrowable3)
            {
              accountId = localThrowable3;throw localThrowable3;
            }
            finally
            {
              if (writer != null) {
                if (accountId != null) {
                  try
                  {
                    ((Writer)writer).close();
                  }
                  catch (Throwable x2)
                  {
                    accountId.addSuppressed(x2);
                  }
                } else {
                  ((Writer)writer).close();
                }
              }
            }
            user.asyncContext.complete();
            user.asyncContext = req.startAsync();
            user.asyncContext.addListener(new Nxt.UserAsyncListener(user));
            user.asyncContext.setTimeout(5000L);
          }
          else
          {
            resp.setContentType("text/plain; charset=UTF-8");
            
            writer = resp.getWriter();accountId = null;
            try
            {
              combinedResponse.writeJSONString((Writer)writer);
            }
            catch (Throwable localThrowable4)
            {
              accountId = localThrowable4;throw localThrowable4;
            }
            finally
            {
              if (writer != null) {
                if (accountId != null) {
                  try
                  {
                    ((Writer)writer).close();
                  }
                  catch (Throwable x2)
                  {
                    accountId.addSuppressed(x2);
                  }
                } else {
                  ((Writer)writer).close();
                }
              }
            }
          }
        }
        else
        {
          if (user.asyncContext != null)
          {
            user.asyncContext.getResponse().setContentType("text/plain; charset=UTF-8");
            
            Object writer = user.asyncContext.getResponse().getWriter();writer = null;
            try
            {
              new JSONObject().writeJSONString((Writer)writer);
            }
            catch (Throwable localThrowable5)
            {
              writer = localThrowable5;throw localThrowable5;
            }
            finally
            {
              if (writer != null) {
                if (writer != null) {
                  try
                  {
                    ((Writer)writer).close();
                  }
                  catch (Throwable x2)
                  {
                    ((Throwable)writer).addSuppressed(x2);
                  }
                } else {
                  ((Writer)writer).close();
                }
              }
            }
            user.asyncContext.complete();
          }
          user.asyncContext = req.startAsync();
          user.asyncContext.addListener(new Nxt.UserAsyncListener(user));
          user.asyncContext.setTimeout(5000L);
        }
      }
    }
  }
  
  public void doPost(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException
  {
    Nxt.Peer peer = null;
    
    JSONObject response = new JSONObject();
    try
    {
      ??? = new Nxt.CountingInputStream(req.getInputStream());
      
      ??? = new BufferedReader(new InputStreamReader(???, "UTF-8"));Throwable localThrowable3 = null;
      JSONObject request;
      try
      {
        request = (JSONObject)JSONValue.parse(???);
      }
      catch (Throwable localThrowable1)
      {
        localThrowable3 = localThrowable1;throw localThrowable1;
      }
      finally
      {
        if (??? != null) {
          if (localThrowable3 != null) {
            try
            {
              ???.close();
            }
            catch (Throwable x2)
            {
              localThrowable3.addSuppressed(x2);
            }
          } else {
            ???.close();
          }
        }
      }
      if (request == null) {
        return;
      }
      peer = Nxt.Peer.addPeer(req.getRemoteHost(), "");
      if (peer != null)
      {
        if (peer.state == 2) {
          peer.setState(1);
        }
        peer.updateDownloadedVolume(???.getCount());
      }
      if ((request.get("protocol") != null) && (((Number)request.get("protocol")).intValue() == 1))
      {
        switch ((String)request.get("requestType"))
        {
        case "getCumulativeDifficulty": 
          response.put("cumulativeDifficulty", Nxt.Block.getLastBlock().cumulativeDifficulty.toString());
          

          break;
        case "getInfo": 
          if (peer != null)
          {
            String announcedAddress = (String)request.get("announcedAddress");
            if (announcedAddress != null)
            {
              announcedAddress = announcedAddress.trim();
              if (announcedAddress.length() > 0) {
                peer.announcedAddress = announcedAddress;
              }
            }
            String application = (String)request.get("application");
            if (application == null)
            {
              application = "?";
            }
            else
            {
              application = application.trim();
              if (application.length() > 20) {
                application = application.substring(0, 20) + "...";
              }
            }
            peer.application = application;
            
            String version = (String)request.get("version");
            if (version == null)
            {
              version = "?";
            }
            else
            {
              version = version.trim();
              if (version.length() > 10) {
                version = version.substring(0, 10) + "...";
              }
            }
            peer.version = version;
            
            String platform = (String)request.get("platform");
            if (platform == null)
            {
              platform = "?";
            }
            else
            {
              platform = platform.trim();
              if (platform.length() > 10) {
                platform = platform.substring(0, 10) + "...";
              }
            }
            peer.platform = platform;
            
            peer.shareAddress = Boolean.TRUE.equals(request.get("shareAddress"));
            if (peer.analyzeHallmark(req.getRemoteHost(), (String)request.get("hallmark"))) {
              peer.setState(1);
            }
          }
          if ((myHallmark != null) && (myHallmark.length() > 0)) {
            response.put("hallmark", myHallmark);
          }
          response.put("application", "NRS");
          response.put("version", "0.5.7");
          response.put("platform", myPlatform);
          response.put("shareAddress", Boolean.valueOf(shareMyAddress));
          

          break;
        case "getMilestoneBlockIds": 
          JSONArray milestoneBlockIds = new JSONArray();
          Nxt.Block block = Nxt.Block.getLastBlock();
          int jumpLength = block.height * 4 / 1461 + 1;
          for (; block.height > 0; goto 1005)
          {
            milestoneBlockIds.add(block.getStringId());
            int i = 0;
            if ((i < jumpLength) && (block.height > 0))
            {
              block = (Nxt.Block)blocks.get(Long.valueOf(block.previousBlock));i++;
            }
          }
          response.put("milestoneBlockIds", milestoneBlockIds);
          

          break;
        case "getNextBlockIds": 
          JSONArray nextBlockIds = new JSONArray();
          Nxt.Block block = (Nxt.Block)blocks.get(Long.valueOf(parseUnsignedLong((String)request.get("blockId"))));
          while ((block != null) && (nextBlockIds.size() < 1440))
          {
            block = (Nxt.Block)blocks.get(Long.valueOf(block.nextBlock));
            if (block != null) {
              nextBlockIds.add(block.getStringId());
            }
          }
          response.put("nextBlockIds", nextBlockIds);
          

          break;
        case "getNextBlocks": 
          Object nextBlocks = new ArrayList();
          int totalLength = 0;
          Nxt.Block block = (Nxt.Block)blocks.get(Long.valueOf(parseUnsignedLong((String)request.get("blockId"))));
          while (block != null)
          {
            block = (Nxt.Block)blocks.get(Long.valueOf(block.nextBlock));
            if (block != null)
            {
              int length = 224 + block.payloadLength;
              if (totalLength + length > 1048576) {
                break;
              }
              ((List)nextBlocks).add(block);
              totalLength += length;
            }
          }
          JSONArray nextBlocksArray = new JSONArray();
          for (Nxt.Block nextBlock : (List)nextBlocks) {
            nextBlocksArray.add(nextBlock.getJSONStreamAware());
          }
          response.put("nextBlocks", nextBlocksArray);
          

          break;
        case "getPeers": 
          JSONArray peers = new JSONArray();
          for (Nxt.Peer otherPeer : peers.values()) {
            if ((otherPeer.blacklistingTime == 0L) && (otherPeer.announcedAddress.length() > 0) && (otherPeer.state == 1) && (otherPeer.shareAddress)) {
              peers.add(otherPeer.announcedAddress);
            }
          }
          response.put("peers", peers);
          

          break;
        case "getUnconfirmedTransactions": 
          JSONArray transactionsData = new JSONArray();
          for (Nxt.Transaction transaction : unconfirmedTransactions.values()) {
            transactionsData.add(transaction.getJSONObject());
          }
          response.put("unconfirmedTransactions", transactionsData);
          

          break;
        case "processBlock": 
          Nxt.Block block = Nxt.Block.getBlock(request);
          boolean accepted;
          if (block == null)
          {
            boolean accepted = false;
            if (peer != null) {
              peer.blacklist();
            }
          }
          else
          {
            ByteBuffer buffer = ByteBuffer.allocate(224 + block.payloadLength);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            
            buffer.put(block.getBytes());
            
            JSONArray transactionsData = (JSONArray)request.get("transactions");
            for (Object transaction : transactionsData) {
              buffer.put(Nxt.Transaction.getTransaction((JSONObject)transaction).getBytes());
            }
            accepted = Nxt.Block.pushBlock(buffer, true);
          }
          response.put("accepted", Boolean.valueOf(accepted));
          

          break;
        case "processTransactions": 
          Nxt.Transaction.processTransactions(request, "transactions");
          

          break;
        default: 
          response.put("error", "Unsupported request type!");
        }
      }
      else
      {
        logDebugMessage("Unsupported protocol " + request.get("protocol"));
        response.put("error", "Unsupported protocol!");
      }
    }
    catch (RuntimeException e)
    {
      logDebugMessage("Error processing POST request", e);
      response.put("error", e.toString());
    }
    resp.setContentType("text/plain; charset=UTF-8");
    
    Nxt.CountingOutputStream cos = new Nxt.CountingOutputStream(resp.getOutputStream());
    Writer writer = new BufferedWriter(new OutputStreamWriter(cos, "UTF-8"));??? = null;
    try
    {
      response.writeJSONString(writer);
    }
    catch (Throwable localThrowable4)
    {
      ??? = localThrowable4;throw localThrowable4;
    }
    finally
    {
      if (writer != null) {
        if (??? != null) {
          try
          {
            writer.close();
          }
          catch (Throwable x2)
          {
            ???.addSuppressed(x2);
          }
        } else {
          writer.close();
        }
      }
    }
    if (peer != null) {
      peer.updateUploadedVolume(cos.getCount());
    }
  }
  
  public void destroy()
  {
    shutdownExecutor(scheduledThreadPool);
    shutdownExecutor(sendToPeersService);
    try
    {
      Nxt.Block.saveBlocks("blocks.nxt", true);
      logMessage("Saved blocks.nxt");
    }
    catch (RuntimeException e)
    {
      logMessage("Error saving blocks", e);
    }
    try
    {
      Nxt.Transaction.saveTransactions("transactions.nxt");
      logMessage("Saved transactions.nxt");
    }
    catch (RuntimeException e)
    {
      logMessage("Error saving transactions", e);
    }
    logMessage("NRS 0.5.7 stopped.");
  }
  
  private static void shutdownExecutor(ExecutorService executor)
  {
    executor.shutdown();
    try
    {
      executor.awaitTermination(10L, TimeUnit.SECONDS);
    }
    catch (InterruptedException e)
    {
      Thread.currentThread().interrupt();
    }
