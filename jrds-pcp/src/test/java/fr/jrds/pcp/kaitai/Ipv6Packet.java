package fr.jrds.pcp.kaitai;

//This is a generated file! Please edit source .ksy file and use kaitai-struct-compiler to rebuild

import io.kaitai.struct.ByteBufferKaitaiStream;
import io.kaitai.struct.KaitaiStruct;
import io.kaitai.struct.KaitaiStream;
import java.io.IOException;

public class Ipv6Packet extends KaitaiStruct {
 public static Ipv6Packet fromFile(String fileName) throws IOException {
     return new Ipv6Packet(new ByteBufferKaitaiStream(fileName));
 }

 public Ipv6Packet(KaitaiStream _io) {
     this(_io, null, null);
 }

 public Ipv6Packet(KaitaiStream _io, KaitaiStruct _parent) {
     this(_io, _parent, null);
 }

 public Ipv6Packet(KaitaiStream _io, KaitaiStruct _parent, Ipv6Packet _root) {
     super(_io);
     this._parent = _parent;
     this._root = _root == null ? this : _root;
     _read();
 }
 private void _read() {
     this.version = this._io.readBitsInt(4);
     this.trafficClass = this._io.readBitsInt(8);
     this.flowLabel = this._io.readBitsInt(20);
     this._io.alignToByte();
     this.payloadLength = this._io.readU2be();
     this.nextHeaderType = this._io.readU1();
     this.hopLimit = this._io.readU1();
     this.srcIpv6Addr = this._io.readBytes(16);
     this.dstIpv6Addr = this._io.readBytes(16);
     this.nextHeader = new ProtocolBody(this._io, nextHeaderType());
     this.rest = this._io.readBytesFull();
 }
 private long version;
 private long trafficClass;
 private long flowLabel;
 private int payloadLength;
 private int nextHeaderType;
 private int hopLimit;
 private byte[] srcIpv6Addr;
 private byte[] dstIpv6Addr;
 private ProtocolBody nextHeader;
 private byte[] rest;
 private final Ipv6Packet _root;
 private final KaitaiStruct _parent;
 public long version() { return version; }
 public long trafficClass() { return trafficClass; }
 public long flowLabel() { return flowLabel; }
 public int payloadLength() { return payloadLength; }
 public int nextHeaderType() { return nextHeaderType; }
 public int hopLimit() { return hopLimit; }
 public byte[] srcIpv6Addr() { return srcIpv6Addr; }
 public byte[] dstIpv6Addr() { return dstIpv6Addr; }
 public ProtocolBody nextHeader() { return nextHeader; }
 public byte[] rest() { return rest; }
 public Ipv6Packet _root() { return _root; }
 public KaitaiStruct _parent() { return _parent; }
}
