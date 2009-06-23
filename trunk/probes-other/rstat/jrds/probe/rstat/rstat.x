





/*
 * Gather statistics on remote machines
 */


























































const RSTAT_CPUSTATES = 4;
const RSTAT_DK_NDRIVE = 4;

/*
 * the cpu stat values
 */

const RSTAT_CPU_USER = 0;
const RSTAT_CPU_NICE = 1;
const RSTAT_CPU_SYS = 2;
const RSTAT_CPU_IDLE = 3;

/*
 * GMT since 0:00, January 1, 1970
 */
struct rstat_timeval {
	int tv_sec;	/* seconds */
	int tv_usec;	/* and microseconds */
};

struct statsvar {				/* RSTATVERS_VAR */
	int cp_time<>; 		/* variable number of CPU states */
	int dk_xfer<>;		/* variable number of disks */
	unsigned v_pgpgin;	/* these are cumulative sum */
	unsigned v_pgpgout;
	unsigned v_pswpin;
	unsigned v_pswpout;
	unsigned v_intr;
	int if_ipackets;
	int if_ierrors;
	int if_opackets;
	int if_oerrors;
	int if_collisions;
	unsigned v_swtch;
	int avenrun[3];
	rstat_timeval boottime;
	rstat_timeval curtime;
};

struct statstime {				/* RSTATVERS_TIME */
	int cp_time[RSTAT_CPUSTATES];
	int dk_xfer[RSTAT_DK_NDRIVE];
	unsigned int v_pgpgin;	/* these are cumulative sum */
	unsigned int v_pgpgout;
	unsigned int v_pswpin;
	unsigned int v_pswpout;
	unsigned int v_intr;
	int if_ipackets;
	int if_ierrors;
	int if_oerrors;
	int if_collisions;
	unsigned int v_swtch;
	int avenrun[3];
	rstat_timeval boottime;
	rstat_timeval curtime;
	int if_opackets;
};

program RSTATPROG {
        /*
         * Version 4 allows for variable number of disk and RSTAT_CPU states.
         */
	version RSTATVERS_VAR {
		statsvar
		RSTATPROC_STATS (void) = 1;
		unsigned int
		RSTATPROC_HAVEDISK (void) = 2;
	} = 4;
	/*
	 * Newest version includes current time and context switching info
	 */
	version RSTATVERS_TIME {
		statstime
		RSTATPROC_STATS(void) = 1;
		unsigned int
		RSTATPROC_HAVEDISK(void) = 2;
	} = 3;
} = 100001;












