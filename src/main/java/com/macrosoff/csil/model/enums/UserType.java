/**
 * File:    UserType.java
 * Author:  Macrosoff Team
 * Date:    2026-04-06
 * Version: 1.1
 * Purpose: Enumeration of the account types that a registered user may hold.
 *          EMPLOYEE has been removed; the application now supports STUDENT
 *          and INSTRUCTOR roles only.
 */
package com.macrosoff.csil.model.enums;

/**
 * UserType
 * Defines the two roles recognised by the CS-IL application.
 * STUDENT  - standard learner; read-only access to problems.
 * INSTRUCTOR - elevated role; may manage problems via InstructorPanel.
 */
public enum UserType {
    STUDENT,
    INSTRUCTOR
}
