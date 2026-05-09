package com.bigcompany.organalyzer;

import java.io.IOException;
import java.util.List;

/**
 * Entry point for the organisational-structure analyser.
 *
 * <p>Reads employee data from a CSV file supplied as the first command-line argument,
 * runs the three analyses defined in {@link OrgAnalyzer}, and prints a human-readable
 * report to standard output.</p>
 *
 * <p><b>Usage:</b></p>
 * <pre>
 *   java -jar org-analyzer.jar &lt;path-to-employees.csv&gt;
 * </pre>
 *
 * <p><b>Sample output:</b></p>
 * <pre>
 *   === Managers earning less than they should ===
 *     Martin Chekov earns 15000.00 less than the required minimum
 *
 *   === Managers earning more than they should ===
 *     None
 *
 *   === Employees with reporting lines that are too long ===
 *     None
 * </pre>
 */
public class Main {

    /**
     * Application entry point.
     *
     * @param args command-line arguments; {@code args[0]} must be the path to the CSV file
     * @throws IOException if the CSV file cannot be read
     */
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: java -jar org-analyzer.jar <path-to-employees-csv>");
            System.exit(1);
        }

        System.out.println("\n===== ORGANIZATIONAL STRUCTURE ANALYSIS =====\n");

        List<Employee> employees = CsvParser.parse(args[0]);
        System.out.printf("Successfully loaded %d employees.\n\n", employees.size());
        OrgAnalyzer analyzer = new OrgAnalyzer(employees);        

        printUnderpaidManagers(analyzer.findUnderpaidManagers());
        printOverpaidManagers(analyzer.findOverpaidManagers());
        printLongReportingLines(analyzer.findLongReportingLines());

        System.out.println("\n===== ANALYSIS COMPLETE =====\n");
    }

    /**
     * Prints managers whose salary is below the required minimum (120 % of subordinate average).
     *
     * @param issues underpaid managers identified by {@link OrgAnalyzer#findUnderpaidManagers()}
     */
    private static void printUnderpaidManagers(List<OrgAnalyzer.SalaryIssue> issues) {
        System.out.println("=== Managers earning less than they should ===");
        if (issues.isEmpty()) {
            System.out.println("None");
        } else {
            issues.forEach(i -> System.out.printf("-  %s earns %.2f less than the required minimum%n",
                    i.manager().fullName(), i.difference()));
        }
        System.out.println();
    }

    /**
     * Prints managers whose salary exceeds the allowed maximum (150 % of subordinate average).
     *
     * @param issues overpaid managers identified by {@link OrgAnalyzer#findOverpaidManagers()}
     */
    private static void printOverpaidManagers(List<OrgAnalyzer.SalaryIssue> issues) {
        System.out.println("=== Managers earning more than they should ===");
        if (issues.isEmpty()) {
            System.out.println("None");
        } else {
            issues.forEach(i -> System.out.printf("-  %s earns %.2f more than the allowed maximum%n",
                    i.manager().fullName(), i.difference()));
        }
        System.out.println();
    }

    /**
     * Prints employees who have more than 4 managers between themselves and the CEO.
     *
     * @param issues affected employees identified by {@link OrgAnalyzer#findLongReportingLines()}
     */
    private static void printLongReportingLines(List<OrgAnalyzer.ReportingLineIssue> issues) {
        System.out.println("=== Employees with reporting lines that are too long ===");
        if (issues.isEmpty()) {
            System.out.println("None");
        } else {
            issues.forEach(i -> System.out.printf("-  %s has a reporting line too long by %d manager(s)%n",
                    i.employee().fullName(), i.excess()));
        }
        System.out.println();
    }
}
