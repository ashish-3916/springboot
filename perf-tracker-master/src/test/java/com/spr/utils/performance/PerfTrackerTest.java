package com.spr.utils.performance;


import org.junit.Assert;
import org.junit.Test;

public class PerfTrackerTest {

    @Test
    public void testOrder() throws InterruptedException {
        PerfTracker.PerfStats start = PerfTracker.start();
        PerfTracker.in("1");
        Thread.sleep(10L);
        PerfTracker.out("1");

        PerfTracker.in("2");
        Thread.sleep(10L);
        PerfTracker.out("2");

        fork();

        PerfTracker.in("1");
        Thread.sleep(10L);
        PerfTracker.out("1");

        PerfTracker.Stat stat = start.stopAndGetStat();
        Assert.assertTrue(stat.findStat("1", 0).getTimeTakenInMillis() > 20);
    }

    private void fork() throws InterruptedException {
        PerfTracker.PerfStats start = PerfTracker.start();
        PerfTracker.in("1");
        Thread.sleep(10L);
        PerfTracker.out("1");
        start.stop();
    }
}
