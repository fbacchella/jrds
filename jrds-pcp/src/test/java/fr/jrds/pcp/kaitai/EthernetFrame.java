package fr.jrds.pcp.kaitai;
//This is a generated file! Please edit source .ksy file and use kaitai-struct-compiler to rebuild

import io.kaitai.struct.ByteBufferKaitaiStream;
import io.kaitai.struct.KaitaiStruct;
import io.kaitai.struct.KaitaiStream;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;


/**
* Ethernet frame is a OSI data link layer (layer 2) protocol data unit
* for Ethernet networks. In practice, many other networks and/or
* in-file dumps adopted the same format for encapsulation purposes.
* @see <a href="https://ieeexplore.ieee.org/document/7428776">Source</a>
*/
public class EthernetFrame extends KaitaiStruct {
 public static EthernetFrame fromFile(String fileName) throws IOException {
     return new EthernetFrame(new ByteBufferKaitaiStream(fileName));
 }

 public enum EtherTypeEnum {
     IPV4(2048),
     X_75_INTERNET(2049),
     NBS_INTERNET(2050),
     ECMA_INTERNET(2051),
     CHAOSNET(2052),
     X_25_LEVEL_3(2053),
     ARP(2054),
     IEEE_802_1Q_TPID(33024),
     IPV6(34525);

     private final long id;
     EtherTypeEnum(long id) { this.id = id; }
     public long id() { return id; }
     private static final Map<Long, EtherTypeEnum> byId = new HashMap<>(9);
     static {
         for (EtherTypeEnum e : EtherTypeEnum.values())
             byId.put(e.id(), e);
     }
     public static EtherTypeEnum byId(long id) { return byId.get(id); }
 }

 public EthernetFrame(KaitaiStream _io) {
     this(_io, null, null);
 }

 public EthernetFrame(KaitaiStream _io, KaitaiStruct _parent) {
     this(_io, _parent, null);
 }

 public EthernetFrame(KaitaiStream _io, KaitaiStruct _parent, EthernetFrame _root) {
     super(_io);
     this._parent = _parent;
     this._root = _root == null ? this : _root;
     _read();
 }
 private void _read() {
     this.dstMac = this._io.readBytes(6);
     this.srcMac = this._io.readBytes(6);
     this.etherType1 = EtherTypeEnum.byId(this._io.readU2be());
     if (etherType1() == EtherTypeEnum.IEEE_802_1Q_TPID) {
         this.tci = new TagControlInfo(this._io, this, _root);
     }
     if (etherType1() == EtherTypeEnum.IEEE_802_1Q_TPID) {
         this.etherType2 = EtherTypeEnum.byId(this._io.readU2be());
     }
     switch (etherType()) {
     case IPV4: {
         this._raw_body = this._io.readBytesFull();
         KaitaiStream _io__raw_body = new ByteBufferKaitaiStream(_raw_body);
         this.body = new Ipv4Packet(_io__raw_body);
         break;
     }
     case IPV6: {
         this._raw_body = this._io.readBytesFull();
         KaitaiStream _io__raw_body = new ByteBufferKaitaiStream(_raw_body);
         this.body = new Ipv6Packet(_io__raw_body);
         break;
     }
     default: {
         this.body = this._io.readBytesFull();
         break;
     }
     }
 }

 /**
  * Tag Control Information (TCI) is an extension of IEEE 802.1Q to
  * support VLANs on normal IEEE 802.3 Ethernet network.
  */
 public static class TagControlInfo extends KaitaiStruct {
     public static TagControlInfo fromFile(String fileName) throws IOException {
         return new TagControlInfo(new ByteBufferKaitaiStream(fileName));
     }

     public TagControlInfo(KaitaiStream _io) {
         this(_io, null, null);
     }

     public TagControlInfo(KaitaiStream _io, EthernetFrame _parent) {
         this(_io, _parent, null);
     }

     public TagControlInfo(KaitaiStream _io, EthernetFrame _parent, EthernetFrame _root) {
         super(_io);
         this._parent = _parent;
         this._root = _root;
         _read();
     }
     private void _read() {
         this.priority = this._io.readBitsInt(3);
         this.dropEligible = this._io.readBitsInt(1) != 0;
         this.vlanId = this._io.readBitsInt(12);
     }
     private long priority;
     private boolean dropEligible;
     private long vlanId;
     private final EthernetFrame _root;
     private final EthernetFrame _parent;

     /**
      * Priority Code Point (PCP) is used to specify priority for
      * different kinds of traffic.
      */
     public long priority() { return priority; }

     /**
      * Drop Eligible Indicator (DEI) specifies if frame is eligible
      * to dropping while congestion is detected for certain classes
      * of traffic.
      */
     public boolean dropEligible() { return dropEligible; }

     /**
      * VLAN Identifier (VID) specifies which VLAN this frame
      * belongs to.
      */
     public long vlanId() { return vlanId; }
     public EthernetFrame _root() { return _root; }
     public EthernetFrame _parent() { return _parent; }
 }
 private EtherTypeEnum etherType;

 /**
  * Ether type can be specied in several places in the frame. If
  * first location bears special marker (0x8100), then it is not the
  * real ether frame yet, an additional payload (`tci`) is expected
  * and real ether type is upcoming next.
  */
 public EtherTypeEnum etherType() {
     if (this.etherType != null)
         return this.etherType;
     this.etherType = (etherType1() == EtherTypeEnum.IEEE_802_1Q_TPID ? etherType2() : etherType1());
     return this.etherType;
 }
 private byte[] dstMac;
 private byte[] srcMac;
 private EtherTypeEnum etherType1;
 private TagControlInfo tci;
 private EtherTypeEnum etherType2;
 private Object body;
 private final EthernetFrame _root;
 private final KaitaiStruct _parent;
 private byte[] _raw_body;

 /**
  * Destination MAC address
  */
 public byte[] dstMac() { return dstMac; }

 /**
  * Source MAC address
  */
 public byte[] srcMac() { return srcMac; }

 /**
  * Either ether type or TPID if it is a IEEE 802.1Q frame
  */
 public EtherTypeEnum etherType1() { return etherType1; }
 public TagControlInfo tci() { return tci; }
 public EtherTypeEnum etherType2() { return etherType2; }
 public Object body() { return body; }
 public EthernetFrame _root() { return _root; }
 public KaitaiStruct _parent() { return _parent; }
 public byte[] _raw_body() { return _raw_body; }
}
