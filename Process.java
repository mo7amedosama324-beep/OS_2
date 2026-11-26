import java.util.Objects;

/**
 * A process definition and the per-run state used by the scheduler.
 */
public class Process {
    private final String name;
    private final int arrivalTime;
    private final int burstTime;
    private final int priority;
    private final int inputOrder;

    int remainingTime;
    int finishTime = -1;
    int firstRunTime = -1;
    int timeEnteredTier2 = -1;

    public Process(String name, int arrivalTime, int burstTime, int priority) {
        this(name, arrivalTime, burstTime, priority, -1);
    }

    Process(String name, int arrivalTime, int burstTime, int priority, int inputOrder) {
        this.name = Objects.requireNonNull(name, "name").trim();
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.priority = priority;
        this.inputOrder = inputOrder;
        this.remainingTime = burstTime;
    }

    Process forSimulation(int order) {
        return new Process(name, arrivalTime, burstTime, priority, order);
    }

    public String getName() {
        return name;
    }

    public int getArrivalTime() {
        return arrivalTime;
    }

    public int getBurstTime() {
        return burstTime;
    }

    public int getPriority() {
        return priority;
    }

    int getInputOrder() {
        return inputOrder;
    }

    @Override
    public String toString() {
        return name + " (arrival=" + arrivalTime + ", burst=" + burstTime
                + ", priority=" + priority + ")";
    }
}
