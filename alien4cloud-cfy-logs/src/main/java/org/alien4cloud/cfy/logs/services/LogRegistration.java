package org.alien4cloud.cfy.logs.services;

import org.alien4cloud.cfy.logs.services.LogQueue;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A registration entry.
 */
@Getter
@Setter
@AllArgsConstructor
public class LogRegistration {
    private String id;
    private long expirationDate;
    /** The log service instance associated with this registration. */
    private LogQueue logQueue;
}