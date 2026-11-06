package com.tv.screenguard;
import org.junit.Test;
import static org.junit.Assert.*;
public class ChildWatchClockTest {
 @Test public void pausesWithoutLosingSharedBudget(){ChildWatchClock c=new ChildWatchClock(1000,7000);c.advance(1500,true);c.advance(2000,false);c.advance(2500,true);assertEquals(8000,c.elapsed());}
 @Test public void resumesFromPersistedProgress(){ChildWatchClock c=new ChildWatchClock(100000,599000);c.advance(100500,true);c.advance(101000,true);assertEquals(600000,c.elapsed());}
 @Test public void clampsUnobservedLongGaps(){ChildWatchClock c=new ChildWatchClock(0,0);c.advance(600000,true);assertEquals(1000,c.elapsed());}
 @Test public void resetStartsFreshRound(){ChildWatchClock c=new ChildWatchClock(0,100);c.reset(1000);c.advance(1500,true);assertEquals(500,c.elapsed());}
 @Test public void rejectsNegativeSavedProgressAndClockReversal(){ChildWatchClock c=new ChildWatchClock(1000,-1);c.advance(900,true);assertEquals(0,c.elapsed());}
}
