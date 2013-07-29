/**
 *  Gesture Test application for Invensense's MPU6/9xxx (w/ DMP).
 */

#include <unistd.h>
#include <dirent.h>
#include <fcntl.h>
#include <stdio.h>
#include <errno.h>
#include <sys/stat.h>
#include <stdlib.h>
#include <features.h>
#include <dirent.h>
#include <string.h>
#include <poll.h>
#include <stddef.h>
#include <linux/input.h>
#include <time.h>
#include <linux/time.h>
#include <unistd.h>
#include <termios.h>

#include "invensense.h"
#include "ml_math_func.h"
#include "storage_manager.h"
#include "ml_stored_data.h"
#include "ml_sysfs_helper.h"

#define DEBUG_PRINT     /* Uncomment to print Gyro & Accel read from Driver */

#define MAX_SYSFS_NAME_LEN  (100)
#define MAX_SYSFS_ATTRB (sizeof(struct sysfs_attrbs) / sizeof(char*))

#define FLICK_UPPER_THRES       3147790
#define FLICK_LOWER_THRES       -3147790
#define FLICK_COUNTER           50
#define POLL_TIME               2000 // 2sec

#define FALSE   0
#define TRUE    1

char *sysfs_names_ptr;

struct sysfs_attrbs {
    char *enable;
    char *power_state;
    char *dmp_on;
    char *dmp_int_on;
    char *self_test;
    char *dmp_firmware;
    char *firmware_loaded;
    char *display_orientation_on;
    char *orientation_on;
    char *event_flick;
    char *event_display_orientation;
    char *event_orientation;
    char *event_tap;
    char *flick_axis;
    char *flick_counter;
    char *flick_int_on;
    char *flick_lower;
    char *flick_upper;
    char *flick_message_on;
    char *tap_min_count;
    char *tap_on;
    char *tap_threshold;
    char *tap_time;
} mpu;

enum {
    tap,
    flick,
    gOrient,
    orient,
    numDMPFeatures
};

struct pollfd pfd[numDMPFeatures];

/*******************************************************************************
 *                       DMP Feature Supported Functions
 ******************************************************************************/

int read_sysfs_int(char *filename, int *var)
{
    int res=0;
    FILE *fp;

    fp = fopen(filename, "r");
    if (fp!=NULL) {
        fscanf(fp, "%d\n", var);
	fclose(fp);
    } else {
        MPL_LOGE("ERR open file to read");
        res= -1;
    }
    return res;
}

int write_sysfs_int(char *filename, int data)
{
    int res=0;
    FILE  *fp;

    fp = fopen(filename, "w");
    if (fp!=NULL) {
        fprintf(fp, "%d\n", data);
	fclose(fp);
    } else {
        MPL_LOGE("ERR open file to write");
        res= -1;
    }
    return res;
}

/**************************************************
    This _kbhit() function is courtesy from Web
***************************************************/
int _kbhit() {
    static const int STDIN = 0;
    static bool initialized = false;

    if (! initialized) {
        // Use termios to turn off line buffering
        struct termios term;
        tcgetattr(STDIN, &term);
        term.c_lflag &= ~ICANON;
        tcsetattr(STDIN, TCSANOW, &term);
        setbuf(stdin, NULL);
        initialized = true;
    }

    int bytesWaiting;
    ioctl(STDIN, FIONREAD, &bytesWaiting);
    return bytesWaiting;
}

int inv_init_sysfs_attributes(void)
{
    unsigned char i = 0;
    char sysfs_path[MAX_SYSFS_NAME_LEN];
    char *sptr;
    char **dptr;

    sysfs_names_ptr =
            (char*)malloc(sizeof(char[MAX_SYSFS_ATTRB][MAX_SYSFS_NAME_LEN]));
    sptr = sysfs_names_ptr;
    if (sptr != NULL) {
        dptr = (char**)&mpu;
        do {
            *dptr++ = sptr;
            sptr += sizeof(char[MAX_SYSFS_NAME_LEN]);
        } while (++i < MAX_SYSFS_ATTRB);
    } else {
        MPL_LOGE("couldn't alloc mem for sysfs paths");
        return -1;
    }

    // get proper (in absolute/relative) IIO path & build MPU's sysfs paths
    inv_get_sysfs_path(sysfs_path);

    sprintf(mpu.enable, "%s%s", sysfs_path, "/buffer/enable");
    sprintf(mpu.power_state, "%s%s", sysfs_path, "/power_state");
    sprintf(mpu.dmp_on,"%s%s", sysfs_path, "/dmp_on");
    sprintf(mpu.dmp_int_on, "%s%s", sysfs_path, "/dmp_int_on");
    sprintf(mpu.self_test, "%s%s", sysfs_path, "/self_test");
    sprintf(mpu.dmp_firmware, "%s%s", sysfs_path, "/dmp_firmware");
    sprintf(mpu.firmware_loaded, "%s%s", sysfs_path, "/firmware_loaded");
    sprintf(mpu.display_orientation_on, "%s%s", sysfs_path, "/display_orientation_on");
    sprintf(mpu.orientation_on, "%s%s", sysfs_path, "/orientation_on");
    sprintf(mpu.event_flick, "%s%s", sysfs_path, "/event_flick");
    sprintf(mpu.event_display_orientation, "%s%s", sysfs_path, "/event_display_orientation");
    sprintf(mpu.event_orientation, "%s%s", sysfs_path, "/event_orientation");
    sprintf(mpu.event_tap, "%s%s", sysfs_path, "/event_tap");
    sprintf(mpu.flick_axis, "%s%s", sysfs_path, "/flick_axis");
    sprintf(mpu.flick_counter, "%s%s", sysfs_path, "/flick_counter");
    sprintf(mpu.flick_int_on, "%s%s", sysfs_path, "/flick_int_on");
    sprintf(mpu.flick_lower, "%s%s", sysfs_path, "/flick_lower");
    sprintf(mpu.flick_upper, "%s%s", sysfs_path, "/flick_upper");
    sprintf(mpu.flick_message_on, "%s%s", sysfs_path, "/flick_message_on");
    sprintf(mpu.tap_min_count, "%s%s", sysfs_path, "/tap_min_count");
    sprintf(mpu.tap_on, "%s%s", sysfs_path, "/tap_on");
    sprintf(mpu.tap_threshold, "%s%s", sysfs_path, "/tap_threshold");
    sprintf(mpu.tap_time, "%s%s", sysfs_path, "/tap_time");

#if 0
    // test print sysfs paths
    dptr = (char**)&mpu;
    for (i = 0; i < MAX_SYSFS_ATTRB; i++) {
        MPL_LOGE("sysfs path: %s", *dptr++);
    }
#endif
    return 0;
}

int DmpFWloaded()
{
    int res;
    read_sysfs_int(mpu.firmware_loaded, &res);
    return res;
}

int enable_flick(int en)
{
   int res=0;
   int flickUpper=0, flickLower=0, flickCounter=0;

   if (write_sysfs_int(mpu.flick_int_on, en) < 0) {
       printf("GT:ERR-can't write 'flick_int_on'");
       res= -1;
   }

   if (en) {
       flickUpper= FLICK_UPPER_THRES;
       flickLower= FLICK_LOWER_THRES;
       flickCounter= FLICK_COUNTER;
   }

   if (write_sysfs_int(mpu.flick_upper, flickUpper) < 0) {
       printf("GT:ERR-can't write 'flick_upper'");
       res= -1;
   }

   if (write_sysfs_int(mpu.flick_lower, flickLower) < 0) {
       printf("GT:ERR-can't write 'flick_lower'");
       res= -1;
   }

   if (write_sysfs_int(mpu.flick_counter, flickCounter) < 0) {
       printf("GT:ERR-can't write 'flick_counter'");
       res= -1;
   }

   if (write_sysfs_int(mpu.flick_message_on, 0) < 0) {
       printf("GT:ERR-can't write 'flick_message_on'");
       res= -1;
   }

   if (write_sysfs_int(mpu.flick_axis, 0) < 0) {
       printf("GT:ERR_can't write 'flick_axis'");
       res= -1;
   }

   return res;
}

int enable_tap(int en)
{
    if (write_sysfs_int(mpu.tap_on, en) < 0) {
        printf("GT:ERR-can't write 'tap_on'");
        return -1;
    }

    return 0;
}

int enable_displ_orient(int en)
{
    if (write_sysfs_int(mpu.display_orientation_on, en) < 0) {
        printf("GT:ERR-can't write 'display_orientation_en'");
        return -1;
    }

    return 0;
}

int enable_orient(int en)
{
    if (write_sysfs_int(mpu.orientation_on, en) < 0) {
        printf("GT:ERR-can't write 'orientation_on'");
        return -1;
    }

    return 0;
}

int flickHandler()
{
    FILE *fp;
    int data;

#ifdef DEBUG_PRINT
    printf("GT:Flick Handler\n");
#endif

    fp = fopen(mpu.event_flick, "rt");
    fscanf(fp, "%d\n", &data);
    fclose (fp);

    printf("Flick= %x\n", data);

    return 0;
}

int tapHandler()
{
    FILE *fp;
    int tap, tap_dir, tap_num;

    fp = fopen(mpu.event_tap, "rt");
    fscanf(fp, "%d\n", &tap);
    fclose(fp);

    tap_dir = tap/8;
    tap_num = tap%8 + 1;

#ifdef DEBUG_PRINT
    printf("GT:Tap Handler **\n");
    printf("Tap= %x\n", tap);
    printf("Tap Dir= %x\n", tap_dir);
    printf("Tap Num= %x\n", tap_num);
#endif

    switch (tap_dir) {
        case 1:
            printf("Tap Axis->X Pos\n");
            break;
        case 2:
            printf("Tap Axis->X Neg\n");
            break;
        case 3:
            printf("Tap Axis->Y Pos\n");
            break;
        case 4:
            printf("Tap Axis->Y Neg\n");
            break;
        case 5:
            printf("Tap Axis->Z Pos\n");
            break;
        case 6:
            printf("Tap Axis->Z Neg\n");
            break;
        default:
            printf("Tap Axis->Unknown\n");
            break;
    }

    return 0;
}

int googleOrientHandler()
{
    FILE *fp;
    int orient;

#ifdef DEBUG_PRINT
    printf("GT:Google Orient Handler\n");
#endif

    fp = fopen(mpu.event_display_orientation, "rt");
    fscanf(fp, "%d\n", &orient);
    fclose(fp);

    printf("Google Orient-> %d\n", orient);

    return 0;
}

int orientHandler()
{
    FILE *fp;
    int orient;

    fp = fopen(mpu.event_orientation, "rt");
    fscanf(fp, "%d\n", &orient);
    fclose(fp);

#ifdef DEBUG_PRINT
    printf("GT:Reg Orient Handler\n");
#endif

    if (orient & 0x01)
	printf("Orient->X Up\n");

    if (orient & 0x02)
	printf("Orient->X Down\n");

    if (orient & 0x04)
	printf("Orient->Y Up\n");

    if (orient & 0x08)
	printf("Orient->Y Down\n");

    if (orient & 0x10)
	printf("Orient->Z Up\n");

    if (orient & 0x20)
	printf("Orient->Z Down\n");

    if (orient & 0x40)
	printf("Orient->Flip\n");

    return 0;
}

int enableDMPFeatures(int en)
{
    int res= -1;

    if (DmpFWloaded())
    {
        /* Currently there's no info regarding DMP's supported features/capabilities */
        /* An error in enabling features below could be an indication of the feature */
        /* not supported in current loaded DMP firmware */

        enable_flick(en);
        enable_tap(en);
        enable_displ_orient(en);
        enable_orient(en);
        res= 0;
    }

    return res;
}

int initFds()
{
    int i;

    for (i=0; i< numDMPFeatures; i++) {
        switch(i) {
            case tap:
                pfd[i].fd = open(mpu.event_tap, O_RDONLY | O_NONBLOCK);
                break;

            case flick:
                pfd[i].fd = open(mpu.event_flick, O_RDONLY | O_NONBLOCK);
                break;

            case gOrient:
                pfd[i].fd = open(mpu.event_display_orientation, O_RDONLY | O_NONBLOCK);
                break;

            case orient:
                pfd[i].fd = open(mpu.event_orientation, O_RDONLY | O_NONBLOCK);
                break;

            default:
                pfd[i].fd = -1;
         }

        pfd[i].events = POLLPRI|POLLERR,
        pfd[i].revents = 0;
#ifdef DEBUG_PRINT
        printf("GT:pfd[%d].fd= %d\n", i, pfd[i].fd);
#endif
    }

    return 0;
}

int closeFds()
{
    int i;
    for (i = 0; i < numDMPFeatures; i++) {
        if (!pfd[i].fd)
            close(pfd[i].fd);
    }
    return 0;
}

/*******************************************************************************
 *                       M a i n  S e l f  T e s t
 ******************************************************************************/

int main(int argc, char **argv)
{
    char data[4];
    int i, res= 0;

    res = inv_init_sysfs_attributes();
    if (res) {
        printf("GT:ERR-Can't allocate mem");
        return -1;
    }

    /* On Gesture/DMP supported features */
    enableDMPFeatures(1);

    /* init Fds to poll for Gesture data */
    initFds();

    /* prompt user to make gesture and how to stop program */
    printf("\n**Please make Gesture to see data.  Press any key to stop Prog**\n\n");

    do {
        for (i=0; i< numDMPFeatures; i++) {
            read(pfd[i].fd, data, 4);
        }

        poll(pfd, numDMPFeatures, POLL_TIME);

        for (i=0; i< numDMPFeatures; i++) {
           if(pfd[i].revents != 0) {
               switch(i) {
                   case tap:
                       tapHandler();
                       break;

                   case flick:
                       flickHandler();
                       break;

                   case gOrient:
                       googleOrientHandler();
                       break;

                   case orient:
                       orientHandler();
                       break;

                   default:
                       printf("GT:ERR-Not supported");
                       break;
               }
               pfd[i].revents= 0;	//no need. reset anyway
           }
        }

    } while (!_kbhit());

    /* Off DMP features */
    enableDMPFeatures(0);

    /* release resources */
    closeFds();
    if (sysfs_names_ptr) {
        free(sysfs_names_ptr);
    }

    printf("\nThank You!\n");

    return res;
}

