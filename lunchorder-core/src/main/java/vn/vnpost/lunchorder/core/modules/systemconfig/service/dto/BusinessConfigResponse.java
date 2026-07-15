package vn.vnpost.lunchorder.core.modules.systemconfig.service.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Read-only subset of {@code system_config} exposed to every authenticated
 * user (not just admins) so client-side cutoff/lock messaging stays in sync
 * with what the admin configured, instead of being hardcoded in the frontend.
 */
@Getter
@Setter
public class BusinessConfigResponse {
    private String cutOffTime;
    private String ticketLockTime;
}
