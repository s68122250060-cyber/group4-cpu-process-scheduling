import java.util.ArrayList;
import java.util.List;

public class Main {
    private static final int QUANTUM = 3;

    public static void main(String[] args) {
        System.out.println("=== Group 4: CPU Process Scheduling ===");

        List<Process> source = createRequiredProcesses();
        FCFSScheduler fcfs = new FCFSScheduler();
        List<Process> resultFCFS = fcfs.schedule(copyProcesses(source));
        printResults("Algorithm A: FCFS", resultFCFS,
                fcfs.getContextSwitchCount(), false);
        verifyMetrics("FCFS", resultFCFS, fcfs.getContextSwitchCount(),
                10.25, 16.75, 0.00, 3, false);

        RoundRobinScheduler rr = new RoundRobinScheduler();
        List<Process> resultRR = rr.schedule(copyProcesses(source), QUANTUM);
        printResults("Algorithm B: Round Robin (Quantum = " + QUANTUM + ")",
                resultRR, rr.getContextSwitchCount(), true);
        verifyMetrics("Round Robin", resultRR, rr.getContextSwitchCount(),
                15.00, 21.50, 4.50, 9, true);
    }

    private static List<Process> createRequiredProcesses() {
        List<Process> processes = new ArrayList<>();
        processes.add(new Process("P1", 0, 8));
        processes.add(new Process("P2", 0, 4));
        processes.add(new Process("P3", 0, 9));
        processes.add(new Process("P4", 0, 5));
        return processes;
    }

    private static List<Process> copyProcesses(List<Process> processes) {
        List<Process> copies = new ArrayList<>(processes.size());
        for (Process p : processes) {
            copies.add(p.copy());
        }
        return copies;
    }

    private static void printResults(String title, List<Process> result,
            int contextSwitchCount, boolean showResponseTime) {
        System.out.println("\n--- " + title + " ---");
        for (Process p : result) {
            String response = showResponseTime
                    ? String.format(", RT=%d", p.getResponseTime()) : "";
            System.out.printf("%s: ST=%d, CT=%d, WT=%d, TAT=%d%s%n",
                    p.getId(), p.getStartTime(), p.getCompletionTime(),
                    p.getWaitingTime(), p.getTurnaroundTime(), response);
        }
        System.out.printf("Average Waiting Time: %.2f%n", averageWaitingTime(result));
        System.out.printf("Average Turnaround Time: %.2f%n", averageTurnaroundTime(result));
        if (showResponseTime) {
            System.out.printf("Average Response Time: %.2f%n", averageResponseTime(result));
        }
        System.out.printf("Context Switch Count: %d%n", contextSwitchCount);
    }

    private static void verifyMetrics(String algorithm, List<Process> result,
            int actualSwitches, double expectedWaiting, double expectedTurnaround,
            double expectedResponse, int expectedSwitches, boolean checkResponse) {
        boolean pass = close(averageWaitingTime(result), expectedWaiting)
                && close(averageTurnaroundTime(result), expectedTurnaround)
                && actualSwitches == expectedSwitches
                && (!checkResponse || close(averageResponseTime(result), expectedResponse));
        System.out.printf("[%s] %s metrics match the specification.%n",
                pass ? "PASS" : "FAIL", algorithm);
    }

    private static double averageWaitingTime(List<Process> result) {
        return result.stream().mapToInt(Process::getWaitingTime).average().orElse(0.0);
    }

    private static double averageTurnaroundTime(List<Process> result) {
        return result.stream().mapToInt(Process::getTurnaroundTime).average().orElse(0.0);
    }

    private static double averageResponseTime(List<Process> result) {
        return result.stream().mapToInt(Process::getResponseTime).average().orElse(0.0);
    }

    private static boolean close(double actual, double expected) {
        return Math.abs(actual - expected) < 0.0001;
    }
}
