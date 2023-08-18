package fr.jrds.pcp.kaitai;

//This is a generated file! Please edit source .ksy file and use kaitai-struct-compiler to rebuild

import io.kaitai.struct.ByteBufferKaitaiStream;
import io.kaitai.struct.KaitaiStruct;
import io.kaitai.struct.KaitaiStream;
import java.io.IOException;


/**
* TCP is one of the core Internet protocols on transport layer (AKA
* OSI layer 4), providing stateful connections with error checking,
* guarantees of delivery, order of segments and avoidance of duplicate
* delivery.
*/
public class TcpSegment extends KaitaiStruct {
 public static TcpSegment fromFile(String fileName) throws IOException {
     return new TcpSegment(new ByteBufferKaitaiStream(fileName));
 }

 public TcpSegment(KaitaiStream _io) {
     this(_io, null, null);
 }

 public TcpSegment(KaitaiStream _io, KaitaiStruct _parent) {
     this(_io, _parent, null);
 }

 public TcpSegment(KaitaiStream _io, KaitaiStruct _parent, TcpSegment _root) {
     super(_io);
     this._parent = _parent;
     this._root = _root == null ? this : _root;
     _read();
 }
 private void _read() {
     this.srcPort = this._io.readU2be();
     this.dstPort = this._io.readU2be();
     this.seqNum = this._io.readU4be();
     this.ackNum = this._io.readU4be();
     this.b12 = this._io.readU1();
     this.b13 = this._io.readU1();
     this.windowSize = this._io.readU2be();
     this.checksum = this._io.readU2be();
     this.urgentPointer = this._io.readU2be();
     this.body = this._io.readBytesFull();
 }
 private int srcPort;
 private int dstPort;
 private long seqNum;
 private long ackNum;
 private int b12;
 private int b13;
 private int windowSize;
 private int checksum;
 private int urgentPointer;
 private byte[] body;
 private final TcpSegment _root;
 private final KaitaiStruct _parent;
 public int srcPort() { return srcPort; }
 public int dstPort() { return dstPort; }
 public long seqNum() { return seqNum; }
 public long ackNum() { return ackNum; }
 public int b12() { return b12; }
 public int b13() { return b13; }
 public int windowSize() { return windowSize; }
 public int checksum() { return checksum; }
 public int urgentPointer() { return urgentPointer; }
 public byte[] body() { return body; }
 public TcpSegment _root() { return _root; }
 public KaitaiStruct _parent() { return _parent; }
}
