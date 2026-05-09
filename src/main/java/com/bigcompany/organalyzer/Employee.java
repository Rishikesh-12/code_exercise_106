package com.bigcompany.organalyzer;

/**
 * Represents a single employee in the organisation.
 *
 * <p>The CEO is identified by a {@code null} {@code managerId}. All other employees
 * must reference a valid manager ID that exists elsewhere in the data set.</p>
 *
 * @param id         unique employee identifier
 * @param firstName  employee's first name
 * @param lastName   employee's last name
 * @param salary     annual gross salary
 * @param managerId  ID of the direct manager, or {@code null} for the CEO
 */
public record Employee(int id, String firstName, String lastName, double salary, Integer managerId) {

    /**
     * Returns the employee's full name as {@code "firstName lastName"}.
     *
     * @return full name
     */
    public String fullName() {
        return firstName + " " + lastName;
    }
}
