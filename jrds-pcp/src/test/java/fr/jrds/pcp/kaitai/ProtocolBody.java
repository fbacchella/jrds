package fr.jrds.pcp.kaitai;

//This is a generated file! Please edit source .ksy file and use kaitai-struct-compiler to rebuild

import io.kaitai.struct.ByteBufferKaitaiStream;
import io.kaitai.struct.KaitaiStruct;
import io.kaitai.struct.KaitaiStream;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;


/**
* Protocol body represents particular payload on transport level (OSI
* layer 4).
* 
* Typically this payload in encapsulated into network level (OSI layer
* 3) packet, which includes "protocol number" field that would be used
* to decide what's inside the payload and how to parse it. Thanks to
* IANA's standardization effort, multiple network level use the same
* IDs for these payloads named "protocol numbers".
* 
* This is effectively a "router" type: it expects to get protocol
* number as a parameter, and then invokes relevant type parser based
* on that parameter.
* @see <a href="http://www.iana.org/assignments/protocol-numbers/protocol-numbers.xhtml">Source</a>
*/
public class ProtocolBody extends KaitaiStruct {

 public enum ProtocolEnum {
     HOPOPT(0),
     ICMP(1),
     IGMP(2),
     GGP(3),
     IPV4(4),
     ST(5),
     TCP(6),
     CBT(7),
     EGP(8),
     IGP(9),
     BBN_RCC_MON(10),
     NVP_II(11),
     PUP(12),
     ARGUS(13),
     EMCON(14),
     XNET(15),
     CHAOS(16),
     UDP(17),
     MUX(18),
     DCN_MEAS(19),
     HMP(20),
     PRM(21),
     XNS_IDP(22),
     TRUNK_1(23),
     TRUNK_2(24),
     LEAF_1(25),
     LEAF_2(26),
     RDP(27),
     IRTP(28),
     ISO_TP4(29),
     NETBLT(30),
     MFE_NSP(31),
     MERIT_INP(32),
     DCCP(33),
     X_3PC(34),
     IDPR(35),
     XTP(36),
     DDP(37),
     IDPR_CMTP(38),
     TP_PLUS_PLUS(39),
     IL(40),
     IPV6(41),
     SDRP(42),
     IPV6_ROUTE(43),
     IPV6_FRAG(44),
     IDRP(45),
     RSVP(46),
     GRE(47),
     DSR(48),
     BNA(49),
     ESP(50),
     AH(51),
     I_NLSP(52),
     SWIPE(53),
     NARP(54),
     MOBILE(55),
     TLSP(56),
     SKIP(57),
     IPV6_ICMP(58),
     IPV6_NONXT(59),
     IPV6_OPTS(60),
     ANY_HOST_INTERNAL_PROTOCOL(61),
     CFTP(62),
     ANY_LOCAL_NETWORK(63),
     SAT_EXPAK(64),
     KRYPTOLAN(65),
     RVD(66),
     IPPC(67),
     ANY_DISTRIBUTED_FILE_SYSTEM(68),
     SAT_MON(69),
     VISA(70),
     IPCV(71),
     CPNX(72),
     CPHB(73),
     WSN(74),
     PVP(75),
     BR_SAT_MON(76),
     SUN_ND(77),
     WB_MON(78),
     WB_EXPAK(79),
     ISO_IP(80),
     VMTP(81),
     SECURE_VMTP(82),
     VINES(83),
     TTP_OR_IPTM(84),
     NSFNET_IGP(85),
     DGP(86),
     TCF(87),
     EIGRP(88),
     OSPFIGP(89),
     SPRITE_RPC(90),
     LARP(91),
     MTP(92),
     AX_25(93),
     IPIP(94),
     MICP(95),
     SCC_SP(96),
     ETHERIP(97),
     ENCAP(98),
     ANY_PRIVATE_ENCRYPTION_SCHEME(99),
     GMTP(100),
     IFMP(101),
     PNNI(102),
     PIM(103),
     ARIS(104),
     SCPS(105),
     QNX(106),
     A_N(107),
     IPCOMP(108),
     SNP(109),
     COMPAQ_PEER(110),
     IPX_IN_IP(111),
     VRRP(112),
     PGM(113),
     ANY_0_HOP(114),
     L2TP(115),
     DDX(116),
     IATP(117),
     STP(118),
     SRP(119),
     UTI(120),
     SMP(121),
     SM(122),
     PTP(123),
     ISIS_OVER_IPV4(124),
     FIRE(125),
     CRTP(126),
     CRUDP(127),
     SSCOPMCE(128),
     IPLT(129),
     SPS(130),
     PIPE(131),
     SCTP(132),
     FC(133),
     RSVP_E2E_IGNORE(134),
     MOBILITY_HEADER(135),
     UDPLITE(136),
     MPLS_IN_IP(137),
     MANET(138),
     HIP(139),
     SHIM6(140),
     WESP(141),
     ROHC(142),
     RESERVED_255(255);

