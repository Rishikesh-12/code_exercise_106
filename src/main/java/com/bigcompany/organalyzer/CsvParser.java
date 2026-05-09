package com.bigcompany.organalyzer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses an employee CSV file into a list of {@link Employee} objects.
 *
 * <p>Expected file format (header required, columns in this exact order):</p>
 * <pre>
 *   Id,firstName,lastName,salary,managerId
 *   123,Joe,Doe,60000,
 *   124,Martin,Chekov,45000,123
 * </pre>
 *
 * <p><b>Assumptions:</b></p>
 * <ul>
 *   <li>The header row is always present and is skipped.</li>
 *   <li>The CEO row has an empty {@code managerId} field (trailing comma is fine).</li>
 *   <li>Blank lines are ignored.</li>
 *   <li>Leading/trailing whitespace around field values is trimmed.</li>
 * </ul>
 */
public class CsvParser {

    /**
     * Parses the CSV file at the given path and returns one {@link Employee} per data row.
     *
     * @param filePath path to the CSV file
     * @return list of employees in file order
     * @throws IOException              if the file cannot be read
     * @throws NumberFormatException    if an {@code id}, {@code salary}, or {@code managerId}
     *                                  field cannot be parsed as a number
     */
    public static List<Employee> parse(String filePath) throws IOException {
        List<Employee> employees = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            reader.readLine(); // skip header
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                // limit -1 preserves the empty trailing field for the CEO's managerId
                String[] parts = line.split(",", -1);
                int id = Integer.parseInt(parts[0].trim());
                String firstName = parts[1].trim();
                String lastName = parts[2].trim();
                double salary = Double.parseDouble(parts[3].trim());
                String managerPart = parts[4].trim();
                Integer managerId = managerPart.isEmpty() ? null : Integer.parseInt(managerPart);
                employees.add(new Employee(id, firstName, lastName, salary, managerId));
            }
        }
        return employees;
    }
}
