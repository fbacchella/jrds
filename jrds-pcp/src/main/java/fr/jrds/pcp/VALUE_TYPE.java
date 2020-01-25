package fr.jrds.pcp;

public enum VALUE_TYPE {
    I32,                /* 32-bit signed integer */
    U32,                /* 32-bit unsigned integer */
    I64,                /* 64-bit signed integer */
    U64,                /* 64-bit unsigned integer */
    FLOAT,              /* 32-bit floating point */
    DOUBLE,             /* 64-bit floating point */
    STRING,             /* array of char */
    AGGREGATE,          /* arbitrary binary data (aggregate) */
    _AGGREGATE_STATIC,  /* static pointer to aggregate */
    EVENT,              /* packed pmEventArray */
    HIGHRES_EVENT,      /* packed pmHighResEventArray */
}
