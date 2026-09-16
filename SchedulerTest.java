import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

public class SchedulerTest {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        run("TC-01 Normal Case", SchedulerTest::testNormalCase);
        run("TC-02 Empty Queue", SchedulerTest::testEmptyQueue);
        run("TC-03 Single Item", SchedulerTest::testSingleItem);
        run("TC-04 Large Queue", SchedulerTest::testLargeQueue);
        run("TC-05 Burst <= Quantum", SchedulerTest::testBurstWithinQuantum);
        run("TC-06 Cancel Case", SchedulerTest::testCancel);

        System.out.printf("%nSummary: %d passed, %d failed%n", passed, failed);
        if (failed > 0) {
            throw new AssertionError(failed + " test case(s) failed");
        }
    }

    private static void testNormalCase() {
        List<Process> source = requiredProcesses();
        FCFSScheduler fcfs = new FCFSScheduler();
        List<Process> fcfsResult = fcfs.schedule(copyProcesses(source));
        assertClose(10.25, averageWaiting(fcfsResult), "FCFS average waiting");
        assertClose(16.75, averageTurnaround(fcfsResult), "FCFS average turnaround");
        assertEquals(3, fcfs.getContextSwitchCount(), "FCFS context switches");

        RoundRobinScheduler rr = new RoundRobinScheduler();
        List<Process> rrResult = rr.schedule(copyProcesses(source), 3);
        assertClose(15.00, averageWaiting(rrResult), "RR average waiting");
        assertClose(21.50, averageTurnaround(rrResult), "RR average turnaround");
        assertClose(4.50, averageResponse(rrResult), "RR average response");
        assertEquals(9, rr.getContextSwitchCount(), "RR context switches");

        rr.schedule(copyProcesses(source), 3);
        assertEquals(9, rr.getContextSwitchCount(), "RR switch-count reset");

        List<Process> lateArrivals = new ArrayList<>();
        lateArrivals.add(new Process("Late", 5, 2));
        lateArrivals.add(new Process("First", 0, 1));
        Process late = find(rr.schedule(lateArrivals, 1), "Late");
        assertEquals(5, late.getStartTime(), "RR arrival and CPU idle support");
    }

    private static void testEmptyQueue() {
        FCFSScheduler fcfs = new FCFSScheduler();
        RoundRobinScheduler rr = new RoundRobinScheduler();
        assertTrue(fcfs.schedule(new ArrayList<>()).isEmpty(), "FCFS empty result");
        assertTrue(rr.schedule(new ArrayList<>(), 3).isEmpty(), "RR empty result");
        assertEquals(0, fcfs.getContextSwitchCount(), "FCFS empty switches");
        assertEquals(0, rr.getContextSwitchCount(), "RR empty switches");
    }

    private static void testSingleItem() {
        List<Process> source = List.of(new Process("P1", 0, 5));
        FCFSScheduler fcfs = new FCFSScheduler();
        RoundRobinScheduler rr = new RoundRobinScheduler();
        Process a = fcfs.schedule(copyProcesses(source)).get(0);
        Process b = rr.schedule(copyProcesses(source), 3).get(0);
        assertSameMetrics(a, b, "single process");
        assertEquals(0, fcfs.getContextSwitchCount(), "FCFS single switches");
        assertEquals(0, rr.getContextSwitchCount(), "RR single switches");
    }

    private static void testLargeQueue() {
        List<Process> source = new ArrayList<>(10_000);
        Random random = new Random(42);
        for (int i = 0; i < 10_000; i++) {
            source.add(new Process("P" + i, random.nextInt(1_000), random.nextInt(20) + 1));
        }
        assertEquals(10_000, new FCFSScheduler().schedule(copyProcesses(source)).size(),
                "FCFS large result size");
        assertEquals(10_000, new RoundRobinScheduler().schedule(copyProcesses(source), 3).size(),
                "RR large result size");
    }

    private static void testBurstWithinQuantum() {
        List<Process> source = new ArrayList<>();
        source.add(new Process("P1", 0, 2));
        source.add(new Process("P2", 0, 3));
        source.add(new Process("P3", 0, 1));
        FCFSScheduler fcfs = new FCFSScheduler();
        RoundRobinScheduler rr = new RoundRobinScheduler();
        List<Process> a = fcfs.schedule(copyProcesses(source));
        List<Process> b = rr.schedule(copyProcesses(source), 3);
        assertEquals(a.size(), b.size(), "edge result size");
        for (int i = 0; i < a.size(); i++) {
            assertEquals(a.get(i).getId(), b.get(i).getId(), "edge completion order");
            assertSameMetrics(a.get(i), b.get(i), "edge process " + a.get(i).getId());
        }
        assertEquals(fcfs.getContextSwitchCount(), rr.getContextSwitchCount(),
                "edge context switches");
    }

    private static void testCancel() {
        Queue<Process> fcfsQueue = cancelQueue();
        FCFSScheduler fcfs = new FCFSScheduler();
        assertTrue(fcfs.cancel(fcfsQueue, "P2"), "FCFS cancels middle process");
        assertFalse(fcfs.cancel(fcfsQueue, "Missing"), "FCFS missing process");
        assertFalse(containsId(fcfsQueue, "P2"), "FCFS queue no longer contains P2");

        Queue<Process> rrQueue = cancelQueue();
        RoundRobinScheduler rr = new RoundRobinScheduler();
        assertTrue(rr.cancel(rrQueue, "P2"), "RR cancels middle process");
        assertFalse(rr.cancel(rrQueue, "Missing"), "RR missing process");
        assertFalse(containsId(rrQueue, "P2"), "RR queue no longer contains P2");
    }

    private static Queue<Process> cancelQueue() {
        Queue<Process> queue = new ArrayDeque<>();
        queue.offer(new Process("P1", 0, 1));
        queue.offer(new Process("P2", 0, 1));
        queue.offer(new Process("P3", 0, 1));
        return queue;
    }

    private static boolean containsId(Queue<Process> queue, String id) {
        return queue.stream().anyMatch(p -> p.getId().equals(id));
    }

    private static List<Process> requiredProcesses() {
        List<Process> processes = new ArrayList<>();
        processes.add(new Process("P1", 0, 8));
        processes.add(new Process("P2", 0, 4));
        processes.add(new Process("P3", 0, 9));
        processes.add(new Process("P4", 0, 5));
        return processes;
    }

    private static List<Process> copyProcesses(List<Process> source) {
        List<Process> copies = new ArrayList<>(source.size());
        for (Process p : source) copies.add(p.copy());
        return copies;
    }

    private static Process find(List<Process> processes, String id) {
        return processes.stream().filter(p -> p.getId().equals(id)).findFirst()
                .orElseThrow(() -> new AssertionError("Missing process " + id));
    }

    private static double averageWaiting(List<Process> processes) {
        return processes.stream().mapToInt(Process::getWaitingTime).average().orElse(0.0);
    }

    private static double averageTurnaround(List<Process> processes) {
        return processes.stream().mapToInt(Process::getTurnaroundTime).average().orElse(0.0);
    }

    private static double averageResponse(List<Process> processes) {
        return processes.stream().mapToInt(Process::getResponseTime).average().orElse(0.0);
    }

    private static void assertSameMetrics(Process expected, Process actual, String label) {
        assertEquals(expected.getStartTime(), actual.getStartTime(), label + " start");
        assertEquals(expected.getCompletionTime(), actual.getCompletionTime(), label + " completion");
        assertEquals(expected.getWaitingTime(), actual.getWaitingTime(), label + " waiting");
        assertEquals(expected.getTurnaroundTime(), actual.getTurnaroundTime(), label + " turnaround");
        assertEquals(expected.getResponseTime(), actual.getResponseTime(), label + " response");
    }

    private static void run(String name, Runnable test) {
        try {
            test.run();
            passed++;
            System.out.println("[PASS] " + name);
        } catch (Throwable error) {
            failed++;
            System.out.println("[FAIL] " + name + ": " + error.getMessage());
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void assertFalse(boolean condition, String message) {
        assertTrue(!condition, message);
    }

    private static void assertEquals(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }

    private static void assertEquals(String expected, String actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }

    private static void assertClose(double expected, double actual, String message) {
        if (Math.abs(expected - actual) >= 0.0001) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }
}
