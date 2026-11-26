import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Swing front end for the CPU scheduling simulator.
 */
public class SchedulerGUI extends JFrame {
    private static final Color PAGE_BACKGROUND = new Color(242, 246, 251);
    private static final Color CARD_BACKGROUND = Color.WHITE;
    private static final Color NAVY = new Color(24, 39, 67);
    private static final Color TEXT_COLOR = new Color(42, 55, 75);
    private static final Color MUTED_TEXT = new Color(113, 127, 148);
    private static final Color ACCENT = new Color(30, 145, 130);
    private static final Color BORDER_COLOR = new Color(222, 229, 238);

    private final SchedulerEngine engine = new SchedulerEngine();
    private final List<Process> processes = new ArrayList<Process>();
    private final JTextField nameField = new JTextField("P1", 5);
    private final JTextField arrivalField = new JTextField("0", 4);
    private final JTextField burstField = new JTextField("1", 4);
    private final JTextField priorityField = new JTextField("1", 4);
    private final JTextField quantumField = new JTextField("3", 4);
    private final JTextField contextSwitchField = new JTextField("0", 4);
    private final JTextArea logArea = new JTextArea();
    private final GanttChart ganttChart = new GanttChart();
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Process", "Arrival Time", "Burst Time", "Finish Time",
                    "Turnaround Time (Finish - Arrival)",
                    "Waiting Time (Turnaround - Burst)",
                    "Response Time (First_Run - Arrival)"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JLabel averagesLabel = new JLabel("Average Turnaround Time: —"
            + "     Average Waiting Time: —");

    public SchedulerGUI() {
        super("CPU Scheduling Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 720));
        setSize(1200, 850);
        setLocationRelativeTo(null);
        getContentPane().setBackground(PAGE_BACKGROUND);
        setLayout(new BorderLayout(14, 14));
        ((javax.swing.JComponent) getContentPane()).setBorder(
                BorderFactory.createEmptyBorder(14, 16, 14, 16));
        add(createTopPanel(), BorderLayout.NORTH);
        add(createCenterPanel(), BorderLayout.CENTER);
        add(createMetricsPanel(), BorderLayout.SOUTH);
        updateProcessList();
    }

    private JPanel createTopPanel() {
        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setOpaque(false);
        top.add(createHeader(), BorderLayout.NORTH);
        top.add(createControls(), BorderLayout.CENTER);
        return top;
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(NAVY);
        header.setBorder(BorderFactory.createEmptyBorder(16, 22, 16, 22));

        JLabel title = new JLabel("CPU SCHEDULER");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        JLabel subtitle = new JLabel("Visualize scheduling algorithms, queues, and process metrics");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(new Color(190, 204, 223));

        JPanel heading = new JPanel(new GridLayout(2, 1, 0, 3));
        heading.setOpaque(false);
        heading.add(title);
        heading.add(subtitle);
        header.add(heading, BorderLayout.WEST);

        JLabel badge = new JLabel("SIMULATION LAB");
        badge.setFont(new Font("Segoe UI", Font.BOLD, 10));
        badge.setForeground(new Color(187, 241, 228));
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(75, 120, 132)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        header.add(badge, BorderLayout.EAST);
        return header;
    }

    private JPanel createControls() {
        JPanel outer = new JPanel(new BorderLayout(0, 10));
        outer.setOpaque(false);

        JPanel presets = createCardPanel("Quick start");
        JPanel presetButtons = new JPanel(new GridLayout(1, 3, 8, 0));
        presetButtons.setOpaque(false);
        JButton preset1 = new JButton("Test Case 1 (Q=3, CS=0)");
        styleButton(preset1, new Color(230, 244, 241), new Color(27, 105, 96));
        preset1.addActionListener(event -> loadPresetOne());
        presetButtons.add(preset1);
        JButton preset2 = new JButton("Test Case 2 (Q=3, CS=1)");
        styleButton(preset2, new Color(230, 238, 250), new Color(47, 86, 145));
        preset2.addActionListener(event -> loadPresetTwo());
        presetButtons.add(preset2);
        JButton clear = new JButton("Clear / Custom Input");
        styleButton(clear, new Color(245, 247, 250), TEXT_COLOR);
        clear.addActionListener(event -> clearInput());
        presetButtons.add(clear);
        presets.add(presetButtons, BorderLayout.CENTER);

        JPanel inputCard = createCardPanel("Add a process");
        JPanel input = new JPanel(new GridBagLayout());
        input.setOpaque(false);
        addField(input, "Name", nameField, 0, 0);
        addField(input, "Arrival", arrivalField, 2, 0);
        addField(input, "Burst", burstField, 4, 0);
        addField(input, "Priority", priorityField, 6, 0);
        JButton addProcess = new JButton("Add Process");
        styleButton(addProcess, ACCENT, Color.WHITE);
        addProcess.addActionListener(event -> addProcess());
        addComponent(input, addProcess, 8, 0);
        addField(input, "Quantum (Q)", quantumField, 0, 1);
        addField(input, "Context Switch (CS)", contextSwitchField, 3, 1);

        JButton sjf = new JButton("Run Preemptive SJF");
        styleButton(sjf, new Color(230, 238, 250), new Color(47, 86, 145));
        sjf.addActionListener(event -> runSimulation(false));
        addComponent(input, sjf, 6, 1);
        JButton acs = new JButton("Run ACS");
        styleButton(acs, ACCENT, Color.WHITE);
        acs.addActionListener(event -> runSimulation(true));
        addComponent(input, acs, 8, 1);
        inputCard.add(input, BorderLayout.CENTER);

        JPanel topCards = new JPanel(new GridLayout(1, 2, 10, 0));
        topCards.setOpaque(false);
        topCards.add(presets);
        topCards.add(inputCard);
        outer.add(topCards, BorderLayout.NORTH);

        JPanel processList = createCardPanel("Loaded processes");
        processSummary.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        processSummary.setForeground(TEXT_COLOR);
        processList.add(processSummary, BorderLayout.CENTER);
        outer.add(processList, BorderLayout.CENTER);
        return outer;
    }

    private final JLabel processSummary = new JLabel("No processes");

    private static JPanel createCardPanel(String title) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(CARD_BACKGROUND);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        JLabel heading = new JLabel(title);
        heading.setFont(new Font("Segoe UI", Font.BOLD, 12));
        heading.setForeground(TEXT_COLOR);
        card.add(heading, BorderLayout.NORTH);
        return card;
    }

