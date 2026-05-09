package com.bigcompany.organalyzer;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrgAnalyzerTest {

    /**
     * Sample data from the assignment:
     *   Joe (CEO, 60000) → Martin (45000), Bob (47000)
     *   Martin → Alice (50000)
     *   Alice → Brett (34000)
     *
     * Expected salary analysis:
     *   Joe:    avg subordinates = (45000+47000)/2 = 46000 → range [55200, 69000] → 60000 is OK
     *   Martin: avg subordinates = 50000             → range [60000, 75000] → 45000 is UNDERPAID by 15000
     *   Alice:  avg subordinates = 34000             → range [40800, 51000] → 50000 is OK
     *
     * Expected reporting lines (depth from CEO, managers-between = depth-1):
     *   Joe=0, Martin/Bob=1(0), Alice=2(1), Brett=3(2) → all within limit of 4
     */
    private static List<Employee> sampleData() {
        return Arrays.asList(
                new Employee(123, "Joe", "Doe", 60000, null),
                new Employee(124, "Martin", "Chekov", 45000, 123),
                new Employee(125, "Bob", "Ronstad", 47000, 123),
                new Employee(300, "Alice", "Hasacat", 50000, 124),
                new Employee(305, "Brett", "Hardleaf", 34000, 300)
        );
    }

    @Test
    void findsUnderpaidManager() {
        OrgAnalyzer analyzer = new OrgAnalyzer(sampleData());
        List<OrgAnalyzer.SalaryIssue> issues = analyzer.findUnderpaidManagers();

        assertEquals(1, issues.size());
        assertEquals("Martin Chekov", issues.get(0).manager().fullName());
        assertEquals(15000.0, issues.get(0).difference(), 0.01);
    }

    @Test
    void noUnderpaidManagersWhenAllInRange() {
        List<Employee> employees = Arrays.asList(
                new Employee(1, "CEO", "Boss", 200000, null),
                new Employee(2, "Mid", "Manager", 60000, 1),
                new Employee(3, "Sub", "One", 50000, 2)
        );
        // Manager 2: avg subordinate = 50000, min = 60000, max = 75000 → 60000 is exactly at min → OK
        OrgAnalyzer analyzer = new OrgAnalyzer(employees);
        assertTrue(analyzer.findUnderpaidManagers().isEmpty());
    }

    @Test
    void noOverpaidManagersInSampleData() {
        OrgAnalyzer analyzer = new OrgAnalyzer(sampleData());
        assertTrue(analyzer.findOverpaidManagers().isEmpty());
    }

    @Test
    void findsOverpaidManager() {
        List<Employee> employees = Arrays.asList(
                new Employee(1, "CEO", "Boss", 130000, null),  // 130000 is within [120000, 150000] of avg 100000
                new Employee(2, "Over", "Paid", 100000, 1),
                new Employee(3, "Sub", "One", 10000, 2),
                new Employee(4, "Sub", "Two", 10000, 2)
        );
        // Manager 2: avg subordinate = 10000, max = 15000 → 100000 is OVERPAID by 85000
        OrgAnalyzer analyzer = new OrgAnalyzer(employees);
        List<OrgAnalyzer.SalaryIssue> issues = analyzer.findOverpaidManagers();

        assertEquals(1, issues.size());
        assertEquals("Over Paid", issues.get(0).manager().fullName());
        assertEquals(85000.0, issues.get(0).difference(), 0.01);
    }

    @Test
    void noLongReportingLinesInSampleData() {
        // Deepest employee (Brett) has 2 managers above → well within limit of 4
        OrgAnalyzer analyzer = new OrgAnalyzer(sampleData());
        assertTrue(analyzer.findLongReportingLines().isEmpty());
    }

    @Test
    void employeeAtExactlyFourManagersIsNotFlagged() {
        // Chain: CEO(0) → A(1) → B(2) → C(3) → D(4) → Employee(5)
        // managersAbove = 4 → exactly at limit, not flagged
        List<Employee> employees = Arrays.asList(
                new Employee(1, "CEO", "Boss", 200000, null),
                new Employee(2, "A", "A", 160000, 1),
                new Employee(3, "B", "B", 120000, 2),
                new Employee(4, "C", "C", 90000, 3),
                new Employee(5, "D", "D", 70000, 4),
                new Employee(6, "Leaf", "Employee", 50000, 5)
        );
        OrgAnalyzer analyzer = new OrgAnalyzer(employees);
        assertTrue(analyzer.findLongReportingLines().isEmpty());
    }

    @Test
    void findsEmployeeWithTooLongReportingLine() {
        // Chain: CEO(0) → A(1) → B(2) → C(3) → D(4) → E(5) → TooDeep(6)
        // managersAbove for TooDeep = 5 → exceeds limit by 1
        List<Employee> employees = new ArrayList<>(Arrays.asList(
                new Employee(1, "CEO", "Boss", 200000, null),
                new Employee(2, "A", "A", 160000, 1),
                new Employee(3, "B", "B", 120000, 2),
                new Employee(4, "C", "C", 90000, 3),
                new Employee(5, "D", "D", 70000, 4),
                new Employee(6, "E", "E", 50000, 5),
                new Employee(7, "Too", "Deep", 40000, 6)
        ));
        OrgAnalyzer analyzer = new OrgAnalyzer(employees);
        List<OrgAnalyzer.ReportingLineIssue> issues = analyzer.findLongReportingLines();

        assertEquals(1, issues.size());
        assertEquals("Too Deep", issues.get(0).employee().fullName());
        assertEquals(1, issues.get(0).excess());
    }

    @Test
    void multipleEmployeesWithLongReportingLines() {
        List<Employee> employees = new ArrayList<>(Arrays.asList(
                new Employee(1, "CEO", "Boss", 200000, null),
                new Employee(2, "A", "A", 160000, 1),
                new Employee(3, "B", "B", 120000, 2),
                new Employee(4, "C", "C", 90000, 3),
                new Employee(5, "D", "D", 70000, 4),
                new Employee(6, "E", "E", 50000, 5),
                new Employee(7, "Too", "Deep", 40000, 6),   // excess 1
                new Employee(8, "Even", "Deeper", 30000, 7) // excess 2
        ));
        OrgAnalyzer analyzer = new OrgAnalyzer(employees);
        List<OrgAnalyzer.ReportingLineIssue> issues = analyzer.findLongReportingLines();

        assertEquals(2, issues.size());
        assertTrue(issues.stream().anyMatch(i -> i.employee().fullName().equals("Too Deep") && i.excess() == 1));
        assertTrue(issues.stream().anyMatch(i -> i.employee().fullName().equals("Even Deeper") && i.excess() == 2));
    }
}
