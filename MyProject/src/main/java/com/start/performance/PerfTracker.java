package com.start.performance;


import com.start.callee.PerfCallerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * User: rajesh
 * Date: 27/07/20
 * Time: 6:17 PM
 */
public class PerfTracker {

    private static final String ROOT_STAT = "__ROOT__s";
    private static final Logger logger = LoggerFactory.getLogger(PerfTracker.class);

    private static final boolean perfTrackerDisabled = "true".equals(System.getProperty("perf.tracker.disabled"));
    private static final Integer samplingFactor = System.getProperty("perf.tracker.sampling.factor") != null ?
                                                  Integer.parseInt(System.getProperty("perf.tracker.sampling.factor")) : null;
    private static final long ALARMING_PICKUP_DELAY = 50_000_000L; // 50ms
    private static final Supplier<Stat> STAT_FALLBACK = () -> new Stat(ROOT_STAT);
    private static final Supplier<String> EMPTY_STRING = () -> "";
    private static final Supplier<Map<String, Long>> EMPTY_MAP = Collections::emptyMap;
    private static final Supplier<Map<String, Object>> EMPTY_OBJ_MAP = Collections::emptyMap;
    private static final String CURRENT_BASE_PACKAGE = "com.spr.utils.performance";
    private static final boolean SKIP_ZEROS = Boolean.parseBoolean(System.getProperty("log.keys.with.no.time.taken", "true"));
    private static final int MAX_CALL_STACK_DEPTH = Integer.parseInt(System.getProperty("perf.stat.max.call.stack.depth", "200"));
    private static final ThreadLocal<PerfStats> PERF_STATS = new ThreadLocal<>();

    public static PerfStats start() {
        return start(PerfCallerInfo.createCalleeInfo(CURRENT_BASE_PACKAGE).getCallingMethodName());
    }

    public static PerfStats start(String type) {
        PerfStats parent = PERF_STATS.get();
        if (parent != null && parent.parentCounts > 100) {
            //probably somebody forgot to stop, reset it
            logger.error("Somewhere PerfStats.stop() call is missed " + type);
            return new PerfStats(type, null, true).start();
        } else {
            return new PerfStats(type, parent, true).start();
        }
    }

    public static void reset() {
        PERF_STATS.remove();
    }

    /**
     * Use to start stats in new thread. Create new child PerfStats in parent thread, and start/stop in child thread
     */
    public static PerfStats newThreadPerfStats() {
        return newThreadPerfStats(PerfCallerInfo.createCalleeInfo(CURRENT_BASE_PACKAGE).getCallingMethodName());
    }

    public static PerfStats newThreadPerfStats(String type) {
        return new PerfStats(type, PERF_STATS.get(), false);
    }

    public static void in(String statName) {
        safe(() -> {
            if (perfTrackerDisabled) {
                return;
            }
            PerfStats perfStats = PERF_STATS.get();
            if (!shouldTrack(perfStats)) {
                return;
            }
            perfStats.in(statName);
        });
    }

    public static void out(String statName) {
        safe(() -> {
            if (perfTrackerDisabled) {
                return;
            }
            PerfStats perfStats = PERF_STATS.get();
            if (!shouldTrack(perfStats)) {
                return;
            }
            perfStats.out(statName);
        });
    }

    public static void out(String statName, boolean skipZero) {
        safe(() -> {
            if (perfTrackerDisabled) {
                return;
            }
            PerfStats perfStats = PERF_STATS.get();
            if (!shouldTrack(perfStats)) {
                return;
            }
            perfStats.out(statName, skipZero);
        });
    }

    public static void trackIfSlow(String statName) {
        safe(() -> {
            PerfStats perfStats = PERF_STATS.get();
            if (perfStats == null) {
                return;
            }
            perfStats.out(statName, true);
        });
    }

    public static Stat getRootStats() {
        PerfStats current = PERF_STATS.get();
        if (current != null) {
            return current.root;
        } else {
            return STAT_FALLBACK.get();
        }
    }

    public static Stat getLastStats() {
        PerfStats current = PERF_STATS.get();
        if (current != null) {
            return current.current;
        } else {
            return STAT_FALLBACK.get();
        }
    }