    private static void styleButton(JButton button, Color background, Color foreground) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 11));
        button.setForeground(foreground);
        button.setBackground(background);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(background.equals(ACCENT)
                        ? ACCENT : BORDER_COLOR),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }

    private static void addField(JPanel panel, String label, JTextField field, int x, int y) {
        JLabel fieldLabel = new JLabel(label);
        fieldLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        fieldLabel.setForeground(MUTED_TEXT);
        addComponent(panel, fieldLabel, x, y);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        field.setForeground(TEXT_COLOR);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(5, 6, 5, 6)));
        addComponent(panel, field, x + 1, y);
    }

    private static void addComponent(JPanel panel, java.awt.Component component, int x, int y) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = x;
        constraints.gridy = y;
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        if (x == 8) {
            constraints.weightx = 0.6;
        }
        panel.add(component, constraints);
    }

    private JPanel createCenterPanel() {
        JPanel chartPanel = createCardPanel("Execution timeline");
        JScrollPane chartScroll = new JScrollPane(ganttChart);
        chartScroll.setBorder(BorderFactory.createEmptyBorder());
        chartScroll.getViewport().setBackground(Color.WHITE);
        chartPanel.add(chartScroll, BorderLayout.CENTER);

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        logArea.setLineWrap(false);
        logArea.setBackground(new Color(26, 37, 57));
        logArea.setForeground(new Color(220, 232, 245));
        logArea.setCaretColor(Color.WHITE);
        logArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createEmptyBorder());
        JPanel logPanel = createCardPanel("Execution log");
        logPanel.add(logScroll, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, chartPanel, logPanel);
        split.setResizeWeight(0.56);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setDividerSize(8);
        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);
        center.add(split, BorderLayout.CENTER);
        return center;
    }

    private JPanel createMetricsPanel() {
        JPanel panel = createCardPanel("Process metrics");
        JTable table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        table.setForeground(TEXT_COLOR);
        table.setRowHeight(25);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(224, 242, 238));
        table.setSelectionForeground(TEXT_COLOR);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 10));
        table.getTableHeader().setForeground(MUTED_TEXT);
        table.getTableHeader().setBackground(new Color(246, 248, 251));
        table.getTableHeader().setPreferredSize(new Dimension(0, 30));
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable source, Object value,
                    boolean selected, boolean focused, int row, int column) {
                Component cell = super.getTableCellRendererComponent(source, value, selected,
                        focused, row, column);
                if (!selected) {
                    cell.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 253));
                }
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return cell;
            }
        };
        for (int column = 0; column < table.getColumnCount(); column++) {
            table.getColumnModel().getColumn(column).setCellRenderer(cellRenderer);
        }
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        panel.add(tableScroll, BorderLayout.CENTER);
        averagesLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        averagesLabel.setForeground(new Color(27, 105, 96));
        averagesLabel.setBorder(BorderFactory.createEmptyBorder(5, 4, 0, 4));
        panel.add(averagesLabel, BorderLayout.SOUTH);
        panel.setPreferredSize(new Dimension(1000, 205));
        return panel;
    }

    private void loadPresetOne() {
        processes.clear();
        processes.add(new Process("P1", 0, 4, 2));
        processes.add(new Process("P2", 1, 7, 3));
        processes.add(new Process("P3", 3, 2, 1));
        quantumField.setText("3");
        contextSwitchField.setText("0");
        updateProcessList();
    }

    private void loadPresetTwo() {
        processes.clear();
        processes.add(new Process("P1", 0, 5, 4));
        processes.add(new Process("P2", 1, 6, 2));
        processes.add(new Process("P3", 4, 4, 3));
        quantumField.setText("3");
        contextSwitchField.setText("1");
        updateProcessList();
    }

    private void clearInput() {
        processes.clear();
        nameField.setText("P1");
        arrivalField.setText("0");
        burstField.setText("1");
        priorityField.setText("1");
        quantumField.setText("3");
        contextSwitchField.setText("0");
        tableModel.setRowCount(0);
        averagesLabel.setText("Average Turnaround Time: —     Average Waiting Time: —");
        ganttChart.setResult(null);
        logArea.setText("");
        updateProcessList();
    }

    private void addProcess() {
        try {
            String name = nameField.getText().trim();
            int arrival = Integer.parseInt(arrivalField.getText().trim());
            int burst = Integer.parseInt(burstField.getText().trim());
            int priority = Integer.parseInt(priorityField.getText().trim());
            if (name.isEmpty() || arrival < 0 || burst <= 0 || priority < 1) {
                throw new IllegalArgumentException(
                        "Enter a name, arrival >= 0, burst > 0, and priority >= 1.");
            }
            for (Process process : processes) {
                if (process.getName().equalsIgnoreCase(name)) {
                    throw new IllegalArgumentException("Process names must be unique.");
                }
            }
            processes.add(new Process(name, arrival, burst, priority));
            nameField.setText("P" + (processes.size() + 1));
            updateProcessList();
        } catch (NumberFormatException exception) {
            showError("Arrival, burst, and priority must be whole numbers.");
        } catch (IllegalArgumentException exception) {
            showError(exception.getMessage());
        }
    }

    private void updateProcessList() {
        if (processes.isEmpty()) {
            processSummary.setText("No processes");
            return;
        }
        StringBuilder summary = new StringBuilder();
        for (int i = 0; i < processes.size(); i++) {
            if (i > 0) {
                summary.append("   |   ");
            }
            summary.append(processes.get(i));
        }
        processSummary.setText("<html>" + escapeHtml(summary.toString())
                .replace("   |   ", " &nbsp;&nbsp;|&nbsp;&nbsp; ") + "</html>");
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void runSimulation(boolean acs) {
        try {
            int quantum = Integer.parseInt(quantumField.getText().trim());
            int contextSwitch = Integer.parseInt(contextSwitchField.getText().trim());
            SimulationResult result = acs
                    ? engine.runAcs(processes, quantum, contextSwitch)
                    : engine.runPreemptiveSjf(processes, contextSwitch);
            displayResult(result, acs ? "ACS" : "Preemptive SJF");
        } catch (NumberFormatException exception) {
            showError("Quantum and context switch must be whole numbers.");
        } catch (IllegalArgumentException exception) {
            showError(exception.getMessage());
        }
    }

    private void displayResult(SimulationResult result, String algorithm) {
        tableModel.setRowCount(0);
        for (SimulationResult.ProcessMetrics row : result.getMetrics()) {
            tableModel.addRow(new Object[]{row.getProcessName(), row.getArrivalTime(),
                    row.getBurstTime(), row.getFinishTime(), row.getTurnaroundTime(),
                    row.getWaitingTime(), row.getResponseTime()});
        }
        DecimalFormat format = new DecimalFormat("0.00");
        averagesLabel.setText("Average Turnaround Time: "
                + format.format(result.getAverageTurnaroundTime())
                + "     Average Waiting Time: "
                + format.format(result.getAverageWaitingTime()));
        StringBuilder log = new StringBuilder(algorithm).append(" execution\n");
        for (String line : result.getLogLines()) {
            log.append(line).append('\n');
        }
        logArea.setText(log.toString());
        logArea.setCaretPosition(0);
        ganttChart.setResult(result);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Invalid input",
                JOptionPane.ERROR_MESSAGE);
    }

    private static final class GanttChart extends JPanel {
        private static final int BLOCK_WIDTH = 52;
        private static final int LEFT_MARGIN = 18;
        private static final int CHART_TOP = 20;
        private static final int BLOCK_HEIGHT = 42;
        private SimulationResult result;

        GanttChart() {
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(650, 125));
        }

        void setResult(SimulationResult result) {
            this.result = result;
            int totalTime = result == null || result.getSegments().isEmpty() ? 0
                    : result.getSegments().get(result.getSegments().size() - 1).getEndTime();
            setPreferredSize(new Dimension(Math.max(650, LEFT_MARGIN
                    + totalTime * BLOCK_WIDTH + 24), 125));
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (result == null || result.getSegments().isEmpty()) {
                graphics.setColor(Color.GRAY);
                graphics.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                graphics.drawString("Run a simulation to display its schedule.", LEFT_MARGIN, 58);
                return;
            }

            graphics.setFont(new Font("Segoe UI", Font.BOLD, 11));
            for (SimulationResult.Segment segment : result.getSegments()) {
                int x = LEFT_MARGIN + segment.getStartTime() * BLOCK_WIDTH;
                int width = (segment.getEndTime() - segment.getStartTime()) * BLOCK_WIDTH;
                graphics.setColor(colorFor(segment.getLabel()));
                graphics.fillRoundRect(x, CHART_TOP, width - 2, BLOCK_HEIGHT, 10, 10);
                graphics.setColor(Color.DARK_GRAY);
                graphics.drawString(segment.getLabel(), x + Math.max(4, width / 2 - 12),
                        CHART_TOP + 25);
                graphics.setColor(MUTED_TEXT);
                graphics.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                graphics.drawString(String.valueOf(segment.getStartTime()), x,
                        CHART_TOP + BLOCK_HEIGHT + 20);
                graphics.setFont(new Font("Segoe UI", Font.BOLD, 11));
            }
            SimulationResult.Segment last =
                    result.getSegments().get(result.getSegments().size() - 1);
            graphics.setColor(MUTED_TEXT);
            graphics.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            graphics.drawString(String.valueOf(last.getEndTime()),
                    LEFT_MARGIN + last.getEndTime() * BLOCK_WIDTH - 5,
                    CHART_TOP + BLOCK_HEIGHT + 20);
        }

        private Color colorFor(String label) {
            if ("CS".equals(label)) {
                return new Color(231, 235, 241);
            }
            if ("IDLE".equals(label)) {
                return new Color(249, 239, 213);
            }
            Color[] processColors = {
                    new Color(213, 237, 232), new Color(220, 231, 248),
                    new Color(248, 230, 207), new Color(236, 224, 245),
                    new Color(249, 222, 226), new Color(222, 239, 215)
            };
            int index = (label.hashCode() & 0x7fffffff) % processColors.length;
            return processColors[index];
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SchedulerGUI().setVisible(true));
    }
}
