/**
 * File:    User.java
 * Author:  Macrosoff Team
 * Date:    2026-04-06
 * Version: 1.1
 * Purpose: Represents a registered user of the CS-IL application. Stores the
 *          username, SHA-256 password hash, account type, join date, and a
 *          Progress object. Provides login authentication by comparing the
 *          supplied plain-text password against the stored hash.
 *          EMPLOYEE account type has been removed; only STUDENT and INSTRUCTOR
 *          are supported.
 */
package com.macrosoff.csil.model;

import com.macrosoff.csil.data.SaveManager;
import com.macrosoff.csil.model.enums.UserType;

import java.util.Date;

/**
 * User
 * Domain class representing one registered account. Passwords are never
 * stored in plain text; they are hashed immediately on construction via
 * SaveManager.hashPassword(). The fromHash() factory is used when loading
 * records from disk where the hash is already known.
 *
 * All setter methods validate their input before assigning to attributes.
 */
public class User {

    // Auto-increment counter shared across all User instances in this session
    private static int nextUserIdentifier = 1;

    // Minimum acceptable length for a username
    private static final int MINIMUM_USERNAME_LENGTH = 1;

    // Unique numeric identifier assigned at construction
    private int userID;

    // The username chosen at registration — used as the login key
    private String userName;

    // Optional email address; stored as an empty string if not provided
    private String userEmail;

    // SHA-256 hex digest of the user's password — never plain text
    private String passwordHash;

    // The account role: STUDENT or INSTRUCTOR
    private UserType userType;

    // The date and time the account was created
    private Date dateJoined;

    // Tracks the user's completion percentage across the problem set
    private Progress progress;

    // ── Constructors ──────────────────────────────────────────────────────────

    /**
     * Constructs a User from a plain-text password.
     * The password is hashed immediately; the plain-text value is never stored.
     *
     * @param userName      the chosen username; stored as "unknown" if null or blank
     * @param plainPassword the plain-text password to hash
     * @param userType      the account role; defaults to STUDENT if null
     */
    public User(String userName, String plainPassword, UserType userType) {
        // Assign a unique identifier and advance the counter
        this.userID = nextUserIdentifier;
        nextUserIdentifier = nextUserIdentifier + 1;

        // Validate username
        if (userName != null && userName.length() >= MINIMUM_USERNAME_LENGTH) {
            this.userName = userName;
        } else {
            this.userName = "unknown";
        }

        // Hash the plain-text password immediately — guard against null input
        if (plainPassword != null) {
            this.passwordHash = SaveManager.hashPassword(plainPassword);
        } else {
            this.passwordHash = SaveManager.hashPassword("");
        }

        // Validate user type
        if (userType != null) {
            this.userType = userType;
        } else {
            this.userType = UserType.STUDENT;
        }

        // Initialise remaining fields to their starting values
        this.userEmail  = "";
        this.dateJoined = new Date();
        this.progress   = new Progress();
    }

    /**
     * Private no-arg constructor used only by the fromHash() factory.
     * Prevents callers from creating an uninitialised User.
     */
    private User() {
        this.userID       = nextUserIdentifier;
        nextUserIdentifier = nextUserIdentifier + 1;
        this.userName     = "unknown";
        this.passwordHash = "";
        this.userType     = UserType.STUDENT;
        this.userEmail    = "";
        this.dateJoined   = new Date();
        this.progress     = new Progress();
    }

    /**
     * Factory method that reconstructs a User from a previously saved password
     * hash. The hash is stored directly — it must NOT be hashed again.
     *
     * @param userName       the stored username
     * @param passwordHash   the already-hashed password string from disk
     * @param userType       the stored account role
     * @param joinedMillis   epoch-millisecond timestamp of account creation
     * @return a fully initialised User instance
     */
    public static User fromHash(String userName, String passwordHash,
                                UserType userType, long joinedMillis) {
        // Use the private constructor to avoid double-hashing
        User restoredUser = new User();

        // Validate and assign username
        if (userName != null && userName.length() >= MINIMUM_USERNAME_LENGTH) {
            restoredUser.userName = userName;
        } else {
            restoredUser.userName = "unknown";
        }

        // Store the hash directly — it was already produced by SaveManager
        if (passwordHash != null) {
            restoredUser.passwordHash = passwordHash;
        } else {
            restoredUser.passwordHash = "";
        }

        // Validate user type
        if (userType != null) {
            restoredUser.userType = userType;
        } else {
            restoredUser.userType = UserType.STUDENT;
        }

        // Restore the original join date if the timestamp is valid
        if (joinedMillis > 0) {
            restoredUser.dateJoined = new Date(joinedMillis);
        } else {
            restoredUser.dateJoined = new Date();
        }

        restoredUser.userEmail = "";
        restoredUser.progress  = new Progress();

        return restoredUser;
    }

    // ── Authentication ────────────────────────────────────────────────────────

    /**
     * Returns true when the supplied username and plain-text password match
     * this account's stored credentials.
     *
     * @param suppliedUserName     the username entered by the user
     * @param suppliedPlainPassword the plain-text password entered by the user
     */
    public boolean login(String suppliedUserName, String suppliedPlainPassword) {
        // Guard against null inputs — they cannot match a stored hash
        if (suppliedUserName == null || suppliedPlainPassword == null) {
            return false;
        }

        boolean usernameMatches = this.userName.equals(suppliedUserName);
        boolean passwordMatches = this.passwordHash.equals(
                SaveManager.hashPassword(suppliedPlainPassword));

        return usernameMatches && passwordMatches;
    }

    /** Performs any session-cleanup actions needed when the user signs out. */
    public void logout() {
        // Reserved for future session-state cleanup
    }

    // ── Setters with validation ───────────────────────────────────────────────

    /**
     * Updates the user's email address.
     *
     * @param newEmail the new email; stored as empty string if null
     */
    public void setUserEmail(String newEmail) {
        if (newEmail != null) {
            this.userEmail = newEmail;
        } else {
            this.userEmail = "";
        }
    }

    /**
     * Replaces the stored password hash with a hash of the supplied plain-text
     * password. The plain-text value is never retained.
     *
     * @param newPlainPassword the new plain-text password to hash and store
     */
    public void setPassword(String newPlainPassword) {
        if (newPlainPassword != null) {
            this.passwordHash = SaveManager.hashPassword(newPlainPassword);
        } else {
            this.passwordHash = SaveManager.hashPassword("");
        }
    }

    /** Updates the user's optional profile email. Alias for setUserEmail(). */
    public void updateProfile(String newEmail) {
        setUserEmail(newEmail);
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public int getUserID() {
        return userID;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    /**
     * Returns the SHA-256 hex digest of the user's password.
     * Used by SaveManager when persisting user records to disk.
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    public UserType getUserType() {
        return userType;
    }

    public Date getDateJoined() {
        return dateJoined;
    }

    public Progress getProgress() {
        return progress;
    }
}
