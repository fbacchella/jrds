package fr.jrds.pcp.kaitai;

//This is a generated file! Please edit source .ksy file and use kaitai-struct-compiler to rebuild

import io.kaitai.struct.ByteBufferKaitaiStream;
import io.kaitai.struct.KaitaiStruct;
import io.kaitai.struct.KaitaiStream;
import java.io.IOException;
import java.util.ArrayList;

public class Ipv4Packet extends KaitaiStruct {
 public static Ipv4Packet fromFile(String fileName) throws IOException {
     return new Ipv4Packet(new ByteBufferKaitaiStream(fileName));
 }

 public Ipv4Packet(KaitaiStream _io) {
     this(_io, null, null);
 }

 public Ipv4Packet(KaitaiStream _io, KaitaiStruct _parent) {
     this(_io, _parent, null);
 }

 public Ipv4Packet(KaitaiStream _io, KaitaiStruct _parent, Ipv4Packet _root) {
     super(_io);
     this._parent = _parent;
     this._root = _root == null ? this : _root;
     _read();
 }
 private void _read() {
     this.b1 = this._io.readU1();
     this.b2 = this._io.readU1();
     this.totalLength = this._io.readU2be();
     this.identification = this._io.readU2be();
     this.b67 = this._io.readU2be();
     this.ttl = this._io.readU1();
     this.protocol = this._io.readU1();
     this.headerChecksum = this._io.readU2be();
     this.srcIpAddr = this._io.readBytes(4);
     this.dstIpAddr = this._io.readBytes(4);
     this._raw_options = this._io.readBytes((ihlBytes() - 20));
     KaitaiStream _io__raw_options = new ByteBufferKaitaiStream(_raw_options);
     this.options = new Ipv4Options(_io__raw_options, this, _root);
     this._raw_body = this._io.readBytes((totalLength() - ihlBytes()));
     KaitaiStream _io__raw_body = new ByteBufferKaitaiStream(_raw_body);
     this.body = new ProtocolBody(_io__raw_body, protocol());
 }
 public static class Ipv4Options extends KaitaiStruct {
     public static Ipv4Options fromFile(String fileName) throws IOException {
         return new Ipv4Options(new ByteBufferKaitaiStream(fileName));
     }

     public Ipv4Options(KaitaiStream _io) {
         this(_io, null, null);
     }

     public Ipv4Options(KaitaiStream _io, Ipv4Packet _parent) {
         this(_io, _parent, null);
     }

     public Ipv4Options(KaitaiStream _io, Ipv4Packet _parent, Ipv4Packet _root) {
         super(_io);
         this._parent = _parent;
         this._root = _root;
         _read();
     }
     private void _read() {
         this.entries = new ArrayList<>();
         while (!this._io.isEof()) {
             this.entries.add(new Ipv4Option(this._io, this, _root));
         }
     }
     private ArrayList<Ipv4Option> entries;
     private Ipv4Packet _root;
     private Ipv4Packet _parent;
     public ArrayList<Ipv4Option> entries() { return entries; }
     public Ipv4Packet _root() { return _root; }
     public Ipv4Packet _parent() { return _parent; }
 }
 public static class Ipv4Option extends KaitaiStruct {
     public static Ipv4Option fromFile(String fileName) throws IOException {
         return new Ipv4Option(new ByteBufferKaitaiStream(fileName));
     }

     public Ipv4Option(KaitaiStream _io) {
         this(_io, null, null);
     }

     public Ipv4Option(KaitaiStream _io, Ipv4Packet.Ipv4Options _parent) {
         this(_io, _parent, null);
     }

     public Ipv4Option(KaitaiStream _io, Ipv4Packet.Ipv4Options _parent, Ipv4Packet _root) {
         super(_io);
         this._parent = _parent;
         this._root = _root;
         _read();
     }
     private void _read() {
         this.b1 = this._io.readU1();
         this.len = this._io.readU1();
         this.body = this._io.readBytes((len() > 2 ? (len() - 2) : 0));
     }
     private Integer copy;
     public Integer copy() {
         if (this.copy != null)
             return this.copy;
         int _tmp = (int) (((b1() & 128) >> 7));
         this.copy = _tmp;
         return this.copy;
     }
     private Integer optClass;
     public Integer optClass() {
         if (this.optClass != null)
             return this.optClass;
         int _tmp = (int) (((b1() & 96) >> 5));
         this.optClass = _tmp;
         return this.optClass;
     }
     private Integer number;
     public Integer number() {
         if (this.number != null)
             return this.number;
         int _tmp = (int) ((b1() & 31));
         this.number = _tmp;
         return this.number;
     }
     private int b1;
     private int len;
     private byte[] body;
     private Ipv4Packet _root;
     private Ipv4Packet.Ipv4Options _parent;
     public int b1() { return b1; }
     public int len() { return len; }
     public byte[] body() { return body; }
     public Ipv4Packet _root() { return _root; }
     public Ipv4Packet.Ipv4Options _parent() { return _parent; }
 }
 private Integer version;
 public Integer version() {
     if (this.version != null)
         return this.version;
     int _tmp = (int) (((b1() & 240) >> 4));
     this.version = _tmp;
     return this.version;
 }
 private Integer ihl;
 public Integer ihl() {
     if (this.ihl != null)
         return this.ihl;
     int _tmp = (int) ((b1() & 15));
     this.ihl = _tmp;
     return this.ihl;
 }
 private Integer ihlBytes;
 public Integer ihlBytes() {
     if (this.ihlBytes != null)
         return this.ihlBytes;
     int _tmp = (int) ((ihl() * 4));
     this.ihlBytes = _tmp;
     return this.ihlBytes;
 }
 private int b1;
 private int b2;
 private int totalLength;
 private int identification;
 private int b67;
 private int ttl;
 private int protocol;
 private int headerChecksum;
 private byte[] srcIpAddr;
 private byte[] dstIpAddr;
 private Ipv4Options options;
 private ProtocolBody body;
 private Ipv4Packet _root;
 private KaitaiStruct _parent;
 private byte[] _raw_options;
 private byte[] _raw_body;
 public int b1() { return b1; }
 public int b2() { return b2; }
 public int totalLength() { return totalLength; }
 public int identification() { return identification; }
 public int b67() { return b67; }
 public int ttl() { return ttl; }
 public int protocol() { return protocol; }
 public int headerChecksum() { return headerChecksum; }
 public byte[] srcIpAddr() { return srcIpAddr; }
 public byte[] dstIpAddr() { return dstIpAddr; }
 public Ipv4Options options() { return options; }
 public ProtocolBody body() { return body; }
 public Ipv4Packet _root() { return _root; }
 public KaitaiStruct _parent() { return _parent; }
 public byte[] _raw_options() { return _raw_options; }
 public byte[] _raw_body() { return _raw_body; }
}