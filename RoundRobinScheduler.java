import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;

// ---------- Algorithm B: Round Robin (Circular Queue) ----------
public class RoundRobinScheduler {
    private int contextSwitchCount;

    public List<Process> schedule(List<Process> processes, int quantum) {
        if (quantum <= 0) {
            throw new IllegalArgumentException("Quantum must be greater than zero");
        }

        contextSwitchCount = 0;
        List<Process> sortedList = new ArrayList<>(processes);
        sortedList.sort(Comparator.comparingInt(Process::getArrivalTime));

        Queue<Process> readyQueue = new ArrayDeque<>();
        int currentTime = 0;
        int nextArrivalIndex = 0;
        Process lastRan = null;
        List<Process> result = new ArrayList<>(sortedList.size());

        while (nextArrivalIndex < sortedList.size() || !readyQueue.isEmpty()) {
            if (readyQueue.isEmpty()) {
                currentTime = Math.max(currentTime,
                        sortedList.get(nextArrivalIndex).getArrivalTime());
                lastRan = null;
                nextArrivalIndex = enqueueArrivals(
                        sortedList, nextArrivalIndex, currentTime, readyQueue);
            }

            Process p = readyQueue.poll();
            if (lastRan != null && !lastRan.getId().equals(p.getId())) {
                contextSwitchCount++;
            }
            if (p.getStartTime() == -1) {
                p.setStartTime(currentTime);
                p.setResponseTime(currentTime - p.getArrivalTime());
            }

            int runTime = Math.min(quantum, p.getRemainingTime());
            currentTime += runTime;
            p.setRemainingTime(p.getRemainingTime() - runTime);
            lastRan = p;

            // Add new arrivals before returning an unfinished process to the queue.
            nextArrivalIndex = enqueueArrivals(
                    sortedList, nextArrivalIndex, currentTime, readyQueue);

            if (p.getRemainingTime() > 0) {
                readyQueue.offer(p);
            } else {
                p.setCompletionTime(currentTime);
                p.setTurnaroundTime(p.getCompletionTime() - p.getArrivalTime());
                p.setWaitingTime(p.getTurnaroundTime() - p.getBurstTime());
                result.add(p);
            }
        }
        return result;
    }

    private int enqueueArrivals(List<Process> sortedList, int nextArrivalIndex,
            int currentTime, Queue<Process> readyQueue) {
        while (nextArrivalIndex < sortedList.size()
                && sortedList.get(nextArrivalIndex).getArrivalTime() <= currentTime) {
            readyQueue.offer(sortedList.get(nextArrivalIndex));
            nextArrivalIndex++;
        }
        return nextArrivalIndex;
    }

    public boolean cancel(Queue<Process> queue, String processId) {
        return queue.removeIf(p -> p.getId().equals(processId));
    }

    public int getContextSwitchCount() {
        return contextSwitchCount;
    }
}
