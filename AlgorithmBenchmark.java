import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AlgorithmBenchmark {
    private static final int[] SIZES = {100, 1_000, 10_000, 50_000};
    private static final int WARM_UP_ROUNDS = 5;
    private static final int MEASUREMENT_ROUNDS = 5;
    private static final int QUANTUM = 3;
    private static final long RANDOM_SEED = 42L;

    public static void main(String[] args) {
        System.out.println("CPU Scheduling Benchmark");
        System.out.printf("Warm-up: %d rounds | Measurements: %d rounds | Seed: %d%n%n",
                WARM_UP_ROUNDS, MEASUREMENT_ROUNDS, RANDOM_SEED);
        System.out.printf("%10s | %14s | %14s%n", "n", "FCFS avg (ms)", "RR avg (ms)");
        System.out.println("-----------+----------------+---------------");

        for (int size : SIZES) {
            List<Process> source = generateProcesses(size);
            warmUp(source);
            System.out.printf("%,10d | %14.3f | %14.3f%n", size,
                    measureFcfs(source), measureRoundRobin(source));
        }
    }

    private static List<Process> generateProcesses(int size) {
        Random random = new Random(RANDOM_SEED);
        List<Process> processes = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            processes.add(new Process("P" + i, 0, random.nextInt(20) + 1));
        }
        return processes;
    }

    private static void warmUp(List<Process> source) {
        for (int round = 0; round < WARM_UP_ROUNDS; round++) {
            new FCFSScheduler().schedule(copyProcesses(source));
            new RoundRobinScheduler().schedule(copyProcesses(source), QUANTUM);
        }
    }

    private static double measureFcfs(List<Process> source) {
        long totalNanoseconds = 0;
        for (int round = 0; round < MEASUREMENT_ROUNDS; round++) {
            List<Process> input = copyProcesses(source);
            long start = System.nanoTime();
            new FCFSScheduler().schedule(input);
            totalNanoseconds += System.nanoTime() - start;
        }
        return totalNanoseconds / (double) MEASUREMENT_ROUNDS / 1_000_000.0;
    }

    private static double measureRoundRobin(List<Process> source) {
        long totalNanoseconds = 0;
        for (int round = 0; round < MEASUREMENT_ROUNDS; round++) {
            List<Process> input = copyProcesses(source);
            long start = System.nanoTime();
            new RoundRobinScheduler().schedule(input, QUANTUM);
            totalNanoseconds += System.nanoTime() - start;
        }
        return totalNanoseconds / (double) MEASUREMENT_ROUNDS / 1_000_000.0;
    }

    private static List<Process> copyProcesses(List<Process> source) {
        List<Process> copies = new ArrayList<>(source.size());
        for (Process p : source) copies.add(p.copy());
        return copies;
    }
}
