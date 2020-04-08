package fr.jrds.pcp;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum ERROR {

    GENERIC          ("Generic error, already reported above"),
    PMNS             ("Problems parsing PMNS definitions"),
    NOPMNS           ("PMNS not accessible"),
    DUPPMNS          ("Attempt to reload the PMNS"),
    TEXT             ("Oneline or help text is not available"),
    APPVERSION       ("Metric not supported by this version of monitored application"),
    VALUE            ("Missing metric value(s)"),
    @Deprecated
    LICENSE          ("Current PCP license does not permit this operation"),
    TIMEOUT          ("Timeout waiting for a response from PMCD"),
    NODATA           ("Empty archive log file"),
    RESET            ("pmcd reset or configuration changed"),
    @Deprecated
    FILE             ("Cannot locate a file"),
    NAME             ("Unknown metric name"),
    PMID             ("Unknown or illegal metric identifier"),
    INDOM            ("Unknown or illegal instance domain identifier"),
    INST             ("Unknown or illegal instance identifier"),
    UNIT             ("Illegal pmUnits specification"),
    CONV             ("Impossible value or scale conversion"),
    TRUNC            ("Truncation in value conversion"),
    SIGN             ("Negative value in conversion to unsigned"),
    PROFILE          ("Explicit instance identifier(s) required"),
    IPC              ("IPC protocol failure"),
    @Deprecated
    PM_ERR_NOASCII   ("ASCII format not supported for this PDU"),
    EOF              ("IPC channel closed"),
    NOTHOST          ("Operation requires context with host source of metric"),
    EOL              ("End of PCP archive log"),
    MODE             ("Illegal mode specification"),
    LABEL            ("Illegal label record at start of a PCP archive log file"),
    LOGREC           ("Corrupted record in a PCP archive log"),
    NOTARCHIVE       ("Operation requires context with archive source of metrics"),
    LOGFILE          ("Missing archive file"),
    NOCONTEXT        ("Attempt to use an illegal context"),
    PROFILESPEC      ("NULL pmInDom with non-NULL instlist"),
    PMID_LOG         ("Metric not defined in the PCP archive log"),
    INDOM_LOG        ("Instance domain identifier not defined in the PCP archive log"),
    INST_LOG         ("Instance identifier not defined in the PCP archive log"),
    NOPROFILE        ("Missing profile - protocol botch"),
    NOAGENT          ("No pmcd agent for domain of request"),
    PERMISSION       ("No permission to perform requested operation"),
    CONNLIMIT        ("PMCD connection limit for this host exceeded"),
    AGAIN            ("Try again. Info not currently available"),
    ISCONN           ("Already connected"),
    NOTCONN          ("Not connected"),
    NEEDPORT         ("Port name required"),
    @Deprecated
    PM_ERR_WANTACK   ("Can not send due to pending acks"),
    NONLEAF          ("PMNS node is not a leaf node"),
    OBJSTYLE         ("User/kernel object style mismatch"),
    PMCDLICENSE      ("PMCD is not licensed to accept connections"),
    TYPE             ("Unknown or illegal metric type"),
    THREAD           ("Operation not supported for multi-threaded applications"),
    NOCONTAINER      ("Container not found"),
    BADSTORE         ("Bad input to pmstore"),
    LOGOVERLAP       ("Archives overlap in time"),
    LOGHOST          ("Archives differ by host"),
    LOGTIMEZONE      ("Archives differ in time zone"),
    LOGCHANGETYPE    ("The type of a metric has changed in an archive"),
    LOGCHANGESEM     ("The semantics of a metric has changed in an archive"),
    LOGCHANGEINDOM   ("The instance domain of a metric has changed in an archive"),
    LOGCHANGEUNITS   ("The units of a metric have changed in an archive"),
    NEEDCLIENTCERT   ("PMCD requires a client certificate"),
    BADDERIVE        ("Derived metric definition failed"),
    NOLABELS         ("No support for label metadata"),
    @Deprecated
    CTXBUSY          ("Context is busy", 97),
    TOOSMALL         ("Insufficient elements in list", 98),
    TOOBIG           ("Result size exceeded", 99),
    FAULT            ("QA fault injected", 100), 
    PMDAREADY        ("Now ready to respond", 1048),
    PMDANOTREADY     ("Not yet ready to respond", 1049),
    NYI              ("Functionality not yet implemented [end-of-range mark]", 8999),
    ;

    static final int BASE = 12345;

    private final int errnum;
    private final String message;

    ERROR(String message) {
        this.errnum = -BASE-ordinal();
        this.message = message;
    }
    ERROR(String message, int errnum) {
        this.errnum = -BASE-errnum;
        this.message = message;
    }

    public int getErrnum() {
        return errnum;
    }

    public String getMessage() {
        return message;
    }

    public final static Map<Integer, ERROR> errors;
    static {
        Map<Integer, ERROR> _errors = new HashMap<>(ERROR.values().length);
        Arrays.stream(ERROR.values()).forEach(e -> _errors.put(e.errnum, e));
        errors = Collections.unmodifiableMap(_errors);
    }

}
