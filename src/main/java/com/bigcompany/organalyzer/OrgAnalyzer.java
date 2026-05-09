package com.bigcompany.organalyzer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Analyses an organisational structure and identifies two categories of issue:
 *
 * <ol>
 *   <li><b>Salary compliance</b> – every manager must earn between 20 % and 50 % more
 *       than the average salary of their <em>direct</em> subordinates.</li>
 *   <li><b>Reporting-line length</b> – no employee should have more than 4 managers
 *       between themselves and the CEO.</li>
 * </ol>
 *
 * <p>Construct an instance with the full employee list, then call the three
 * {@code find*()} methods to obtain the relevant issues.</p>
 *
 * <p><b>Assumption – reporting-line definition:</b> "managers between an employee
 * and the CEO" counts every node in the chain <em>excluding</em> both the employee
 * and the CEO themselves. An employee who reports directly to the CEO therefore has
 * 0 managers between them and the CEO. Employees at depth &gt; 5 from the CEO
 * (depth 0) are flagged; the excess is {@code depth − 5}.</p>
 */
public class OrgAnalyzer {

    /** Minimum ratio of manager salary to average subordinate salary (inclusive). */
    private static final double MIN_RATIO = 1.20;

    /** Maximum ratio of manager salary to average subordinate salary (inclusive). */
    private static final double MAX_RATIO = 1.50;

    /** Maximum number of managers allowed between an employee and the CEO. */
    private static final int MAX_MANAGERS_BETWEEN = 4;

    /** All employees keyed by their ID for O(1) look-up. */
    private final Map<Integer, Employee> employeeById;

    /** Direct subordinates grouped by their manager's ID. */
    private final Map<Integer, List<Employee>> subordinatesByManagerId;

    /**
     * Builds the internal lookup structures from the supplied employee list.
     *
     * @param employees full list of employees (CEO included); must not be {@code null}
     */
    public OrgAnalyzer(List<Employee> employees) {
        employeeById = employees.stream().collect(Collectors.toMap(Employee::id, e -> e));
        subordinatesByManagerId = employees.stream()
                .filter(e -> e.managerId() != null)
                .collect(Collectors.groupingBy(Employee::managerId));
    }

    /**
     * Returns every manager whose salary is below the required minimum —
     * i.e. less than 120 % of the average salary of their direct subordinates.
     *
     * @return list of {@link SalaryIssue} records, one per underpaid manager;
     *         empty if all managers are within range
     */
    public List<SalaryIssue> findUnderpaidManagers() {
        List<SalaryIssue> issues = new ArrayList<>();
        for (Map.Entry<Integer, List<Employee>> entry : subordinatesByManagerId.entrySet()) {
            Employee manager = employeeById.get(entry.getKey());
            if (manager == null) continue;
            double avgSubSalary = averageSalary(entry.getValue());
            double minRequired = avgSubSalary * MIN_RATIO;
            if (manager.salary() < minRequired) {
                issues.add(new SalaryIssue(manager, minRequired - manager.salary()));
            }
        }
        return issues;
    }

    /**
     * Returns every manager whose salary exceeds the allowed maximum —
     * i.e. more than 150 % of the average salary of their direct subordinates.
     *
     * @return list of {@link SalaryIssue} records, one per overpaid manager;
     *         empty if all managers are within range
     */
    public List<SalaryIssue> findOverpaidManagers() {
        List<SalaryIssue> issues = new ArrayList<>();
        for (Map.Entry<Integer, List<Employee>> entry : subordinatesByManagerId.entrySet()) {
            Employee manager = employeeById.get(entry.getKey());
            if (manager == null) continue;
            double avgSubSalary = averageSalary(entry.getValue());
            double maxAllowed = avgSubSalary * MAX_RATIO;
            if (manager.salary() > maxAllowed) {
                issues.add(new SalaryIssue(manager, manager.salary() - maxAllowed));
            }
        }
        return issues;
    }

    /**
     * Returns every employee whose reporting line is too long — i.e. there are
     * more than 4 managers between them and the CEO.
     *
     * @return list of {@link ReportingLineIssue} records, one per affected employee;
     *         empty if every reporting line is within the allowed length
     */
    public List<ReportingLineIssue> findLongReportingLines() {
        List<ReportingLineIssue> issues = new ArrayList<>();
        for (Employee employee : employeeById.values()) {
            if (employee.managerId() == null) continue; // CEO has no managers above them
            int managersAbove = countManagersAbove(employee);
            if (managersAbove > MAX_MANAGERS_BETWEEN) {
                issues.add(new ReportingLineIssue(employee, managersAbove - MAX_MANAGERS_BETWEEN));
            }
        }
        return issues;
    }

    /**
     * Walks up the hierarchy from {@code employee} to the CEO and counts the number
     * of intermediate managers (i.e. excluding the employee and the CEO themselves).
     *
     * <p>Example: employee → mgr1 → mgr2 → CEO returns 1 (only mgr1 is "between").</p>
     *
     * @param employee the employee to start from
     * @return number of managers between {@code employee} and the CEO
     */
    private int countManagersAbove(Employee employee) {
        int count = 0;
        Employee current = employee;
        while (current.managerId() != null) {
            current = employeeById.get(current.managerId());
            if (current == null) break; // guard against malformed data
            count++;
        }
        // count == depth from CEO (e.g. 1 means direct report → 0 managers between)
        return count - 1;
    }

    /**
     * Computes the arithmetic mean of the salaries in {@code employees}.
     *
     * @param employees non-empty list of employees
     * @return average salary, or 0 if the list is empty
     */
    private double averageSalary(List<Employee> employees) {
        return employees.stream().mapToDouble(Employee::salary).average().orElse(0);
    }

    // -------------------------------------------------------------------------
    // Result types
    // -------------------------------------------------------------------------

    /**
     * Describes a salary-compliance violation for a single manager.
     *
     * @param manager    the manager with the violation
     * @param difference the absolute amount by which the salary falls below the minimum
     *                   or exceeds the maximum (always positive)
     */
    public record SalaryIssue(Employee manager, double difference) {}

    /**
     * Describes a reporting-line violation for a single employee.
     *
     * @param employee the employee whose reporting line is too long
     * @param excess   the number of extra managers beyond the allowed maximum of 4
     */
    public record ReportingLineIssue(Employee employee, int excess) {}
}
