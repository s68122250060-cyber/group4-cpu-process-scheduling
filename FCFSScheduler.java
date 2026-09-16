import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;

// ---------- Algorithm A: First-Come, First-Served ----------
public class FCFSScheduler {
    private int contextSwitchCount;

    public List<Process> schedule(List<Process> processes) {
        contextSwitchCount = 0;
        List<Process> sortedList = new ArrayList<>(processes);
        sortedList.sort(Comparator.comparingInt(Process::getArrivalTime));

        int currentTime = 0;
        List<Process> result = new ArrayList<>(sortedList.size());
        for (Process p : sortedList) {
            if (currentTime < p.getArrivalTime()) {
                currentTime = p.getArrivalTime();
            }
            p.setStartTime(currentTime);
            p.setResponseTime(p.getStartTime() - p.getArrivalTime());
            currentTime += p.getBurstTime();
            p.setRemainingTime(0);
            p.setCompletionTime(currentTime);
            p.setWaitingTime(p.getStartTime() - p.getArrivalTime());
            p.setTurnaroundTime(p.getCompletionTime() - p.getArrivalTime());
            result.add(p);
        }

        contextSwitchCount = Math.max(0, result.size() - 1);
        return result;
    }

    public boolean cancel(Queue<Process> queue, String processId) {
        return queue.removeIf(p -> p.getId().equals(processId));
    }

    public int getContextSwitchCount() {
        return contextSwitchCount;
    }
}
