package com.asg.spindleserp.security.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.security.dto.UserDTO;
import com.asg.spindleserp.security.entity.User;

import java.util.List;
import java.util.Optional;

/**
 * Service contract for User management.
 *
 * DataTableResponse uses the project's existing
 * com.asg.spindleserp.common.dto.DataTableResponse (non-generic).
 */
public interface UserService {

    // ── CRUD ─────────────────────────────────────────────────────────────────

    UserDTO createUser(UserDTO dto);

    Optional<UserDTO> findById(Long id);

    UserDTO updateUser(Long id, UserDTO dto);

    /** Soft-delete — sets deleted = true, never physically removes. */
    void deleteUser(Long id);

    UserDTO toggleStatus(Long id);

    UserDTO changePassword(Long id, String newPassword);

    // ── Lists ─────────────────────────────────────────────────────────────────

    List<UserDTO> findAll();

    List<UserDTO> findEnabled();

    List<UserDTO> search(String query);

    // ── DataTable ─────────────────────────────────────────────────────────────

    DataTableResponse datatableList(int draw, int start, int length, String search);

    // ── Auth helpers ──────────────────────────────────────────────────────────

    void recordLogin(String username);

    // ── Conversion ────────────────────────────────────────────────────────────

    UserDTO toDTO(User user);

    User toEntity(UserDTO dto);
}
