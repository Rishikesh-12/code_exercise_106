# org-analyzer

A command-line tool that reads an employee CSV file and reports organisational-structure issues:

1. **Underpaid managers** – salary is less than 120 % of their direct subordinates' average
2. **Overpaid managers** – salary is more than 150 % of their direct subordinates' average
3. **Long reporting lines** – more than 4 managers between an employee and the CEO

---

## Requirements

| Tool | Version |
|------|---------|
| Java | 17+     |
| Maven | 3.6+   |

---

## Project structure

```
org-analyzer/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/bigcompany/organalyzer/
    │   │   ├── Main.java          # entry point
    │   │   ├── Employee.java      # data record
    │   │   ├── CsvParser.java     # CSV → Employee list
    │   │   └── OrgAnalyzer.java   # analysis logic + result types
    │   └── resources/
    │       └── employees.csv      # sample data
    └── test/
        └── java/com/bigcompany/organalyzer/
            └── OrgAnalyzerTest.java
```

---

## Build

```bash
mvn clean package
```

This compiles the project and produces `target/org-analyzer-1.0.0.jar`.


## Test

```bash
mvn test
```
To run the unit tests

## Run

```bash
java -jar target/org-analyzer-1.0.0.jar <path-to-employees.csv>
```

**Example using the bundled sample file:**

```bash
java -jar target/org-analyzer-1.0.0.jar src/main/resources/employees.csv
```

**Sample output:**

```
===== ORGANIZATIONAL STRUCTURE ANALYSIS =====

Successfully loaded 5 employees.

=== Managers earning less than they should ===
- Martin Chekov earns 15000.00 less than the required minimum

=== Managers earning more than they should ===
None

=== Employees with reporting lines that are too long ===
None


===== ANALYSIS COMPLETE=====
```

---

## CSV format

```
Id,firstName,lastName,salary,managerId
123,Joe,Doe,60000,
124,Martin,Chekov,45000,123
125,Bob,Ronstad,47000,123
300,Alice,Hasacat,50000,124
305,Brett,Hardleaf,34000,300
```

| Column      | Type    | Notes                                      |
|-------------|---------|--------------------------------------------|
| `Id`        | integer | unique employee identifier                 |
| `firstName` | string  |                                            |
| `lastName`  | string  |                                            |
| `salary`    | number  | annual gross salary                        |
| `managerId` | integer | ID of direct manager; **empty for CEO**    |

- The header row must always be present.
- Up to 1 000 data rows are supported.
- Blank lines are ignored.

---

## Business rules

### Salary compliance

For every manager (any employee with at least one direct subordinate):

```
min_required = avg(direct subordinate salaries) × 1.20
max_allowed  = avg(direct subordinate salaries) × 1.50
```

- If `manager.salary < min_required` → **underpaid** by `min_required − manager.salary`
- If `manager.salary > max_allowed`  → **overpaid**  by `manager.salary − max_allowed`

### Reporting-line length

"Managers between an employee and the CEO" counts every node in the chain **excluding** both the employee and the CEO themselves.

| Depth from CEO | Managers between | Status        |
|----------------|-----------------|---------------|
| 1 (direct)     | 0               | OK            |
| 5              | 4               | OK (at limit) |
| 6              | 5               | Too long by 1 |

---

## Tests

```bash
mvn test
```

The test suite (`OrgAnalyzerTest`) covers:

| Test | Scenario |
|------|----------|
| `findsUnderpaidManager` | Martin Chekov is underpaid by 15 000 in sample data |
| `noUnderpaidManagersWhenAllInRange` | Manager exactly at 120 % boundary is not flagged |
| `noOverpaidManagersInSampleData` | No overpaid managers in sample data |
| `findsOverpaidManager` | Manager earning far above 150 % limit is flagged |
| `noLongReportingLinesInSampleData` | All employees in sample data are within depth limit |
| `employeeAtExactlyFourManagersIsNotFlagged` | Depth-5 employee (4 managers above) is not flagged |
| `findsEmployeeWithTooLongReportingLine` | Depth-6 employee is flagged with excess = 1 |
| `multipleEmployeesWithLongReportingLines` | Two employees flagged with excess 1 and 2 respectively |

---

## Assumptions

- The CSV header is always present and columns are in the documented order.
- An employee with an empty `managerId` is treated as a CEO/root node.
- If multiple employees have no `managerId`, each is treated as an independent organizational root.
- An employee is considered a manager if they have at least one direct subordinate.
- "Managers between employee and CEO" excludes both endpoints (see table above).
- Salary values are treated as plain numbers (no currency conversion).
- Invalid or missing manager references terminate hierarchy traversal safely.