    public static Long get(String stateName) {
        Stat stat = getRootStats().findStat(stateName, 0);
        if (stat != null) {
            return stat.getTimeTakenInMillis();
        } else {
            return -1L;
        }
    }

    public static Long avg(String stateName) {
        Stat stat = getRootStats().findStat(stateName, 0);
        if (stat != null) {
            return stat.getTimeTakenInMillis() / (stat.count == 0 ? 1 : stat.count);
        } else {
            return null;
        }
    }

    public static Long getEndTime(String stateName) {
        Stat stat = getRootStats().findStat(stateName, 0);
        if (stat != null) {
            return TimeUnit.NANOSECONDS.toMillis(stat.startTime + stat.getTimeTaken());
        } else {
            return null;
        }
    }

    public static void executorDelay(long timeTaken) {
        if (timeTaken > 0) {
            if (timeTaken > ALARMING_PICKUP_DELAY) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Task picked up with delay:" + timeTaken + " threadName:" + Thread.currentThread().getName());
                }
            }
            track("pickupDelay", timeTaken);
        }
    }

    public static void executorDelay(String name, long timeTaken) {
        if (timeTaken > ALARMING_PICKUP_DELAY) {
            if (logger.isDebugEnabled()) {
                logger.debug("Task picked up with delay:" + timeTaken + " threadName:" + Thread.currentThread().getName() + " name:" + name);
            }
        }
        track(name, timeTaken);
    }

    public static void track(String statName, long timeTaken) {
        track(statName, timeTaken, 1);
    }

    public static void track(String statName, long timeTaken, int count) {
        safe(() -> {
            PerfStats perfStats = PERF_STATS.get();
            if (perfStats == null) {
                return;
            }
            perfStats.in(statName, System.nanoTime() - timeTaken);
            perfStats.out((int) timeTaken, count);
        });
    }

    public static <T> T track(String name, Supplier<T> callable) {
        try {
            in(name);
            return callable.get();
        } finally {
            out(name);
        }
    }

    public static void track(String name, Runnable runnable) {
        try {
            in(name);
            runnable.run();
        } finally {
            out(name);
        }
    }

    // propagates checked exception
    public static <T, E extends Exception> T trackChecked(String name, SupplierWithException<T, E> callable) throws E {
        try {
            in(name);
            return callable.get();
        } finally {
            out(name);
        }
    }

    // propagates checked exception
    public static <E extends Exception> void trackChecked(String name, RunnableWithException<E> runnable) throws E {
        try {
            in(name);
            runnable.get();
        } finally {
            out(name);
        }
    }

    public static void merge(Map<String, Long> perfStats, String prefix) {
        if (perfStats == null) {
            return;
        }
        for (Entry<String, Long> entry : perfStats.entrySet()) {
            if (entry.getValue() != null) {
                track(prefix + "." + entry.getKey(), entry.getValue());
            }
        }
    }

    public static void mergeNested(Map<String, Object> perfStats) {
        safe(() -> {
            if (perfStats == null || PERF_STATS.get() == null) {
                return;
            }
            perfStats.entrySet().stream().findFirst().ifPresent(entry -> PERF_STATS.get().merge(entry.getKey(), entry.getValue()));
        });
    }

    public static class PerfStats {

        private final PerfStats parent;
        private final int parentCounts;
        private final AtomicInteger statCount;
        private final Stat root;
        private Stat current;
        private boolean stopped;
        private boolean shouldTrack;

        private PerfStats(String type, PerfStats parent, boolean sameThread) {
            String prefix = (type == null || type.isEmpty()) ? "" : (type + ".");
            if (parent == null) {
                this.parentCounts = 0;
                this.statCount = new AtomicInteger(1);
                this.root = new Stat(prefix + "totalTime").start();
            } else {
                this.parentCounts = parent.parentCounts + 1;
                this.statCount = parent.statCount;
                if (sameThread) {
                    this.root = parent.current.push(prefix + "totalTime", statCount).start();
                } else {
                    this.root = parent.current.newThreadChild(prefix + "totalTime", statCount).start();
                }
            }
            this.parent = sameThread ? parent : null;
            this.current = this.root;
            if (samplingFactor == null || (new Random().nextInt() % samplingFactor == 0)) {
                shouldTrack = true;
            }
        }

        private void in(String statName) {
            if (statCount.get() > 1000) {
                return;
            }
            this.current = this.current.push(statName, statCount).start();
        }

        private void in(String statName, long time) {
            if (statCount.get() > 1000) {
                return;
            }
            this.current = this.current.push(statName, statCount).start(time);
        }

        private void out(String statName) {
            out(statName, false);
        }

        private void out(String statName, boolean skipZeros) {
            Stat stop = this.current.stop();
            this.current = stop.pop();
            if (!stop.name.equals(statName) && !ROOT_STAT.equals(current.name)) {
                out(statName);
                if (logger.isDebugEnabled()) {
                    logger.debug("Unordered out call for " + statName + ", current: " + stop.name);
                }
            }
            if ((skipZeros || SKIP_ZEROS) && stop.timeTaken == 0) {
                this.current.removeChild(stop);
            }
        }

        private void out(int timeTaken, int count) {
            if (statCount.get() > 1000) {
                return;
            }
            Stat stop = this.current.stop(timeTaken, count);
            this.current = stop.pop();
        }

        public PerfStats start() {
            PERF_STATS.set(this);
            return this;
        }

        /**
         * Use stopAndGetNestedMap or stopAndGetStacked instead.
         */
        public void stop() {
            stopAndGetStat();
        }

        public String stopAndGetStacked() {
            return stopAndGetStat().toStacked();
        }

        public Map<String, Object> stopAndGetNestedMap() {
            return stopAndGetStat().toNestedMap();
        }

        public Stat stopAndGetStat() {
            return safe(() -> {
                if (!stopped) {
                    this.root.stop();
                    stopped = true;
                }
                if (parent != null) {
                    //same thread, so just created a child one to collect stats separately, so set parent
                    PERF_STATS.set(this.parent);
                } else {
                    //different thread than parent thread, cant start parent here
                    PERF_STATS.remove();
                }
                if (statCount.get() > 1000) {
                    Stat stat = new Stat("To many stats collection. Final stats may be wrong!");
                    stat.child = root;
                    return stat;
                }
                return root;
            }, STAT_FALLBACK);
        }

        public void merge(String statName, Object value) {
            int[] ct = toCt(value);
            if (value instanceof int[] || value instanceof List<?>) {
                in(statName, 0);
                out(ct[1], ct[0]);
            } else if (value instanceof Map<?, ?>) {
                in(statName, 0);
                //noinspection unchecked
                for (Entry<String, Object> entry : ((Map<String, Object>) value).entrySet()) {
                    if ("ct".equals(entry.getKey())) {
                        continue;
                    }
                    merge(entry.getKey(), entry.getValue());
                }
                out(ct[1], ct[0]);
            }
        }

        private int[] toCt(Object value) {
            if (value instanceof int[]) {
                return (int[]) value;
            } else if (value instanceof List<?>) {
                return new int[]{((Number) ((List<?>) value).get(0)).intValue(), ((Number) ((List<?>) value).get(1)).intValue()};
            } else if (value instanceof Map<?, ?>) {
                return toCt(((Map<?, ?>) value).get("ct"));
            } else {
                return new int[]{0, 0};
            }
        }
    }

    public static class Stat {

        private final String name;
        private final long creationTime;
        private long startTime;
        private long timeTaken;
        private int count;
        private boolean dbCall = false;
        private Stat parent;
        private Stat child;
        private Stat peer;

        private Stat(String name) {
            this.name = name;
            this.creationTime = System.nanoTime();
        }

        private Stat start() {
            return start(System.nanoTime());
        }

        private Stat startDBCall() {
            Stat start = start(System.nanoTime());
            start.dbCall = true;
            return start;
        }

        private Stat start(long startTime) {
            this.startTime = startTime;
            return this;
        }

        private Stat stop() {
            this.timeTaken += (System.nanoTime() - startTime);
            this.count++;
            return this;
        }


        private Stat stop(int timeTaken, int count) {
            this.timeTaken += timeTaken;
            this.count += count;
            return this;
        }

        private Stat push(String name, AtomicInteger statCount) {
            Stat find = child;
            Stat leafChild = null;
            while (find != null) {
                if (find.name.equals(name)) {
                    break;
                }
                leafChild = find;
                find = find.peer;
            }
            if (find == null) {
                find = new Stat(name);
                statCount.incrementAndGet();
                find.parent = this;
                if (leafChild != null) {
                    leafChild.peer = find;
                } else {
                    this.child = find;
                }
            }
            return find;
        }

        private Stat newThreadChild(String name, AtomicInteger statCount) {
            Stat leafChild = child;
            while (leafChild != null && leafChild.peer != null) {
                leafChild = leafChild.peer;
            }

            Stat newChild = new Stat(name);
            statCount.incrementAndGet();
            newChild.parent = this;
            if (leafChild != null) {
                leafChild.peer = newChild;
            } else {
                this.child = newChild;
            }
            return newChild;
        }

        private Stat pop() {
            if (this.parent == null) {
                return STAT_FALLBACK.get();
            }
            return this.parent;
        }

        private void toStacked(int depth, StringBuilder sb, int callStackDepth, long rootStartTime) {
            for (int i = 0; i < depth; i++) {
                sb.append("  ");
            }
            sb.append(name).append(": ").append(count)
                    .append(" : ").append(timeElapsedRelativeToRoot(rootStartTime))
                    .append(" : ").append(timeTaken).append('\n');

            if (child != null && callStackDepth < MAX_CALL_STACK_DEPTH) {
                child.toStacked(depth + 1, sb, callStackDepth + 1, this.startTime);
            }
            if (peer != null && callStackDepth < MAX_CALL_STACK_DEPTH) {
                peer.toStacked(depth, sb, callStackDepth + 1, rootStartTime);
            }
        }

        private long timeElapsedRelativeToRoot(long rootStartTime) {
            return TimeUnit.NANOSECONDS.toMillis(this.creationTime - rootStartTime);
        }

        private void toMap(LinkedHashMap<String, int[]> stats, int callStackDepth) {
            stats.merge(name, new int[]{count, (int) getTimeTakenInMillis()}, (ct1, ct2) -> new int[]{ct1[0] + ct2[0], ct1[1] + ct2[1]});
            if (child != null && callStackDepth < MAX_CALL_STACK_DEPTH) {
                child.toMap(stats, callStackDepth + 1);
            }
            if (peer != null && callStackDepth < MAX_CALL_STACK_DEPTH) {
                peer.toMap(stats, callStackDepth + 1);
            }
        }

        private LinkedHashMap<String, Object> toNestedMap(LinkedHashMap<String, Object> parent, int callStackDepth) {
            LinkedHashMap<String, Object> stats = new LinkedHashMap<>(3);
            if (this.child != null) {
                stats.put("ct", new int[]{count, (int) getTimeTakenInMillis()});
                parent.put(name, stats);
            } else {
                parent.put(name, new int[]{count, (int) getTimeTakenInMillis()});
            }
            Stat next = this.child;
            while (next != null && callStackDepth < MAX_CALL_STACK_DEPTH) {
                next.toNestedMap(stats, callStackDepth + 1);
                next = next.peer;
            }
            return parent;
        }

        public String getName() {
            return name;
        }

        private long getTimeTaken() {
            return timeTaken;
        }

        public long getTimeTakenInMillis() {
            return TimeUnit.NANOSECONDS.toMillis(timeTaken);
        }

        public String toStacked() {
            return safe(() -> {
                StringBuilder sb = new StringBuilder();
                toStacked(0, sb, 0, creationTime);
                return sb.toString();
            }, EMPTY_STRING);
        }

        public Map<String, Long> toMap() {
            return toMap(true);
        }

        public Map<String, Long> toMap(boolean withCount) {
            return safe(() -> {
                LinkedHashMap<String, int[]> stats = new LinkedHashMap<>();
                toMap(stats, 0);
                LinkedHashMap<String, Long> transformed = new LinkedHashMap<>();
                stats.forEach((s, ct) -> {
                    if (withCount) {
                        transformed.put(s + ":" + ct[0], (long) ct[1]);
                    } else {
                        transformed.put(s, (long) ct[1]);
                    }
                });
                return transformed;
            }, EMPTY_MAP);
        }

        public Map<String, Object> toNestedMap() {
            return safe(() -> toNestedMap(new LinkedHashMap<>(2), 0), EMPTY_OBJ_MAP);
        }

        public void removeChild(Stat remove) {
            Stat find = this.child;
            Stat previous = null;
            while (find != null) {
                if (find == remove) {
                    break;
                }
                previous = find;
                find = find.peer;
            }
            if (find == null) {
                return;
            }
            if (previous != null) {
                previous.peer = find.peer;
            } else {
                this.child = find.peer;
            }
            find.parent = null;
            find.peer = null;
        }

        public Stat findStat(String stateName, int callStackDepth) {
            if (name.equals(stateName)) {
                return this;
            }
            if (peer != null && callStackDepth < MAX_CALL_STACK_DEPTH) {
                return peer.findStat(stateName, callStackDepth + 1);
            }
            if (child != null && callStackDepth < MAX_CALL_STACK_DEPTH) {
                return child.findStat(stateName, callStackDepth + 1);
            }
            return null;
        }

        public Map<String, Stat> findStats(Set<String> stateNames) {
            Map<String, Stat> toReturn = new HashMap<>();
            populateStats(new HashSet<>(stateNames), toReturn, 0);
            return toReturn;
        }

        public long timeTakenThatEndsWith(String stateName) {
            AtomicLong toReturn = new AtomicLong(0);
            timeTakenThatEndsWith(stateName, toReturn, 0);
            return toReturn.get();
        }

        private void timeTakenThatEndsWith(String stateName, AtomicLong stats, int callStackDepth) {
            if (name.endsWith(stateName)) {
                stats.addAndGet(this.timeTaken);
            }

            if (peer != null && callStackDepth < MAX_CALL_STACK_DEPTH) {
                peer.timeTakenThatEndsWith(stateName, stats, callStackDepth + 1);
            }
            if (child != null && callStackDepth < MAX_CALL_STACK_DEPTH) {
                child.timeTakenThatEndsWith(stateName, stats, callStackDepth + 1);
            }
        }

        private void populateStats(Set<String> stateNames, Map<String, Stat> stats, int callStackDepth) {
            if (stateNames == null || stateNames.isEmpty()) {
                return;
            }
            if (stateNames.contains(name)) {
                stats.put(name, this);
                stateNames.remove(name);
            }

            if (peer != null && callStackDepth < MAX_CALL_STACK_DEPTH) {
                peer.populateStats(stateNames, stats, callStackDepth + 1);
            }
            if (child != null && callStackDepth < MAX_CALL_STACK_DEPTH) {
                child.populateStats(stateNames, stats, callStackDepth + 1);
            }
        }

        public Stat findFirstLevelMax() {
            Stat find = this.child;
            Stat max = this.child;
            while (find != null) {
                if (max.timeTaken < find.timeTaken) {
                    max = find;
                }
                find = find.peer;
            }
            return max != null ? max : this;
        }

        public Stat findSlowestDBCall() {
            return findSlowestDBCall(this.child, new AtomicInteger(0));
        }

        private static Stat findSlowestDBCall(Stat node, AtomicInteger iteration) {
            if (node != null && iteration.incrementAndGet() < 1000) { // max iteration count 1000
                return max(node, max(findSlowestDBCall(node.child, iteration), findSlowestDBCall(node.peer, iteration)));
            }
            return null;
        }

        private static Stat max(Stat node1, Stat node2) {
            if (node1 == null || !node1.dbCall) {
                return node2;
            } else if (node2 == null || !node2.dbCall) {
                return node1;
            }
            return node1.timeTaken > node2.timeTaken ? node1 : node2;
        }

        @Override
        public String toString() {
            return toStacked();
        }
    }

    private static <V> V safe(Callable<V> callable, Supplier<V> def) {
        try {
            return callable.call();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return def.get();
        }
    }

    private static void safe(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static class DB {

        public static void in(String statName) {
            safe(() -> {
                PerfStats perfStats = PERF_STATS.get();
                if (perfStats == null) {
                    return;
                }
                if (perfStats.statCount.get() > 1000) {
                    return;
                }
                perfStats.current = perfStats.current.push(statName, perfStats.statCount).startDBCall();
            });
        }
    }

    @FunctionalInterface
    public interface SupplierWithException<T, E extends Exception> {

        T get() throws E;
    }

    @FunctionalInterface
    public interface RunnableWithException<E extends Exception> {

        void get() throws E;
    }

    private static boolean shouldTrack(PerfStats perfStats) {
        return perfStats != null && perfStats.shouldTrack;
    }


}
