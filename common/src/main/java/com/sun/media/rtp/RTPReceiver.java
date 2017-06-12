/*     */ package com.sun.media.rtp;
/*     */ 
/*     */ import com.sun.media.Log;
import com.sun.media.protocol.rtp.DataSource;
import com.sun.media.rtp.util.*;

import javax.media.Format;
import javax.media.protocol.PushBufferStream;
import javax.media.rtp.ReceiveStream;
import javax.media.rtp.SessionAddress;
import javax.media.rtp.event.ActiveReceiveStreamEvent;
import javax.media.rtp.event.NewReceiveStreamEvent;
import javax.media.rtp.event.RemotePayloadChangeEvent;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

/*     */
/*     */
/*     */
/*     */
/*     */
/*     */
/*     */
/*     */
/*     */
/*     */
/*     */
/*     */
/*     */
/*     */
/*     */
/*     */
/*     */
/*     */
/*     */
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public class RTPReceiver
/*     */   extends PacketFilter
/*     */ {
/*     */   SSRCCache cache;
/*     */   RTPDemultiplexer rtpdemultiplexer;
/*  38 */   int lastseqnum = -1;
/*  39 */   private boolean rtcpstarted = false;
/*  40 */   private boolean setpriority = false;
/*  41 */   private boolean mismatchprinted = false;
/*  42 */   private String content = "";
/*     */   
/*  44 */   SSRCTable probationList = new SSRCTable();
/*     */   
/*     */   static final int MAX_DROPOUT = 3000;
/*     */   
/*     */   static final int MAX_MISORDER = 100;
/*     */   
/*     */   static final int SEQ_MOD = 65536;
/*     */   static final int MIN_SEQUENTIAL = 2;
/*  52 */   private boolean initBC = false;
/*  53 */   public String filtername() { return "RTP Packet Receiver"; }
/*  54 */   public String controlstr = "javax.media.rtp.RTPControl";
/*     */   
/*  56 */   private int errorPayload = -1;
/*     */   
/*     */   public RTPReceiver(SSRCCache cache, RTPDemultiplexer rtpdemux)
/*     */   {
/*  60 */     this.cache = cache;
/*  61 */     this.rtpdemultiplexer = rtpdemux;
/*     */     
/*     */ 
/*     */ 
/*  65 */     setConsumer(null);
/*     */   }
/*     */   
/*     */ 
/*     */   public RTPReceiver(SSRCCache cache, RTPDemultiplexer rtpdemux, PacketSource source)
/*     */   {
/*  71 */     this(cache, rtpdemux);
/*  72 */     setSource(source);
/*     */   }
/*     */   
/*     */ 
/*     */   public RTPReceiver(SSRCCache cache, RTPDemultiplexer rtpdemux, DatagramSocket sock)
/*     */   {
/*  78 */     this(cache, rtpdemux, new RTPRawReceiver(sock, cache.sm.defaultstats));
/*     */   }
/*     */   
/*     */   public RTPReceiver(SSRCCache cache, RTPDemultiplexer rtpdemux, int port, String address)
/*     */     throws UnknownHostException, IOException
/*     */   {
/*  84 */     this(cache, rtpdemux, new RTPRawReceiver(port & 0xFFFFFFFE, address, cache.sm.defaultstats));
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Packet handlePacket(Packet p)
/*     */   {
/* 125 */     return handlePacket((RTPPacket)p);
/*     */   }
/*     */   
/*     */   public Packet handlePacket(Packet p, int index) {
/* 129 */     return null;
/*     */   }
/*     */   
/*     */   public Packet handlePacket(Packet p, SessionAddress a) {
/* 133 */     return null;
/*     */   }
/*     */   
/*     */   public Packet handlePacket(Packet p, SessionAddress a, boolean b)
/*     */   {
/* 138 */     return null;
/*     */   }
/*     */   
/*     */   public Packet handlePacket(RTPPacket p) {
/* 142 */     SSRCInfo info = null;
/* 143 */     if ((p.base instanceof UDPPacket)) {
/* 144 */       InetAddress remoteAddress = ((UDPPacket)p.base).remoteAddress;
/*     */       
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/* 152 */       if ((this.cache.sm.bindtome) && (!this.cache.sm.isBroadcast(this.cache.sm.dataaddress)) && (!remoteAddress.equals(this.cache.sm.dataaddress)))
/*     */       {
/*     */ 
/*     */ 
/* 156 */         return null;
/*     */       }
/*     */     }
/* 159 */     else if ((p.base instanceof Packet)) {
/* 160 */       p.base.toString();
/*     */     }
/*     */     
/*     */ 
/*     */ 
/*     */ 
/* 166 */     if (info == null)
/*     */     {
/* 168 */       if ((p.base instanceof UDPPacket)) {
/* 169 */         info = this.cache.get(p.ssrc, ((UDPPacket)p.base).remoteAddress, ((UDPPacket)p.base).remotePort, 1);
/*     */ 
/*     */       }
/*     */       else
/*     */       {
/* 174 */         info = this.cache.get(p.ssrc, null, 0, 1);
/*     */       }
/*     */     }
/*     */     
/*     */ 
/*     */ 
/* 180 */     if (info == null) {
/* 181 */       return null;
/*     */     }
/*     */     
/*     */ 
/* 185 */     for (int i = 0; i < p.csrc.length; i++) {
/* 186 */       SSRCInfo cinfo = null;
/* 187 */       if ((p.base instanceof UDPPacket)) {
/* 188 */         cinfo = this.cache.get(p.csrc[i], ((UDPPacket)p.base).remoteAddress, ((UDPPacket)p.base).remotePort, 1);
/*     */ 
/*     */       }
/*     */       else
/*     */       {
/* 193 */         cinfo = this.cache.get(p.csrc[i], null, 0, 1);
/*     */       }
/*     */       
/*     */ 
/* 197 */       if (cinfo != null) {
/* 198 */         cinfo.lastHeardFrom = p.receiptTime;
/*     */       }
/*     */     }
/*     */     
/*     */ 
/*     */ 
/* 204 */     if ((info.lastPayloadType != -1) && (info.lastPayloadType == p.payloadType) && (this.mismatchprinted))
/*     */     {
/*     */ 
/* 207 */       return null;
/*     */     }
/*     */     
/*     */ 
/*     */ 
/* 212 */     if (!info.sender) {
/* 213 */       info.initsource(p.seqnum);
/* 214 */       info.payloadType = p.payloadType;
/*     */     }
/*     */     
/* 217 */     int deltaseq = p.seqnum - info.maxseq;
/* 218 */     if (info.maxseq + 1 != p.seqnum)
/*     */     {
/*     */ 
/*     */ 
/* 222 */       if (deltaseq > 0) {
/* 223 */         info.stats.update(0, deltaseq - 1);
/*     */       }
/*     */     }
/*     */     
/* 227 */     if (info.wrapped) {
/* 228 */       info.wrapped = false;
/*     */     }
/* 230 */     boolean justOutOfProbation = false;
/*     */     
/* 232 */     if (info.probation > 0) {
/* 233 */       if (p.seqnum == info.maxseq + 1) {
/* 234 */         info.probation -= 1;
/* 235 */         info.maxseq = p.seqnum;
/* 236 */         if (info.probation == 0)
/*     */         {
/* 238 */           justOutOfProbation = true;
/*     */         }
/*     */       }
/*     */       else {
/* 242 */         info.probation = 1;
/* 243 */         info.maxseq = p.seqnum;
/* 244 */         info.stats.update(2);
/*     */       }
/*     */       
/*     */     }
/* 248 */     else if (deltaseq < 100)
/*     */     {
/*     */ 
/* 251 */       if (p.seqnum < info.maxseq) {
/* 252 */         info.cycles += 65536;
/* 253 */         info.wrapped = true;
/*     */       }
/* 255 */       info.maxseq = p.seqnum;
/*     */     }
/* 257 */     else if (deltaseq <= 65436) {
/* 258 */       info.stats.update(3);
/* 259 */       if (p.seqnum == info.lastbadseq) {
/* 260 */         info.initsource(p.seqnum);
/*     */       } else {
/* 262 */         info.lastbadseq = (p.seqnum + 1 & 0xFFFF);
/*     */       }
/*     */     }
/*     */     else {
/* 266 */       info.stats.update(4);
/*     */     }
/*     */     
/* 269 */     boolean unicast = this.cache.sm.isUnicast();
/*     */     
/* 271 */     if (unicast) {
/* 272 */       if (!this.rtcpstarted) {
/* 273 */         this.cache.sm.startRTCPReports(((UDPPacket)p.base).remoteAddress);
/* 274 */         this.rtcpstarted = true;
/*     */         
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/* 282 */         byte[] lsb = this.cache.sm.controladdress.getAddress();
/* 283 */         int address = lsb[3] & 0xFF;
/* 284 */         if ((address & 0xFF) == 255) {
/* 285 */           this.cache.sm.addUnicastAddr(this.cache.sm.controladdress);
/*     */         }
/*     */         else {
/* 288 */           InetAddress localaddr = null;
/* 289 */           boolean localfound = true;
/*     */           try {
/* 291 */             localaddr = InetAddress.getLocalHost();
/*     */           } catch (UnknownHostException e) {
/* 293 */             localfound = false;
/*     */           }
/* 295 */           if (localfound) {
/* 296 */             this.cache.sm.addUnicastAddr(localaddr);
/*     */           }
/*     */         }
/*     */       }
/* 300 */       else if (!this.cache.sm.isSenderDefaultAddr(((UDPPacket)p.base).remoteAddress))
/*     */       {
/* 302 */         this.cache.sm.addUnicastAddr(((UDPPacket)p.base).remoteAddress);
/*     */       }
/*     */     }
/*     */     
/*     */ 
/*     */ 
/*     */ 
/* 309 */     info.received += 1;
/* 310 */     info.stats.update(1);
/* 311 */     if (info.probation > 0)
/*     */     {
/*     */ 
/*     */ 
/* 315 */       this.probationList.put(info.ssrc, p.clone());
/*     */       
/*     */ 
/* 318 */       return null;
/*     */     }
/*     */     
/*     */ 
/*     */ 
/* 323 */     info.maxseq = p.seqnum;
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/* 336 */     if ((info.lastPayloadType != -1) && (info.lastPayloadType != p.payloadType))
/*     */     {
/* 338 */       info.currentformat = null;
/* 339 */       if (info.dsource != null) {
/* 340 */         RTPControlImpl control = (RTPControlImpl)info.dsource.getControl(this.controlstr);
/* 341 */         if (control != null) {
/* 342 */           control.currentformat = null;
/* 343 */           control.payload = -1;
/*     */         }
/*     */       }
/* 346 */       info.lastPayloadType = p.payloadType;
/*     */       
/*     */ 
/*     */ 
/*     */ 
/* 351 */       if (info.dsource != null) {
/*     */         try {
/* 353 */           info.dsource.stop();
/*     */         } catch (IOException e) {
/* 355 */           System.err.println("Stopping DataSource after PCE " + e.getMessage());
/*     */         }
/*     */       }
/* 358 */       RemotePayloadChangeEvent event = new RemotePayloadChangeEvent(this.cache.sm, (ReceiveStream)info, info.lastPayloadType, p.payloadType);
/*     */       
/*     */ 
/*     */ 
/*     */ 
/* 363 */       this.cache.eventhandler.postEvent(event);
/*     */     }
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/* 371 */     if (info.currentformat == null) {
/* 372 */       info.currentformat = this.cache.sm.formatinfo.get(p.payloadType);
/* 373 */       if (info.currentformat == null) {
/* 374 */         if (this.errorPayload != p.payloadType) {
/* 375 */           Log.error("No format has been registered for RTP Payload type " + p.payloadType);
/* 376 */           this.errorPayload = p.payloadType;
/*     */         }
/* 378 */         return p;
/*     */       }
/* 380 */       if (info.dstream != null) {
/* 381 */         info.dstream.setFormat(info.currentformat);
/*     */       }
/*     */     }
/* 384 */     if (info.currentformat == null) {
/* 385 */       System.err.println("No Format for PT= " + p.payloadType);
/* 386 */       return p;
/*     */     }
/*     */     
/*     */ 
/* 390 */     if (info.dsource != null) {
/* 391 */       RTPControlImpl control = (RTPControlImpl)info.dsource.getControl(this.controlstr);
/*     */       
/* 393 */       if (control != null)
/*     */       {
/*     */ 
/* 396 */         Format fmt = this.cache.sm.formatinfo.get(p.payloadType);
/* 397 */         control.currentformat = fmt;
/*     */       }
/*     */     }
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/* 406 */     if (!this.initBC) {
/* 407 */       ((BufferControlImpl)this.cache.sm.buffercontrol).initBufferControl(info.currentformat);
/* 408 */       this.initBC = true;
/*     */     }
/*     */     
/* 411 */     if (!info.streamconnect)
/*     */     {
/* 413 */       DataSource source = (DataSource)this.cache.sm.dslist.get(info.ssrc);
/*     */       
/*     */ 
/*     */ 
/*     */ 
/* 418 */       if (source == null)
/*     */       {
/* 420 */         DataSource defaultsource = this.cache.sm.getDataSource(null);
/*     */         
/*     */ 
/* 423 */         if (defaultsource == null) {
/* 424 */           source = this.cache.sm.createNewDS(null);
/* 425 */           this.cache.sm.setDefaultDSassigned(info.ssrc);
/*     */ 
/*     */ 
/*     */ 
/*     */         }
/* 430 */         else if (!this.cache.sm.isDefaultDSassigned()) {
/* 431 */           source = defaultsource;
/* 432 */           this.cache.sm.setDefaultDSassigned(info.ssrc);
/*     */         }
/*     */         else {
/* 435 */           source = this.cache.sm.createNewDS(info.ssrc);
/*     */         }
/*     */       }
/*     */       
/* 439 */       PushBufferStream[] streams = source.getStreams();
/*     */       
/* 441 */       info.dsource = source;
/* 442 */       info.dstream = ((RTPSourceStream)streams[0]);
/* 443 */       info.dstream.setContentDescriptor(this.content);
/* 444 */       info.dstream.setFormat(info.currentformat);
/*     */       
/*     */ 
/*     */ 
/*     */ 
/* 449 */       RTPControlImpl control = (RTPControlImpl)info.dsource.getControl(this.controlstr);
/*     */       
/* 451 */       if (control != null)
/*     */       {
/*     */ 
/* 454 */         Format fmt = this.cache.sm.formatinfo.get(p.payloadType);
/* 455 */         control.currentformat = fmt;
/* 456 */         control.stream = info;
/*     */       }
/*     */       
/* 459 */       info.streamconnect = true;
/*     */     }
/*     */     
/*     */ 
/*     */ 
/* 464 */     if (info.dsource != null) {
/* 465 */       info.active = true;
/*     */     }
/* 467 */     if (!info.newrecvstream) {
/* 468 */       NewReceiveStreamEvent evt = new NewReceiveStreamEvent(this.cache.sm, (ReceiveStream)info);
/*     */       
/*     */ 
/* 471 */       info.newrecvstream = true;
/* 472 */       this.cache.eventhandler.postEvent(evt);
/*     */     }
/*     */     
/* 475 */     if ((info.lastRTPReceiptTime != 0L) && (info.lastPayloadType == p.payloadType))
/*     */     {
/* 477 */       long abstimediff = p.receiptTime - info.lastRTPReceiptTime;
/* 478 */       abstimediff = abstimediff * this.cache.clockrate[info.payloadType] / 1000L;
/* 479 */       long rtptimediff = p.timestamp - info.lasttimestamp;
/* 480 */       double timediff = abstimediff - rtptimediff;
/* 481 */       if (timediff < 0.0D)
/* 482 */         timediff = -timediff;
/* 483 */       info.jitter += 0.0625D * (timediff - info.jitter);
/*     */     }
/*     */     
/* 486 */     info.lastRTPReceiptTime = p.receiptTime;
/* 487 */     info.lasttimestamp = p.timestamp;
/* 488 */     info.payloadType = p.payloadType;
/*     */     
/* 490 */     info.lastPayloadType = p.payloadType;
/* 491 */     info.bytesreceived += p.payloadlength;
/* 492 */     info.lastHeardFrom = p.receiptTime;
/* 493 */     if (info.quiet) {
/* 494 */       info.quiet = false;
/*     */       
/* 496 */       ActiveReceiveStreamEvent event = null;
/* 497 */       if ((info instanceof ReceiveStream)) {
/* 498 */         event = new ActiveReceiveStreamEvent(this.cache.sm, info.sourceInfo, (ReceiveStream)info);
/*     */       }
/*     */       else
/*     */       {
/* 502 */         event = new ActiveReceiveStreamEvent(this.cache.sm, info.sourceInfo, null);
/*     */       }
/*     */       
/* 505 */       this.cache.eventhandler.postEvent(event);
/*     */     }
/*     */     
/*     */ 
/* 509 */     SourceRTPPacket sp = new SourceRTPPacket(p, info);
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/* 524 */     if (info.dsource != null) {
/* 525 */       if (this.mismatchprinted)
/* 526 */         this.mismatchprinted = false;
/* 527 */       if (justOutOfProbation)
/*     */       {
/*     */ 
/* 530 */         RTPPacket pp = (RTPPacket)this.probationList.remove(info.ssrc);
/* 531 */         if (pp != null) {
/* 532 */           this.rtpdemultiplexer.demuxpayload(new SourceRTPPacket(pp, info));
/*     */         }
/*     */       }
/* 535 */       this.rtpdemultiplexer.demuxpayload(sp);
/*     */     }
/*     */     
/* 538 */     return p;
/*     */   }
/*     */ }


/* Location:              /Users/jeaminw/Desktop/tmp/jmf-2.1.1e.jar!/com/sun/media/rtp/RTPReceiver.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */