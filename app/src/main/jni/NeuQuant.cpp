//
// Created by sujin.kim on 2018. 8. 25..
//
#include <com_cinemagra_holymoly_cinemagraph_AnimatedGifEncoder.h>
#include <jni.h>
typedef unsigned char BYTE;

/*
 * NeuQuant Neural-Net Quantization Algorithm
 * ------------------------------------------
 *
 * Copyright (c) 1994 Anthony Dekker
 *
 * NEUQUANT Neural-Net quantization algorithm by Anthony Dekker, 1994. See
 * "Kohonen neural networks for optimal colour quantization" in "Network:
 * Computation in Neural Systems" Vol. 5 (1994) pp 351-367. for a discussion of
 * the algorithm.
 *
 * Any party obtaining a copy of these files from the author, directly or
 * indirectly, is granted, free of charge, a full and unrestricted irrevocable,
 * world-wide, paid up, royalty-free, nonexclusive right and license to deal in
 * this software and documentation files (the "Software"), including without
 * limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons who
 * receive copies from any such party to do so, with the only requirement being
 * that this copyright notice remain intact.
 */

//	 Ported to Java 12/00 K Weiner


class NeuQuant {
private:
    static const int netsize = 256;
    static const int prime1 = 499;
    static const int prime2 = 491;
    static const int prime3 = 487;
    static const int prime4 = 503;
    static const int minpicturebytes = (3 * prime4);
    /* minimum size for input image */

    /*
     * Program Skeleton ---------------- [select samplefac in range 1..30] [read
     * image from input file] pic = (unsigned char*) malloc(3*width*height);
     * initnet(pic,3*width*height,samplefac); learn(); unbiasnet(); [write output
     * image header, using writecolourmap(f)] inxbuild(); write output image using
     * inxsearch(b,g,r)
     */

    /*
     * Network Definitions -------------------
     */
    static const int maxnetpos = 255;
    static const int netbiasshift = 4; /* bias for colour values */
    static const int ncycles = 100; /* no. of learning cycles */
    static const int initrad = (netsize >> 3);
    static const int radiusbiasshift = 6; /* at 32.0 biased by 6 bits */
    static const int radiusbias = (((int) 1) << radiusbiasshift);
    static const int initradius = (initrad * radiusbias);
    static const int radiusdec = 30; /* factor of 1/30 each cycle */
    static const int alphabiasshift = 10; /* alpha starts at 1.0 */
    static const int initalpha = (((int) 1) << alphabiasshift);
    static const int radbiasshift = 8;
    static const int radbias = (((int) 1) << radbiasshift);
    static const const int alpharadbshift = (alphabiasshift + radbiasshift);
    static const int alpharadbias = (((int) 1) << alpharadbshift);

    int netindex[256];
    int radpower[initrad];
    int alphadec; /* biased by 10 bits */



public :
    int **network; /* the network itself - [netsize][4] */
    BYTE *thepicture; /* the input image itself */
    int lengthcount; /* lengthcount = H*W*3 */
    int samplefac; /* sampling factor 1..30 */
    int bias[netsize];
    int freq[netsize];
    static const int betashift = 10;
    static const int gammashift = 10; /* gamma = 1024 */
    static const int intbiasshift = 16; /* bias for fractions */
    static const int intbias = (((int) 1) << intbiasshift);
    static const int beta = (intbias >> betashift); /* beta = 1/1024 */
    static const int betagamma = (intbias << (gammashift - betashift));

    NeuQuant() {

    };

    void inxbuild() {


        int i, j, smallpos, smallval;
        int *p;
        int *q;
        int previouscol, startpos;
        previouscol = 0;
        startpos = 0;

        for (i = 0; i < netsize; i++) {

            p = network[i];

            smallpos = i;

            smallval = p[1]; /* index on g */

            /* find smallest in i..netsize-1 */

            for (j = i + 1; j < netsize; j++) {

                q = network[j];

                if (q[1] < smallval) { /* index on g */

                    smallpos = j;

                    smallval = q[1]; /* index on g */

                }

            }

            q = network[smallpos];

            /* swap p (i) and q (smallpos) entries */

            if (i != smallpos) {

                j = q[0];

                q[0] = p[0];

                p[0] = j;

                j = q[1];

                q[1] = p[1];

                p[1] = j;

                j = q[2];

                q[2] = p[2];

                p[2] = j;

                j = q[3];

                q[3] = p[3];

                p[3] = j;

            }

            /* smallval entry is now in position i */

            if (smallval != previouscol) {

                netindex[previouscol] = (startpos + i) >> 1;

                for (j = previouscol + 1; j < smallval; j++)

                    netindex[j] = i;

                previouscol = smallval;

                startpos = i;

            }

        }

        netindex[previouscol] = (startpos + maxnetpos) >> 1;

        for (j = previouscol + 1; j < 256; j++)

            netindex[j] = maxnetpos; /* really 256 */

    }

    void alterneigh(int rad, int i, int b, int g, int r) {


        int j, k, lo, hi, a, m;

        int *p;


        lo = i - rad;

        if (lo < -1)

            lo = -1;

        hi = i + rad;

        if (hi > netsize)

            hi = netsize;


        j = i + 1;

        k = i - 1;

        m = 1;

        while ((j < hi) || (k > lo)) {

            a = radpower[m++];

            if (j < hi) {

                p = network[j++];

                p[0] -= (a * (p[0] - b)) / alpharadbias;

                p[1] -= (a * (p[1] - g)) / alpharadbias;

                p[2] -= (a * (p[2] - r)) / alpharadbias;
            }

            if (k > lo) {

                p = network[k--];

                p[0] -= (a * (p[0] - b)) / alpharadbias;

                p[1] -= (a * (p[1] - g)) / alpharadbias;

                p[2] -= (a * (p[2] - r)) / alpharadbias;


            }

        }

    }

    void altersingle(int alpha, int i, int b, int g, int r) {


        /* alter hit neuron */

        int *n = network[i];

        n[0] -= (alpha * (n[0] - b)) / initalpha;

        n[1] -= (alpha * (n[1] - g)) / initalpha;

        n[2] -= (alpha * (n[2] - r)) / initalpha;

    }

    /*

     * Search for biased BGR values ----------------------------

     */

    int contest(int b, int g, int r) {




        /* finds closest neuron (min dist) and updates freq */

        /* finds best neuron (min dist-bias) and returns position */

        /* for frequently chosen neurons, freq[i] is high and bias[i] is negative */

        /* bias[i] = gamma*((1/netsize)-freq[i]) */




        int i, dist, a, biasdist, betafreq;

        int bestpos, bestbiaspos, bestd, bestbiasd;

        int *n;


        bestd = ~(((int) 1) << 31);

        bestbiasd = bestd;

        bestpos = -1;

        bestbiaspos = bestpos;


        for (i = 0; i < netsize; i++) {

            n = network[i];

            dist = n[0] - b;

            if (dist < 0)

                dist = -dist;

            a = n[1] - g;

            if (a < 0)

                a = -a;

            dist += a;

            a = n[2] - r;

            if (a < 0)

                a = -a;

            dist += a;

            if (dist < bestd) {

                bestd = dist;

                bestpos = i;

            }

            biasdist = dist - ((bias[i]) >> (intbiasshift - netbiasshift));

            if (biasdist < bestbiasd) {

                bestbiasd = biasdist;

                bestbiaspos = i;

            }

            betafreq = (freq[i] >> betashift);

            freq[i] -= betafreq;

            bias[i] += (betafreq << gammashift);

        }

        freq[bestpos] += beta;

        bias[bestpos] -= betagamma;

        return (bestbiaspos);

    }


    void learn() {


        int i, j, b, g, r;

        int radius, rad, alpha, step, delta, samplepixels;

        BYTE *p;

        int pix, lim;


        if (lengthcount < minpicturebytes)

            samplefac = 1;

        alphadec = 30 + ((samplefac - 1) / 3);

        p = thepicture;

        pix = 0;

        lim = lengthcount;

        samplepixels = lengthcount / (3 * samplefac);

        delta = samplepixels / ncycles;

        alpha = initalpha;

        radius = initradius;


        rad = radius >> radiusbiasshift;

        if (rad <= 1)

            rad = 0;

        for (i = 0; i < rad; i++)

            radpower[i] = alpha * (((rad * rad - i * i) * radbias) / (rad * rad));


        if (lengthcount < minpicturebytes)

            step = 3;

        else if ((lengthcount % prime1) != 0)

            step = 3 * prime1;

        else {

            if ((lengthcount % prime2) != 0)

                step = 3 * prime2;

            else {

                if ((lengthcount % prime3) != 0)

                    step = 3 * prime3;

                else

                    step = 3 * prime4;

            }

        }


        i = 0;

        while (i < samplepixels) {

            b = (p[pix + 0] & 0xff) << netbiasshift;

            g = (p[pix + 1] & 0xff) << netbiasshift;

            r = (p[pix + 2] & 0xff) << netbiasshift;

            j = contest(b, g, r);


            altersingle(alpha, j, b, g, r);

            if (rad != 0)

                alterneigh(rad, j, b, g, r); /* alter neighbours */

            pix += step;

            if (pix >= lim)

                pix -= lengthcount;


            i++;

            if (delta == 0)

                delta = 1;

            if (i % delta == 0) {

                alpha -= alpha / alphadec;

                radius -= radius / radiusdec;

                rad = radius >> radiusbiasshift;

                if (rad <= 1)

                    rad = 0;

                for (j = 0; j < rad; j++)

                    radpower[j] = alpha * (((rad * rad - j * j) * radbias) / (rad * rad));

            }

        }
    }

    int map(int b, int g, int r) {


        int i, j, dist, a, bestd;

        int *p;

        int best;


        bestd = 1000; /* biggest possible dist is 256*3 */

        best = -1;

        i = netindex[g]; /* index on g */

        j = i - 1; /* start at netindex[g] and work outwards */




        while ((i < netsize) || (j >= 0)) {

            if (i < netsize) {

                p = network[i];

                dist = p[1] - g; /* inx key */

                if (dist >= bestd)

                    i = netsize; /* stop iter */

                else {

                    i++;

                    if (dist < 0)

                        dist = -dist;

                    a = p[0] - b;

                    if (a < 0)

                        a = -a;

                    dist += a;

                    if (dist < bestd) {

                        a = p[2] - r;

                        if (a < 0)

                            a = -a;

                        dist += a;

                        if (dist < bestd) {

                            bestd = dist;

                            best = p[3];

                        }

                    }

                }

            }

            if (j >= 0) {

                p = network[j];

                dist = g - p[1]; /* inx key - reverse dif */

                if (dist >= bestd)

                    j = -1; /* stop iter */

                else {

                    j--;

                    if (dist < 0)

                        dist = -dist;

                    a = p[0] - b;

                    if (a < 0)

                        a = -a;

                    dist += a;

                    if (dist < bestd) {

                        a = p[2] - r;

                        if (a < 0)

                            a = -a;

                        dist += a;

                        if (dist < bestd) {

                            bestd = dist;

                            best = p[3];

                        }

                    }

                }

            }

        }

        return (best);

    }


    void unbiasnet() {


        int i;


        for (i = 0; i < netsize; i++) {

            network[i][0] >>= netbiasshift;

            network[i][1] >>= netbiasshift;

            network[i][2] >>= netbiasshift;

            network[i][3] = i; /* record colour no */

        }

    }

    int** process() {

        learn();

        unbiasnet();

        inxbuild();
        return network;
    }

};

NeuQuant nq = NeuQuant();

JNIEXPORT jbyteArray JNICALL
Java_com_cinemagra_holymoly_cinemagraph_AnimatedGifEncoder_getColorTab(JNIEnv *env, jobject obj,
                                                                        jbyteArray bytes, jint len,
                                                                        jint sample) {
    int l = env->GetArrayLength(bytes);
    BYTE *thepic = new BYTE[l];
    env->GetByteArrayRegion(bytes, 0, l, (jbyte *) thepic);

    //init
    int *p;
    nq.thepicture = thepic;
    nq.lengthcount = len;
    nq.samplefac = sample;
    nq.network = new int *[256];

    for (int i = 0; i < 256; i++) {
        nq.network[i] = new int[4];
        p = nq.network[i];
        p[0] = p[1] = p[2] = (i << (4 + 8)) / 256;
        nq.freq[i] = (((int) 1) << 16) / 256; /* 1/netsize */
        nq.bias[i] = 0;
    }

    int** network = nq.process();

    const int ll = 3 * 256;
    jbyte colorTab[ll];
    int index[256];
    for (int i = 0; i < 256; i++)
        index[nq.network[i][3]] = i;
    int k = 0;
    for (int i = 0; i < 256; i++) {
        int j = index[i];
        colorTab[k++] = (jbyte) (network[j][0]);
        colorTab[k++] = (jbyte) (network[j][1]);
        colorTab[k++] = (jbyte) (network[j][2]);
    }
    jbyteArray ret = env->NewByteArray(ll);
    env->SetByteArrayRegion (ret, 0, ll, colorTab);
    return ret;

}

JNIEXPORT jint JNICALL
Java_com_cinemagra_holymoly_cinemagraph_AnimatedGifEncoder_getIndex(JNIEnv *env, jobject obj,
                                                                     jint b, jint g, jint r) {

    return nq.map(b, g, r);

}