     private final long id;
     ProtocolEnum(long id) { this.id = id; }
     public long id() { return id; }
     private static final Map<Long, ProtocolEnum> byId = new HashMap<>(144);
     static {
         for (ProtocolEnum e : ProtocolEnum.values())
             byId.put(e.id(), e);
     }
     public static ProtocolEnum byId(long id) { return byId.get(id); }
 }

 public ProtocolBody(KaitaiStream _io, int protocolNum) {
     this(_io, null, null, protocolNum);
 }

 public ProtocolBody(KaitaiStream _io, KaitaiStruct _parent, int protocolNum) {
     this(_io, _parent, null, protocolNum);
 }

 public ProtocolBody(KaitaiStream _io, KaitaiStruct _parent, ProtocolBody _root, int protocolNum) {
     super(_io);
     this._parent = _parent;
     this._root = _root == null ? this : _root;
     this.protocolNum = protocolNum;
     _read();
 }
 private void _read() {
     switch (protocol()) {
     case TCP: {
         this.body = new TcpSegment(this._io);
         break;
     }
     case IPV6_NONXT: {
         this.body = new NoNextHeader(this._io, this, _root);
         break;
     }
     case HOPOPT: {
         this.body = new OptionHopByHop(this._io, this, _root);
         break;
     }
     case IPV6: {
         this.body = new Ipv6Packet(this._io);
         break;
     }
     case IPV4: {
         this.body = new Ipv4Packet(this._io);
         break;
     }
     default:
         throw new IllegalArgumentException(protocol().toString());
     }
 }

 /**
  * Dummy type for IPv6 "no next header" type, which signifies end of headers chain.
  */
 public static class NoNextHeader extends KaitaiStruct {
     public static NoNextHeader fromFile(String fileName) throws IOException {
         return new NoNextHeader(new ByteBufferKaitaiStream(fileName));
     }

     public NoNextHeader(KaitaiStream _io) {
         this(_io, null, null);
     }

     public NoNextHeader(KaitaiStream _io, ProtocolBody _parent) {
         this(_io, _parent, null);
     }

     public NoNextHeader(KaitaiStream _io, ProtocolBody _parent, ProtocolBody _root) {
         super(_io);
         this._parent = _parent;
         this._root = _root;
         _read();
     }
     private void _read() {
     }
     private final ProtocolBody _root;
     private final ProtocolBody _parent;
     public ProtocolBody _root() { return _root; }
     public ProtocolBody _parent() { return _parent; }
 }
 public static class OptionHopByHop extends KaitaiStruct {
     public static OptionHopByHop fromFile(String fileName) throws IOException {
         return new OptionHopByHop(new ByteBufferKaitaiStream(fileName));
     }

     public OptionHopByHop(KaitaiStream _io) {
         this(_io, null, null);
     }

     public OptionHopByHop(KaitaiStream _io, ProtocolBody _parent) {
         this(_io, _parent, null);
     }

     public OptionHopByHop(KaitaiStream _io, ProtocolBody _parent, ProtocolBody _root) {
         super(_io);
         this._parent = _parent;
         this._root = _root;
         _read();
     }
     private void _read() {
         this.nextHeaderType = this._io.readU1();
         this.hdrExtLen = this._io.readU1();
         this.body = this._io.readBytes((hdrExtLen() - 1));
         this.nextHeader = new ProtocolBody(this._io, nextHeaderType());
     }
     private int nextHeaderType;
     private int hdrExtLen;
     private byte[] body;
     private ProtocolBody nextHeader;
     private final ProtocolBody _root;
     private final ProtocolBody _parent;
     public int nextHeaderType() { return nextHeaderType; }
     public int hdrExtLen() { return hdrExtLen; }
     public byte[] body() { return body; }
     public ProtocolBody nextHeader() { return nextHeader; }
     public ProtocolBody _root() { return _root; }
     public ProtocolBody _parent() { return _parent; }
 }
 private ProtocolEnum protocol;
 public ProtocolEnum protocol() {
     if (this.protocol != null)
         return this.protocol;
     this.protocol = ProtocolEnum.byId(protocolNum());
     return this.protocol;
 }
 private KaitaiStruct body;
 private final int protocolNum;
 private final ProtocolBody _root;
 private final KaitaiStruct _parent;
 public KaitaiStruct body() { return body; }

 /**
  * Protocol number as an integer.
  */
 public int protocolNum() { return protocolNum; }
 public ProtocolBody _root() { return _root; }
 public KaitaiStruct _parent() { return _parent; }
}
